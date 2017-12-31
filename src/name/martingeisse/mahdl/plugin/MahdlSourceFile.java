package name.martingeisse.mahdl.plugin;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.Module;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class MahdlSourceFile extends PsiFileBase {

	public MahdlSourceFile(@NotNull FileViewProvider viewProvider) {
		super(viewProvider, MahdlLanguage.INSTANCE);
	}

	@NotNull
	@Override
	public FileType getFileType() {
		return MahdlFileType.INSTANCE;
	}

	public Module getModule() {
		for (PsiElement child : getChildren()) {
			if (child instanceof Module) {
				return (Module)child;
			}
		}
		return null;
	}

}
