package name.martingeisse.mahdl.plugin.processor;

import com.intellij.extapi.psi.ASTDelegatePsiElement;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.MahdlFileType;
import name.martingeisse.mahdl.plugin.MahdlSourceFile;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantExpressionEvaluator;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.definition.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

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
public abstract class ModuleProcessor {

	private static final BigInteger MAX_SIZE_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);

	private final Module module;

	private ConstantExpressionEvaluator constantExpressionEvaluator;
	private ConstantExpressionEvaluator exceptionThrowingConstantExpressionEvaluator;
	private Map<String, ConstantValue> constants;
	private ModuleAnalyzer moduleAnalyzer;
	private Map<String, Named> definitions;
	private ExpressionTypeChecker expressionTypeChecker;
	private InconsistentAssignmentDetector inconsistentAssignmentDetector;

	public ModuleProcessor(Module module) {
		this.module = module;
	}

	public Module getModule() {
		return module;
	}

	public Map<String, ConstantValue> getConstants() {
		return constants;
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
					onError(module.getModuleName(), "module '" + moduleName + "' should be defined in a file named '" + expectedFileName + "'");
				}
				break;
			}
		}

		// evaluate constants
		constantExpressionEvaluator = new ConstantExpressionEvaluator(module) {

			@Override
			protected void onError(PsiElement errorSource, String message) {
				ModuleProcessor.this.onError(errorSource, message);
			}

			@Override
			protected ProcessedDataType processDataType(DataType dataType) {
				return ModuleProcessor.this.processDataType(dataType);
			}

		};
		constantExpressionEvaluator.processConstantDefinitions();
		constants = constantExpressionEvaluator.getDefinedConstants();
		exceptionThrowingConstantExpressionEvaluator = new ConstantExpressionEvaluator() {

			@Override
			protected void onError(PsiElement errorSource, String message) {
				throw new ConstantEvaluationException();
			}

			@Override
			protected ProcessedDataType processDataType(DataType dataType) {
				return ModuleProcessor.this.processDataType(dataType);
			}

		};

		// collect definitions
		moduleAnalyzer = new ModuleAnalyzer(module) {

			@Override
			protected void onError(PsiElement errorSource, String message) {
				ModuleProcessor.this.onError(errorSource, message);
			}

			@Override
			protected ProcessedDataType processDataType(DataType dataType) {
				return ModuleProcessor.this.processDataType(dataType);
			}

		};
		moduleAnalyzer.registerConstants(constants);
		moduleAnalyzer.analyzeNonConstants();
		definitions = moduleAnalyzer.getDefinitions();

		// this object checks for type errors in expressions and determines their result type
		expressionTypeChecker = new ExpressionTypeChecker(definitions);

		// this object detects duplicate or missing assignments
		inconsistentAssignmentDetector = new InconsistentAssignmentDetector() {

			@Override
			protected void onError(PsiElement errorSource, String message) {
				ModuleProcessor.this.onError(errorSource, message);
			}

		};

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
					onError(signalLike.getDataTypeElement(), dataTypeFamily.getDisplayString() + " type not allowed for " + usage);
				}
			}
			if (signalLike.getInitializer() != null) {
				// TODO: if the initializer is constant, we should try to convert that value to the signalLike's type.
				// Converting a constant value can handle a broader range of types than a run-time conversion.
				// For example, a constant integer can be assigned to a vector signalLike if the integer fits into
				// the vector size, but at runtime this is forbidden because integers are.
				// TODO use tryEvaluateConstantExpression() for that
				ProcessedDataType initializerDataType = expressionTypeChecker.check(signalLike.getInitializer());
				if (!signalLike.getProcessedDataType().canConvertRuntimeValueOfTypeImplicitly(initializerDataType)) {
					onError(signalLike.getInitializer(), "cannot assign value of type " + initializerDataType +
						" to a signal or register of type " + signalLike.getProcessedDataType());
				}
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
				onError(moduleInstanceElement.getModuleName(), "unknown module: '" + moduleInstanceElement.getModuleName().getReference().getCanonicalText() + "'");
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
					onError(portConnection.getPortName(), "unknown port '" + portName + "' in module '" + moduleInstanceElement.getModuleName().getReference().getCanonicalText());
				} else {
					Expression expression = portConnection.getExpression();
					// TODO check type
				}
			}
		}
	}

	private void processDoBlock(ImplementationItem_DoBlock doBlock) {
		DoBlockTrigger trigger = doBlock.getTrigger();
		if (trigger instanceof DoBlockTrigger_Clocked) {
			Expression clockExpression = ((DoBlockTrigger_Clocked)trigger).getClockExpression();
			// TODO check
		}
		processStatement(doBlock.getStatement());
	}

	private void processStatement(Statement statement) {
		if (statement instanceof Statement_Assignment) {
			Statement_Assignment assignment = (Statement_Assignment)statement;
			// TODO assignment.getLeftSide();
			// TODO assignment.getRightSide();
			inconsistentAssignmentDetector.handleAssignment(assignment);
		} else if (statement instanceof Statement_Block) {
			Statement_Block block = (Statement_Block)statement;
			for (Statement subStatement : block.getBody().getAll()) {
				processStatement(subStatement);
			}
		} else if (statement instanceof Statement_IfThen) {
			// TODO
		} else if (statement instanceof Statement_IfThenElse) {
			// TODO
		} else if (statement instanceof Statement_Switch) {
			// TODO
		} else if (statement instanceof Statement_Break) {
			// TODO
		}
	}

	private ProcessedDataType processDataType(DataType dataType) {
		if (dataType instanceof DataType_Bit) {
			return ProcessedDataType.Bit.INSTANCE;
		} else if (dataType instanceof DataType_Vector) {
			DataType_Vector vector = (DataType_Vector) dataType;
			int size = processConstantSizeExpression(vector.getSize());
			return size < 0 ? ProcessedDataType.Unknown.INSTANCE : new ProcessedDataType.Vector(size);
		} else if (dataType instanceof DataType_Memory) {
			DataType_Memory memory = (DataType_Memory) dataType;
			int firstSize = processConstantSizeExpression(memory.getFirstSize());
			int secondSize = processConstantSizeExpression(memory.getSecondSize());
			return (firstSize < 0 || secondSize < 0) ? ProcessedDataType.Unknown.INSTANCE : new ProcessedDataType.Memory(firstSize, secondSize);
		} else if (dataType instanceof DataType_Integer) {
			return ProcessedDataType.Integer.INSTANCE;
		} else if (dataType instanceof DataType_Text) {
			return ProcessedDataType.Text.INSTANCE;
		} else {
			onError(dataType, "unknown data type");
			return ProcessedDataType.Unknown.INSTANCE;
		}
	}

	private int processConstantSizeExpression(Expression expression) {
		ConstantValue value = constantExpressionEvaluator.evaluate(expression);
		if (value.getDataTypeFamily() == ProcessedDataType.Family.UNKNOWN) {
			return -1;
		}
		BigInteger integerValue = value.convertToInteger();
		if (integerValue == null) {
			onError(expression, "cannot convert " + value + " to integer");
			return -1;
		}
		if (integerValue.compareTo(MAX_SIZE_VALUE) > 0) {
			onError(expression, "size too large: " + integerValue);
			return -1;
		}
		int intValue = integerValue.intValue();
		if (intValue < 0) {
			onError(expression, "size cannot be negative: " + integerValue);
			return -1;
		}
		return intValue;
	}

	protected abstract void onError(PsiElement errorSource, String message);

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
