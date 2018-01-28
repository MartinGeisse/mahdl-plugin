package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.constant.ConstantValue;
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

	private int handleContainerValue(FormallyConstantEvaluationContext context, ConstantValue containerValue) {
		if (containerValue instanceof ConstantValue.Unknown) {
			return -1;
		} else if (containerValue instanceof ConstantValue.Vector) {
			return ((ConstantValue.Vector) containerValue).getSize();
		} else {
			context.evaluationInconsistency(container.getErrorSource(), "range-select found container value " + containerValue);
			return -1;
		}
	}

}
