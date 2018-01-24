package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public final class ProcessedRangeSelection extends ProcessedExpression {

	private final ProcessedExpression container;
	private final ProcessedExpression fromIndex;
	private final ProcessedExpression toIndex;

	private ProcessedRangeSelection(PsiElement errorSource,
									ProcessedDataType dataType,
									ProcessedExpression container,
									ProcessedExpression fromIndex,
									ProcessedExpression toIndex) throws TypeErrorException {
		super(errorSource, dataType);
		if (!(container.getDataType() instanceof ProcessedDataType.Vector)) {
			throw new TypeErrorException();
		}
		if (!(fromIndex.getDataType() instanceof ProcessedDataType.Integer)) {
			throw new TypeErrorException();
		}
		if (!(toIndex.getDataType() instanceof ProcessedDataType.Integer)) {
			throw new TypeErrorException();
		}
		this.container = container;
		this.fromIndex = fromIndex;
		this.toIndex = toIndex;
	}

	public ProcessedExpression getContainer() {
		return container;
	}

	public ProcessedExpression getFromIndex() {
		return fromIndex;
	}

	public ProcessedExpression getToIndex() {
		return toIndex;
	}

}
