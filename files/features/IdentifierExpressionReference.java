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

		Grammar grammar = PsiUtil.getAncestor(expression, Grammar.class);
		if (grammar == null) {
			return null;
		}

		String identifier = expression.getIdentifier().getText();

		// terminals
		for (TerminalDeclaration terminalDeclaration : grammar.getTerminalDeclarations().getIdentifiers().getAll()) {
			String terminalName = terminalDeclaration.getName();
			if (terminalName != null && terminalName.equals(identifier)) {
				return terminalDeclaration;
			}
		}

		// nonterminals / productions
		for (Production production : grammar.getProductions().getAll()) {
			String nonterminalName = production.getName();
			if (nonterminalName != null && nonterminalName.equals(identifier)) {
				return production;
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

		if (psiElement instanceof TerminalDeclaration) {
			String newName = ((TerminalDeclaration) psiElement).getName();
			if (newName != null) {
				return PsiUtil.setText(expression.getIdentifier(), newName);
			}
		}

		if (psiElement instanceof Production) {
			String newName = ((Production) psiElement).getName();
			if (newName != null) {
				return PsiUtil.setText(expression.getIdentifier(), newName);
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

		if (psiElement instanceof Production) {
			String productionName = ((Production) psiElement).getName();
			if (productionName != null) {
				String id = getCanonicalText();
				if (id.equals(productionName)) {
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

		Grammar grammar = PsiUtil.getAncestor(expression, Grammar.class);
		if (grammar == null) {
			return new Object[0];
		}

		List<Object> variants = new ArrayList<>();

		// terminals
		for (TerminalDeclaration more : grammar.getTerminalDeclarations().getIdentifiers().getAll()) {
			variants.add(more.getIdentifier().getText());
		}

		// nonterminals
		for (Production production : grammar.getProductions().getAll()) {
			PsiElement nameIdentifier = production.getNameIdentifier();
			if (nameIdentifier != null) {
				variants.add(nameIdentifier.getText());
			}
		}

		return variants.toArray();
	}

	@Override
	public boolean isSoft() {
		return false;
	}

}
