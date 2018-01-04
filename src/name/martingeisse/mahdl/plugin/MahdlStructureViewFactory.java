package name.martingeisse.mahdl.plugin;

import com.intellij.ide.structureView.*;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import name.martingeisse.mahdl.plugin.input.psi.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO commented out for now
 */
public class MahdlStructureViewFactory implements PsiStructureViewFactory {

	private static boolean shouldInclude(PsiElement element) {
		// TODO
//		if (element instanceof Grammar_TerminalDeclarations) {
//			return true;
//		}
//		if (element instanceof Grammar_PrecedenceTable) {
//			return true;
//		}
//		if (element instanceof Production) {
//			return true;
//		}
		return false;
	}

	@Nullable
	@Override
	public StructureViewBuilder getStructureViewBuilder(@NotNull PsiFile psiFile) {
		if (!(psiFile instanceof MahdlSourceFile)) {
			return null;
		}
		Module module = ((MahdlSourceFile) psiFile).getModule();
		if (module == null) {
			return null;
		}
		return new TreeBasedStructureViewBuilder() {
			@NotNull
			@Override
			public StructureViewModel createStructureViewModel(@Nullable Editor editor) {
				return new TextEditorBasedStructureViewModel(editor, psiFile) {

					@NotNull
					@Override
					public StructureViewTreeElement getRoot() {
						return new MyStructureViewElement(module);
					}

					@NotNull
					@Override
					protected Class[] getSuitableClasses() {
						throw new UnsupportedOperationException();
					}

					@Override
					protected boolean isSuitable(PsiElement element) {
						return shouldInclude(element);
					}
				};
			}
		};
	}

	public static class MyStructureViewElement implements StructureViewTreeElement, ItemPresentation {

		private final PsiElement element;

		public MyStructureViewElement(PsiElement element) {
			this.element = element;
		}

		@Override
		public Object getValue() {
			return element;
		}

		@NotNull
		@Override
		public ItemPresentation getPresentation() {
			return this;
		}

		@Nullable
		@Override
		public String getPresentableText() {
			// TODO
//			if (element instanceof Grammar_TerminalDeclarations) {
//				return "%terminals {...}";
//			}
//			if (element instanceof Grammar_PrecedenceTable) {
//				return "%precedence {...}";
//			}
//			if (element instanceof Production) {
//				String name = ((Production) element).getName();
//				return (name == null ? "(production)" : name);
//			}
			return element.toString();
		}

		@Nullable
		@Override
		public String getLocationString() {
			return null;
		}

		@Nullable
		@Override
		public Icon getIcon(boolean b) {
			return null;
		}

		@NotNull
		@Override
		public TreeElement[] getChildren() {
			List<TreeElement> children = new ArrayList<>();
			checkIncludeChildrenOf(element, children);
			return children.toArray(new TreeElement[children.size()]);
		}

		private static void checkIncludeChildrenOf(PsiElement psiParent, List<TreeElement> destination) {
			for (PsiElement psiChild : psiParent.getChildren()) {
				if (shouldInclude(psiChild)) {
					destination.add(new MyStructureViewElement(psiChild));
				} else {
					checkIncludeChildrenOf(psiChild, destination);
				}
			}
		}

		@Override
		public void navigate(boolean requestFocus) {
			((Navigatable) element).navigate(requestFocus);
		}

		@Override
		public boolean canNavigate() {
			return element instanceof Navigatable && ((Navigatable) element).canNavigate();
		}

		@Override
		public boolean canNavigateToSource() {
			return canNavigate();
		}

	}
}
