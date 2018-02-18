/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.functions;

import com.google.common.collect.ImmutableList;
import com.intellij.psi.PsiElement;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.expression.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.BitSet;
import java.util.List;

/**
 *
 */
public abstract class StringEncodingFunction extends FixedSignatureFunction {

	public StringEncodingFunction() {
		super(ImmutableList.of(
			ProcessedDataType.Text.INSTANCE,
			ProcessedDataType.Integer.INSTANCE
		));
	}

	@NotNull
	@Override
	protected ProcessedDataType internalCheckType(@NotNull List<ProcessedExpression> arguments, ErrorHandler errorHandler) {
		int size = arguments.get(1).evaluateFormallyConstant(new ProcessedExpression.FormallyConstantEvaluationContext(errorHandler)).
			convertToInteger().intValueExact();
		return new ProcessedDataType.Memory(size, 8);
	}

	@NotNull
	@Override
	public ConstantValue applyToConstantValues(@NotNull PsiElement errorSource, @NotNull List<ConstantValue> arguments, @NotNull ProcessedExpression.FormallyConstantEvaluationContext context) {
		String text = arguments.get(0).convertToString();
		int size = arguments.get(1).convertToInteger().intValueExact();
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try {
			encode(text, byteArrayOutputStream);
		} catch (IOException e) {
			return context.error(errorSource, e.toString());
		}
		if (byteArrayOutputStream.size() > size) {
			return context.error(errorSource, "encoded text is " + byteArrayOutputStream.size() +
				" bytes, but target size is only " + size + " bytes");
		}
		byte[] data = byteArrayOutputStream.toByteArray();
		BitSet bits = new BitSet();
		for (int i = 0; i < data.length; i++) {
			byte b = data[i];
			for (int j = 0; j < 8; j++) {
				if ((b & 128) != 0) {
					bits.set(8 * i + j);
				}
				b = (byte) (b << 1);
			}
		}
		return new ConstantValue.Memory(size, 8, bits);
	}

	protected abstract void encode(String text, OutputStream out) throws IOException;

}
