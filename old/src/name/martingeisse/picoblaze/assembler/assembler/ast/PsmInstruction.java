/**
 * Copyright (c) 2015 Martin Geisse
 *
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.picoblaze.assembler.assembler.ast;

import name.martingeisse.picoblaze.assembler.assembler.IPicoblazeAssemblerErrorHandler;
import name.martingeisse.picoblaze.assembler.assembler.Range;

/**
 * A PSM element that represents an instruction. Subclasses are
 * defined for specific instructions.
 */
public abstract class PsmInstruction extends PsmElement {

	/**
	 * Constructor.
	 * @param fullRange the full syntactic range of the renaming, or null if not known
	 */
	public PsmInstruction(final Range fullRange) {
		super(fullRange);
	}

	/**
	 * Encodes this instruction with respect to the specified context.
	 * @param context the context to take constants and labels from
	 * @param errorHandler the error handler
	 * @return the encoded instruction
	 */
	public abstract int encode(Context context, final IPicoblazeAssemblerErrorHandler errorHandler);

}
