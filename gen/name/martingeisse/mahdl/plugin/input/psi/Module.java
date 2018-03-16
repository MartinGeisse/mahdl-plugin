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

public final class Module extends ASTWrapperPsiElement implements PsiNamedElement,PsiNameIdentifierOwner {

    public Module(@NotNull ASTNode node) {
        super(node);
    }

        public Optional<LeafPsiElement> getNativeness() {
            return (Optional<LeafPsiElement>)InternalPsiUtil.getChild(this, 0);
        }
        public QualifiedModuleName getModuleName() {
            return (QualifiedModuleName)InternalPsiUtil.getChild(this, 2);
        }
        public ListNode<PortDefinitionGroup> getPortDefinitionGroups() {
            return (ListNode<PortDefinitionGroup>)InternalPsiUtil.getChild(this, 6);
        }
        public ListNode<ImplementationItem> getImplementationItems() {
            return (ListNode<ImplementationItem>)InternalPsiUtil.getChild(this, 8);
        }
    
	
		public String getName() {
			return name.martingeisse.mahdl.plugin.input.psi.PsiUtil.getName(this);
		}

		public PsiElement setName(String newName) throws IncorrectOperationException {
			return name.martingeisse.mahdl.plugin.input.psi.PsiUtil.setName(this, newName);
		}

		
	    
            public PsiElement getNameIdentifier() {
                return name.martingeisse.mahdl.plugin.input.psi.PsiUtil.getNameIdentifier(this);
            }

        
	
	
			public void superclassDelete() throws IncorrectOperationException {
			super.delete();
		}
	
			public void delete() throws IncorrectOperationException {
			name.martingeisse.mahdl.plugin.input.psi.PsiUtil.delete(this);
		}
	
}
