/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.picoblaze.assembler;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import name.martingeisse.mahdl.plugin.actions.AbstractModuleAndConsoleAction;
import name.martingeisse.mahdl.plugin.util.UserMessageException;
import name.martingeisse.picoblaze.assembler.assembler.IPicoblazeAssemblerErrorHandler;
import name.martingeisse.picoblaze.assembler.assembler.Range;
import name.martingeisse.picoblaze.assembler.assembler.ast.AstBuilder;
import name.martingeisse.picoblaze.assembler.assembler.ast.Context;
import name.martingeisse.picoblaze.assembler.assembler.ast.PsmFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Base class for all actions that operate on a module and can output text to a console.
 */
public class PicoblazeAssemblerAction extends AbstractModuleAndConsoleAction {

	public PicoblazeAssemblerAction() {
		super("Assemble Picoblaze Code");
	}

	@Override
	public void update(@Nullable AnActionEvent event) {
		if (event == null) {
			return;
		}
		PsiFile psiFile = event.getDataContext().getData(CommonDataKeys.PSI_FILE);
		boolean enabled = psiFile != null && psiFile.getLanguage() == PicoblazeAssemblerLanguage.INSTANCE;
		event.getPresentation().setEnabledAndVisible(enabled);
	}

	@NotNull
	protected String getConsoleTitle(@NotNull AnActionEvent event) {
		return "Picoblaze";
	}

	protected void onConsoleOpened(@NotNull AnActionEvent event, @NotNull ConsoleViewImpl console) {
		console.print("Assembling Picoblaze code...", ConsoleViewContentType.NORMAL_OUTPUT);
	}

	@Override
	protected void actionPerformed(@NotNull AnActionEvent event, ConsoleViewImpl console) throws Exception {
		VirtualFile file = event.getDataContext().getData(CommonDataKeys.VIRTUAL_FILE);
		if (file == null) {
			console.print("no file selected\n", ConsoleViewContentType.ERROR_OUTPUT);
			return;
		}
		IPicoblazeAssemblerErrorHandler errorHandler = new IPicoblazeAssemblerErrorHandler() {

			@Override
			public void handleWarning(Range range, String message) {
				handle(range, message, ConsoleViewContentType.LOG_WARNING_OUTPUT);
			}

			@Override
			public void handleError(Range range, String message) {
				handle(range, message, ConsoleViewContentType.ERROR_OUTPUT);
			}

			private void handle(Range range, String message, ConsoleViewContentType contentType) {
				console.print("line " + range.getStartLine() + ", column " + range.getStartColumn() + ": " + message, contentType);
			}

		};
		final AstBuilder astBuilder = new AstBuilder();
		try (InputStream inputStream = file.getInputStream()) {
			try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
				astBuilder.parse(reader, errorHandler);
			}
		}
		PsmFile psmFile = astBuilder.getResult();
		Context context = new Context(errorHandler);
		psmFile.collectConstantsAndLabels(context);
		int[] encodedInstructions = psmFile.encode(context, errorHandler);
		MyVoidWriteAction action = () -> {

			// determine folder
			VirtualFile folder = file.getParent();
			if (folder == null) {
				throw new UserMessageException("could not determine folder");
			}

			// determine output file name
			String inputFileName = file.getName();
			String outputFileName;
			if (inputFileName.endsWith(".psm")) {
				outputFileName = inputFileName.substring(0, inputFileName.length() - 4) + ".mif";
			} else {
				outputFileName = inputFileName + ".mif";
			}

			// create output file
			VirtualFile outputFile = folder.findChild(outputFileName);
			if (outputFile == null) {
				outputFile = folder.createChildData(this, outputFileName);
			} else if (outputFile.isDirectory()) {
				throw new UserMessageException("collision with existing folder while creating output file " + outputFileName + "'");
			}

			// write to output file
			try (OutputStream outputStream = outputFile.getOutputStream(this)) {
				try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
					for (final int instruction : encodedInstructions) {
						final String hex = Integer.toHexString(instruction);
						final String zeros = "00000".substring(hex.length());
						outputStreamWriter.write(zeros);
						outputStreamWriter.write(hex);
						outputStreamWriter.write('\n');
					}
				}
			}

		};
		runWriteAction(action);
		console.print("Done.", ConsoleViewContentType.NORMAL_OUTPUT);
	}

}
