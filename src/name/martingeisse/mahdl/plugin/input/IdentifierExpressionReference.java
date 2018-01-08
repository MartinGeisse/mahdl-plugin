package name.martingeisse.mahdl.plugin.input;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.IncorrectOperationException;
import name.martingeisse.mahdl.plugin.input.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class IdentifierExpressionReference extends LocalReference {

	private final Expression_Identifier expression;

	public IdentifierExpressionReference(Expression_Identifier expression) {
		this.expression = expression;
	}

	@Override
	public LeafPsiElement getElement() {
		return expression.getIdentifier();
	}

	@Override
	protected boolean isElementTargetable(PsiElement potentialTarget) {
		return (potentialTarget instanceof PortDefinition || potentialTarget instanceof SignalLikeDefinition || potentialTarget instanceof ImplementationItem_ModuleInstance);
	}

	@NotNull
	@Override
	public Object[] getVariants() {

		// note: if this returns PSI elements, they must be PsiNamedElement or contain the name in meta-data

		Module module = PsiUtil.getAncestor(expression, Module.class);
		if (module == null) {
			return new Object[0];
		}

		List<Object> variants = new ArrayList<>();

		// ports
		for (PortDefinitionGroup group : module.getPortDefinitionGroups().getAll()) {
			for (PortDefinition definition : group.getDefinitions().getAll()) {
				String definitionName = definition.getName();
				if (definitionName != null) {
					variants.add(definitionName);
				}
			}
		}

		// implementation items. TODO it should work to add non-instance names here and add instance names in the instance reference. test that.
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			if (implementationItem instanceof ImplementationItem_SignalLikeDefinitionGroup) {
				for (SignalLikeDefinition definition : ((ImplementationItem_SignalLikeDefinitionGroup) implementationItem).getDefinitions().getAll()) {
					String definitionName = definition.getName();
					if (definitionName != null) {
						variants.add(definitionName);
					}
				}
			}
		}

		return variants.toArray();
	}

	@Override
	public boolean isSoft() {
		return false;
	}

}
