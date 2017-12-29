package name.martingeisse.verilog.plugin.input;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PrecedenceDeclarationSymbolReference implements PsiReference {

	private final PrecedenceDeclarationSymbol precedenceDeclarationSymbol;

	public PrecedenceDeclarationSymbolReference(PrecedenceDeclarationSymbol precedenceDeclarationSymbol) {
		this.precedenceDeclarationSymbol = precedenceDeclarationSymbol;
	}

	@Override
	public PsiElement getElement() {
		return precedenceDeclarationSymbol;
	}

	@Override
	public TextRange getRangeInElement() {
		return new TextRange(0, getCanonicalText().length());
	}

	@Nullable
	@Override
	public PsiElement resolve() {
		Grammar grammar = PsiUtil.getAncestor(precedenceDeclarationSymbol, Grammar.class);
		if (grammar == null) {
			return null;
		}
		String identifier = precedenceDeclarationSymbol.getIdentifier().getText();
		for (TerminalDeclaration terminalDeclaration : grammar.getTerminalDeclarations().getIdentifiers().getAll()) {
			String terminalName = terminalDeclaration.getName();
			if (terminalName != null && terminalName.equals(identifier)) {
				return terminalDeclaration;
			}
		}
		return null;
	}

	@NotNull
	@Override
	public String getCanonicalText() {
		return precedenceDeclarationSymbol.getIdentifier().getText();
	}

	@Override
	public PsiElement handleElementRename(String newName) throws IncorrectOperationException {
		return PsiUtil.setText(precedenceDeclarationSymbol.getIdentifier(), newName);
	}

	@Override
	public PsiElement bindToElement(@NotNull PsiElement psiElement) throws IncorrectOperationException {
		if (psiElement instanceof TerminalDeclaration) {
			String newName = ((TerminalDeclaration) psiElement).getName();
			if (newName != null) {
				return PsiUtil.setText(precedenceDeclarationSymbol.getIdentifier(), newName);
			}
		}
		throw new IncorrectOperationException();
	}

	@Override
	public boolean isReferenceTo(PsiElement psiElement) {
		if (psiElement instanceof TerminalDeclaration) {
			String terminalName = ((TerminalDeclaration) psiElement).getName();
			if (terminalName != null) {
				String id = getCanonicalText();
				if (id.equals(terminalName)) {
					PsiElement resolved = resolve();
					return (resolved != null && resolved.equals(psiElement));
				}
			}
		}
		return false;
	}

	// note: if this returns PSI elements, they must be PsiNamedElement or contain the name in meta-data
	@NotNull
	@Override
	public Object[] getVariants() {
		Grammar grammar = PsiUtil.getAncestor(precedenceDeclarationSymbol, Grammar.class);
		if (grammar == null) {
			return new Object[0];
		}
		List<Object> variants = new ArrayList<>();
		for (TerminalDeclaration more : grammar.getTerminalDeclarations().getIdentifiers().getAll()) {
			variants.add(more.getIdentifier().getText());
		}
		return variants.toArray();
	}

	@Override
	public boolean isSoft() {
		return false;
	}

}
