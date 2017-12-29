package name.martingeisse.mahdl.plugin;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 *
 */
public class MahdlFileType extends LanguageFileType {

	@NonNls
	public static final String DEFAULT_EXTENSION = "mahdl";

	public static final MahdlFileType INSTANCE = new MahdlFileType();

	public MahdlFileType() {
		super(MahdlLanguage.INSTANCE);
	}

	@NotNull
	@Override
	public String getName() {
		return "MAHDL";
	}

	@NotNull
	@Override
	public String getDescription() {
		return MahdlBundle.message("filetype.description.mahdl");
	}

	@NotNull
	@Override
	public String getDefaultExtension() {
		return DEFAULT_EXTENSION;
	}

	@Nullable
	@Override
	public Icon getIcon() {
		return Icons.MAHDL_FILE;
	}
}
