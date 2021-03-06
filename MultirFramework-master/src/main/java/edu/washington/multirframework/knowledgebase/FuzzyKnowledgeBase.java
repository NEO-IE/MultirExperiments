//sg
package edu.washington.multirframework.knowledgebase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import edu.washington.multirframework.data.FuzzyFact;

/**
 * In case of numerical relations, it makes sense to define fuzzy knowledge bases, 
 * such knowledge bases don't have just hard facts, but usually ranges.
 * Current representation is as follows : 
 * EntityId		Attribute1:val1,val2,val3;Attribute2:val1,val2,val3
 */

public class FuzzyKnowledgeBase {
	private HashMap<String, ArrayList<FuzzyFact>> facts;
	private static final String DELIM = "\t";
	private static final String FACT_DELIM = ";";
	private static final int ENTITY_INDEX = 0;
	private static final int FACTS_INDEX = 1;	

	
	
	
	public FuzzyKnowledgeBase(String fuzzyKbFile) throws IOException {
		facts = new HashMap<>();
		BufferedReader br = new BufferedReader(new FileReader(new File(fuzzyKbFile)));
		String factsLine = null;
		while((factsLine = br.readLine()) != null) {
			String entityId = factsLine.split(DELIM)[ENTITY_INDEX];
			String factsSplit[] = factsLine.split(DELIM)[FACTS_INDEX].split(FACT_DELIM);
			for(String fact : factsSplit) {
		
				if(null == facts.get(entityId)) {
					ArrayList<FuzzyFact> arr = new ArrayList<FuzzyFact>();
					arr.add(new FuzzyFact(fact));
					facts.put(entityId, arr);
				} else {
					facts.get(entityId).add(new FuzzyFact(fact));
				}
		
			}
		}
		br.close();
	}
	
	/**
	 * returns a list of fuzzy facts for a given entity
	 * @param entityId
	 * @return
	 */
	public ArrayList<FuzzyFact> getFactsForId(String entityId) {
		return facts.get(entityId);
	}
	
	/***
	 * tester
	 * @throws IOException 
	 */
	public static void main(String args[]) throws IOException {
		System.out.println(System.getProperty("user.dir"));
		String fuzzyKbFile = "/mnt/a99/d0/aman/MultirExperiments/data/numericalkb/fuzzyKb.tsv";
		FuzzyKnowledgeBase fkb = new FuzzyKnowledgeBase(fuzzyKbFile);
		for(String id : fkb.facts.keySet()) {
			System.out.println(id + " -> " + fkb.getFactsForId(id));
		}
	}
	
}
