/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.actions;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import name.martingeisse.mahdl.plugin.MahdlSourceFile;
import name.martingeisse.mahdl.plugin.codegen.DesignVerilogGenerator;
import name.martingeisse.mahdl.plugin.util.UserMessageException;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 */
public class GenerateVerilogAction extends AbstractModuleAndConsoleAction {

	public GenerateVerilogAction() {
		super("generate verilog");
	}

	@NotNull
	protected String getConsoleTitle(@NotNull AnActionEvent event) {
		return "Generate Verilog";
	}

	protected void onConsoleOpened(@NotNull AnActionEvent event, @NotNull ConsoleViewImpl console) {
		console.print("Generating Verilog...", ConsoleViewContentType.NORMAL_OUTPUT);
	}

	protected void execute(@NotNull AnActionEvent event, @NotNull ConsoleViewImpl console, @NotNull MahdlSourceFile actionTargetSourceFile) throws Exception {

		// we need a project module to place output files in. Should this use ModuleRootManager?
		Module projectModule = event.getDataContext().getData(LangDataKeys.MODULE);
		if (projectModule == null) {
			console.print("No project module available to place output files in", ConsoleViewContentType.ERROR_OUTPUT);
			return;
		}

		// the file must contain a HDL module. This excludes files with fatal syntax errors, but not those with recoverable errors.
		if (actionTargetSourceFile.getModule() == null) {
			console.print("Input file contains fatal syntax errors", ConsoleViewContentType.ERROR_OUTPUT);
			return;
		}

		// do it!
		VirtualFile verilogFolder = createVerilogFolder(projectModule, console);
		DesignVerilogGenerator.OutputConsumer outputConsumer = new FlatVerilogFolderOutputConsumer(verilogFolder);
		new DesignVerilogGenerator(actionTargetSourceFile.getModule(), outputConsumer).generate();
		console.print("Done.", ConsoleViewContentType.NORMAL_OUTPUT);
	}

	// can be called from any thread
	private VirtualFile createVerilogFolder(@NotNull Module projectModule, @NotNull ConsoleViewImpl console) throws Exception {
		MyReturnWriteAction<VirtualFile> action = () -> {
			VirtualFile projectModuleFile = projectModule.getModuleFile();
			if (projectModuleFile == null) {
				throw new UserMessageException("could not locate project module folder");
			}
			VirtualFile projectModuleFolder = projectModuleFile.getParent();
			final VirtualFile existingVerilogFolder = projectModuleFolder.findChild("verilog");
			final VirtualFile verilogFolder;
			if (existingVerilogFolder == null) {
				try {
					verilogFolder = projectModuleFolder.createChildDirectory(this, "verilog");
				} catch (IOException e) {
					throw new UserMessageException("Could not create 'verilog' folder: " + e);
				}
			} else {
				verilogFolder = existingVerilogFolder;
			}
			return verilogFolder;
		};
		return runWriteAction(action);
	}

}
