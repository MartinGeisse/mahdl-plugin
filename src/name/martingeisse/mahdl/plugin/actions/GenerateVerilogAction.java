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
		MutableObject<Exception> exceptionHolder = new MutableObject<>();
		MutableObject<VirtualFile> verilogFolderHolder = new MutableObject<>();
		ApplicationManager.getApplication().runWriteAction(() -> {
			try {
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
						console.print("Could not create 'verilog' folder: " + e, ConsoleViewContentType.ERROR_OUTPUT);
						return;
					}
				} else {
					verilogFolder = existingVerilogFolder;
				}
				verilogFolderHolder.setValue(verilogFolder);
			} catch (Exception e) {
				exceptionHolder.setValue(e);
			}
		});
		if (exceptionHolder.getValue() != null) {
			throw exceptionHolder.getValue();
		}
		return verilogFolderHolder.getValue();
	}

	private Properties readAssociatedProperties(VirtualFile toplevelModuleFile, ConsoleViewImpl console) {
		VirtualFile propertiesFile = findPropertiesFile(toplevelModuleFile, console);
		if (propertiesFile == null) {
			return null;
		}
		try (InputStream inputStream = propertiesFile.getInputStream()) {
			Properties properties = new Properties();
			properties.load(inputStream);
			return properties;
		} catch (IOException e) {
			console.print("Exception while reading associated properties file: " + e, ConsoleViewContentType.ERROR_OUTPUT);
			return null;
		}
	}

	private VirtualFile findPropertiesFile(VirtualFile toplevelModuleFile, ConsoleViewImpl console) {
		String grammarFileName = toplevelModuleFile.getName();
		if (!grammarFileName.endsWith(".mahdl")) {
			console.print("Toplevel module file extension is not .mahdl", ConsoleViewContentType.ERROR_OUTPUT);
			return null;
		}
		String propertiesFileName = grammarFileName.substring(0, grammarFileName.length() - ".mahdl".length()) + ".properties";
		VirtualFile propertiesFile = toplevelModuleFile.getParent().findChild(propertiesFileName);
		if (propertiesFile == null) {
			console.print("Could not find associated properties file", ConsoleViewContentType.ERROR_OUTPUT);
			return null;
		}
		return propertiesFile;
	}

}
