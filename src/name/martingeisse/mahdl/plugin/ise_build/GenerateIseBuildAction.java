/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.ise_build;

import com.google.common.collect.ImmutableSet;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import name.martingeisse.mahdl.plugin.MahdlSourceFile;
import name.martingeisse.mahdl.plugin.actions.AbstractModuleAndConsoleAction;
import name.martingeisse.mahdl.plugin.actions.Configuration;
import name.martingeisse.mahdl.plugin.actions.FlatVerilogFolderOutputConsumer;
import name.martingeisse.mahdl.plugin.codegen.DesignVerilogGenerator;
import name.martingeisse.mahdl.plugin.util.UserMessageException;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

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

		// load associated properties file
		Configuration configuration;
		{
			VirtualFile virtualFile = actionTargetSourceFile.getVirtualFile();
			if (virtualFile == null) {
				console.print("Toplevel module is not inside a file", ConsoleViewContentType.ERROR_OUTPUT);
				return;
			}
			Properties properties = readAssociatedProperties(virtualFile, console);
			if (properties == null) {
				return;
			}
			configuration = new Configuration(properties);
		}

		// generate Verilog files
		String buildName = actionTargetSourceFile.getModule().getName();
		VirtualFile buildFolder = createBuildFolder(projectModule, console, buildName);
		DesignVerilogGenerator.OutputConsumer outputConsumer = new FlatVerilogFolderOutputConsumer(buildFolder);
		DesignVerilogGenerator designGenerator = new DesignVerilogGenerator(actionTargetSourceFile.getModule(), outputConsumer);
		designGenerator.generate();

		// generate build files
		BuildContext buildContext = new BuildContext(designGenerator.getToplevelModule(),
			ImmutableSet.copyOf(designGenerator.getGeneratedModules()), configuration, buildFolder);
		generate(buildFolder, "environment.sh", new EnvironmentVariablesScriptGenerator(buildContext));
		generate(buildFolder, "build.xst", new XstScriptGenerator(buildContext));
		generate(buildFolder, "build.prj", new XstProjectGenerator(buildContext));
		generate(buildFolder, "build.ucf", new UcfGenerator(buildContext));
		generate(buildFolder, "build.sh", new BuildScriptGenerator(buildContext));
		generate(buildFolder, "upload.sh", new UploadScriptGenerator(buildContext));

		console.print("Done.", ConsoleViewContentType.NORMAL_OUTPUT);
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

	private void generate(VirtualFile outputFolder, String fileName, TextFileGenerator generator) throws Exception {
		MutableObject<Exception> exceptionHolder = new MutableObject<>();
		ApplicationManager.getApplication().runWriteAction(() -> {
			try {
				VirtualFile outputFile = outputFolder.findChild(fileName);
				if (outputFile == null) {
					outputFile = outputFolder.createChildData(this, fileName);
				} else if (outputFile.isDirectory()) {
					throw new UserMessageException("collision with existing folder while creating output file " + fileName + "'");
				}
				try (OutputStream outputStream = outputFile.getOutputStream(this)) {
					generator.generate(outputStream);
				}
			} catch (Exception e) {
				exceptionHolder.setValue(e);
			}
		});
		if (exceptionHolder.getValue() != null) {
			throw exceptionHolder.getValue();
		}
	}

}
