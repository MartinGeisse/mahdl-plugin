package name.martingeisse.mahdl.plugin.processor;

import com.intellij.extapi.psi.ASTDelegatePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.MahdlFileType;
import name.martingeisse.mahdl.plugin.MahdlSourceFile;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantExpressionEvaluator;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.definition.*;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * This class handles the common logic between error annotations, code generation etc., and provides a unified
 * framework for the individual steps such as constant evaluation, type checking, name resolution, and so on.
 *
 * It emerged from the observation that even a simple task such as annotating the code with error markers is
 * similar to compiling it, in that information about the code must be collected in multiple interdependent steps.
 * Without a central framework for these steps, a lot of code gets duplicated between them.
 *
 * For example, it is not possible to check type correctness without evaluating constants, becuase constants are
 * used to specify array sizes. Type correctness is needed to evaluate constants though. Both of these steps can
 * run into the same errors in various sub-steps, so they would take an annotation holder to report these errors --
 * but there would need to be an agreement which step reports which errors. And so on.
 */
public abstract class ModuleProcessor {

	private static final BigInteger MAX_SIZE_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);

	private final Module module;

	private ConstantExpressionEvaluator constantExpressionEvaluator;
	private Map<String, ConstantValue> constants;
	private ModuleAnalyzer moduleAnalyzer;
	private Map<String, Named> definitions;
	private Set<String> previouslyAssignedSignals;
	private Set<String> newlyAssignedSignals;

	public ModuleProcessor(Module module) {
		this.module = module;
	}

	public Module getModule() {
		return module;
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

		// process named definitions
		for (Named item : definitions.values()) {
			processDefinition(item);
		}

		// process do-blocks
		previouslyAssignedSignals = new HashSet<>();
		newlyAssignedSignals = new HashSet<>();
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			// we collect all newly assigned signals in a separate set and add them at the end of the current do-block
			// because assigning to a signal multiple times within the same do-block is allowed
			if (implementationItem instanceof ImplementationItem_DoBlock) {
				processDoBlock((ImplementationItem_DoBlock)implementationItem);
			}
			previouslyAssignedSignals.addAll(newlyAssignedSignals);
			newlyAssignedSignals.clear();
		}

		// now check that all ports and signals without initializer have been assigned to
		// TODO check assignment to instance ports
		for (Named definition : definitions.values()) {
			if (definition instanceof Port) {
				Port port = (Port)definition;
				if (port.getDirectionElement() instanceof PortDirection_Output) {
					if (port.getInitializer() == null && !previouslyAssignedSignals.contains(port.getName())) {
						onError(port.getNameElement(), "missing assignment for port '" + port.getName() + "'");
					}
				}
			} else if (definition instanceof Signal) {
				Signal signal = (Signal)definition;
				if (signal.getInitializer() == null && !previouslyAssignedSignals.contains(signal.getName())) {
					onError(signal.getNameElement(), "missing assignment for signal '" + signal.getName() + "'");
				}
			}
		}

	}

	private void processDefinition(Named item) {
		if (item instanceof Constant) {
			// constants have been handled by the constant evaluator already
			return;
		} else if (item instanceof SignalLike) {
			SignalLike signalLike = (SignalLike)item;
			ProcessedDataType.Family dataTypeFamily = signalLike.getProcessedDataType().getFamily();
			if (dataTypeFamily == ProcessedDataType.Family.UNKNOWN) {
				// don't complain about type errors if we don't even know the type -- in that case,
				// the reason why we don't know the type already appears as an error
				return;
			}
			String usage;
			boolean valid;
			if (item instanceof Port) {
				usage = "ports";
				valid = dataTypeFamily == ProcessedDataType.Family.BIT || dataTypeFamily == ProcessedDataType.Family.VECTOR;
			} else if (item instanceof Signal || item instanceof Register) {
				usage = "signals";
				valid = dataTypeFamily == ProcessedDataType.Family.BIT || dataTypeFamily == ProcessedDataType.Family.VECTOR || dataTypeFamily == ProcessedDataType.Family.MEMORY;
			} else {
				return;
			}
			if (!valid) {
				onError(signalLike.getDataTypeElement(), dataTypeFamily.getDisplayString() + " type not allowed for " + usage);
			}
		} else if (item instanceof ModuleInstance) {
			// TODO
		}
	}



	private void processDoBlock(ImplementationItem_DoBlock doBlock) {

		// look for duplicate assignments
		foreachPreOrder(doBlock, element -> {
			if (element instanceof Expression) {
				return false;
			}
			if (element instanceof Statement_Assignment) {
				handleAssignedTo(((Statement_Assignment) element).getLeftSide());
			}
			return true;
		});
	}

	private void handleAssignedTo(Expression destination) {
		if (destination instanceof Expression_Identifier) {
			LeafPsiElement signalNameElement = ((Expression_Identifier) destination).getIdentifier();
			handleAssignedTo(signalNameElement.getText(), signalNameElement);
		} else if (destination instanceof Expression_IndexSelection) {
			Expression container = ((Expression_IndexSelection) destination).getContainer();
			handleAssignedTo(container);
		} else if (destination instanceof Expression_RangeSelection) {
			Expression container = ((Expression_RangeSelection) destination).getContainer();
			handleAssignedTo(container);
		} else if (destination instanceof Expression_Parenthesized) {
			Expression inner = ((Expression_Parenthesized) destination).getExpression();
			handleAssignedTo(inner);
		} else if (destination instanceof Expression_BinaryConcat) {
			Expression_BinaryConcat typed = (Expression_BinaryConcat)destination;
			handleAssignedTo(typed.getLeftOperand());
			handleAssignedTo(typed.getRightOperand());
		} else if (destination instanceof Expression_InstancePort) {
			Expression_InstancePort typed = (Expression_InstancePort)destination;
			handleAssignedTo(typed.getInstanceName().getText() + '.' + typed.getPortName().getText(), typed);
		}
	}

	private void handleAssignedTo(String signalName, PsiElement errorSource) {
		if (previouslyAssignedSignals.contains(signalName)) {
			onError(errorSource, "signal " + signalName + " was already assigned to in another do-block");
		}
		newlyAssignedSignals.add(signalName);
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

	//
	// low-level helper methods
	//

	/**
	 * The specified body is executed for each element, pre-order, and should return true if its
	 * children should be visited too.
	 */
	private void foreachPreOrder(PsiElement root, Predicate<PsiElement> body) {
		if (!body.test(root)) {
			return;
		}
		if (root instanceof ASTDelegatePsiElement) {
			InternalPsiUtil.foreachChild((ASTDelegatePsiElement) root, child -> foreachPreOrder(child, body));
		}
	}

}
