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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.function.Consumer;

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

		// we need this file to find associated files
		VirtualFile virtualFile = actionTargetSourceFile.getVirtualFile();
		if (virtualFile == null) {
			console.print("Toplevel module is not inside a file", ConsoleViewContentType.ERROR_OUTPUT);
			return;
		}

		// load associated properties file
		Configuration configuration = new Configuration(readAssociatedProperties(virtualFile));

		// generate Verilog files
		String buildName = actionTargetSourceFile.getModule().getName();
		VirtualFile buildFolder = createBuildFolder(projectModule, console, buildName);
		DesignVerilogGenerator.OutputConsumer outputConsumer = new FlatVerilogFolderOutputConsumer(buildFolder);
		DesignVerilogGenerator designGenerator = new DesignVerilogGenerator(actionTargetSourceFile.getModule(), outputConsumer);
		designGenerator.generate();

		// generate build files
		Consumer<VirtualFile> makeExecutable = file -> {
			File localFile = new File(file.getPath());
			if (localFile.exists()) {
				if (!localFile.setExecutable(true, true)) {
					console.print("Could not make " + localFile.getName() + " executable", ConsoleViewContentType.LOG_WARNING_OUTPUT);
				}
			}
		};
		BuildContext buildContext = new BuildContext(designGenerator.getToplevelModule(),
			ImmutableSet.copyOf(designGenerator.getGeneratedModules()), configuration, buildFolder);
		generate(buildFolder, "environment.sh", new EnvironmentVariablesScriptGenerator(buildContext));
		generate(buildFolder, "build.xst", new XstScriptGenerator(buildContext));
		generate(buildFolder, "build.prj", new XstProjectGenerator(buildContext));
		generate(buildFolder, "build.sh", new BuildScriptGenerator(buildContext), makeExecutable);
		generate(buildFolder, "upload.sh", new UploadScriptGenerator(buildContext), makeExecutable);
		copyConstraints(virtualFile, buildFolder);

		console.print("Done.", ConsoleViewContentType.NORMAL_OUTPUT);
	}

	@NotNull
	private Properties readAssociatedProperties(VirtualFile toplevelModuleFile) {
		VirtualFile propertiesFile = findAssociatedFile(toplevelModuleFile, ".properties");
		try (InputStream inputStream = propertiesFile.getInputStream()) {
			Properties properties = new Properties();
			properties.load(inputStream);
			return properties;
		} catch (IOException e) {
			throw new UserMessageException("Exception while reading associated properties file: " + e);
		}
	}

	private void copyConstraints(VirtualFile toplevelModuleFile, VirtualFile buildFolder) throws Exception {
		VirtualFile constraintsFile = findAssociatedFile(toplevelModuleFile, ".ucf");
		MyVoidWriteAction action = () -> {
			String targetFilename = "build.ucf";
			VirtualFile existingTargetFile = buildFolder.findChild(targetFilename);
			if (existingTargetFile != null) {
				existingTargetFile.delete(this);
			}
			constraintsFile.copy(this, buildFolder, targetFilename);
		};
		runWriteAction(action);
	}

	private VirtualFile findAssociatedFile(VirtualFile toplevelModuleFile, String dotExtension) {
		String grammarFileName = toplevelModuleFile.getName();
		if (!grammarFileName.endsWith(".mahdl")) {
			throw new UserMessageException("Toplevel module file extension is not .mahdl");
		}
		String associatedFileName = grammarFileName.substring(0, grammarFileName.length() - ".mahdl".length()) + dotExtension;
		VirtualFile associatedFile = toplevelModuleFile.getParent().findChild(associatedFileName);
		if (associatedFile == null) {
			throw new UserMessageException("Could not find associated " + dotExtension + " file");
		}
		return associatedFile;
	}

	// can be called from any thread
	private VirtualFile createBuildFolder(@NotNull Module projectModule, @NotNull ConsoleViewImpl console, String buildSubfolderName) throws Exception {
		MyReturnWriteAction<VirtualFile> action = () -> {
			VirtualFile projectModuleFile = projectModule.getModuleFile();
			if (projectModuleFile == null) {
				throw new UserMessageException("could not locate project module folder");
			}
			VirtualFile projectModuleFolder = projectModuleFile.getParent();
			VirtualFile buildParentFolder = createOrUseFolder(projectModuleFolder, "ise", console);
			VirtualFile buildSubfolder = createOrUseFolder(buildParentFolder, buildSubfolderName, console);
			return buildSubfolder;
		};
		return runWriteAction(action);
	}

	@NotNull
	private VirtualFile createOrUseFolder(VirtualFile parentFolder, String name, @NotNull ConsoleViewImpl console) {
		final VirtualFile existingSubfolder = parentFolder.findChild(name);
		if (existingSubfolder == null) {
			try {
				return parentFolder.createChildDirectory(this, name);
			} catch (IOException e) {
				throw new UserMessageException("Could not create '" + name + "' folder: " + e);
			}
		} else {
			return existingSubfolder;
		}
	}

	private void generate(VirtualFile outputFolder, String fileName, TextFileGenerator generator) throws Exception {
		generate(outputFolder, fileName, generator, file -> {
		});
	}

	private void generate(VirtualFile outputFolder, String fileName, TextFileGenerator generator, Consumer<VirtualFile> filePostProcessor) throws Exception {
		MyVoidWriteAction action = () -> {
			VirtualFile outputFile = outputFolder.findChild(fileName);
			if (outputFile == null) {
				outputFile = outputFolder.createChildData(this, fileName);
			} else if (outputFile.isDirectory()) {
				throw new UserMessageException("collision with existing folder while creating output file " + fileName + "'");
			}
			try (OutputStream outputStream = outputFile.getOutputStream(this)) {
				generator.generate(outputStream);
			}
			filePostProcessor.accept(outputFile);
		};
		runWriteAction(action);
	}

}
