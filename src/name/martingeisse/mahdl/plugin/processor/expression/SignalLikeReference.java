package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.definition.Constant;
import name.martingeisse.mahdl.plugin.processor.definition.SignalLike;

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

	@Override
	public ConstantValue evaluateFormallyConstant(FormallyConstantEvaluationContext context) {
		String name = definition.getName();
		ConstantValue value = context.getDefinedConstants().get(name);
		if (definition instanceof Constant) {
			if (value == null) {
				return context.error(this, "missing pre-computed value for constant '" + name + "'");
			} else {
				return value;
			}
		} else {
			if (value == null) {
				return context.error(this, "undefined constant: '" + name + "'");
			} else {
				return context.error(this, "found pre-computed constant value for non-constant '" + name + "' -- WTF?");
			}
		}
	}

}
