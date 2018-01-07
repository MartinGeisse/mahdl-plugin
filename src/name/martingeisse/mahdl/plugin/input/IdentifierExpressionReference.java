package name.martingeisse.mahdl.plugin.input;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.IncorrectOperationException;
import name.martingeisse.mahdl.plugin.input.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class IdentifierExpressionReference implements PsiReference {

	private final Expression_Identifier expression;

	public IdentifierExpressionReference(Expression_Identifier expression) {
		this.expression = expression;
	}

	@Override
	public PsiElement getElement() {
		return expression;
	}

	@Override
	public TextRange getRangeInElement() {
		return new TextRange(0, getCanonicalText().length());
	}

	@Nullable
	@Override
	public PsiElement resolve() {

		Module module = PsiUtil.getAncestor(expression, Module.class);
		if (module == null) {
			return null;
		}

		String identifier = expression.getIdentifier().getText();

		// ports
		for (PortDefinitionGroup group : module.getPortDefinitionGroups().getAll()) {
			for (PortDefinition definition : group.getDefinitions().getAll()) {
				String definitionName = definition.getName();
				if (definitionName != null && definitionName.equals(identifier)) {
					return definition;
				}
			}
		}

		// local definitions. We even return module instances although it is not legally possible to use them in an
		// identifier expression, because it makes life easier for a user who accidentally does just that.
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			if (implementationItem instanceof ImplementationItem_SignalLikeDefinitionGroup) {
				for (SignalLikeDefinition definition : ((ImplementationItem_SignalLikeDefinitionGroup) implementationItem).getDefinitions().getAll()) {
					String definitionName = definition.getName();
					if (definitionName != null && definitionName.equals(identifier)) {
						return definition;
					}
				}
			} else if (implementationItem instanceof ImplementationItem_ModuleInstance) {
				String instanceName = implementationItem.getName();
				if (instanceName != null && instanceName.equals(identifier)) {
					return implementationItem;
				}
			}
		}

		return null;
	}

	@NotNull
	@Override
	public String getCanonicalText() {
		return expression.getIdentifier().getText();
	}

	@Override
	public PsiElement handleElementRename(String newName) throws IncorrectOperationException {
		return PsiUtil.setText(expression.getIdentifier(), newName);
	}

	@Override
	public PsiElement bindToElement(@NotNull PsiElement psiElement) throws IncorrectOperationException {
		if (psiElement instanceof PortDefinition || psiElement instanceof SignalLikeDefinition || psiElement instanceof ImplementationItem_ModuleInstance) {
			String newName = ((PsiNamedElement) psiElement).getName();
			if (newName != null) {
				return PsiUtil.setText(expression.getIdentifier(), newName);
			}
		}
		throw new IncorrectOperationException();
	}

	@Override
	public boolean isReferenceTo(PsiElement psiElement) {
		if (psiElement instanceof PortDefinition || psiElement instanceof SignalLikeDefinition || psiElement instanceof ImplementationItem_ModuleInstance) {
			String elementName = ((PsiNamedElement) psiElement).getName();
			if (elementName != null) {
				String thisName = getCanonicalText();
				if (elementName.equals(thisName)) {
					PsiElement resolved = resolve();
					return (resolved != null && resolved.equals(psiElement));
				}
			}
		}
		return false;
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
