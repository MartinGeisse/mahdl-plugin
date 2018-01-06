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

public final class PortDefinitionGroup extends ASTWrapperPsiElement  {

    public PortDefinitionGroup(@NotNull ASTNode node) {
        super(node);
    }

        public PortDirection getDirection() {
            return (PortDirection)InternalPsiUtil.getChild(this, 0);
        }
        public DataType getDataType() {
            return (DataType)InternalPsiUtil.getChild(this, 1);
        }
        public ListNode<PortDefinition> getDefinitions() {
            return (ListNode<PortDefinition>)InternalPsiUtil.getChild(this, 2);
        }
    
			
	
	
	
}
