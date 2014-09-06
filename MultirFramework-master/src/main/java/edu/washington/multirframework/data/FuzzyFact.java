package edu.washington.multirframework.data;

import java.util.ArrayList;

import scala.actors.threadpool.Arrays;

public class FuzzyFact {
	private static final int VALS_INDEX = 1;
	private static final int ATTR_INDEX = 0;
	private static final String ATTR_VAL_DELIM = ":";
	private static final String VALS_DELIM = ",";
	
	String relName;
	ArrayList<String> factList;
	public FuzzyFact(String fuzzyFactString) {
			String factSplit[] = fuzzyFactString.split(ATTR_VAL_DELIM);
			relName = factSplit[ATTR_INDEX];
			factList = new ArrayList<String>(Arrays.asList(factSplit[VALS_INDEX].split(VALS_DELIM)));
		}
	
	@Override
	public String toString() {
		return relName + " - " + factList;
	}
}
