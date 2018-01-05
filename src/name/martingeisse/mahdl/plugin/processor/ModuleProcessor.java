package name.martingeisse.mahdl.plugin.processor;

import com.google.common.collect.ImmutableMap;
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
import java.util.HashMap;
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

	private ImmutableMap<String, Named> definitions;
	private Map<String, ConstantValue> constants;
	private ConstantExpressionEvaluator constantExpressionEvaluator;
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

		// collect definitions TODO see TODO comment in ModuleAnalyzer
		definitions = new ModuleAnalyzer(module) {
			@Override
			protected void onError(PsiElement errorSource, String message) {
				ModuleProcessor.this.onError(errorSource, message);
			}
		}.analyze();

		// evaluate constants
		constants = new HashMap<>();
		constantExpressionEvaluator = new ConstantExpressionEvaluator(constants) {
			@Override
			protected void onError(PsiElement errorSource, String message) {
				ModuleProcessor.this.onError(errorSource, message);
			}
		};
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			if (implementationItem instanceof ImplementationItem_SignalLikeDefinition) {
				ImplementationItem_SignalLikeDefinition signalLike = (ImplementationItem_SignalLikeDefinition)implementationItem;
				if (signalLike.getKind() instanceof SignalLikeKind_Constant) {
					// for constants, the data type must be valid based on the constants defined above
					ProcessedDataType processedDataType = processDataType(signalLike.getDataType());
					for (DeclaredSignalLike declaredSignalLike : signalLike.getSignalNames().getAll()) {
						if (declaredSignalLike instanceof DeclaredSignalLike_WithoutInitializer) {

							DeclaredSignalLike_WithoutInitializer typedDeclaredSignalLike = (DeclaredSignalLike_WithoutInitializer)declaredSignalLike;
							onError(declaredSignalLike, "constant must have an initializer");
							constants.put(typedDeclaredSignalLike.getIdentifier().getText(), ConstantValue.Unknown.INSTANCE);

						} else if (declaredSignalLike instanceof DeclaredSignalLike_WithInitializer) {

							DeclaredSignalLike_WithInitializer typedDeclaredSignalLike = (DeclaredSignalLike_WithInitializer)declaredSignalLike;
							ConstantValue value = constantExpressionEvaluator.evaluate(typedDeclaredSignalLike.getInitializer());
							constants.put(typedDeclaredSignalLike.getIdentifier().getText(), processedDataType.convertValueImplicitly(value));

						} else {

							onError(declaredSignalLike, "unknown PSI node");

						}
					}
				}
			}
		}

		// process the module
		for (PortDefinition port : module.getPorts().getAll()) {
			processPortDefinition(port);
		}
		previouslyAssignedSignals = new HashSet<>();
		newlyAssignedSignals = new HashSet<>();
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			// we collect all newly assigned signals in a separate set and add them at the end of the current do-block
			// because assigning to a signal multiple times within the same do-block is allowed
			if (implementationItem instanceof ImplementationItem_SignalLikeDefinition) {
				processSignalLikeDefinition((ImplementationItem_SignalLikeDefinition) implementationItem);
			} else if (implementationItem instanceof ImplementationItem_ModuleInstance) {
				processModuleInstance((ImplementationItem_ModuleInstance) implementationItem);
			} else if (implementationItem instanceof ImplementationItem_DoBlock) {
				processDoBlock((ImplementationItem_DoBlock)implementationItem);
			}
			previouslyAssignedSignals.addAll(newlyAssignedSignals);
			newlyAssignedSignals.clear();
		}

		// now check that all ports and signals without initializer have been assigned to
		for (Named definition : definitions.values()) {
			if (definition instanceof Signal || definition instanceof Port) {
				String kind = (definition instanceof Signal) ? "signal" : "port";
				SignalLike signalOrPort = (SignalLike)definition;
				if (signalOrPort.getInitializer() == null && !previouslyAssignedSignals.contains(signalOrPort.getName())) {
					onError(signalOrPort.getNameElement(), "missing assignment for " + kind + " '" + signalOrPort.getName() + "'");
				}
			}
		}

	}

	private void processPortDefinition(PortDefinition port) {
		DataType dataType = port.getDataType();
		processDataType(dataType);
		if (!(dataType instanceof DataType_Bit) && !(dataType instanceof DataType_Vector)) {
			onError(dataType, "data type '" + dataType.getText() + "' not allowed for ports");
		}
	}

	private void processSignalLikeDefinition(ImplementationItem_SignalLikeDefinition signalLikeDefinition) {
		processDataType(signalLikeDefinition.getDataType());
		if (signalLikeDefinition.getKind() instanceof SignalLikeKind_Constant) {
			// TODO ???? the initializers for constants have been evaluated and annotated already, because we needed them
			// to reason about data types
			return;
		}
		// TODO
	}

	private void processModuleInstance(ImplementationItem_ModuleInstance moduleInstance) {
		// TODO
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
		if (destination instanceof Expression_Signal) {
			LeafPsiElement signalNameElement = ((Expression_Signal) destination).getSignalName();
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
