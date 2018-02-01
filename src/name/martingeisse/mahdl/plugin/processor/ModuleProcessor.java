/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.MahdlFileType;
import name.martingeisse.mahdl.plugin.MahdlSourceFile;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantExpressionEvaluator;
import name.martingeisse.mahdl.plugin.processor.expression.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.definition.*;
import name.martingeisse.mahdl.plugin.processor.expression.ExpressionProcessor;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;
import name.martingeisse.mahdl.plugin.processor.expression_old.ExpressionTypeChecker;
import name.martingeisse.mahdl.plugin.processor.type.DataTypeProcessor;
import name.martingeisse.mahdl.plugin.processor.type.FormallyConstantExpressionEvaluator;
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

	private FormallyConstantExpressionEvaluator formallyConstantExpressionEvaluator;
	private DataTypeProcessor dataTypeProcessor;
	private ModuleAnalyzer moduleAnalyzer;
	private ExpressionProcessor expressionProcessor;

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
		return moduleAnalyzer.getDefinitions();
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
		// a mutual dependency between the type system, constant evaluation and expression processing.
		formallyConstantExpressionEvaluator = new FormallyConstantExpressionEvaluator() {

			@NotNull
			@Override
			public ConstantValue evaluate(@NotNull ExtendedExpression expression) {
				// TODO what about annotations that would be placed in foreign files?
				return expressionProcessor.process(expression).evaluateFormallyConstant(
					new ProcessedExpression.FormallyConstantEvaluationContext(errorHandler));
			}

			@NotNull
			@Override
			public ConstantValue evaluate(@NotNull Expression expression) {
				// TODO what about annotations that would be placed in foreign files?
				return expressionProcessor.process(expression).evaluateFormallyConstant(
					new ProcessedExpression.FormallyConstantEvaluationContext(errorHandler));
			}

		};
		dataTypeProcessor = new DataTypeProcessor(errorHandler, formallyConstantExpressionEvaluator);
		moduleAnalyzer = new ModuleAnalyzer(errorHandler, dataTypeProcessor, formallyConstantExpressionEvaluator, module);
		expressionProcessor = new ExpressionProcessor(errorHandler, getDefinitions()::get, dataTypeProcessor);

		// Process defined constants first. These are mutually order-sensitive, but order-insensitive with respect to
		// other definitions.
		moduleAnalyzer.analyzeAndEvaluateConstants();

		// next, analyze other items, but don't evaluate anything yet
		moduleAnalyzer.analyzeNonConstants();

		//
		// TODO





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

	private void processDefinition(@NotNull Named item) {
		// constants have been handled by the constant evaluator already
		if (!(item instanceof Constant)) {
			if (item instanceof SignalLike) {
				SignalLike signalLike = (SignalLike) item;
				ProcessedDataType.Family dataTypeFamily = signalLike.getProcessedDataType().getFamily();
				if (dataTypeFamily == ProcessedDataType.Family.UNKNOWN) {
					// don't complain about type errors if we don't even know the type -- in that case,
					// the reason why we don't know the type already appears as an error
					return;
				}
				allowedDataTypeCheck:
				{
					String usage;
					boolean valid;
					if (item instanceof Port) {
						usage = "ports";
						valid = dataTypeFamily == ProcessedDataType.Family.BIT || dataTypeFamily == ProcessedDataType.Family.VECTOR;
					} else if (item instanceof Signal || item instanceof Register) {
						usage = "signals";
						valid = dataTypeFamily == ProcessedDataType.Family.BIT || dataTypeFamily == ProcessedDataType.Family.VECTOR || dataTypeFamily == ProcessedDataType.Family.MEMORY;
					} else {
						break allowedDataTypeCheck;
					}
					if (!valid) {
						errorHandler.onError(signalLike.getDataTypeElement(), dataTypeFamily.getDisplayString() + " type not allowed for " + usage);
					}
				}
				if (signalLike.getInitializer() != null) {
					// TODO: if the initializer is constant, we should try to convert that value to the signalLike's type.
					// Converting a constant value can handle a broader range of types than a run-time conversion.
					// For example, a constant integer can be assigned to a vector signalLike if the integer fits into
					// the vector size, but at runtime this is forbidden because integers are.
					// TODO use tryEvaluateConstantExpression() for that
//					ProcessedDataType initializerDataType = expressionTypeChecker.check(signalLike.getInitializer());
//					checkRuntimeAssignment(signalLike.getInitializer(), signalLike.getProcessedDataType(), initializerDataType);
				}
			} else if (item instanceof ModuleInstance) {
				ModuleInstance moduleInstance = (ModuleInstance) item;
				String instanceName = moduleInstance.getName();
				ImplementationItem_ModuleInstance moduleInstanceElement = moduleInstance.getModuleInstanceElement();
				PsiElement untypedResolvedModule = moduleInstanceElement.getModuleName().getReference().resolve();
				Module resolvedModule;
				if (untypedResolvedModule instanceof Module) {
					resolvedModule = (Module) untypedResolvedModule;
				} else {
					errorHandler.onError(moduleInstanceElement.getModuleName(), "unknown module: '" + moduleInstanceElement.getModuleName().getReference().getCanonicalText() + "'");
					resolvedModule = null;
				}
				Map<String, PortDefinitionGroup> portNameToDefinitionGroup = new HashMap<>();
				for (PortDefinitionGroup portDefinitionGroup : resolvedModule.getPortDefinitionGroups().getAll()) {
					for (PortDefinition portDefinition : portDefinitionGroup.getDefinitions().getAll()) {
						String portName = portDefinition.getName();
						if (portName != null) {
							portNameToDefinitionGroup.put(portName, portDefinitionGroup);
						}
					}
				}
				for (PortConnection portConnection : moduleInstanceElement.getPortConnections().getAll()) {
					String portName = portConnection.getPortName().getIdentifier().getText();
					inconsistentAssignmentDetector.handleAssignedToInstancePort(instanceName, portName, portConnection.getPortName());
					PortDefinitionGroup portDefinitionGroup = portNameToDefinitionGroup.get(portName);
					if (portDefinitionGroup == null) {
						errorHandler.onError(portConnection.getPortName(), "unknown port '" + portName + "' in module '" + moduleInstanceElement.getModuleName().getReference().getCanonicalText());
					} else {
						// TODO what happens if this detects an undefined type in the port? We can't place annotations in another file, right?
						ProcessedDataType portType = dataTypeProcessor.processDataType(portDefinitionGroup.getDataType());
						Expression expression = portConnection.getExpression();
						ProcessedDataType expressionType = expressionTypeChecker.checkExpression(expression);
						if (portDefinitionGroup.getDirection() instanceof PortDirection_In) {
//							checkRuntimeAssignment(expression, portType, expressionType);
						} else if (portDefinitionGroup.getDirection() instanceof PortDirection_Out) {
//							checkRuntimeAssignment(expression, expressionType, portType);
//							checkLValue(expression, true, false);
						}
					}
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
		ProcessedDataType conditionType = expressionTypeChecker.checkExpression(condition);
		if (conditionType.getFamily() != ProcessedDataType.Family.BIT) {
			errorHandler.onError(condition, "cannot use an expression of type " + conditionType + " as condition");
		}
		processStatement(thenBranch);
		if (elseBranch != null) {
			processStatement(elseBranch);
		}
	}


}
