package name.martingeisse.mahdl.plugin.annotator;

import com.google.common.collect.ImmutableMap;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.MahdlFileType;
import name.martingeisse.mahdl.plugin.MahdlSourceFile;
import name.martingeisse.mahdl.plugin.definition.*;
import name.martingeisse.mahdl.plugin.input.psi.*;

import java.util.Set;

/**
 *
 */
final class ModuleAnnotationRun {

	// outer objects passed to the constructor
	private final Module module;
	private final AnnotationHolder annotationHolder;

	// information that is collected in the preparation step
	private ImmutableMap<String, Named> definitions;

	// additional information that is collected during the main annotation step
	private Set<String> previouslyAssignedSignals;
	private Set<String> newlyAssignedSignals;


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
					annotationHolder.createErrorAnnotation((PsiElement)module.getModuleName(),
						"module '" + moduleName + "' should be defined in a file named '" + expectedFileName + "'");
				}
				break;
			}
		}

		// collect definitions
		definitions = ModuleAnalyzer.analyze(module, annotationHolder);

		// annotate the module
		for (PortDefinition port : module.getPorts().getAll()) {
			annotatePortDefinition(port);
		}
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			// we collect all newly assigned signals in a separate set and add them at the end of the current do-block
			// because assigning to a signal multiple times within the same do-block is allowed
			if (implementationItem instanceof ImplementationItem_SignalLikeDefinition) {
				annotateSignalLikeDefinition((ImplementationItem_SignalLikeDefinition) implementationItem);
			} else if (implementationItem instanceof ImplementationItem_ModuleInstance) {
				annotateModuleInstance((ImplementationItem_ModuleInstance) implementationItem);
			} else if (implementationItem instanceof ImplementationItem_DoBlock) {
				annotateDoBlock((ImplementationItem_DoBlock)implementationItem);
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
					annotationHolder.createErrorAnnotation(signalOrPort.getNameElement(),
						"missing assignment for " + kind + " '" + signalOrPort.getName() + "'");
				}
			}
		}

	}

	//
	// main annotation methods
	//

	private void annotatePortDefinition(PortDefinition port) {
		DataType dataType = port.getDataType();
		annotateDataType(dataType);
		if (!(dataType instanceof DataType_Bit) && !(dataType instanceof DataType_Vector)) {
			annotationHolder.createErrorAnnotation(dataType, "data type '" + dataType.getText() + "' not allowed for ports");
		}
	}

	private void annotateSignalLikeDefinition(ImplementationItem_SignalLikeDefinition signalLikeDefinition) {
		annotateDataType(signalLikeDefinition.getDataType());
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
