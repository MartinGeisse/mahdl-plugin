/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.expression;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public final class ProcessedRangeSelection extends ProcessedExpression {

	private final ProcessedExpression container;
	private final int fromIndex;
	private final int toIndex;

	public ProcessedRangeSelection(@NotNull PsiElement errorSource,
								   @NotNull ProcessedDataType dataType,
								   @NotNull ProcessedExpression container,
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

	@NotNull
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
	@NotNull
	protected ConstantValue evaluateFormallyConstantInternal(@NotNull FormallyConstantEvaluationContext context) {
		return container.evaluateFormallyConstant(context).selectRange(fromIndex, toIndex);
	}

	@NotNull
	@Override
	protected ProcessedExpression performSubFolding(@NotNull ErrorHandler errorHandler) {
		ProcessedExpression container = this.container.performFolding(errorHandler);
		if (container != this.container) {
			try {
				return new ProcessedRangeSelection(getErrorSource(), getDataType(), container, fromIndex, toIndex);
			} catch (TypeErrorException e) {
				errorHandler.onError(getErrorSource(), "internal type error during folding of range selection");
				return this;
			}
		} else {
			return this;
		}
	}

}
