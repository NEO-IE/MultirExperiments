//sg
/**
 * This class marks countries in a piece of text
 * This is used to fill the entity linking column in freebase
 */
package edu.washington.multir.preprocess.marker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.lang3.text.WordUtils;

import scala.actors.threadpool.Arrays;

public class CountryMarker implements Marker{
	/**
	 * We maintain 3 forms of every country : a) India, Indi and Ind which has
	 * no space in its name. Some of the variations like US, USA were hardcoded
	 * in the file Then we iterate over words in the sentence and see if they
	 * match one of 3 forms of any of the countries
	 */
	HashMap<String, String> freeBaseMapping;
	HashMap<String, String> completeNameMapping;
	HashSet<String> countryList;
	String[] popularAbbrList = { "USA", "UK", "US" };
	HashSet<String> popularAbbrSet;


	public CountryMarker(String countriesFile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(new File(
				countriesFile)));
		String countryName = null;
		popularAbbrSet = new HashSet<String>(Arrays.asList(popularAbbrList));
		completeNameMapping = new HashMap<String, String>();
		freeBaseMapping = new HashMap<String, String>();
		while ((countryName = br.readLine()) != null) {
			String vars[] = countryName.split("\t");
			String oriName = vars[0];
			int nl = vars[0].length();
			// inflections of country names will never involve more than 2 last
			// chars
			if (nl > 3) {
				// save the characters we have churned out
				String suffix = vars[0].substring(nl - 2, nl);
				vars[0] = vars[0].substring(0, nl - 2);
				completeNameMapping.put(vars[0].toLowerCase(), suffix);
				// need this because chinese must be stored as China in the derby, their
				// Freebase entry

			}
			freeBaseMapping.put(oriName, vars[1]);
			freeBaseMapping.put(vars[0].toLowerCase(), vars[1]);

		}
		countryList = new HashSet<String>(freeBaseMapping.keySet());
		// "US/USA gets special treatment as always

		br.close();
	}

	/**
	 * This method returns matchings of all the countries in the following
	 * format StartOffset EndOffSet + 1 Country (as in freebase) freebase id 1
	 * The last one signifies the confidence with which we find the entity TODO
	 * : verify whether it is really the score of the ner or not? TODO : This is
	 * (will be) painfully slow, find out another way of doing this
	 */

	@Override
	public ArrayList<Marking> mark(String sentence) {
		ArrayList<Marking> res = new ArrayList<Marking>();
		int wordOffSet = 0;
		String words[] = sentence.split(" ");
		
		// for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
		// String word = token.get(TextAnnotation.class);
		for (String word : words) {
			int wl = word.length();
			int rightTrimLen = 2;
			if (wl <= 3) { // US perhaps?
				if (popularAbbrSet.contains(word)) { // we have a country match
					String entityName = word;
					String freeBaseId = freeBaseMapping.get(word.toLowerCase());
					Marking m = new Marking(wordOffSet, wordOffSet + 1, WordUtils.capitalize(entityName), freeBaseId, 1, Marking.COUNTRY);
					res.add(m);
				}
		
			} else if(countryList.contains(word)) { //exact match?
				String freeBaseId = freeBaseMapping.get(word);
				Marking m = new Marking(wordOffSet, wordOffSet + 1, WordUtils.capitalize(word), freeBaseId, 1, Marking.COUNTRY);
				res.add(m);
			
			} else {
			while (rightTrimLen <= (wl - 4)) {
				String keyWord = word.substring(0, wl - rightTrimLen);
				
				rightTrimLen++;
				keyWord = keyWord.toLowerCase(); // map keys are lower case
				//System.out.println(keyWord);
				if (countryList.contains(keyWord)) { // we have a country match
					String entityName = keyWord
							+ completeNameMapping.get(keyWord);
					String freeBaseId = freeBaseMapping.get(keyWord);
					Marking m = new Marking(wordOffSet, wordOffSet + 1, WordUtils.capitalize(entityName), freeBaseId, 1, Marking.COUNTRY);
					res.add(m);
					break;
				}
			}
			}
			wordOffSet++;
		}
		return res;
	}

	public static void main(String args[]) throws IOException {
		/**
		 * This is a tester for the country tagger
		 */
		String countriesFile = "meta/country_freebase_mapping";
		CountryMarker cmr = new CountryMarker(countriesFile);
		//System.out.println(cmr.mark("Israeli officials have voiced Indian fears that the import of materials like cement into the strip could be used to re-store the network of tunnels destroyed during the conflict and which Palestinian fighters have used to infiltrate Israel ."));
		System.out.println(cmr.mark("Aruba has a GDP of 1330167550"));
	}
}
