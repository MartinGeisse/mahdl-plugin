/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import name.martingeisse.mahdl.plugin.MahdlFileType;
import name.martingeisse.mahdl.plugin.MahdlSourceFile;
import name.martingeisse.mahdl.plugin.input.ReferenceResolutionException;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.definition.*;
import name.martingeisse.mahdl.plugin.processor.definition.PortConnection;
import name.martingeisse.mahdl.plugin.processor.definition.PortDirection;
import name.martingeisse.mahdl.plugin.processor.expression.ExpressionProcessor;
import name.martingeisse.mahdl.plugin.processor.expression.ExpressionProcessorImpl;
import name.martingeisse.mahdl.plugin.processor.statement.ProcessedDoBlock;
import name.martingeisse.mahdl.plugin.processor.statement.StatementProcessor;
import name.martingeisse.mahdl.plugin.processor.type.DataTypeProcessor;
import name.martingeisse.mahdl.plugin.processor.type.DataTypeProcessorImpl;
import name.martingeisse.mahdl.plugin.util.UserMessageException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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
	private final ErrorHandler errorHandler;

	private DataTypeProcessor dataTypeProcessor;
	private ExpressionProcessor expressionProcessor;
	private DefinitionProcessor definitionProcessor;

	private AssignmentValidator assignmentValidator;
	private StatementProcessor statementProcessor;
	private List<ProcessedDoBlock> processedDoBlocks;

	public ModuleProcessor(@NotNull Module module, @NotNull ErrorHandler errorHandler) {
		this.module = module;
		this.errorHandler = errorHandler;
	}

	@NotNull
	private Map<String, Named> getDefinitions() {
		return definitionProcessor.getDefinitions();
	}

	public ModuleDefinition process() {

		// make sure the module name matches the file name and sits in the right folder
		validateModuleNameAgainstFilePath();

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
			if (definition instanceof SignalLike && !(definition instanceof Constant)) {
				definition.processExpressions(expressionProcessor);
			}
		}

		// this object detects duplicate or missing assignments
		assignmentValidator = new AssignmentValidator(errorHandler);

		// process named definitions
		for (Named item : getDefinitions().values()) {
			processDefinition(item);
			assignmentValidator.finishSection();
		}

		// process do-blocks
		statementProcessor = new StatementProcessor(errorHandler, expressionProcessor, assignmentValidator);
		processedDoBlocks = new ArrayList<>();
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			// We collect all newly assigned signals in a separate set and add them at the end of the current do-block
			// because assigning to a signal multiple times within the same do-block is allowed. Note that the call
			// to the AssignmentValidator is done by the StatementProcessor, so we don't have to call it here.
			if (implementationItem instanceof ImplementationItem_DoBlock) {
				processedDoBlocks.add(statementProcessor.process((ImplementationItem_DoBlock) implementationItem));
			}
			assignmentValidator.finishSection();
		}

		// now check that all ports and signals without initializer have been assigned to
		assignmentValidator.checkMissingAssignments(getDefinitions().values());

		return new ModuleDefinition(module.getModuleName().getText(), ImmutableMap.copyOf(getDefinitions()), ImmutableList.copyOf(processedDoBlocks));
	}

	private void validateModuleNameAgainstFilePath() {
		QualifiedModuleName name = module.getModuleName();
		String canonicalName = PsiUtil.canonicalizeQualifiedModuleName(name);
		Module moduleForName;
		VirtualFile fileForName;
		try {
			moduleForName = PsiUtil.resolveModuleName(name);
			fileForName = PsiUtil.resolveModuleNameToVirtualFile(name);
		} catch (ReferenceResolutionException e) {
			errorHandler.onError(name, "module name '" + canonicalName + "' should refer to this file -- " + e.getMessage());
			return;
		}
		if (moduleForName != module) {
			errorHandler.onError(name, "module name '" + canonicalName + "' refers to different file " + fileForName.getPath());
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

	private void processDefinition(@NotNull Named item) {
		if (item instanceof SignalLike) {

			// Inconsistencies in the initializer vs. other assignments:
			// - ports cannot have an initializer
			// - constants cannot be assigned to other than the initializer (the assignment validator ensures that
			//   already while checking expressions)
			// - signals must be checked here
			// - for registers, the initializer does not conflict with other assignments
			SignalLike signalLike = (SignalLike) item;
			if (signalLike instanceof Signal && signalLike.getInitializer() != null) {
				assignmentValidator.considerAssignedTo(signalLike, signalLike.getInitializer());
			}

		} else if (item instanceof ModuleInstance) {

			// process port assignments
			ModuleInstance moduleInstance = (ModuleInstance) item;
			for (PortConnection portConnection : moduleInstance.getPortConnections().values()) {
				if (portConnection.getPort().getDirection() == PortDirection.IN) {
					assignmentValidator.validateAssignmentToInstancePort(moduleInstance,
						portConnection.getPort(), portConnection.getPortNameElement());
				} else {
					assignmentValidator.validateAssignmentTo(portConnection.getProcessedExpression(), AssignmentValidator.TriggerKind.CONTINUOUS);
				}
			}

		}
	}

}
