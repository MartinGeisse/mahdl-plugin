/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.ise_build;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import name.martingeisse.mahdl.plugin.MahdlSourceFile;
import name.martingeisse.mahdl.plugin.actions.AbstractModuleAndConsoleAction;
import name.martingeisse.mahdl.plugin.actions.FlatVerilogFolderOutputConsumer;
import name.martingeisse.mahdl.plugin.codegen.DesignVerilogGenerator;
import name.martingeisse.mahdl.plugin.util.UserMessageException;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 *
 */
public class GenerateIseBuildAction extends AbstractModuleAndConsoleAction {

	public GenerateIseBuildAction() {
		super("generate ISE build");
	}

	@NotNull
	protected String getConsoleTitle(@NotNull AnActionEvent event) {
		return "Generate ISE Build";
	}

	protected void onConsoleOpened(@NotNull AnActionEvent event, @NotNull ConsoleViewImpl console) {
		console.print("Generating ISE build...", ConsoleViewContentType.NORMAL_OUTPUT);
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
		String buildName = actionTargetSourceFile.getModule().getName();
		VirtualFile verilogFolder = createBuildFolder(projectModule, console, buildName);
		DesignVerilogGenerator.OutputConsumer outputConsumer = new FlatVerilogFolderOutputConsumer(verilogFolder);
		new DesignVerilogGenerator(actionTargetSourceFile.getModule(), outputConsumer).generate();
		console.print("Done.", ConsoleViewContentType.NORMAL_OUTPUT);
	}

	// can be called from any thread
	private VirtualFile createBuildFolder(@NotNull Module projectModule, @NotNull ConsoleViewImpl console, String buildSubfolderName) throws Exception {
		MutableObject<Exception> exceptionHolder = new MutableObject<>();
		MutableObject<VirtualFile> buildFolderHolder = new MutableObject<>();
		ApplicationManager.getApplication().runWriteAction(() -> {
			try {
				VirtualFile projectModuleFile = projectModule.getModuleFile();
				if (projectModuleFile == null) {
					throw new UserMessageException("could not locate project module folder");
				}
				VirtualFile projectModuleFolder = projectModuleFile.getParent();
				VirtualFile buildParentFolder = createOrUseFolder(projectModuleFolder, "ise", console);
				if (buildParentFolder == null)
					return;
				VirtualFile buildSubfolder = createOrUseFolder(buildParentFolder, buildSubfolderName, console);
				if (buildSubfolder == null) {
					return;
				}
				buildFolderHolder.setValue(buildSubfolder);
			} catch (Exception e) {
				exceptionHolder.setValue(e);
			}
		});
		if (exceptionHolder.getValue() != null) {
			throw exceptionHolder.getValue();
		}
		return buildFolderHolder.getValue();
	}

	private VirtualFile createOrUseFolder(VirtualFile parentFolder, String name, @NotNull ConsoleViewImpl console) {
		final VirtualFile existingSubfolder = parentFolder.findChild(name);
		if (existingSubfolder == null) {
			try {
				return parentFolder.createChildDirectory(this, name);
			} catch (IOException e) {
				console.print("Could not create '" + name + "' folder: " + e, ConsoleViewContentType.ERROR_OUTPUT);
				return null;
			}
		} else {
			return existingSubfolder;
		}
	}

}
