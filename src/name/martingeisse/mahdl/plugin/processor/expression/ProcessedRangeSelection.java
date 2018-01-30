package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.Expression;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;

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

	@NotNull
	private ConstantValue evaluateRangeSelectionFixed(@NotNull Expression_RangeSelectionFixed rangeSelection) {
		return evaluateRangeSelectionHelper(rangeSelection.getContainer(), rangeSelection.getFrom(), rangeSelection.getTo(), 0);
	}

	private ConstantValue evaluateRangeSelectionHelper(Expression container, Expression from, Expression other, int direction) {
		ConstantValue containerValue = evaluate(container);
		int containerSize = handleContainerValue(container, containerValue, false, "range-select");
		String containerTypeText = containerValue.getDataType().toString();
		int intFrom = handleIndexValue(containerSize, containerTypeText, from);
		int intTo = (direction == 0) ? handleIndexValue(containerSize, containerTypeText, other) : null;
		if (containerSize < 0 || intFrom < 0 || intTo < 0) {
			return ConstantValue.Unknown.INSTANCE;
		} else {
			// all error cases should be handled above and should have reported an error already, so we don't have to do that here
			return containerValue.selectRange(intFrom, intTo);
		}
	}

}
