/*
 * Copyright (c) 2018 Martin Geisse
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.mahdl.plugin.actions;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.psi.PsiFile;
import name.martingeisse.mahdl.plugin.MahdlLanguage;
import name.martingeisse.mahdl.plugin.MahdlSourceFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Base class for all actions that operate on a module and can output text to a console.
 */
public abstract class AbstractModuleAndConsoleMahdlFileAction extends AbstractModuleAndConsoleAction {

	public AbstractModuleAndConsoleMahdlFileAction() {
	}

	public AbstractModuleAndConsoleMahdlFileAction(Icon icon) {
		super(icon);
	}

	public AbstractModuleAndConsoleMahdlFileAction(@Nullable String text) {
		super(text);
	}

	public AbstractModuleAndConsoleMahdlFileAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
		super(text, description, icon);
	}

	@Override
	public void update(@Nullable AnActionEvent event) {
		if (event == null) {
			return;
		}
		PsiFile psiFile = event.getDataContext().getData(CommonDataKeys.PSI_FILE);
		boolean enabled = psiFile != null && psiFile.getLanguage() == MahdlLanguage.INSTANCE;
		event.getPresentation().setEnabledAndVisible(enabled);
	}

	@Override
	protected void actionPerformed(@Nullable AnActionEvent event, ConsoleViewImpl console) throws Exception {
		PsiFile psiFile = event.getDataContext().getData(CommonDataKeys.PSI_FILE);
		if (!(psiFile instanceof MahdlSourceFile)) {
			console.print("The input file is not a MaHDL module file", ConsoleViewContentType.ERROR_OUTPUT);
			return;
		}
		execute(event, console, (MahdlSourceFile) psiFile);
	}

	protected abstract void execute(@NotNull AnActionEvent event, @NotNull ConsoleViewImpl console, @NotNull MahdlSourceFile sourceFile) throws Exception;

}
