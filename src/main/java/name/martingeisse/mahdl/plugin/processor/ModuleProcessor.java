/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.ReferenceResolutionException;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.definition.*;
import name.martingeisse.mahdl.plugin.processor.definition.PortDirection;
import name.martingeisse.mahdl.plugin.processor.expression.ExpressionProcessor;
import name.martingeisse.mahdl.plugin.processor.expression.ExpressionProcessorImpl;
import name.martingeisse.mahdl.plugin.processor.statement.ProcessedDoBlock;
import name.martingeisse.mahdl.plugin.processor.statement.StatementProcessor;
import name.martingeisse.mahdl.plugin.processor.type.DataTypeProcessor;
import name.martingeisse.mahdl.plugin.processor.type.DataTypeProcessorImpl;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * This class handles the common logic between error annotations, code generation etc., and provides a unified framework
 * for the individual steps such as constant evaluation, type checking, name resolution, and so on.
 * <p>
 * It emerged from the observation that even a simple task such as annotating the code with error markers is similar to
 * compiling it, in that information about the code must be collected in multiple interdependent steps. Without a
 * central framework for these steps, a lot of code gets duplicated between them.
 * <p>
 * For example, it is not possible to check type correctness without evaluating constants, becuase constants are used to
 * specify array sizes. Type correctness is needed to evaluate constants though. Both of these steps can run into the
 * same errors in various sub-steps, so they would take an annotation holder to report these errors -- but there would
 * need to be an agreement which step reports which errors. And so on.
 */
public final class ModuleProcessor {

	private final Module module;
	private final String canonicalModuleName;
	private final ErrorHandler errorHandler;

	private DataTypeProcessor dataTypeProcessor;
	private ExpressionProcessor expressionProcessor;
	private DefinitionProcessor definitionProcessor;

	private AssignmentValidator assignmentValidator;
	private StatementProcessor statementProcessor;
	private List<ProcessedDoBlock> processedDoBlocks;

	public ModuleProcessor(@NotNull Module module, @NotNull ErrorHandler errorHandler) {
		this.module = module;
		this.canonicalModuleName = PsiUtil.canonicalizeQualifiedModuleName(module.getModuleName());
		this.errorHandler = errorHandler;
	}

	@NotNull
	private Map<String, Named> getDefinitions() {
		return definitionProcessor.getDefinitions();
	}

	public ModuleDefinition process() {

		// make sure the module name matches the file name and sits in the right folder
		validateModuleNameAgainstFilePath();

		// validate nativeness (but still continue even if violated, since the keyword may be misplaced)
		boolean isNative = module.getNativeness().getIt() != null;
		if (isNative) {
			ImmutableList<ImplementationItem> implementationItems = module.getImplementationItems().getAll();
			if (!implementationItems.isEmpty()) {
				errorHandler.onError(implementationItems.get(0), "native module cannot contain implementation items");
			}
		}

		// Create helper objects. These objects work together, especially during constant definition analysis, due to
		// a mutual dependency between the type system, constant evaluation and expression processing. Note the
		// LocalDefinitionResolver parameter to the ExpressionProcessorImpl calling getDefinitions() on the fly,
		// not in advance, to break the dependency cycle.
		expressionProcessor = new ExpressionProcessorImpl(errorHandler, name -> getDefinitions().get(name));
		dataTypeProcessor = new DataTypeProcessorImpl(errorHandler, expressionProcessor);
		definitionProcessor = new DefinitionProcessor(errorHandler, dataTypeProcessor, expressionProcessor);

		// process module definitions
		definitionProcessor.processPorts(module.getPortDefinitionGroups());
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			if (isConstant(implementationItem)) {
				definitionProcessor.process(implementationItem);
			}
		}
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			if (!isConstant(implementationItem)) {
				definitionProcessor.process(implementationItem);
			}
		}
		for (Named definition : getDefinitions().values()) {
			if (!(definition instanceof Constant)) {
				definition.processExpressions(expressionProcessor);
			}
		}

		// Process do-blocks and check for missing / duplicate assignments. Do so in the original file's order so when
		// an error message could in principle appear in one of multiple places, it appears in the topmost place.
		assignmentValidator = new AssignmentValidator(errorHandler);
		List<Pair<Runnable, PsiElement>> runnables = new ArrayList<>();
		for (Named item : getDefinitions().values()) {
			// Inconsistencies regarding signal-likes in the initializer vs. other assignments:
			// - ports cannot have an initializer
			// - constants cannot be assigned to other than the initializer (the assignment validator ensures that
			//   already while checking expressions)
			// - signals must be checked here
			// - for registers, the initializer does not conflict with other assignments
			if (item instanceof Signal) {
				Signal signal = (Signal)item;
				if (signal.getInitializer() != null) {
					runnables.add(Pair.of(() -> {
						assignmentValidator.considerAssignedTo(signal, signal.getNameElement());
						assignmentValidator.finishSection();
					}, signal.getNameElement()));
				}
			}
		}
		processedDoBlocks = new ArrayList<>();
		statementProcessor = new StatementProcessor(errorHandler, expressionProcessor, assignmentValidator);
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			runnables.add(Pair.of(() -> {
				// We collect all newly assigned signals in a separate set and add them at the end of the current do-block
				// because assigning to a signal multiple times within the same do-block is allowed. Note that the
				// per-assignment call to the AssignmentValidator is done by the StatementProcessor, so we don't have
				// to call it here.
				if (implementationItem instanceof ImplementationItem_DoBlock) {
					processedDoBlocks.add(statementProcessor.process((ImplementationItem_DoBlock) implementationItem));
				}
				assignmentValidator.finishSection();
			}, implementationItem));
		}
		runnables.sort(Comparator.comparing(pair -> pair.getRight().getTextRange().getStartOffset()));
		for (Pair<Runnable, PsiElement> pair : runnables) {
			pair.getLeft().run();
		}

		// now check that all ports and signals without initializer have been assigned to
		assignmentValidator.checkMissingAssignments(getDefinitions().values());

		return new ModuleDefinition(isNative, canonicalModuleName, ImmutableMap.copyOf(getDefinitions()), ImmutableList.copyOf(processedDoBlocks));
	}

	private void validateModuleNameAgainstFilePath() {
		QualifiedModuleName name = module.getModuleName();
		Module moduleForName;
		try {
			moduleForName = PsiUtil.resolveModuleName(name, PsiUtil.ModuleNameResolutionUseCase.NAME_DECLARATION_VALIDATION);
		} catch (ReferenceResolutionException e) {
			errorHandler.onError(name, e.getMessage());
			return;
		}
		if (moduleForName != module) {
			VirtualFile fileForName = PsiUtil.getVirtualFile(moduleForName);
			String path = (fileForName == null ? "(null)" : fileForName.getPath());
			errorHandler.onError(name, "module name '" + canonicalModuleName + "' refers to different file " + path);
		}
	}

	private boolean isConstant(ImplementationItem item) {
		if (item instanceof ImplementationItem_SignalLikeDefinitionGroup) {
			SignalLikeKind kind = ((ImplementationItem_SignalLikeDefinitionGroup) item).getKind();
			return kind instanceof SignalLikeKind_Constant;
		} else {
			return false;
		}
	}

}
