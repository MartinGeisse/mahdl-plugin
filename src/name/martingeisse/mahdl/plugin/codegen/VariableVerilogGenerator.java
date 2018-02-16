/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.codegen;

import com.google.common.collect.ImmutableMap;
import name.martingeisse.mahdl.plugin.processor.expression.*;

/**
 * This class is like {@link ExpressionVerilogGenerator} but for L-expressions. The differences in principle are:
 * - different extraction rules
 * - extracted signals use the opposite assignment direction
 *
 * Note that even L-expression can be very complex, such as in the assignment
 *
 * 		register vector[7:0] x;
 * 		register vector[7:0] y;
 * 		signal vector[7:0] foo;
 *
 * 		...
 *
 * 		(x _ y)[11:4] = foo;
 *
 * In this case, the concatenation (x _ y) will be extracted to a synthetic signal.
 *
 * This class uses the {@link ExpressionVerilogGenerator} to generate nested R-expressions inside L-expressions,
 * such as selection indices.
 */
public final class VariableVerilogGenerator {

	public static final int NESTING_TOPLEVEL = 0;
	public static final int NESTING_INSIDE_OPERATION = 1;
	public static final int NESTING_INSIDE_SELECTION = 2;
	public static final int NESTING_IDENTIFIER_ONLY = 3;

	private static final ImmutableMap<Class<? extends ProcessedExpression>, Integer> EXTRACTION_NEEDED_NESTING_LEVELS;

	static {
		ImmutableMap.Builder builder = ImmutableMap.builder();

		// operations
		builder.put(ProcessedBinaryOperation.class, NESTING_INSIDE_OPERATION);

		// selection
		builder.put(ProcessedIndexSelection.class, NESTING_INSIDE_SELECTION);
		builder.put(ProcessedRangeSelection.class, NESTING_INSIDE_SELECTION);

		// primary
		builder.put(InstancePortReference.class, NESTING_IDENTIFIER_ONLY);

		EXTRACTION_NEEDED_NESTING_LEVELS = builder.build();
	}

	private final Extractor extractor;
	private final ExpressionVerilogGenerator expressionVerilogGenerator;

	public VariableVerilogGenerator(Extractor extractor, ExpressionVerilogGenerator expressionVerilogGenerator) {
		this.extractor = extractor;
		this.expressionVerilogGenerator = expressionVerilogGenerator;
	}

	/**
	 * Generates the code for the specified L-expression to the builder, writing helper signals to the output as needed.
	 */
	public void generate(ProcessedExpression expression, StringBuilder builder) {
		generate(expression, builder, NESTING_TOPLEVEL);
	}

	/**
	 * Generates the code for the specified L-expression to the builder, writing helper signals to the output as needed.
	 * Any expressions that conflict with the specified current nesting level will be extracted.
	 */
	public void generate(ProcessedExpression expression, StringBuilder builder, int nesting) {
		// TODO
	}

	//
	// extraction
	//

	private void extract(ProcessedExpression expression, StringBuilder builder) {
		if (expression instanceof SignalLikeReference) {
			builder.append(((SignalLikeReference) expression).getDefinition().getName());
		} else if (expression instanceof SyntheticSignalLikeExpression) {
			builder.append(((SyntheticSignalLikeExpression) expression).getName());
		} else {
			builder.append(extractor.extract(expression));
		}
	}

	public interface Extractor {
		String extract(ProcessedExpression expression);
	}

}
