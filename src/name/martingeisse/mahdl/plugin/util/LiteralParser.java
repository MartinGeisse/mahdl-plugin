/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.util;

import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;
import name.martingeisse.mahdl.plugin.processor.expression.ConstantValue;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class LiteralParser {

	private static final Pattern VECTOR_PATTERN = Pattern.compile("([0-9]+)([bodh])([0-9]+)");

	// prevent instantiation
	private LiteralParser() {
	}

	public static ConstantValue parseLiteral(@NotNull Expression_Literal literalExpression) throws ParseException {
		return parseLiteral(literalExpression.getLiteral());
	}

	public static ConstantValue parseLiteral(@NotNull Literal literal) throws ParseException {
		try {
			if (literal instanceof Literal_Vector) {
				return parseVector(((Literal_Vector) literal).getValue().getText());
			} else if (literal instanceof Literal_Integer) {
				String text = ((Literal_Integer) literal).getValue().getText();
				return new ConstantValue.Integer(new BigInteger(text));
			} else if (literal instanceof Literal_Text) {
				return parseText(((Literal_Text) literal).getValue());
			} else {
				throw new ParseException("unknown literal type");
			}
		} catch (ParseException e) {
			throw e;
		} catch (Exception e) {
			throw new ParseException(e.getMessage());
		}
	}

	@NotNull
	public static ConstantValue.Vector parseVector(@NotNull String literalText) throws ParseException {
		Matcher matcher = VECTOR_PATTERN.matcher(literalText);
		if (!matcher.matches()) {
			throw new ParseException("malformed vector");
		}
		int size = Integer.parseInt(matcher.group(1));
		char radixCode = matcher.group(2).charAt(0);
		String digits = matcher.group(3);
		int radix = radixCode == 'b' ? 2 : radixCode == 'o' ? 8 : radixCode == 'd' ? 10 : radixCode == 'h' ? 16 : 0;
		if (radix == 0) {
			throw new ParseException("unknown radix '" + radixCode);
		}
		final BigInteger integerValue = new BigInteger(digits, radix);
		if (integerValue.bitLength() > size) {
			throw new ParseException("vector literal contains a value larger than its sepcified size");
		}
		return new ConstantValue.Vector(size, integerValue);
	}

	@NotNull
	private static ConstantValue parseText(@NotNull LeafPsiElement textElement) throws ParseException {
		String rawText = textElement.getText();
		if (rawText.charAt(0) != '"' || rawText.charAt(rawText.length() - 1) != '"') {
			throw new ParseException("missing quotation marks");
		}
		StringBuilder builder = new StringBuilder();
		boolean escape = false;
		for (int i = 1; i < rawText.length() - 1; i++) {
			char c = rawText.charAt(i);
			if (escape) {
				// escapes are not supported (yet), and it's not clear whether we need them
				throw new ParseException("text escape sequences are not supported yet");
			} else if (c == '\\') {
				escape = true;
			} else {
				builder.append(c);
			}
		}
		if (escape) {
			throw new ParseException("unterminated escape sequence");
		}
		return new ConstantValue.Text(builder.toString());
	}

	public static final class ParseException extends Exception {
		public ParseException(String message) {
			super(message);
		}
	}

}
