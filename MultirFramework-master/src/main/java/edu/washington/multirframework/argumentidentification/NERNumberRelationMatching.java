package edu.washington.multirframework.argumentidentification;

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
import java.util.regex.Pattern;

import org.apache.commons.lang.NumberUtils;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.KBArgument;
import edu.washington.multirframework.knowledgebase.KnowledgeBase;

public class NERNumberRelationMatching implements RelationMatching {

	static HashMap<String, String> freeBaseMapping;
	static HashSet<String> countryList;
	static HashSet<String> countryIdList;
	static Pattern numberPat;

	static NERNumberRelationMatching instance;

	private NERNumberRelationMatching() {

	}

	public static NERNumberRelationMatching getInstance() {
		if (instance == null) {
			instance = new NERNumberRelationMatching();
		}
		return instance;
	}

	static {
		numberPat = Pattern.compile("^[\\+-]?\\d+([,\\.]\\d+)?([eE]-?\\d+)?$");
		String countriesFile = "data/countries_file";
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(
					countriesFile)));
			String countryRecord = null;
			freeBaseMapping = new HashMap<String, String>();

			while ((countryRecord = br.readLine()) != null) {
				System.out.println(countryRecord);
				String vars[] = countryRecord.split("\t");
				String countryName = vars[1].toLowerCase();
				String countryId = vars[0];
				// System.out.println(countryName);
				freeBaseMapping.put(countryName, countryId);
			}
			countryList = new HashSet<String>(freeBaseMapping.keySet());
			countryIdList = new HashSet<String>(freeBaseMapping.values());
			// System.out.println(countryList);
			// "US/USA gets special treatment as always
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
			String arg1Name = arg1.getArgName();
			String arg2Name = arg2.getArgName();
			Set<String> relationsFound = new HashSet<String>();
			List<String> arg1Ids = entityMap.get(arg1Name);
			List<String> arg2Ids = entityMap.get(arg2Name);
			if (null == arg1Ids && null == arg2Ids) { //useless
				continue;
			} else if (null == arg2Ids && isCountry(arg1Ids.get(0)) && isNumber(arg2Name)) { //country and number 
				System.out.println(arg1Name + " - " + arg2Name); 
			} else if (null == arg1Ids && isCountry(arg2Ids.get(0)) && isNumber(arg1Name)) { //number and country 
				System.out.println(arg2Name + " - " + arg1Name);
			} else if(null == arg1Ids || null == arg2Ids) { //the non null entity is not a country
				continue;
			} else {
				int countryArg = 0;
				for (String arg1Id : arg1Ids) {
					for (String arg2Id : arg2Ids) {
						if ((countryArg = isCountryNumberPair(arg1Name, arg1Id, arg2Name, arg2Id)) != -1) { //exact match
							if (countryArg == 1) {
								System.out.println(arg1Name + " - " + arg2Name);
							} else {
								System.out.println(arg2Name + " - " + arg1Name);
							}
						}
					}
				}
			}
		}
		return distantSupervisionAnnotations;
	}

	/**
	 * Checks whether the supplied arguments form a country number pair
	 * 
	 * @return
	 */
	static int isCountryNumberPair(String arg1Name, String arg1Id, String arg2Name, String arg2Id) {
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
}
