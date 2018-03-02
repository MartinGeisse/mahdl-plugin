/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.input.reference;

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

		// This reference type can be used both in an instance port connection and in an instance port expression,
		// so we use a helper method to locate the instance in either case, even with syntax errors.
		PsiElement someElementInsideInstanceDefinition = resolveSomeElementInsideInstanceDefinition(instancePortName.getParent());
		if (someElementInsideInstanceDefinition == null) {
			return null;
		}
		ImplementationItem_ModuleInstance moduleInstanceElement = PsiUtil.getAncestor(someElementInsideInstanceDefinition, ImplementationItem_ModuleInstance.class);
		if (moduleInstanceElement == null) {
			// at least resolve to inside the instance
			return someElementInsideInstanceDefinition;
		}

		// now that we found a PSI element that is part of the instance definition, both cases are handled the same way
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

	@Nullable
	private ImplementationItem_ModuleInstance resolveModuleInstance() {
		PsiElement someElementInsideInstanceDefinition = resolveSomeElementInsideInstanceDefinition(instancePortName.getParent());
		if (someElementInsideInstanceDefinition == null) {
			return null;
		} else {
			return PsiUtil.getAncestor(someElementInsideInstanceDefinition, ImplementationItem_ModuleInstance.class);
		}
	}

	@Nullable
	private static PsiElement resolveSomeElementInsideInstanceDefinition(@Nullable PsiElement startingPoint) {
		if (startingPoint == null) {
			return null;
		} else if (startingPoint instanceof Expression_InstancePort) {
			return ((Expression_InstancePort) startingPoint).getInstanceName().getReference().resolve();
		} else if (startingPoint instanceof PortConnection) {
			return startingPoint;
		} else if (startingPoint instanceof ImplementationItem) {
			return null;
		} else {
			return resolveSomeElementInsideInstanceDefinition(startingPoint.getParent());
		}
	}

	@Nullable
	@Override
	public PsiElement resolve() {
		PsiElement resolvedModule = resolveModule();
		if (!(resolvedModule instanceof Module)) {
			return resolvedModule;
		}
		Module targetModule = (Module) resolvedModule;
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
		return (element instanceof PortDefinition ? (PortDefinition) element : null);
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

		// collect defined port names
		List<String> portNames = new ArrayList<>();
		PsiElement resolvedModule = resolveModule();
		if (resolvedModule instanceof Module) {
			Module targetModule = (Module) resolvedModule;
			for (PortDefinitionGroup portDefinitionGroup : targetModule.getPortDefinitionGroups().getAll()) {
				if (portDefinitionGroup instanceof PortDefinitionGroup_Valid) {
					for (PortDefinition portDefinition : ((PortDefinitionGroup_Valid) portDefinitionGroup).getDefinitions().getAll()) {
						String definitionPortName = portDefinition.getName();
						if (definitionPortName != null) {
							portNames.add(definitionPortName);
						}
					}
				}
			}
		}

		// TODO for an output port, multiple connections may be allowed. Check if all code supports that.
		// TODO for an output port, do not prevent using that port in an expression (Which is also handled here)
		// even if it already has a connection.
		// -->
		// - check if multiple connections for an output port are allowed
		// - don't filter output ports here
		// - if the above is too complex, consider allowing input port connections only, maybe with a '=' syntax instead of ':'
		// - but then, why use port connections at all? A simple "do (*) {...}" would be enough.
		//   - But more wordy.
		//     - But this happens in Java all the time and doesn't seem to be a problem, especially with autocomplete.
		//     - pro+con at the same time: The assignment syntax reflects signal direction (harder to write, easier to read)
		//       - but code gets read more often than written!
		//     - pro: output signals would need a local signal declaration anyway, even if using a port connection.
		//       - without a connection, not only is there no additional line, but one wouldn't even use a helper signal
		//         in many cases and just use the instance port expression directly -> even less complexity.
		//       - so for output signals, connections are a clear loss anyway.
		//     - for input signals, a connection would use the same value expression as an assignment to the
		//       instance port expression. So no gain here but also no loss.
		//       - but less wordy (discussion above) for port connections
		//     - for structural descriptions, using assignments only keeps all code "between" instances --> very
		//       readable and concise "wiring-style" code, like building objects with references in Java by setting
		//       public fields.
		//     - readability without input port connections is roughly equal to with input port connections, but
		//       - syntax is simplified (fewer special syntaxes)
		//       - less choice in the way things are expressed
		//       - no syntactic distinction between input ports and output ports


		// remove port names for which a connection already exists. For now, don't remove ports which are used in
		// assignments or expressions outside the instance definition -- we don't know yet if that actually helps or if
		// it rather confuses the user.
		ImplementationItem_ModuleInstance moduleInstance = resolveModuleInstance();
		if (moduleInstance != null) {
			for (PortConnection portConnection : moduleInstance.getPortConnections().getAll()) {
				if (portConnection instanceof PortConnection_Valid) {
					portNames.remove(((PortConnection_Valid) portConnection).getPortName().getIdentifier().getText());
				}
			}
		}

		return portNames.toArray();
	}

	@Override
	public boolean isSoft() {
		return false;
	}

}
