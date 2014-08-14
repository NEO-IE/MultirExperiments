//sg
package edu.washington.multir.preprocess.marker;

/**
 * A marking represents a highlighted region of interest in the text
 * For current case, it will either be a number or a country.
 * @author aman
 *
 */
public class Marking implements Comparable<Marking> {
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
	Integer startOffset, endOffset;
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
	@Override
	public int compareTo(Marking arg0) {
		assert(startOffset != arg0.startOffset); //or we are doing something really wrong
		// TODO Auto-generated method stub
		if(startOffset < arg0.startOffset) {
			return -1;
		} else {
			return 1;
		}
	}
	/**
	 * Returns the string in the format required for the column SENTNAMEDENTITYLINKINGINFO
	 * @return
	 */
	public String getLinkString() {
		String res = startOffset + " " + endOffset + " " + entityName + " " + freebaseId + " " + markingConfidence;
		return res;
	}
	
	/**
	 * Returns the string that can be directly fed to the freebasenotable column of the derby db
	 * @return
	 */
	public String getTypeString() {
		String res = startOffset + " " + endOffset + " " + markingType + " " + freebaseId;
		return res;
	}
}
