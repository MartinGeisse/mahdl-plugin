package name.martingeisse.mahdl.plugin.input;

import com.intellij.lexer.FlexAdapter;

/**
 *
 */
public class MahdlLexer extends FlexAdapter {

	public MahdlLexer() {
		super(new FlexGeneratedMahdlLexer(null));
	}

}
