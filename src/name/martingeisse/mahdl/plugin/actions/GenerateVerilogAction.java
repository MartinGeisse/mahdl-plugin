package name.martingeisse.mahdl.plugin.actions;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import name.martingeisse.mahdl.plugin.MahdlSourceFile;
import name.martingeisse.mahdl.plugin.codegen.VerilogGenerator;
import name.martingeisse.mahdl.plugin.util.UserMessageException;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 *
 */
public class GenerateVerilogAction extends AbstractModuleAndConsoleAction {

	public GenerateVerilogAction() {
		super("generate verilog");
	}

	protected String getConsoleTitle(AnActionEvent event) {
		return "Generate Verilog";
	}

	protected void onConsoleOpened(AnActionEvent event, ConsoleViewImpl console) {
		console.print("Generating Verilog...", ConsoleViewContentType.NORMAL_OUTPUT);
	}

	protected void execute(AnActionEvent event, ConsoleViewImpl console, MahdlSourceFile sourceFile) throws Exception {

		// we need a project module to place output files in. Should this use ModuleRootManager?
		Module projectModule = event.getDataContext().getData(LangDataKeys.MODULE);
		if (projectModule == null) {
			console.print("No project module available to place output files in", ConsoleViewContentType.ERROR_OUTPUT);
			return;
		}

		// the file must contain a HDL module. This excludes files with fatal syntax errors, but not those with recoverable errors.
		if (sourceFile.getModule() == null) {
			console.print("Input file contains fatal syntax errors", ConsoleViewContentType.ERROR_OUTPUT);
			return;
		}

		// do it!
		StringWriter buffer = new StringWriter();
		new VerilogGenerator(sourceFile.getModule(), buffer).run();
		ApplicationManager.getApplication().runWriteAction(() -> {
			try {

				// create the output folder and resource folder
				VirtualFile moduleFolder = projectModule.getModuleFile().getParent();
				final VirtualFile existingOutputFolder = moduleFolder.findChild("verilog");
				final VirtualFile outputFolder;
				if (existingOutputFolder == null) {
					try {
						outputFolder = moduleFolder.createChildDirectory(this, "verilog");
					} catch (IOException e) {
						console.print("Could not create 'verilog' folder: " + e, ConsoleViewContentType.ERROR_OUTPUT);
						return;
					}
				} else {
					outputFolder = existingOutputFolder;
				}

				String fileName = sourceFile.getModule().getModuleName().getText() + ".v";
				VirtualFile outputFile = outputFolder.findChild(fileName);
				if (outputFile == null) {
					outputFile = outputFolder.createChildData(this, fileName);
				} else if (outputFile.isDirectory()) {
					throw new UserMessageException("collision with existing folder while creating output file " + fileName + "'");
				}
				try (OutputStream outputStream = outputFile.getOutputStream(this)) {
					try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
						outputStreamWriter.write(buffer.toString());
					}
				}

			} catch (IOException e) {
				throw new RuntimeException("unexpected IOException", e);
			}
		});

		console.print("Done.", ConsoleViewContentType.NORMAL_OUTPUT);
	}

}
