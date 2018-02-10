/**
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.codegen;

import com.google.common.collect.ImmutableMap;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.functions.StandardFunction;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.expression.*;
import name.martingeisse.mahdl.plugin.processor.statement.*;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;

/**
 *
 */
public final class StatementVerilogGenerator {

	private final ExpressionVerilogGenerator expressionVerilogGenerator;

	public StatementVerilogGenerator(ExpressionVerilogGenerator expressionVerilogGenerator) {
		this.expressionVerilogGenerator = expressionVerilogGenerator;
	}

	/**
	 * Generates the code for the specified do-block to the builder, writing helper signals to the output as needed.
	 */
	public void generate(ProcessedDoBlock doBlock, StringBuilder builder) {
		builder.append("\talways @(");
		if (doBlock.getClock() == null) {
			builder.append('*');
		} else {
			builder.append("posedge ");
			expressionVerilogGenerator.generate(doBlock.getClock(), builder, ExpressionVerilogGenerator.NESTING_IDENTIFIER_ONLY);
		}
		builder.append(") begin\n");
		generate(doBlock.getBody(), builder, 2);
		builder.append("\tend\n");
	}

	/**
	 * Generates the code for the specified statement to the builder, writing helper signals to the output as needed.
	 */
	public void generate(ProcessedStatement statement, StringBuilder builder, int indentation) {

		if (statement instanceof ProcessedBlock) {

			ProcessedBlock block = (ProcessedBlock) statement;
			indent(builder, indentation);
			builder.append("begin\n");
			for (ProcessedStatement childStatement : block.getStatements()) {
				generate(childStatement, builder, indentation + 1);
			}
			indent(builder, indentation);
			builder.append("end\n");

		} else if (statement instanceof ProcessedAssignment) {

			ProcessedAssignment assignment = (ProcessedAssignment) statement;
			indent(builder, indentation);
			expressionVerilogGenerator.generate(assignment.getLeftHandSide(), builder, ExpressionVerilogGenerator.NESTING_INSIDE_SWITCH);
			builder.append(" <= ");
			expressionVerilogGenerator.generate(assignment.getRightHandSide(), builder, ExpressionVerilogGenerator.NESTING_INSIDE_SWITCH);
			builder.append(";\n");

		} else if (statement instanceof ProcessedIf) {

			ProcessedIf processedIf = (ProcessedIf)statement;
			indent(builder, indentation);
			builder.append("if (");
			expressionVerilogGenerator.generate(processedIf.getCondition(), builder, ExpressionVerilogGenerator.NESTING_INSIDE_SWITCH);
			builder.append(") begin\n");
			generate(processedIf.getThenBranch(), builder, indentation + 1);
			if (!(processedIf.getElseBranch() instanceof Nop)) {
				indent(builder, indentation);
				builder.append("end else begin\n");
				generate(processedIf.getElseBranch(), builder, indentation + 1);
			}
			indent(builder, indentation);
			builder.append("end\n");

		} else if (!(statement instanceof Nop)) {
			throw new ModuleCannotGenerateCodeException("unknown statement: " + statement);
		}

	}

	private void indent(StringBuilder builder, int indentation) {
		for (int i = 0; i < indentation; i++) {
			builder.append('\t');
		}
	}

}
