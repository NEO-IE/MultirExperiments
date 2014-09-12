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
			
			String arg1Name = country.getArgName();
			String arg2Name = number.getArgName();
			Set<String> relationsFound = new HashSet<String>();
			
			if(entityMap.containsKey(arg1Name)){
				if(entityMap.containsKey(arg2Name)){
					NumberArgument numArg = (NumberArgument) number;
					List<String> arg1Ids = entityMap.get(arg1Name);
					List<String> arg2Ids = entityMap.get(arg2Name);
					for(String arg1Id : arg1Ids){
						for(String arg2Id: arg2Ids){
							List<String> relations = KB.getRelationsBetweenArgumentIds(arg1Id,arg2Id);
							for(String rel : relations){
								if(!relationsFound.contains(rel) && (RelationUnitMap.getUnit(rel) == numArg.getUnit())){
									KBArgument kbarg1 = new KBArgument(country, arg1Id);
									KBArgument kbarg2 = new KBArgument(number, arg2Id);
									Triple<KBArgument,KBArgument,String> t = 
											new Triple<>(kbarg1,kbarg2,rel);
									distantSupervisionAnnotations.add(t);
									relationsFound.add(rel);
								}
							}
						}
					}
				}
			}
		}
		return distantSupervisionAnnotations;
	}
}
