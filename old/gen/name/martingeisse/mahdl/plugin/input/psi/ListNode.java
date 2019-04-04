package name.martingeisse.mahdl.plugin.input.psi;

import com.google.common.collect.ImmutableList;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public final class ListNode<T extends PsiElement> extends ASTWrapperPsiElement {

    private final TokenSet elementTypes;
    private final Class<T> elementClass;

    public ListNode(@NotNull ASTNode node, TokenSet elementTypes, Class<T> elementClass) {
        super(node);
        this.elementTypes = elementTypes;
        this.elementClass = elementClass;
    }

    public <S extends PsiElement> ListNode<S> cast(Class<S> subclass) {
        if (!elementClass.isAssignableFrom(subclass)) {
            throw new ClassCastException(subclass.getName() + " is not a subclass of " + elementClass.getName());
        }
        return (ListNode)this;
    }

	public final ImmutableList<T> getAll() {
		ImmutableList.Builder<T> builder = ImmutableList.builder();
		addAllTo(builder);
		return builder.build();
	}

	public final void addAllTo(List<T> list) {
        foreach(list::add);
	}

	public final void addAllTo(ImmutableList.Builder<T> builder) {
        foreach(builder::add);
	}

    public final void foreach(Consumer<T> consumer) {
        InternalPsiUtil.foreachChild(this, child -> {
            if (elementTypes.contains(child.getNode().getElementType())) {
                consumer.accept(elementClass.cast(child));
            } else if (child instanceof ListNode<?> && child.getNode().getElementType() == getNode().getElementType()) {
                ListNode<?> typedChild = (ListNode<?>)child;
                typedChild.cast(elementClass).foreach(consumer);
            }
        });
    }

}
