/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.processor.statement;

import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.input.psi.Expression;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;
import name.martingeisse.mahdl.plugin.processor.expression.TypeErrorException;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public final class ProcessedAssignment extends ProcessedStatement {

	private final ProcessedExpression leftHandSide;
	private final ProcessedExpression rightHandSide;

	public ProcessedAssignment(PsiElement errorSource, ProcessedExpression leftHandSide, ProcessedExpression rightHandSide) throws TypeErrorException {
		super(errorSource);
		if (!(leftHandSide.getDataType() instanceof ProcessedDataType.Unknown)) {
			if (!(rightHandSide.getDataType() instanceof ProcessedDataType.Unknown)) {
				if (!leftHandSide.getDataType().equals(rightHandSide.getDataType())) {
					throw new TypeErrorException();
				}
			}
		}
		this.leftHandSide = leftHandSide;
		this.rightHandSide = rightHandSide;
	}

	public ProcessedExpression getLeftHandSide() {
		return leftHandSide;
	}

	public ProcessedExpression getRightHandSide() {
		return rightHandSide;
	}

}
