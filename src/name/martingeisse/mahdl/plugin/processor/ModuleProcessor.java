/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.MahdlFileType;
import name.martingeisse.mahdl.plugin.MahdlSourceFile;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.definition.*;
import name.martingeisse.mahdl.plugin.processor.expression.ExpressionProcessor;
import name.martingeisse.mahdl.plugin.processor.expression.ExpressionProcessorImpl;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;
import name.martingeisse.mahdl.plugin.processor.type.DataTypeProcessor;
import name.martingeisse.mahdl.plugin.processor.type.DataTypeProcessorImpl;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
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
	private DataTypeProcessor actualDataTypeProcessor;
	private ExpressionProcessor expressionProcessor;
	private DefinitionProcessor definitionProcessor;

	private InconsistentAssignmentDetector inconsistentAssignmentDetector;
//	private RuntimeAssignmentChecker runtimeAssignmentChecker;

	public ModuleProcessor(@NotNull Module module, @NotNull ErrorHandler errorHandler) {
		this.module = module;
		this.errorHandler = errorHandler;
	}

	@NotNull
	public Module getModule() {
		return module;
	}

	@NotNull
	public Map<String, Named> getDefinitions() {
		return definitionProcessor.getDefinitions();
	}

	public void process() {

		// make sure the module name matches the file name
		{
			MahdlSourceFile mahdlSourceFile = PsiUtil.getAncestor(module, MahdlSourceFile.class);
			if (mahdlSourceFile != null) {
				String moduleName = module.getModuleName().getText();
				String expectedFileName = moduleName + '.' + MahdlFileType.DEFAULT_EXTENSION;
				String actualFileName = mahdlSourceFile.getName();
				if (!actualFileName.equals(expectedFileName)) {
					errorHandler.onError(module.getModuleName(), "module '" + moduleName + "' should be defined in a file named '" + expectedFileName + "'");
				}
			}
		}

		// Create helper objects. These objects work together, especially during constant definition analysis, due to
		// a mutual dependency between the type system, constant evaluation and expression processing. Note the
		// distinction between dataTypeProcessor and actualDataTypeProcessor used to break the dependency cycle.
		dataTypeProcessor = t -> actualDataTypeProcessor.processDataType(t);
		expressionProcessor = new ExpressionProcessorImpl(errorHandler, name -> getDefinitions().get(name), dataTypeProcessor);
		actualDataTypeProcessor = new DataTypeProcessorImpl(errorHandler, expressionProcessor);
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
				((SignalLike) definition).processInitializer(expressionProcessor);
			}
		}


		// --- TODO ---


		// this object detects duplicate or missing assignments
		inconsistentAssignmentDetector = new InconsistentAssignmentDetector(errorHandler);

		// this object checks run-time assignments and detects type errors and assignment to non-L-values
		runtimeAssignmentChecker = new RuntimeAssignmentChecker(errorHandler, expressionTypeChecker, definitions);

		// process named definitions
		for (Named item : getDefinitions().values()) {
			processDefinition(item);
			inconsistentAssignmentDetector.finishSection();
		}

		// process do-blocks
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			// we collect all newly assigned signals in a separate set and add them at the end of the current do-block
			// because assigning to a signal multiple times within the same do-block is allowed
			if (implementationItem instanceof ImplementationItem_DoBlock) {
				processDoBlock((ImplementationItem_DoBlock) implementationItem);
			}
			inconsistentAssignmentDetector.finishSection();
		}

		// now check that all ports and signals without initializer have been assigned to
		inconsistentAssignmentDetector.checkMissingAssignments(getDefinitions().values());

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

		// TODO fold constant sub-expressions.
		// This implicitly also removes all usages of defined constants, so any later processing stage doesn't have to
		// deal with them anymore (in type specifiers, they are already gone
		// due to the way ProcessedDataType works).

		if (item instanceof SignalLike) {
			SignalLike signalLike = (SignalLike) item;
			ProcessedDataType.Family dataTypeFamily = signalLike.getProcessedDataType().getFamily();
			if (item instanceof Port) {
				if (dataTypeFamily != ProcessedDataType.Family.UNKNOWN &&
					dataTypeFamily != ProcessedDataType.Family.BIT &&
					dataTypeFamily != ProcessedDataType.Family.VECTOR) {
					errorHandler.onError(signalLike.getDataTypeElement(), dataTypeFamily.getDisplayString() + " type not allowed for ports");
				}
			} else if (item instanceof Signal || item instanceof Register) {
				if (dataTypeFamily != ProcessedDataType.Family.UNKNOWN &&
					dataTypeFamily != ProcessedDataType.Family.BIT &&
					dataTypeFamily != ProcessedDataType.Family.VECTOR &&
					dataTypeFamily != ProcessedDataType.Family.MEMORY) {
					errorHandler.onError(signalLike.getDataTypeElement(), dataTypeFamily.getDisplayString() + " type not allowed for signals and registers");
				}
			}
		} else if (item instanceof ModuleInstance) {
			ModuleInstance moduleInstance = (ModuleInstance) item;
			ImplementationItem_ModuleInstance moduleInstanceElement = moduleInstance.getModuleInstanceElement();

			// resolve the module definition
			Module resolvedModule;
			{
				PsiElement untypedResolvedModule = moduleInstanceElement.getModuleName().getReference().resolve();
				if (untypedResolvedModule instanceof Module) {
					resolvedModule = (Module) untypedResolvedModule;
				} else {
					errorHandler.onError(moduleInstanceElement.getModuleName(), "unknown module: '" + moduleInstanceElement.getModuleName().getReference().getCanonicalText() + "'");
					return;
				}
			}

			// build a map of the ports from the module definition
			Map<String, PortDefinitionGroup> portNameToDefinitionGroup = new HashMap<>();
			for (PortDefinitionGroup portDefinitionGroup : resolvedModule.getPortDefinitionGroups().getAll()) {
				for (PortDefinition portDefinition : portDefinitionGroup.getDefinitions().getAll()) {
					String portName = portDefinition.getName();
					if (portName != null) {
						portNameToDefinitionGroup.put(portName, portDefinitionGroup);
					}
				}
			}

			// process port assignments
			for (PortConnection portConnection : moduleInstanceElement.getPortConnections().getAll()) {
				String portName = portConnection.getPortName().getIdentifier().getText();
				PortDefinitionGroup portDefinitionGroup = portNameToDefinitionGroup.get(portName);
				if (portDefinitionGroup == null) {
					errorHandler.onError(portConnection.getPortName(), "unknown port '" + portName + "' in module '" + moduleInstanceElement.getModuleName().getReference().getCanonicalText());
					continue;
				}

				// --- TODO below ---
				// TODO perform implicit type conversion
				// TODO what happens if this detects an undefined type in the port? We can't place annotations in another file, right?
				ProcessedDataType portType = dataTypeProcessor.processDataType(portDefinitionGroup.getDataType());
				Expression expression = portConnection.getExpression();
				ProcessedDataType expressionType = expressionTypeChecker.checkExpression(expression);
				if (portDefinitionGroup.getDirection() instanceof PortDirection_In) {
					inconsistentAssignmentDetector.handleAssignedToInstancePort(moduleInstance.getName(), portName, portConnection.getPortName());
//							checkRuntimeAssignment(expression, portType, expressionType);
				} else if (portDefinitionGroup.getDirection() instanceof PortDirection_Out) {
//							checkRuntimeAssignment(expression, expressionType, portType);
//							checkLValue(expression, true, false);
				}

			}

		}
	}

	private void processDoBlock(@NotNull ImplementationItem_DoBlock doBlock) {
		DoBlockTrigger trigger = doBlock.getTrigger();
		if (trigger instanceof DoBlockTrigger_Clocked) {
			Expression clockExpression = ((DoBlockTrigger_Clocked) trigger).getClockExpression();
			ProcessedDataType clockExpressionType = expressionTypeChecker.checkExpression(clockExpression);
			if (clockExpressionType.getFamily() != ProcessedDataType.Family.BIT) {
				errorHandler.onError(clockExpression, "cannot use an expression of type " + clockExpressionType + " as clock");
			}
		}
		processStatement(doBlock.getStatement());
	}

	private void processStatement(@NotNull Statement statement) {
		if (statement instanceof Statement_Assignment) {
			Statement_Assignment assignment = (Statement_Assignment) statement;
			inconsistentAssignmentDetector.handleAssignment(assignment);
			ProcessedDataType leftType = expressionTypeChecker.checkExpression(assignment.getLeftSide());
			ProcessedDataType rightType = expressionTypeChecker.checkExpression(assignment.getRightSide());
//			checkRuntimeAssignment(assignment.getRightSide(), leftType, rightType);
			// TODO assign to signal / register in comb / clocked context
//			checkLValue(assignment.getLeftSide(), true, true);
		} else if (statement instanceof Statement_Block) {
			Statement_Block block = (Statement_Block) statement;
			for (Statement subStatement : block.getBody().getAll()) {
				processStatement(subStatement);
			}
		} else if (statement instanceof Statement_IfThen) {
			Statement_IfThen ifThenStatement = (Statement_IfThen) statement;
			processIfStatement(ifThenStatement.getCondition(), ifThenStatement.getThenBranch(), null);
		} else if (statement instanceof Statement_IfThenElse) {
			Statement_IfThenElse ifThenElseStatement = (Statement_IfThenElse) statement;
			processIfStatement(ifThenElseStatement.getCondition(), ifThenElseStatement.getThenBranch(), ifThenElseStatement.getElseBranch());
		} else if (statement instanceof Statement_Switch) {
			// TODO
			throw new RuntimeException("switch statements not implemented yet");
		} else if (statement instanceof Statement_Break) {
			// TODO
			throw new RuntimeException("break statements not implemented yet");
		}
	}

	private void processIfStatement(@NotNull Expression condition, @NotNull Statement thenBranch, @Nullable Statement elseBranch) {
		ProcessedExpression processedCondition = expressionProcessor.process(condition);
		processStatement(thenBranch);
		if (elseBranch != null) {
			processStatement(elseBranch);
		}
		if (!(processedCondition.getDataType() instanceof ProcessedDataType.Unknown) && !(processedCondition.getDataType() instanceof ProcessedDataType.Bit)) {
			errorHandler.onError(condition, "condition must be of type bit");
		}
	}

}
