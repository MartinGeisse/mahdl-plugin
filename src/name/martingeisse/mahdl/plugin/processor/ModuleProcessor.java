package name.martingeisse.mahdl.plugin.processor;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.MahdlFileType;
import name.martingeisse.mahdl.plugin.MahdlSourceFile;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantExpressionEvaluator;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.definition.*;
import name.martingeisse.mahdl.plugin.processor.type.DataTypeProcessorImpl;
import name.martingeisse.mahdl.plugin.processor.type.ExpressionTypeChecker;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

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

	private ConstantExpressionEvaluator constantExpressionEvaluator;
	private DataTypeProcessorImpl dataTypeProcessor;
	private ConstantExpressionEvaluator exceptionThrowingConstantExpressionEvaluator;
	private ModuleAnalyzer moduleAnalyzer;
	private Map<String, Named> definitions;
	private ExpressionTypeChecker expressionTypeChecker;
	private InconsistentAssignmentDetector inconsistentAssignmentDetector;
	private RuntimeAssignmentChecker runtimeAssignmentChecker;

	public ModuleProcessor(Module module, ErrorHandler errorHandler) {
		this.module = module;
		this.errorHandler = errorHandler;
	}

	public Module getModule() {
		return module;
	}

	public Map<String, ConstantValue> getConstants() {
		return constantExpressionEvaluator.getDefinedConstants();
	}

	public Map<String, Named> getDefinitions() {
		return definitions;
	}

	public void process() {

		// make sure the module name matches the file name
		for (PsiElement element = module; element != null; element = element.getParent()) {
			if (element instanceof MahdlSourceFile) {
				String moduleName = module.getModuleName().getText();
				String expectedFileName = moduleName + '.' + MahdlFileType.DEFAULT_EXTENSION;
				String actualFileName = ((MahdlSourceFile) element).getName();
				if (!actualFileName.equals(expectedFileName)) {
					errorHandler.onError(module.getModuleName(), "module '" + moduleName + "' should be defined in a file named '" + expectedFileName + "'");
				}
				break;
			}
		}

		// evaluate constants. The ConstantExpressionEvaluator and the DataTypeProcessor work in lockstep here since
		// every constant definition may use the constants defined above for its data type.
		constantExpressionEvaluator = new ConstantExpressionEvaluator(errorHandler, module);
		dataTypeProcessor = new DataTypeProcessorImpl(errorHandler, constantExpressionEvaluator);
		constantExpressionEvaluator.processConstantDefinitions(dataTypeProcessor);
		exceptionThrowingConstantExpressionEvaluator = new ConstantExpressionEvaluator((errorSource, message) -> {
			throw new ConstantEvaluationException();
		}, module);

		// collect definitions
		moduleAnalyzer = new ModuleAnalyzer(errorHandler, dataTypeProcessor, module);
		moduleAnalyzer.registerConstants(constantExpressionEvaluator.getDefinedConstants());
		moduleAnalyzer.analyzeNonConstants();
		definitions = moduleAnalyzer.getDefinitions();

		// this object checks for type errors in expressions and determines their result type
		expressionTypeChecker = new ExpressionTypeChecker(errorHandler, definitions);

		// this object detects duplicate or missing assignments
		inconsistentAssignmentDetector = new InconsistentAssignmentDetector(errorHandler);

		// this object checks run-time assignments and detects type errors and assignment to non-L-values
		runtimeAssignmentChecker = new RuntimeAssignmentChecker(errorHandler, definitions);

		// process named definitions
		for (Named item : definitions.values()) {
			processDefinition(item);
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
		inconsistentAssignmentDetector.checkMissingAssignments(definitions.values());

	}

	private void processDefinition(Named item) {
		if (item instanceof Constant) {
			// constants have been handled by the constant evaluator already
			return;
		} else if (item instanceof SignalLike) {
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
				ProcessedDataType initializerDataType = expressionTypeChecker.check(signalLike.getInitializer());
				checkRuntimeAssignment(signalLike.getInitializer(), signalLike.getProcessedDataType(), initializerDataType);
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
					ProcessedDataType expressionType = expressionTypeChecker.check(expression);
					if (portDefinitionGroup.getDirection() instanceof PortDirection_In) {
						checkRuntimeAssignment(expression, portType, expressionType);
					} else if (portDefinitionGroup.getDirection() instanceof PortDirection_Out) {
						checkRuntimeAssignment(expression, expressionType, portType);
						checkLValue(expression, true, false);
					}
				}
			}
		}
	}

	private void processDoBlock(ImplementationItem_DoBlock doBlock) {
		DoBlockTrigger trigger = doBlock.getTrigger();
		if (trigger instanceof DoBlockTrigger_Clocked) {
			Expression clockExpression = ((DoBlockTrigger_Clocked) trigger).getClockExpression();
			ProcessedDataType clockExpressionType = expressionTypeChecker.check(clockExpression);
			if (clockExpressionType.getFamily() != ProcessedDataType.Family.BIT) {
				errorHandler.onError(clockExpression, "cannot use an expression of type " + clockExpressionType + " as clock");
			}
		}
		processStatement(doBlock.getStatement());
	}

	private void processStatement(Statement statement) {
		if (statement instanceof Statement_Assignment) {
			Statement_Assignment assignment = (Statement_Assignment) statement;
			inconsistentAssignmentDetector.handleAssignment(assignment);
			ProcessedDataType leftType = expressionTypeChecker.check(assignment.getLeftSide());
			ProcessedDataType rightType = expressionTypeChecker.check(assignment.getRightSide());
			checkRuntimeAssignment(assignment.getRightSide(), leftType, rightType);
			// TODO assign to signal / register in comb / clocked context
			checkLValue(assignment.getLeftSide(), true, true);
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

	private void processIfStatement(Expression condition, Statement thenBranch, Statement elseBranch) {
		ProcessedDataType conditionType = expressionTypeChecker.check(condition);
		if (conditionType.getFamily() != ProcessedDataType.Family.BIT) {
			errorHandler.onError(condition, "cannot use an expression of type " + conditionType + " as condition");
		}
		processStatement(thenBranch);
		if (elseBranch != null) {
			processStatement(elseBranch);
		}
	}

	private ConstantValue tryEvaluateConstantExpression(Expression expression) {
		try {
			return exceptionThrowingConstantExpressionEvaluator.evaluate(expression);
		} catch (ConstantEvaluationException e) {
			return null;
		}
	}

	private static class ConstantEvaluationException extends RuntimeException {
	}

}
