package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public abstract class ProcessedRangeSelection extends ProcessedExpression {

	private final ProcessedExpression container;
	private final ProcessedExpression fromIndex;
	private final ProcessedExpression toIndex;

	private ProcessedRangeSelection(PsiElement errorSource,
									ProcessedDataType dataType,
									ProcessedExpression container,
									ProcessedExpression index) {
		super(errorSource, dataType);
		this.container = container;
		this.index = index;
	}

	public ProcessedExpression getContainer() {
		return container;
	}

	public ProcessedExpression getIndex() {
		return index;
	}

	public static final class BitFromVector extends ProcessedRangeSelection {

		public BitFromVector(PsiElement errorSource, ProcessedExpression container, ProcessedExpression index) throws TypeErrorException {
			super(errorSource, ProcessedDataType.Bit.INSTANCE, container, index);
			if (!(container.getDataType() instanceof ProcessedDataType.Vector)) {
				throw new TypeErrorException();
			}
			ProcessedDataType.Vector containerType = (ProcessedDataType.Vector) container.getDataType();
			if (index.getDataType() instanceof ProcessedDataType.Vector) {
				ProcessedDataType.Vector indexType = (ProcessedDataType.Vector) index.getDataType();
				if (containerType.getSize() < (1 << indexType.getSize())) {
					throw new TypeErrorException();
				}
			} else if (!(index.getDataType() instanceof ProcessedDataType.Integer)) {
				throw new TypeErrorException();
			}
		}

	}

	public static final class VectorFromMemory extends ProcessedRangeSelection {

		public VectorFromMemory(PsiElement errorSource, ProcessedExpression container, ProcessedExpression index) throws TypeErrorException {
			super(errorSource, typeCheck(container, index), container, index);
		}

		private static ProcessedDataType typeCheck(ProcessedExpression container, ProcessedExpression index) throws TypeErrorException {
			if (!(container.getDataType() instanceof ProcessedDataType.Memory)) {
				throw new TypeErrorException();
			}
			ProcessedDataType.Memory containerType = (ProcessedDataType.Memory) container.getDataType();
			if (index.getDataType() instanceof ProcessedDataType.Vector) {
				ProcessedDataType.Vector indexType = (ProcessedDataType.Vector) index.getDataType();
				if (containerType.getFirstSize() < (1 << indexType.getSize())) {
					throw new TypeErrorException();
				}
			} else if (!(index.getDataType() instanceof ProcessedDataType.Integer)) {
				throw new TypeErrorException();
			}
			return new ProcessedDataType.Vector(containerType.getSecondSize());
		}

	}

}
