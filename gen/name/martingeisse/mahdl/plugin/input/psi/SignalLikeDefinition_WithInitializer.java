package name.martingeisse.mahdl.plugin.input.psi;

import com.intellij.lang.ASTNode;
import com.intellij.lang.LightPsiParser;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.TokenType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import org.jetbrains.annotations.NotNull;
import com.intellij.util.IncorrectOperationException;
import com.intellij.psi.PsiReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;

public final class SignalLikeDefinition_WithInitializer extends SignalLikeDefinition  {

    public SignalLikeDefinition_WithInitializer(@NotNull ASTNode node) {
        super(node);
    }

        public LeafPsiElement getIdentifier() {
            return (LeafPsiElement)InternalPsiUtil.getChild(this, 0);
        }
        public Expression getInitializer() {
            return (Expression)InternalPsiUtil.getChild(this, 2);
        }
    
		
        public LeafPsiElement getNameIdentifier() {
			return name.martingeisse.mahdl.plugin.input.psi.PsiUtil.getNameIdentifier(this);
        }

		
		public String getName() {
			LeafPsiElement nameIdentifier = getNameIdentifier();
			return (nameIdentifier == null ? null : nameIdentifier.getText());
		}

		public PsiElement setName(String newName) throws IncorrectOperationException {
			LeafPsiElement nameIdentifier = getNameIdentifier();
			if (nameIdentifier == null) {
				throw new IncorrectOperationException("name identifier not found");
			}
			return (LeafPsiElement) nameIdentifier.replaceWithText(newName);
		}

	
	
	
			public void delete() throws IncorrectOperationException {
			name.martingeisse.mahdl.plugin.input.psi.PsiUtil.delete(this);
		}
	
}
