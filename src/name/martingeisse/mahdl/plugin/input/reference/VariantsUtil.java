package name.martingeisse.mahdl.plugin.input.reference;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides common code for getVariants() (autocomplete support).
 *
 * Some common code is useful because of the way auto-complete works in IntelliJ: The IDE inserts the dummy string
 * "IntellijIdeaRulezzz" at the cursor, re-parses the file, obtains a reference for the PSI element for that string
 * and calls getVariants() on it. Depending on the context (especially depending on tokens *after* the cursor), this
 * may yield different kinds of references unintentionally.
 */
class VariantsUtil {

	// prevent instantiation
	private VariantsUtil() {
	}

	/**
	 * Returns ports of the current module as well as local names, including both signal-likes and module instances --
	 * everything that can be at the beginning of a (sub-)expression.
	 */
	public static Object[] getExpressionVariants(PsiElement referenceElement) {

		Module module = PsiUtil.getAncestor(referenceElement, Module.class);
		if (module == null) {
			return new Object[0];
		}

		List<Object> variants = new ArrayList<>();

		// ports
		for (PortDefinitionGroup group : module.getPortDefinitionGroups().getAll()) {
			if (group instanceof PortDefinitionGroup_Valid) {
				for (PortDefinition definition : ((PortDefinitionGroup_Valid) group).getDefinitions().getAll()) {
					String definitionName = definition.getName();
					if (definitionName != null) {
						variants.add(definitionName);
					}
				}
			}
		}

		// implementation items
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			if (implementationItem instanceof ImplementationItem_SignalLikeDefinitionGroup) {
				for (SignalLikeDefinition definition : ((ImplementationItem_SignalLikeDefinitionGroup) implementationItem).getDefinitions().getAll()) {
					String definitionName = definition.getName();
					if (definitionName != null) {
						variants.add(definitionName);
					}
				}
			} else if (implementationItem instanceof ImplementationItem_ModuleInstance) {
				variants.add(((ImplementationItem_ModuleInstance) implementationItem).getInstanceName().getText());
			}
		}

		return variants.toArray();

	}

}
