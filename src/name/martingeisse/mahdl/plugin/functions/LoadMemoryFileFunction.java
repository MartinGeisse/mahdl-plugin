/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.functions;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import name.martingeisse.mahdl.plugin.processor.ErrorHandler;
import name.martingeisse.mahdl.plugin.processor.expression.ConstantValue;
import name.martingeisse.mahdl.plugin.processor.expression.ProcessedExpression;
import name.martingeisse.mahdl.plugin.processor.type.ProcessedDataType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 *
 */
public abstract class LoadMemoryFileFunction extends FixedSignatureFunction {

	public LoadMemoryFileFunction(ImmutableList<ProcessedDataType> argumentTypes) {
		super(ImmutableList.of(
			ProcessedDataType.Text.INSTANCE,
			ProcessedDataType.Integer.INSTANCE,
			ProcessedDataType.Integer.INSTANCE
		));
	}

	@NotNull
	@Override
	protected ProcessedDataType internalCheckType(@NotNull List<ProcessedExpression> arguments, ErrorHandler errorHandler) {
		ProcessedExpression.FormallyConstantEvaluationContext context = new ProcessedExpression.FormallyConstantEvaluationContext(errorHandler);
		int firstSize = arguments.get(1).evaluateFormallyConstant(context).convertToInteger().intValueExact();
		int secondSize = arguments.get(1).evaluateFormallyConstant(context).convertToInteger().intValueExact();
		return new ProcessedDataType.Memory(firstSize, secondSize);
	}

	@NotNull
	@Override
	public ConstantValue applyToConstantValues(@NotNull PsiElement errorSource, @NotNull List<ConstantValue> arguments, @NotNull ProcessedExpression.FormallyConstantEvaluationContext context) {
		String filename = arguments.get(0).convertToString();
		int firstSize = arguments.get(1).convertToInteger().intValueExact();
		int secondSize = arguments.get(2).convertToInteger().intValueExact();

		// locate the file
		VirtualFile file = locateFile(errorSource, filename, context);
		if (file == null) {
			return ConstantValue.Unknown.INSTANCE;
		}

		// read the file
		ConstantValue value;
		try (InputStream inputStream = file.getInputStream()) {
			value = parseFileContents(inputStream, firstSize, secondSize, errorSource, context);
		} catch (IOException e) {
			return context.error(errorSource, e.toString());
		}

		// Make sure the file contents correspond to the return type. This fills with zeroes if there are too few rows,
		// but fails if there are too few columns or too many rows or columns.
		if (value instanceof ConstantValue.Unknown) {
			return value;
		}
		if (!(value instanceof ConstantValue.Memory)) {
			return context.error(errorSource, "file loader returned value of type " + value.getDataTypeFamily());
		}
		ConstantValue.Memory memoryValue = (ConstantValue.Memory) value;
		if (memoryValue.getSecondSize() != secondSize) {
			return context.error(errorSource, "file loader returned memory with cell size " +
				memoryValue.getSecondSize() + ", expected " + secondSize);
		}
		if (memoryValue.getFirstSize() > firstSize) {
			return context.error(errorSource, "file loader returned memory with " + memoryValue.getFirstSize() +
				"rows, expected at most " + firstSize);
		}
		return new ConstantValue.Memory(firstSize, secondSize, memoryValue.getBits());

	}

	private VirtualFile locateFile(@NotNull PsiElement anchor, String filename, @NotNull ProcessedExpression.FormallyConstantEvaluationContext context) {
		if (filename.indexOf('/') != -1 || filename.startsWith(".")) {
			context.error(anchor, "invalid filename: " + filename);
			return null;
		}
		PsiFile psiFile = anchor.getContainingFile();
		if (psiFile == null) {
			context.error(anchor, "element is not inside a PsiFile");
			return null;
		}
		VirtualFile containingFile = psiFile.getVirtualFile();
		if (containingFile == null) {
			context.error(anchor, "element is not inside a VirtualFile");
			return null;
		}
		VirtualFile folder = containingFile.getParent();
		VirtualFile file = folder.findChild(filename);
		if (file == null) {
			context.error(anchor, "file not found: " + filename);
			return null;
		}
		return file;
	}

	protected abstract ConstantValue parseFileContents(@NotNull InputStream inputStream,
													   int firstSize,
													   int secondSize,
													   @NotNull PsiElement errorSource,
													   @NotNull ProcessedExpression.FormallyConstantEvaluationContext context);

}
