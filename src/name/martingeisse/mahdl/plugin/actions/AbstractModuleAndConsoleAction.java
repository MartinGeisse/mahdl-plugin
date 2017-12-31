package name.martingeisse.mahdl.plugin.actions;

import com.intellij.execution.ExecutionManager;
import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.actions.CloseAction;
import com.intellij.ide.actions.PinActiveTabAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import name.martingeisse.mahdl.plugin.MahdlLanguage;
import name.martingeisse.mahdl.plugin.MahdlSourceFile;
import name.martingeisse.mahdl.plugin.util.SelfDescribingRuntimeException;
import name.martingeisse.mahdl.plugin.util.UserMessageException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;

/**
 * Base class for all actions that operate on a module and can output text to a console.
 */
public abstract class AbstractModuleAndConsoleAction extends AnAction {

	public AbstractModuleAndConsoleAction() {
	}

	public AbstractModuleAndConsoleAction(Icon icon) {
		super(icon);
	}

	public AbstractModuleAndConsoleAction(@Nullable String text) {
		super(text);
	}

	public AbstractModuleAndConsoleAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
		super(text, description, icon);
	}

	@Override
	public void update(AnActionEvent event) {
		PsiFile psiFile = event.getDataContext().getData(CommonDataKeys.PSI_FILE);
		boolean enabled = psiFile != null && psiFile.getLanguage() == MahdlLanguage.INSTANCE;
		event.getPresentation().setEnabledAndVisible(enabled);
	}

	@Override
	public void actionPerformed(AnActionEvent event) {

		// we need a project to show a console
		Project project = getEventProject(event);
		if (project == null) {
			return;
		}
		RunContentDescriptor runContentDescriptor = createConsole(project, getConsoleTitle(event));
		ConsoleViewImpl console = (ConsoleViewImpl) runContentDescriptor.getExecutionConsole();
		onConsoleOpened(event, console);

		// we need a MaHDL input file to process
		PsiFile psiFile = event.getDataContext().getData(CommonDataKeys.PSI_FILE);
		if (!(psiFile instanceof MahdlSourceFile)) {
			console.print("The input file is not a MaHDL module file", ConsoleViewContentType.ERROR_OUTPUT);
			return;
		}

		// do it!
		try {
			execute(event, console, (MahdlSourceFile)psiFile);
		} catch (UserMessageException e) {
			console.print(e.getMessage(), ConsoleViewContentType.ERROR_OUTPUT);
		} catch (SelfDescribingRuntimeException e) {
			printError(console, e::describe);
		} catch (Exception e) {
			console.print("unexpected exception\n", ConsoleViewContentType.ERROR_OUTPUT);
			printError(console, e::printStackTrace);
		}

	}

	protected abstract String getConsoleTitle(AnActionEvent event);

	protected void onConsoleOpened(AnActionEvent event, ConsoleViewImpl console) {
	}

	protected abstract void execute(AnActionEvent event, ConsoleViewImpl console, MahdlSourceFile sourceFile) throws Exception;

	private static RunContentDescriptor createConsole(@NotNull Project project, String title) {

		ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();

		DefaultActionGroup toolbarActions = new DefaultActionGroup();
		JComponent consoleComponent = new JPanel(new BorderLayout());

		JPanel toolbarPanel = new JPanel(new BorderLayout());
		toolbarPanel.add(ActionManager.getInstance().createActionToolbar(ActionPlaces.RUNNER_TOOLBAR, toolbarActions, false).getComponent());
		consoleComponent.add(toolbarPanel, BorderLayout.WEST);
		consoleComponent.add(consoleView.getComponent(), BorderLayout.CENTER);

		RunContentDescriptor descriptor = new RunContentDescriptor(consoleView, null, consoleComponent, title, null);

		Executor executor = DefaultRunExecutor.getRunExecutorInstance();
		for (AnAction action : consoleView.createConsoleActions()) {
			toolbarActions.add(action);
		}
		toolbarActions.add(new PinActiveTabAction());
		toolbarActions.add(new CloseAction(executor, descriptor, project));
		ExecutionManager.getInstance(project).getContentManager().showRunContent(executor, descriptor);
		consoleView.allowHeavyFilters();
		return descriptor;

	}

	protected static void printError(ConsoleViewImpl console, Consumer<PrintWriter> printable) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		printable.accept(printWriter);
		printWriter.flush();
		console.print(stringWriter.toString(), ConsoleViewContentType.ERROR_OUTPUT);
	}

}
