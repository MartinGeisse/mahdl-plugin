package name.martingeisse.mahdl.plugin.input.psi;

import com.intellij.extapi.psi.ASTDelegatePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;

import java.util.function.Consumer;

public final class InternalPsiUtil {

	private static TokenSet ignoredElementTypes;

	private static void initializeIgnoredElementTypes() {
		if (ignoredElementTypes == null) {
            name.martingeisse.mahdl.plugin.input.MahdlParserDefinition parserDefinition = new name.martingeisse.mahdl.plugin.input.MahdlParserDefinition();
			ignoredElementTypes = TokenSet.orSet(parserDefinition.getWhitespaceTokens(), parserDefinition.getCommentTokens());
		}
	}

	// prevent instantiation
	private InternalPsiUtil() {
	}

	/**
	 * This method works similar to parent.getChildren()[childIndex], except that it deals with all nodes, not just
	 * subclasses of CompositeElement.
	 */
	public static PsiElement getChild(ASTDelegatePsiElement parent, int childIndex) {
		initializeIgnoredElementTypes();
		PsiElement child = skipWhitespace(parent.getFirstChild());
		for (int i = 0; i < childIndex; i++) {
			child = skipWhitespace(child.getNextSibling());
		}
		return child;
	}

	/**
	 * Invokes the specified consumer on the children returned by getChild().
	 */
	public static void foreachChild(ASTDelegatePsiElement parent, Consumer<PsiElement> consumer) {
		initializeIgnoredElementTypes();
		PsiElement child = skipWhitespace(parent.getFirstChild());
		while (child != null) {
			consumer.accept(child);
			child = skipWhitespace(child.getNextSibling());
		}
	}

	private static PsiElement skipWhitespace(PsiElement element) {
		while (element != null && ignoredElementTypes.contains(element.getNode().getElementType())) {
			element = element.getNextSibling();
		}
		return element;
	}

}
