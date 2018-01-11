package name.martingeisse.mahdl.plugin.processor;

import com.intellij.psi.PsiElement;

/**
 * This interface is called when an error was detected. It might, for example, add an error annotation, throw an
 * exception, or ignore the error.
 */
public interface ErrorHandler {

	/**
	 * This method is called by the helper object when an error was detected.
	 */
	void onError(PsiElement errorSource, String message);

}
