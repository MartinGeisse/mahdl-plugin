package name.martingeisse.mahdl.plugin.input;

import com.intellij.psi.tree.IElementType;
import name.martingeisse.mahdl.plugin.MahdlLanguage;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class MahdlElementType extends IElementType {

	public MahdlElementType(@NotNull String debugName) {
		super(debugName, MahdlLanguage.INSTANCE);
	}

}
