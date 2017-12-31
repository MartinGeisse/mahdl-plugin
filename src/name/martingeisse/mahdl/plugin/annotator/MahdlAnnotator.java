package name.martingeisse.mahdl.plugin.annotator;

import com.intellij.extapi.psi.ASTDelegatePsiElement;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.MahdlFileType;
import name.martingeisse.mahdl.plugin.MahdlSourceFile;
import name.martingeisse.mahdl.plugin.input.psi.*;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 *
 */
public class MahdlAnnotator implements Annotator {

	/**
	 * This method gets called on ALL PsiElements, post-order.
	 */
	@Override
	public void annotate(@NotNull PsiElement psiElement, @NotNull AnnotationHolder annotationHolder) {
		if (psiElement instanceof Module) {
			annotate((Module) psiElement, annotationHolder);
		}
	}

	private void annotate(@NotNull Module module, @NotNull AnnotationHolder annotationHolder) {

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
		Map<String, InterfaceItem> interfaceItemsByName = new HashMap<>();
		for (InterfaceItem interfaceItem : module.getInterfaceItems().getAll()) {
			for (LeafPsiElement element : interfaceItem.getIdentifiers().getAll()) {
				String name = element.getText();
				if (interfaceItemsByName.put(name, interfaceItem) != null) {
					annotationHolder.createErrorAnnotation(element.getNode(), "redefinition of port name '" + name + "'");
				}
			}
		}

		// find constants among them
		Set<String> constantPortNames = new HashSet<>();
		for (Map.Entry<String, InterfaceItem> entry : interfaceItemsByName.entrySet()) {
			if (entry.getValue().getDirection() instanceof PortDirection_Const) {
				constantPortNames.add(entry.getKey());
			}
		}

		// Map data about implementation items: signals by name; instances by name; which do-blocks assign to which
		// signals. Finds duplicate names and signals assigned to by multiple do-blocks.
		Set<String> definedNames = new HashSet<>(interfaceItemsByName.keySet());
		Map<String, DataType> signalNameToDataType = new HashMap<>();
		Map<String, Expression> signalNameToInitializer = new HashMap<>();
		Map<String, ImplementationItem_ModuleInstance> moduleInstances = new HashMap<>();
		SignalAssignmentCollector signalAssignmentCollector = new SignalAssignmentCollector(annotationHolder);
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			if (implementationItem instanceof ImplementationItem_SignalDeclaration) {
				ImplementationItem_SignalDeclaration typedImplementationItem = (ImplementationItem_SignalDeclaration) implementationItem;
				DataType dataType = ((ImplementationItem_SignalDeclaration) implementationItem).getDataType();
				for (DeclaredSignal declaredSignal : typedImplementationItem.getSignalNames().getAll()) {
					if (declaredSignal instanceof DeclaredSignal_WithoutInitializer) {
						DeclaredSignal_WithoutInitializer typedDeclaredSignal = (DeclaredSignal_WithoutInitializer) declaredSignal;
						String signalName = typedDeclaredSignal.getIdentifier().getText();
						if (definedNames.add(signalName)) {
							signalNameToDataType.put(signalName, dataType);
						} else {
							annotationHolder.createErrorAnnotation(typedDeclaredSignal.getIdentifier().getNode(), "redefinition of '" + signalName + "'");
						}
					} else if (declaredSignal instanceof DeclaredSignal_WithInitializer) {
						DeclaredSignal_WithInitializer typedDeclaredSignal = (DeclaredSignal_WithInitializer) declaredSignal;
						String signalName = typedDeclaredSignal.getIdentifier().getText();
						if (definedNames.add(signalName)) {
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
				if (definedNames.add(instanceName)) {
					moduleInstances.put(instanceName, moduleInstance);
				} else {
					annotationHolder.createErrorAnnotation(moduleInstance.getInstanceName().getNode(), "redefinition of '" + instanceName + "'");
				}
			} else if (implementationItem instanceof ImplementationItem_DoBlock) {
				ImplementationItem_DoBlock doBlock = (ImplementationItem_DoBlock) implementationItem;
				// TODO needs to know about "assignments" by initializers and port connections to place the annotations at the right place!
				// maybe do this in the ModuleAnnotator too
				signalAssignmentCollector.handle(doBlock);
			}
		}
		Map<String, ImplementationItem_DoBlock> assigningDoBlocks = signalAssignmentCollector.getAssigningDoBlocks();

		// finally, the real annotation pass which finds various problems
		new ModuleAnnotator(module, annotationHolder, interfaceItemsByName, constantPortNames, signalNameToDataType,
			signalNameToInitializer, moduleInstances, assigningDoBlocks).run();

	}

}
