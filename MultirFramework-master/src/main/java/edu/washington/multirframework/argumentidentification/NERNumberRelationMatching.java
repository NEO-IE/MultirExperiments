package edu.washington.multirframework.argumentidentification;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.FuzzyFact;
import edu.washington.multirframework.data.KBArgument;
import edu.washington.multirframework.knowledgebase.FuzzyKnowledgeBase;
import edu.washington.multirframework.knowledgebase.KnowledgeBase;

public class NERNumberRelationMatching implements RelationMatching {

	private static HashMap<String, String> freeBaseMapping;
	private static HashSet<String> countryList;
	private static HashSet<String> countryIdList;
	private static Pattern numberPat;
	private static FuzzyKnowledgeBase fkb;
	private static NERNumberRelationMatching instance;
	private static final String fuzzyKbFile = "/mnt/a99/d0/aman/MultirExperiments/data/numericalkb/fuzzyKb.tsv";
	/**
	 * All the one time inits follow
	 */
	static {
		try {
			fkb = new FuzzyKnowledgeBase(fuzzyKbFile);

			numberPat = Pattern
					.compile("^[\\+-]?\\d+([,\\.]\\d+)?([eE]-?\\d+)?$");
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
			countryList = new HashSet<String>(freeBaseMapping.keySet());
			countryIdList = new HashSet<String>(freeBaseMapping.values());
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

	private NERNumberRelationMatching() {

	}

	public static NERNumberRelationMatching getInstance() {
		if (instance == null) {
			instance = new NERNumberRelationMatching();
		}
		return instance;
	}

	@Override
	public List<Triple<KBArgument, KBArgument, String>> matchRelations(
			List<Pair<Argument, Argument>> sententialInstances,
			KnowledgeBase KB, CoreMap sentence, Annotation doc) {

		Map<String, List<String>> entityMap = KB.getEntityMap();
		List<Triple<KBArgument, KBArgument, String>> distantSupervisionAnnotations = new ArrayList<>();

		/**
		 * One of the attributes has to be country, does not matter which one.
		 * The other has to be a number
		 */
		for (Pair<Argument, Argument> si : sententialInstances) {
			// for all the argument pairs
			Argument arg1 = si.first;
			Argument arg2 = si.second;
			Argument countryArg = null;
			Argument numberArg = null;
			String arg1Name = arg1.getArgName();
			String arg2Name = arg2.getArgName();
			List<String> arg1Ids = entityMap.get(arg1Name);
			List<String> arg2Ids = entityMap.get(arg2Name);
			String entityId = null, num = null, countryName = null;
			if (null == arg1Ids && null == arg2Ids) { // useless
				continue;
			} else if (null == arg2Ids && isCountry(arg1Ids.get(0))
					&& isNumber(arg2Name)) { // country and number
				entityId = arg1Ids.get(0);
				countryName = arg1Name;
				num = arg2Name;
				countryArg = arg1;
				numberArg = arg2;
				//System.out.println(arg1Name + " - " + arg2Name);
			} else if (null == arg1Ids && isCountry(arg2Ids.get(0))
					&& isNumber(arg1Name)) { // number and country
				entityId = arg2Ids.get(0);
				countryName = arg2Name;
				num = arg1Name;
				countryArg = arg2;
				numberArg = arg1;
				//System.out.println(arg2Name + " - " + arg1Name);
			} else if (null == arg1Ids || null == arg2Ids) { // the non null
																// entity is not
																// a country
				continue;
			} else {
				int countryArgPos = 0;
				for (String arg1Id : arg1Ids) {
					for (String arg2Id : arg2Ids) {
						if ((countryArgPos = isCountryNumberPair(arg1Name, arg1Id,
								arg2Name, arg2Id)) != -1) { // exact match
							if (countryArgPos == 1) {
								entityId = arg1Id;
								countryName = arg1Name;
								num = arg2Name;
								countryArg = arg1;
								numberArg = arg2;
								//System.out.println(arg1Name + " - " + arg2Name);
							} else {
								entityId = arg2Id;
								countryName = arg2Name;
								num = arg1Name;
								countryArg = arg2;
								numberArg = arg1;
								//System.out.println(arg2Name + " - " + arg1Name);
							}
						}
					}
				}
			}
			StringBuilder rel = new StringBuilder("");
			if ((null != entityId) && (null != num) && fuzzyMatch(entityId, num, rel)) {
				KBArgument kbarg1 = new KBArgument(countryArg, entityId);
				KBArgument kbarg2 = new KBArgument(numberArg, num);
				Triple<KBArgument,KBArgument,String> t = 
						new Triple<>(kbarg1,kbarg2,rel.toString());
				distantSupervisionAnnotations.add(t);
				System.out.println("Match: " + countryName + " -> " + rel.toString() + " = " + num);
			}

		}
		return distantSupervisionAnnotations;
	}

	/**
	 * Checks whether the supplied arguments form a country number pair
	 * 
	 * @return
	 */
	static int isCountryNumberPair(String arg1Name, String arg1Id,
			String arg2Name, String arg2Id) {
		boolean isPair = countryIdList.contains(arg1Id)
				&& numberPat.matcher(arg2Name).matches()
				|| countryIdList.contains(arg2Id)
				&& numberPat.matcher(arg1Name).matches();
		if (!isPair) {
			return -1;
		}
		if (isCountry(arg2Id)) {
			return 2;
		}
		if (isCountry(arg1Id)) {
			return 1;
		}
		return -1;
	}

	static boolean isCountry(String argId) {
		return countryIdList.contains(argId);
	}

	static boolean isNumber(String numberArg) {
		return numberPat.matcher(numberArg).matches();
	}

	/**
	 * Takes an entityId, a number and iterates over all the relations of the
	 * number to see if there can be a match. TODO : This code should be moved
	 * to a class of its own. We plan to implement several methods like Gaussian
	 * matching etc. things may get out of hand pretty soon.
	 * 
	 * @param entityId
	 * @param num
	 */
	boolean fuzzyMatch(String entityId, String num, StringBuilder rel) {
		Number number = null;
		for (Locale l : Locale.getAvailableLocales()) {
			NumberFormat format = NumberFormat.getInstance(l);
			try {
				number = format.parse(num);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				continue;
			}
			break;
		}
		if(null == number){
			return false;
		}
		Double numVal = number.doubleValue();
		
		ArrayList<FuzzyFact> facts = fkb.getFactsForId(entityId);
		for (FuzzyFact fact : facts) {
			if (fact.isMatch(numVal, rel)) {
				return true;
			}
		}
		return false;
	}

}
