package name.martingeisse.mahdl.plugin.analysis;

import com.google.common.collect.ImmutableMap;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.verilog.plugin.input.psi.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public final class ModuleAnalyzer {

	// prevent instantiation
	private ModuleAnalyzer() {
	}

	/**
	 * Analyzes the specified module, collecting module-local definitions.
	 *
	 * The annotationHolder is optional. If given, it is used to report the following problems:
	 * - port declarations without corresponding port definitions
	 * - name collisions between module-local definitions
	 *
	 */
	public static final ModuleAnalysisResult analyze(Module module, AnnotationHolder annotationHolder) {
		Set<String> portNames = new HashSet<>();
		Map<String, name.martingeisse.mahdl.plugin.analysis.PortDirection> portDirections = new HashMap<>();
		Map<String, ModuleLocalDefinition> localDefinitions = new HashMap<>();

		for (PortDeclaration portDeclaration : module.getPorts().getAll()) {
			if (portDeclaration instanceof PortDeclaration_Plain) {
				LeafPsiElement leaf = ((PortDeclaration_Plain) portDeclaration).getIdentifier();
				if (!portNames.add(leaf.getText())) {
					annotationHolder.createErrorAnnotation(leaf.getNode(), "redeclaration of port " + leaf.getText());
				}
			} else if (portDeclaration instanceof PortDeclaration_WithDefinition) {
				PortDefinition definition = ((PortDeclaration_WithDefinition) portDeclaration).getDefinition();
				for (LeafPsiElement identifier : definition.getIdentifiers().getAll()) {
					String portName =identifier.getText();
					if (!portNames.add(portName)) {
						annotationHolder.createErrorAnnotation(identifier.getNode(), "redeclaration of port " + portName);
					}
					portDirections.put(portName, convertPortDirection(definition.getDirection()));

				}

				definition.get


				LeafPsiElement leaf = typedDeclaration.get

			}
		}




		return new ModuleAnalysisResult(
			ImmutableMap.copyOf(portDirections),

		)
		ImmutableMap.copyOf(definitions);
	}

	private static name.martingeisse.mahdl.plugin.analysis.PortDirection convertPortDirection(name.martingeisse.mahdl.plugin.input.psi.PortDirection direction) {
		if (direction instanceof PortDirection_Input) {
			return name.martingeisse.mahdl.plugin.analysis.PortDirection.INPUT;
		} else if (direction instanceof PortDirection_Output) {
			return name.martingeisse.mahdl.plugin.analysis.PortDirection.OUTPUT;
		} else if (direction instanceof PortDirection_Inout) {
			return name.martingeisse.mahdl.plugin.analysis.PortDirection.INOUT;
		}
		// corrupted PSI: this will cause errors elsewhere already, so let's try to add as few follow-up errors as possible
		return name.martingeisse.mahdl.plugin.analysis.PortDirection.INOUT;
	}


}
