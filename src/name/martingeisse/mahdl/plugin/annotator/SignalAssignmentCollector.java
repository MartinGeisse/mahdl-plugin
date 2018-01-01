package name.martingeisse.mahdl.plugin.annotator;

import com.intellij.extapi.psi.ASTDelegatePsiElement;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import name.martingeisse.mahdl.plugin.input.psi.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Builds a map that tells which signal gets assigned by which do-block, adding an error annotation if multiple
 * do-blocks assign to the same signal.
 */
final class SignalAssignmentCollector {

	private final Map<String, ImplementationItem_DoBlock> assigningDoBlocks = new HashMap<>();
	private Set<String> newlyAssignedSignals;

	/**
	 * The specified body is executed for each element, pre-order, and should return true if its
	 * children should be visited too.
	 */
	private void foreachPreOrder(PsiElement root, Predicate<PsiElement> body) {
		if (!body.test(root)) {
			return;
		}
		if (root instanceof ASTDelegatePsiElement) {
			InternalPsiUtil.foreachChild((ASTDelegatePsiElement) root, child -> foreachPreOrder(child, body));
		}
	}

	private void check(Statement statement) {
		foreachPreOrder(statement, element -> {
			if (element instanceof Expression) {
				return false;
			}
			if (element instanceof Statement_Assignment) {
				check(((Statement_Assignment) element).getLeftSide());
			}
			return true;
		});
	}

	private void check(Expression destination) {
		if (destination instanceof Expression_Signal) {
			LeafPsiElement signalNameElement = ((Expression_Signal) destination).getSignalName();
			check(signalNameElement, signalNameElement.getText());
		} else if (destination instanceof Expression_IndexSelection) {
			Expression container = ((Expression_IndexSelection) destination).getContainer();
			check(container);
		} else if (destination instanceof Expression_RangeSelection) {
			Expression container = ((Expression_RangeSelection) destination).getContainer();
			check(container);
		} else if (destination instanceof Expression_Parenthesized) {
			Expression inner = ((Expression_Parenthesized) destination).getExpression();
			check(inner);
		} else if (destination instanceof Expression_BinaryConcat) {
			Expression_BinaryConcat typed = (Expression_BinaryConcat)destination;
			check(typed.getLeftOperand());
			check(typed.getRightOperand());
		} else if (destination instanceof Expression_InstancePort) {
			Expression_InstancePort typed = (Expression_InstancePort)destination;
			check(typed, typed.getInstanceName().getText() + '.' + typed.getPortName().getText());
		}
	}

	private void check(PsiElement element, String signalName) {
		if (assigningDoBlocks.containsKey(signalName)) {
			annotationHolder.createErrorAnnotation(element.getNode(), "signal " + signalName + " was already assigned to in another do-block");
		}
		newlyAssignedSignals.add(signalName);
	}

}
