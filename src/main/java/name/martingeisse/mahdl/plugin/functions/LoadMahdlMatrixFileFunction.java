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
import name.martingeisse.mahdl.plugin.util.HeadBodyReader;
import name.martingeisse.mahdl.plugin.util.LiteralParser;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 */
public final class LoadMahdlMatrixFileFunction extends FixedSignatureFunction {

	private static final Pattern ROW_PATTERN = Pattern.compile("[0-9a-fA-F]+");

	public LoadMahdlMatrixFileFunction() {
		super(ImmutableList.of(
			ProcessedDataType.Text.INSTANCE,
			ProcessedDataType.Integer.INSTANCE,
			ProcessedDataType.Integer.INSTANCE
		));
	}

	@NotNull
	@Override
	public String getName() {
		return "loadMatrix";
	}

	@NotNull
	@Override
	protected ProcessedDataType internalCheckType(@NotNull List<ProcessedExpression> arguments, ErrorHandler errorHandler) {
		ProcessedExpression.FormallyConstantEvaluationContext context = new ProcessedExpression.FormallyConstantEvaluationContext(errorHandler);
		int firstSize = arguments.get(1).evaluateFormallyConstant(context).convertToInteger().intValueExact();
		int secondSize = arguments.get(1).evaluateFormallyConstant(context).convertToInteger().intValueExact();
		return new ProcessedDataType.Matrix(firstSize, secondSize);
	}

	@NotNull
	@Override
	public ConstantValue applyToConstantValues(@NotNull PsiElement errorSource, @NotNull List<ConstantValue> arguments, @NotNull ProcessedExpression.FormallyConstantEvaluationContext context) {
		String filename = arguments.get(0).convertToString();
		int rows = arguments.get(1).convertToInteger().intValueExact();
		int columns = arguments.get(2).convertToInteger().intValueExact();

		// locate the file
		VirtualFile file = locateFile(errorSource, filename, context);
		if (file == null) {
			return ConstantValue.Unknown.INSTANCE;
		}

		// read the file
		MutableObject<BitSet> resultBitSetHolder = new MutableObject();
		try (InputStream inputStream = file.getInputStream()) {
			try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
				new HeadBodyReader() {

					private boolean rowsOk = false, columnsOk = false;
					private BitSet bits;
					private int firstEmptyBodyLine = -1;

					@Override
					protected void onHeadProperty(String key, String value) throws FormatException {
						switch (key) {

							case "rows":
								if (expectNonNegativeInteger(key, value) != rows) {
									throw new FormatException("mismatching number of rows");
								}
								rowsOk = true;
								break;

							case "columns":
								if (expectNonNegativeInteger(key, value) != columns) {
									throw new FormatException("mismatching number of columns");
								}
								columnsOk = true;
								break;

							default:
								throw new FormatException("unknown property: " + key);

						}
					}

					@Override
					protected void onStartBody() throws FormatException {
						if (!rowsOk) {
							throw new FormatException("missing 'rows' property");
						}
						if (!columnsOk) {
							throw new FormatException("missing 'columns' property");
						}
						bits = new BitSet(rows * columns);
						resultBitSetHolder.setValue(bits);
					}

					@Override
					protected void onBodyLine(int totalLineIndex, int bodyLineIndex, String line) throws FormatException {
						line = line.trim();
						if (line.isEmpty()) {
							if (firstEmptyBodyLine == -1) {
								firstEmptyBodyLine = totalLineIndex;
							}
							return;
						}
						if (firstEmptyBodyLine != -1) {
							throw new FormatException("body contains empty line(s) starting at line " + firstEmptyBodyLine);
						}
						if (!ROW_PATTERN.matcher(line).matches()) {
							throw new FormatException("invalid value at line " + totalLineIndex);
						}
						ConstantValue.Vector rowValue;
						try {
							rowValue = LiteralParser.parseVector(columns + "h" + line);
						} catch (LiteralParser.ParseException e) {
							// shouldn't happen since that line passed the ROW_PATTERN already
							throw new FormatException("unexpected exception while parsing line " + totalLineIndex + ": " + e.toString());
						}
						BitSet rowBits = rowValue.getBits();
						int rowBaseIndex = bodyLineIndex * columns;
						for (int i = 0; i < columns; i++) {
							bits.set(rowBaseIndex + i, rowBits.get(i));
						}
					}

					private int expectNonNegativeInteger(String key, String text) throws FormatException {
						int value;
						try {
							value = Integer.parseInt(text);
						} catch (NumberFormatException e) {
							throw new FormatException("invalid value for property '" + key + "'");
						}
						if (value < 0) {
							throw new FormatException("property '" + key + "' cannot be negative");
						}
						return value;
					}

				}.readFrom(inputStreamReader);
			}
		} catch (HeadBodyReader.FormatException e) {
			return context.error(errorSource, e.getMessage());
		} catch (IOException e) {
			return context.error(errorSource, e.toString());
		}
		return new ConstantValue.Matrix(rows, columns, resultBitSetHolder.getValue());
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
		VirtualFile containingFile = psiFile.getOriginalFile().getVirtualFile();
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

}
