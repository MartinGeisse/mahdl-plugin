package name.martingeisse.mahdl.plugin.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.MahdlFileType;
import name.martingeisse.mahdl.plugin.MahdlSourceFile;
import name.martingeisse.mahdl.plugin.input.psi.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
final class ModuleAnnotationRun {

	// outer objects passed to the constructor
	private final Module module;
	private final AnnotationHolder annotationHolder;

	// information that is collected in the preparation step
	private final Map<String, PsiElement> definedNames = new HashMap<>();
	private final Set<String> signalsNeedingAssignment = new HashSet<>();
	private final Map<String, PortDefinition> portsByName = new HashMap<>();
	private final Map<String, DataType> signalNameToDataType = new HashMap<>();
	private final Map<String, Expression> signalNameToInitializer = new HashMap<>();
	private final Map<String, ImplementationItem_ModuleInstance> moduleInstances = new HashMap<>();

	// additional information that is collected during the main annotation step
	private final Set<String> previouslyAssignedSignals = new HashSet<>();
	private final Set<String> newlyAssignedSignals = new HashSet<>();


	public ModuleAnnotationRun(Module module, AnnotationHolder annotationHolder) {
		this.module = module;
		this.annotationHolder = annotationHolder;
	}

	public void annotate() {

		// make sure the module name matches the file name
		for (PsiElement element = module; element != null; element = element.getParent()) {
			if (element instanceof MahdlSourceFile) {
				String moduleName = module.getModuleName().getText();
				String expectedFileName = moduleName + '.' + MahdlFileType.DEFAULT_EXTENSION;
				String actualFileName = ((MahdlSourceFile) element).getName();
				if (!actualFileName.equals(expectedFileName)) {
					annotationHolder.createErrorAnnotation(module.getModuleName().getNode(),
						"module '" + moduleName + "' should be defined in a file named '" + expectedFileName + "'");
				}
				break;
			}
		}

		// map ports by name and find duplicates
		for (PortDefinition port : module.getPorts().getAll()) {
			for (LeafPsiElement element : port.getIdentifiers().getAll()) {
				String name = element.getText();
				if (definedNames.put(name, element) == null) {
					portsByName.put(name, port);
					if (port.getDirection() instanceof PortDirection_Output) {
						signalsNeedingAssignment.add(name);
					}
				} else {
					annotationHolder.createErrorAnnotation(element.getNode(), "redefinition of port name '" + name + "'");
				}
			}
		}

		// Map data about implementation items: signals by name; instances by name; which do-blocks assign to which
		// signals. Finds duplicate names and signals assigned to by multiple do-blocks.
		// TODO treat signals, constants and registers differently! put in different maps!
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			if (implementationItem instanceof ImplementationItem_SignalLikeDefinition) {
				ImplementationItem_SignalLikeDefinition typedImplementationItem = (ImplementationItem_SignalLikeDefinition) implementationItem;
				DataType dataType = ((ImplementationItem_SignalLikeDefinition) implementationItem).getDataType();
				for (DeclaredSignalLike declaredSignal : typedImplementationItem.getSignalNames().getAll()) {
					if (declaredSignal instanceof DeclaredSignalLike_WithoutInitializer) {
						DeclaredSignalLike_WithoutInitializer typedDeclaredSignal = (DeclaredSignalLike_WithoutInitializer) declaredSignal;
						String signalName = typedDeclaredSignal.getIdentifier().getText();
						if (definedNames.put(signalName, typedDeclaredSignal.getIdentifier()) == null) {
							signalNameToDataType.put(signalName, dataType);
							signalsNeedingAssignment.add(signalName);
						} else {
							annotationHolder.createErrorAnnotation(typedDeclaredSignal.getIdentifier().getNode(), "redefinition of '" + signalName + "'");
						}
					} else if (declaredSignal instanceof DeclaredSignalLike_WithInitializer) {
						DeclaredSignalLike_WithInitializer typedDeclaredSignal = (DeclaredSignalLike_WithInitializer) declaredSignal;
						String signalName = typedDeclaredSignal.getIdentifier().getText();
						if (definedNames.put(signalName, typedDeclaredSignal.getIdentifier()) == null) {
							signalNameToDataType.put(signalName, dataType);
							signalNameToInitializer.put(signalName, typedDeclaredSignal.getInitializer());
						} else {
							annotationHolder.createErrorAnnotation(typedDeclaredSignal.getIdentifier().getNode(), "redefinition of '" + signalName + "'");
						}
					}
				}
			} else if (implementationItem instanceof ImplementationItem_ModuleInstance) {
				ImplementationItem_ModuleInstance moduleInstance = (ImplementationItem_ModuleInstance) implementationItem;
				String instanceName = moduleInstance.getInstanceName().getText();
				if (definedNames.put(instanceName, moduleInstance.getInstanceName()) == null) {
					moduleInstances.put(instanceName, moduleInstance);
				} else {
					annotationHolder.createErrorAnnotation(moduleInstance.getInstanceName().getNode(), "redefinition of '" + instanceName + "'");
				}
			}
		}

		// this is the main annotation step
		for (PortDefinition port : module.getPorts().getAll()) {
			annotatePortDefinition(port);
		}
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			// we collect all newly assigned signals in a separate set and add them at the end of the current do-block
			// because assigning to a signal multiple times within the same do-block is allowed
			if (implementationItem instanceof ImplementationItem_SignalLikeDefinition) {
				annotateSignalDeclaration((ImplementationItem_SignalLikeDefinition) implementationItem);
			} else if (implementationItem instanceof ImplementationItem_ModuleInstance) {
				annotateModuleInstance((ImplementationItem_ModuleInstance) implementationItem);
			} else if (implementationItem instanceof ImplementationItem_DoBlock) {
				annotateDoBlock((ImplementationItem_DoBlock)implementationItem);
			}
			previouslyAssignedSignals.addAll(newlyAssignedSignals);
			newlyAssignedSignals.clear();
		}

		// now check that all signals needing that have been assigned to
		Set<String> unassignedSignals = new HashSet<>(signalsNeedingAssignment);
		unassignedSignals.removeAll(previouslyAssignedSignals);
		for (String unassignedSignal : unassignedSignals) {
			PsiElement identifierElement = definedNames.get(unassignedSignal);
			if (identifierElement == null) {
				// shouldn't happen, but if it does, at least put the message somewhere
				identifierElement = module.getModuleName();
			}
			annotationHolder.createErrorAnnotation(identifierElement.getNode(), "no assignment found for signal '" + unassignedSignal + "'");
		}

	}

	//
	// main annotation methods
	//

	private void annotatePortDefinition(PortDefinition port) {
		DataType dataType = port.getDataType();
		annotateDataType(dataType);
		if (!(dataType instanceof DataType_Bit) && !(dataType instanceof DataType_Vector)) {
			annotationHolder.createErrorAnnotation(dataType.getNode(), "data type '" + dataType.getText() + "' not allowed for ports");
		}
	}

	private void annotateSignalDeclaration(ImplementationItem_SignalLikeDefinition signalLikeDefinition) {

	}

	private void annotateModuleInstance(ImplementationItem_ModuleInstance moduleInstance) {

	}

	private void annotateDoBlock(ImplementationItem_DoBlock doBlock) {

	}

	//
	// common helper methods
	//

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

	// note: in v1, a constant expression cannot refer to constant ports
	private boolean annotateConstantExpression(Expression expression) {
		annotateExpression(expression);

	}

	private DataType annotateExpression(Expression expression) {

	}

}
