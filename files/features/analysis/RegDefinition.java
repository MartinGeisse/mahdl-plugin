package name.martingeisse.mahdl.plugin.analysis;

/**
 * Note: A reg is defined as (note the opposite order vs. the constructor parameters)
 *
 *   reg[dataRangeFrom:dataRangeTo] foo[arrayRangeFrom:arrayRangeTo]
 *
 * and accessed as
 *
 *   reg[arrayIndex]
 *   reg[arrayIndex][dataIndex]
 *   reg[arrayIndex][dataIndexFrom:dataIndexTo]
 *
 */
public final class RegDefinition extends VariableDefinition {

	private final Range dataRange;

	public RegDefinition(String name, Range arrayRange, Range dataRange) {
		super(name, arrayRange);
		this.dataRange = dataRange;
	}

	public Range getDataRange() {
		return dataRange;
	}

}
