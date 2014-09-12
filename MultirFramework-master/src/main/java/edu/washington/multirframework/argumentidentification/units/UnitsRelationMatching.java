//sg
package edu.washington.multirframework.argumentidentification.units;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multirframework.argumentidentification.RelationMatching;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.KBArgument;
import edu.washington.multirframework.data.NumberArgument;
import edu.washington.multirframework.knowledgebase.KnowledgeBase;

/**
 * This function takes a list of country number pairs and returns triples which are guaranteed to be
 * present in KB 
 * @author aman
 *
 */
public class UnitsRelationMatching implements RelationMatching {

	private static HashMap<String, String> freeBaseMapping;
	private static UnitsRelationMatching instance = null;
	
	private static double threshold = 0.00001;
	
	private UnitsRelationMatching() {
		
	}
	public static UnitsRelationMatching getInstance() {
		if(null == instance) {
			instance = new UnitsRelationMatching();
			return instance;
		}
		return instance;
	}
	static {
		try {
			

			String countriesFile = "data/countries_file";

			BufferedReader br = new BufferedReader(new FileReader(new File(
					countriesFile)));
			String countryRecord = null;
			freeBaseMapping = new HashMap<String, String>();

			while ((countryRecord = br.readLine()) != null) {
				System.out.println(countryRecord);
				String vars[] = countryRecord.split("\t");
				String countryName = vars[1].toLowerCase();
				String countryId = vars[0];

				freeBaseMapping.put(countryName, countryId);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	@Override
	public List<Triple<KBArgument, KBArgument, String>> matchRelations(
			List<Pair<Argument, Argument>> countryNumberPairs,
			KnowledgeBase KB, CoreMap sentence, Annotation doc) {
		Map<String, List<String>> entityMap = KB.getEntityMap();
		List<Triple<KBArgument, KBArgument, String>> distantSupervisionAnnotations = new ArrayList<>();
		
		for(Pair<Argument, Argument> countryNumberPair : countryNumberPairs) {
			Argument country = countryNumberPair.first;
			NumberArgument number = (NumberArgument)countryNumberPair.second;
			String rel = null;
			if(true /*(rel = relationExists(country, number)) != null) {
				KBArgument kbarg1 = new KBArgument(country, freeBaseMapping.get(country.getArgName()));
				KBArgument kbarg2 = new KBArgument(number.getArgument(), number.getArgName());
				Triple<KBArgument,KBArgument,String> t = 
						new Triple<>(kbarg1, kbarg2, rel.toString());
				distantSupervisionAnnotations.add(t);
				System.out.println("Units Match: " + country.getArgName() + " -> " + rel + " = " + kbarg2.getArgName());
			}
		}
		return distantSupervisionAnnotations;
	}
	*/
	/**
	 * Because our argument identifier only locates countries, and because our sentential instance generator ensures that we always
	 * have a country followed by a number, we can be assured that the ordering assumed in this code will be in place. I should probably
	 * add some asserts in the code to be doubly sure.
	 * Apart from that, here is how it works : 
	 * a) Iterate over all the pairs of arguments, which again, are supposedly (country, number)
	 * b) Parse the number in the argument to get a double value, say numD
	 * c) For the country, go to the kb to get a list of all the numbers and the relations. This will be a list in the following format:
	 * India -> (1300000000, POP), (34, Something else)
	 * d) For each element of this list, extract the number, parse it to double and compare it with numD, if they match with some lax, 
	 * check if the unit matches, if so, add the entity number pair to kb
	 */
	@Override
	public List<Triple<KBArgument,KBArgument,String>> matchRelations(
			List<Pair<Argument,Argument>> sententialInstances,
			KnowledgeBase KB, CoreMap sentence, Annotation doc) {
		
		Map<String,List<String>> entityMap =KB.getEntityMap();
		List<Triple<KBArgument,KBArgument,String>> distantSupervisionAnnotations = new ArrayList<>();
		
		/**
		 * The way things have been setup, the first argument is supposed to be a country and the second argument is supposed to be a number
		 */
		for(Pair<Argument,Argument> si : sententialInstances){
			Argument country = si.first;
			Argument number = si.second;
			
			String countryName = country.getArgName();
			String numberStr = number.getArgName();
			Double numberFoundInSentence = Double.parseDouble(numberStr);
			Set<String> relationsFound = new HashSet<String>();
			
			if(entityMap.containsKey(countryName)){
				//if(entityMap.containsKey(arg2Name)){ it may not be
					NumberArgument numArg = (NumberArgument) number;
					List<String> countryIds = entityMap.get(countryName);
					//List<String> arg2Ids = entityMap.get(arg2Name);
					for(String countryId : countryIds){
						List<Pair<String,String>> numberRelPairsForCountry = KB.getEntityPairRelationMap().get(countryId);
							//List<String> relations = KB.getRelationsBetweenArgumentIds(arg1Id,arg2Id);
							for(Pair<String, String> numRel : numberRelPairsForCountry) {
								String rel = numRel.second;
								Double num = Double.parseDouble(numRel.first);
								
								if(isEqual(num, numberFoundInSentence) && !relationsFound.contains(rel) && (RelationUnitMap.getUnit(rel) == numArg.getUnit())){
									KBArgument kbarg1 = new KBArgument(country, countryId);
									KBArgument kbarg2 = new KBArgument(number, numberStr);
									Triple<KBArgument,KBArgument,String> t = 
											new Triple<>(kbarg1,kbarg2,rel);
									distantSupervisionAnnotations.add(t);
									relationsFound.add(rel);
								}
							}
						}
					}
		}
		return distantSupervisionAnnotations;
	}
	
	/**
	 * Compare the 2 given numbers with some lax
	 * @param a
	 * @param b
	 * @return
	 */
	boolean isEqual(Double a, Double b) {
		return Math.abs(a - b) < threshold; 
	}
}
