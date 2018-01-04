package name.martingeisse.mahdl.plugin;

import com.intellij.lang.CodeDocumentationAwareCommenterEx;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import name.martingeisse.mahdl.plugin.input.Symbols;
import org.jetbrains.annotations.Nullable;

public class MahdlCommenter implements CodeDocumentationAwareCommenterEx {

	@Override
	public boolean isDocumentationCommentText(PsiElement element) {
		return false;
	}

	@Nullable
	@Override
	public IElementType getLineCommentTokenType() {
		return Symbols.LINE_COMMENT;
	}

	@Nullable
	@Override
	public IElementType getBlockCommentTokenType() {
		return Symbols.BLOCK_COMMENT;
	}

	@Nullable
	@Override
	public IElementType getDocumentationCommentTokenType() {
		return null;
	}

	@Nullable
	@Override
	public String getDocumentationCommentPrefix() {
		return "/**";
	}

	@Nullable
	@Override
	public String getDocumentationCommentLinePrefix() {
		return "*";
	}

	@Nullable
	@Override
	public String getDocumentationCommentSuffix() {
		return "*/";
	}

	@Override
	public boolean isDocumentationComment(PsiComment psiComment) {
		return false;
	}

	@Nullable
	@Override
	public String getLineCommentPrefix() {
		return "//";
	}

	@Nullable
	@Override
	public String getBlockCommentPrefix() {
		return "/*";
	}

	@Nullable
	@Override
	public String getBlockCommentSuffix() {
		return "*/";
	}

	@Nullable
	@Override
	public String getCommentedBlockCommentPrefix() {
		return null;
	}

	@Nullable
	@Override
	public String getCommentedBlockCommentSuffix() {
		return null;
	}
}
