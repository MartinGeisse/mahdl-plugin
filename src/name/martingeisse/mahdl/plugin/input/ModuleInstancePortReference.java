/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
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
 * TODO Port connection: auto-complete doesn't work. Not sure if this is the correct class for that.
 */
public class ModuleInstancePortReference implements PsiReference {

	private final InstancePortName instancePortName;

	public ModuleInstancePortReference(@NotNull InstancePortName instancePortName) {
		this.instancePortName = instancePortName;
	}

	@Override
	@NotNull
	public PsiElement getElement() {
		return instancePortName;
	}

	@Override
	@NotNull
	public TextRange getRangeInElement() {
		return new TextRange(0, getCanonicalText().length());
	}

	@Nullable
	private PsiElement resolveModule() {
		PsiElement parent = instancePortName.getParent();

		// this reference type can be used both in an instance port connection and in an instance port expression
		PsiElement someElementInsideInstanceDefinition;
		if (parent instanceof Expression_InstancePort) {
			someElementInsideInstanceDefinition = ((Expression_InstancePort) parent).getInstanceName().getReference().resolve();
			if (someElementInsideInstanceDefinition == null) {
				// the instance name before the dot is unknown, so the port name is meaningless
				return null;
			}
		} else if (parent instanceof PortConnection) {
			someElementInsideInstanceDefinition = instancePortName;
		} else {
			// port name element is lost in a PSI soup and not even part of a real port reference
			return null;
		}

		// now that we found a PSI element that is part of the instance definition, both cases are handled the same way
		ImplementationItem_ModuleInstance moduleInstanceElement = PsiUtil.getAncestor(someElementInsideInstanceDefinition, ImplementationItem_ModuleInstance.class);
		if (moduleInstanceElement == null) {
			// the element which defines the instance name was found but is lost in a PSI soup -- at least resolve to the name-defining element.
			return someElementInsideInstanceDefinition;
		}
		PsiElement moduleNameDefiningElement = moduleInstanceElement.getModuleName().getReference().resolve();
		if (moduleNameDefiningElement == null) {
			// the module name is unknown
			return moduleInstanceElement.getModuleName();
		}
		Module module = PsiUtil.getAncestor(moduleNameDefiningElement, Module.class);
		if (module == null) {
			// the module name defining element is lost in a PSI soup
			return moduleNameDefiningElement;
		}
		return module;
	}

	// Works similar to resolve(), but won't return anything other than an ImplementationItem_ModuleInstance for the
	// module instance to which the port belongs. Any failure case doesn't resolve the reference "as good as we can" but
	// just returns null.
	@Nullable
	public ImplementationItem_ModuleInstance resolveModuleOnly() {
		PsiElement element = resolveModule();
		return (element instanceof ImplementationItem_ModuleInstance ? (ImplementationItem_ModuleInstance)element : null);
	}

	@Nullable
	@Override
	public PsiElement resolve() {
		PsiElement resolvedModule = resolveModule();
		if (!(resolvedModule instanceof Module)) {
			return resolvedModule;
		}
		Module targetModule = (Module)resolvedModule;
		String referencePortName = getCanonicalText();
		for (PortDefinitionGroup portDefinitionGroup : targetModule.getPortDefinitionGroups().getAll()) {
			if (portDefinitionGroup instanceof PortDefinitionGroup_Valid) {
				for (PortDefinition portDefinition : ((PortDefinitionGroup_Valid) portDefinitionGroup).getDefinitions().getAll()) {
					String definitionPortName = portDefinition.getName();
					if (referencePortName.equals(definitionPortName)) {
						return portDefinition;
					}
				}
			}
		}
		// we found a module, but that module doesn't have a matching port. At least resolve to the module.
		return targetModule;
	}

	// Works similar to resolve(), but won't return anything other than a PortDefinition. That is, any failure case
	// doesn't resolve the reference "as good as we can" but just returns null.
	@Nullable
	public PortDefinition resolvePortDefinitionOnly() {
		PsiElement element = resolve();
		return (element instanceof PortDefinition ? (PortDefinition)element : null);
	}

	@NotNull
	@Override
	public String getCanonicalText() {
		return instancePortName.getIdentifier().getText();
	}

	@Override
	@NotNull
	public PsiElement handleElementRename(@Nullable String newName) throws IncorrectOperationException {
		if (newName == null) {
			throw new IncorrectOperationException("new name is null");
		}
		return PsiUtil.setText(instancePortName.getIdentifier(), newName);
	}

	@Override
	@NotNull
	public PsiElement bindToElement(@NotNull PsiElement psiElement) throws IncorrectOperationException {
		if (psiElement instanceof PortDefinition) {
			String newName = ((PsiNamedElement) psiElement).getName();
			if (newName != null) {
				return PsiUtil.setText(instancePortName.getIdentifier(), newName);
			}
		}
		throw new IncorrectOperationException();
	}

	@Override
	public boolean isReferenceTo(@Nullable PsiElement psiElement) {
		if (psiElement instanceof PortDefinition) {
			String candidatePortName = ((PortDefinition) psiElement).getName();
			if (candidatePortName != null && candidatePortName.equals(getCanonicalText())) {
				PsiElement resolved = resolve();
				return (resolved != null && resolved.equals(psiElement));
			}
		}
		return false;
	}

	@NotNull
	@Override
	public Object[] getVariants() {

		// note: if this returns PSI elements, they must be PsiNamedElement or contain the name in meta-data

		List<Object> variants = new ArrayList<>();
		PsiElement resolvedModule = resolveModule();
		if (resolvedModule instanceof Module) {
			Module targetModule = (Module)resolvedModule;
			for (PortDefinitionGroup portDefinitionGroup : targetModule.getPortDefinitionGroups().getAll()) {
				if (portDefinitionGroup instanceof PortDefinitionGroup_Valid) {
					for (PortDefinition portDefinition : ((PortDefinitionGroup_Valid) portDefinitionGroup).getDefinitions().getAll()) {
						String definitionPortName = portDefinition.getName();
						if (definitionPortName != null) {
							variants.add(definitionPortName);
						}
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
