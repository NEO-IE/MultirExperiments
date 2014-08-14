//sg
package edu.washington.multir.preprocess.marker;

/**
 * A marking represents a highlighted region of interest in the text
 * For current case, it will either be a number or a country.
 * @author aman
 *
 */
public class Marking {
	public Marking(int startOffset, int endOffset, String entityName,
			String freebaseId, double markingConfidence, String markingType) {
		super();
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.entityName = entityName;
		this.freebaseId = freebaseId;
		this.markingConfidence = markingConfidence;
		this.markingType = markingType;
	}
	public static final String COUNTRY = "/location/country";
	public static final String NUMBER = "/neo/number";
	int startOffset, endOffset;
	String entityName, freebaseId;
	double markingConfidence;
	String markingType;
	@Override
	public String toString() {
		return "Marking [startOffset=" + startOffset + ", endOffset="
				+ endOffset + ", entityName=" + entityName + ", freebaseId="
				+ freebaseId + ", markingConfidence=" + markingConfidence
				+ ", markingType=" + markingType + "]";
	}

}
