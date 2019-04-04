/**
 * Copyright (c) 2015 Martin Geisse
 *
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.picoblaze.assembler.assembler.ast;

import name.martingeisse.picoblaze.assembler.assembler.Range;

/**
 * A PSM element that represents an instruction label. Note that this class
 * stores only a source code location and a name -- the location of the label
 * in the generated code is derived later from its source code location.
 */
public class PsmLabel extends PsmElement {

	/**
	 * the nameRange
	 */
	private final Range nameRange;

	/**
	 * the name
	 */
	private final String name;

	/**
	 * Creates a new label.
	 * @param fullRange the full syntactic range of the renaming, or null if not known
	 * @param nameRange the syntactic range of the label name
	 * @param name the name of the label
	 */
	public PsmLabel(final Range fullRange, final Range nameRange, final String name) {
		super(fullRange);
		this.nameRange = nameRange;
		this.name = name;
	}

	/**
	 * Getter method for the nameRange.
	 * @return the nameRange
	 */
	public Range getNameRange() {
		return nameRange;
	}

	/**
	 * Getter method for the name.
	 * @return the name
	 */
	public String getName() {
		return name;
	}

}
