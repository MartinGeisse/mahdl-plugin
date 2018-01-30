package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public final class ProcessedRangeSelection extends ProcessedExpression {

	private final ProcessedExpression container;
	private final int fromIndex;
	private final int toIndex;

	public ProcessedRangeSelection(PsiElement errorSource,
									ProcessedDataType dataType,
									ProcessedExpression container,
									int fromIndex,
									int toIndex) throws TypeErrorException {
		super(errorSource, dataType);
		if (!(container.getDataType() instanceof ProcessedDataType.Vector)) {
			throw new TypeErrorException();
		}
		if (toIndex < 0 || fromIndex < toIndex || fromIndex >= ((ProcessedDataType.Vector) container.getDataType()).getSize()) {
			throw new TypeErrorException();
		}
		this.container = container;
		this.fromIndex = fromIndex;
		this.toIndex = toIndex;
	}

	public ProcessedExpression getContainer() {
		return container;
	}

	public int getFromIndex() {
		return fromIndex;
	}

	public int getToIndex() {
		return toIndex;
	}

	@Override
	protected ConstantValue evaluateFormallyConstantInternal(FormallyConstantEvaluationContext context) {
		return container.evaluateFormallyConstant(context).selectRange(fromIndex, toIndex);
	}

}
