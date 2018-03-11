/**
 * Copyright (c) 2015 Martin Geisse
 *
 * This file is distributed under the terms of the MIT license.
 */
package name.martingeisse.picoblaze.assembler.assembler.ast;

import name.martingeisse.picoblaze.assembler.assembler.Range;

/**
 * A PSM element that represents an assembling address directive.
 */
public class PsmAddress extends PsmElement {

	/**
	 * the addressRange
	 */
	private final Range addressRange;

	/**
	 * the address
	 */
	private final int address;

	/**
	 * Creates a new assembling address directive.
	 * @param fullRange the full syntactic range of the renaming, or null if not known
	 * @param addressRange the syntactic range of the address
	 * @param address the specified address to assemble at
	 */
	public PsmAddress(final Range fullRange, final Range addressRange, final int address) {
		super(fullRange);
		this.addressRange = addressRange;
		this.address = address;
		checkValue(address, 0x000, 0x3ff, "address");
	}

	/**
	 * Getter method for the addressRange.
	 * @return the addressRange
	 */
	public Range getAddressRange() {
		return addressRange;
	}

	/**
	 * Getter method for the address.
	 * @return the address
	 */
	public int getAddress() {
		return address;
	}

}
