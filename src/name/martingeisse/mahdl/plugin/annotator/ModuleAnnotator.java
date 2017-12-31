package name.martingeisse.mahdl.plugin.annotator;

import com.intellij.extapi.psi.ASTDelegatePsiElement;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * This class does the main annotator work. It must be constructed with all the data collected about the module to
 * annotate, then performs various checks and adds annotations. Instances of this class cannot be reused.
 */
final class ModuleAnnotator {

	private final Module module;
	private final AnnotationHolder annotationHolder;
	private final Map<String, InterfaceItem> interfaceItemsByName;
	private final Set<String> constantPortNames;
	private final Map<String, DataType> signalNameToDataType;
	private final Map<String, Expression> signalNameToInitializer;
	private final Map<String, ImplementationItem_ModuleInstance> moduleInstances;
	private final Map<String, ImplementationItem_DoBlock> assigningDoBlocks;

	public ModuleAnnotator(Module module,
						   AnnotationHolder annotationHolder,
						   Map<String, InterfaceItem> interfaceItemsByName,
						   Set<String> constantPortNames,
						   Map<String, DataType> signalNameToDataType,
						   Map<String, Expression> signalNameToInitializer,
						   Map<String, ImplementationItem_ModuleInstance> moduleInstances,
						   Map<String, ImplementationItem_DoBlock> assigningDoBlocks) {
		this.module = module;
		this.annotationHolder = annotationHolder;
		this.interfaceItemsByName = interfaceItemsByName;
		this.constantPortNames = constantPortNames;
		this.signalNameToDataType = signalNameToDataType;
		this.signalNameToInitializer = signalNameToInitializer;
		this.moduleInstances = moduleInstances;
		this.assigningDoBlocks = assigningDoBlocks;
	}

	public void run() {
		for (InterfaceItem interfaceItem : module.getInterfaceItems().getAll()) {
			annotatePortCharacteristics(interfaceItem);
		}
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			if (implementationItem instanceof ImplementationItem_SignalDeclaration) {
				ImplementationItem_SignalDeclaration typedImplementationItem = (ImplementationItem_SignalDeclaration) implementationItem;
				// TODO
			} else if (implementationItem instanceof ImplementationItem_ModuleInstance) {
				ImplementationItem_ModuleInstance moduleInstance = (ImplementationItem_ModuleInstance) implementationItem;
				// TODO
			} else if (implementationItem instanceof ImplementationItem_DoBlock) {
				ImplementationItem_DoBlock doBlock = (ImplementationItem_DoBlock) implementationItem;
				// TODO
			}
		}
	}

	private void annotatePortCharacteristics(InterfaceItem interfaceItem) {

		DataType dataType = interfaceItem.getDataType();
		if (dataType instanceof DataType_Bit || dataType instanceof DataType_Vector) {
			return;
		}
		if (!(dataType instanceof DataType_Integer)) {
			annotationHolder.createErrorAnnotation(dataType.getNode(), "data type '" + dataType.getText() + "' not allowed for ports");
			return;
		}

		PortDirection direction = interfaceItem.getDirection();
		if (!(direction instanceof PortDirection_Const)) {
			annotationHolder.createErrorAnnotation(dataType.getNode(), "integer is only allowed for const ports");
			return;
		}

		annotateDataType(dataType);

	}

	private void annotateDataType(DataType dataType) {
		if (dataType instanceof DataType_Vector) {
			DataType_Vector vector = (DataType_Vector) dataType;
			annotateConstantSizeExpression(vector.getSize());
		} else if (dataType instanceof DataType_Memory) {
			DataType_Memory memory = (DataType_Memory) dataType;
			annotateConstantSizeExpression(memory.getFirstSize());
			annotateConstantSizeExpression(memory.getSecondSize());
		}
	}

	private void annotateConstantSizeExpression(Expression expression) {
		if (!annotateConstantExpression(expression)) {
			return;
		}
		// TODO must be >0. If the size contains any usage of constants, skip this test, since the constants may
		// be changed at instantiation. Repeat the full test for instantiation
	}

	private boolean annotateConstantExpression(Expression expression) {
		return annotatePreOrder(expression, element -> {
			if (element instanceof Expression_Signal) {
				String signalName = ((Expression_Signal) element).getSignalName().getText();
				if (!constantPortNames.contains(signalName)) {
					return "unknown constant: " + signalName;
				}
			}
			if (element instanceof Expression_InstancePort || element instanceof Expression_FunctionCall) {
				return "expression must be constant";
			}
			return null;
		}, annotationHolder);
	}

	private boolean annotatePreOrder(PsiElement root, Function<PsiElement, String> validator, AnnotationHolder annotationHolder) {
		String message = validator.apply(root);
		if (message != null) {
			annotationHolder.createErrorAnnotation(root.getNode(), message);
			return false;
		}
		if (root instanceof ASTDelegatePsiElement) {
			MutableBoolean result = new MutableBoolean(true);
			InternalPsiUtil.foreachChild((ASTDelegatePsiElement) root, child -> {
				if (!annotatePreOrder(child, validator, annotationHolder)) {
					result.setFalse();
				}
			});
			return result.getValue();
		} else {
			return true;
		}
	}

}
