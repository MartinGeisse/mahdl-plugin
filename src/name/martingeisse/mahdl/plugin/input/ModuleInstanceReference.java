package name.martingeisse.mahdl.plugin.input;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ModuleInstanceReference extends LocalReference {

	private final InstanceReferenceName instanceReferenceName;

	public ModuleInstanceReference(InstanceReferenceName instanceReferenceName) {
		this.instanceReferenceName = instanceReferenceName;
	}

	@Override
	public LeafPsiElement getElement() {
		return instanceReferenceName.getIdentifier();
	}

	@Override
	protected boolean isElementTargetable(PsiElement potentialTarget) {
		return (potentialTarget instanceof PortDefinition || potentialTarget instanceof SignalLikeDefinition || potentialTarget instanceof ImplementationItem_ModuleInstance);
	}

	@NotNull
	@Override
	public Object[] getVariants() {
		// note: if this returns PSI elements, they must be PsiNamedElement or contain the name in meta-data
		Module module = PsiUtil.getAncestor(instanceReferenceName, Module.class);
		if (module == null) {
			return new Object[0];
		}
		List<Object> variants = new ArrayList<>();
		for (ImplementationItem implementationItem : module.getImplementationItems().getAll()) {
			if (implementationItem instanceof ImplementationItem_ModuleInstance) {
				String instanceName = implementationItem.getName();
				if (instanceName != null) {
					variants.add(instanceName);
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
