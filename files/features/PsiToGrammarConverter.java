package name.martingeisse.verilog.plugin.input;

import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mapag.grammar.extended.*;
import name.martingeisse.mapag.grammar.extended.expression.*;
import name.martingeisse.mapag.ide.MapagSourceFile;
import name.martingeisse.mapag.input.psi.*;

/**
 * Converts the PSI to an extended grammar.
 */
public class PsiToGrammarConverter {

	private final boolean failOnErrors;
	private final GrammarToPsiMap backMap = new GrammarToPsiMap();

	public PsiToGrammarConverter(boolean failOnErrors) {
		this.failOnErrors = failOnErrors;
	}

	public Grammar convert(MapagSourceFile mapagSourceFile) {
		name.martingeisse.mapag.input.psi.Grammar psiGrammar = mapagSourceFile.getGrammar();
		if (psiGrammar == null) {
			throw new RuntimeException("could not find grammar PSI node");
		}
		return convert(psiGrammar);
	}

	public GrammarToPsiMap getBackMap() {
		return backMap;
	}

	public Grammar convert(name.martingeisse.mapag.input.psi.Grammar psiGrammar) {

		return null;
	}

	// prevents calling .getText() on non-leaf PSI nodes by accident
	private static String getText(LeafPsiElement element) {
		return element.getText();
	}

}
