package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.definition.SignalLike;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public final class SignalLikeReference extends ProcessedExpression {

	private final SignalLike definition;

	public SignalLikeReference(PsiElement errorSource, SignalLike definition) {
		super(errorSource, definition.getProcessedDataType());
		this.definition = definition;
	}

	public SignalLike getDefinition() {
		return definition;
	}

}
