package edu.washington.multirframework.data;

import java.util.ArrayList;

import scala.actors.threadpool.Arrays;

public class FuzzyFact {
	private static final int VALS_INDEX = 1;
	private static final int ATTR_INDEX = 0;
	private static final String ATTR_VAL_DELIM = ":";
	private static final String VALS_DELIM = ",";
	
	String relName;
	ArrayList<Double> factList; //every number is a float
	public FuzzyFact(String fuzzyFactString) {
			String factSplit[] = fuzzyFactString.split(ATTR_VAL_DELIM);
			relName = factSplit[ATTR_INDEX];
			factList = new ArrayList<Double>();
			for(String num : factSplit[VALS_INDEX].split(VALS_DELIM)) {
				factList.add(Double.parseDouble(num));
			}
		}
	
	@Override
	public String toString() {
		return relName + " - " + factList;
	}
	
	/**
	 * finds out if the number num is matches this fact. Definition of match can be changed
	 * @param num
	 * @return
	 */
	public boolean isMatch(Double num, StringBuilder rel) {
		for(Double val : factList) {
			if((val - 0.1 * val) <= num && num <= (val + 0.1 * val)) {
				rel.append(this.relName);
				System.out.println("Match for " + this.relName + " Original: " + val + "Got: " + num);
				return true;
			}
		}
		return false;
	}
}
