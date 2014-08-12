//sg
/**
 * This class marks countries in a piece of text
 * This is used to fill the entity linking column in freebase
 */
package edu.washington.multir.preprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.lang3.text.WordUtils;

public class CountryMarker {
	/**
	 * We maintain 3 forms of every country : a) India, Indi and Ind which has
	 * no space in its name. Some of the variations like US, USA were hardcoded
	 * in the file Then we iterate over words in the sentence and see if they
	 * match one of 3 forms of any of the countries
	 */
	HashMap<String, String> freeBaseMapping;
	HashMap<String, String> completeNameMapping;
	HashSet<String> countryList;

	public CountryMarker(String countriesFile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(new File(
				countriesFile)));
		String countryName = null;
		completeNameMapping = new HashMap<String, String>();
		freeBaseMapping = new HashMap<String, String>();
		while ((countryName = br.readLine()) != null) {
			String vars[] = countryName.split("\t");
			int nl = vars[0].length();
			// inflections of country names will never involve more than 2 last
			// chars
			if (nl > 3) {
				// save the characters we have churned out
				String suffix = vars[0].substring(nl - 2, nl);
				vars[0] = vars[0].substring(0, nl - 2);
				completeNameMapping.put(vars[0].toLowerCase(), suffix);
				// need this because chinese must be stored as China, their
				// Freebase entry

			}

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

	ArrayList<String> getEntityLinkInformation(String sentence) {
		StringBuilder entityLinkStringBuilder = new StringBuilder();
		StringBuilder typeStringBuilder = new StringBuilder();
		ArrayList<String> res = new ArrayList<String>();
		int wordOffSet = 0;
		String words[] = sentence.split(" ");
		String popularAbbrList[] = { "USA", "UK", "US" };
		HashSet<String> popularAbbrSet = new HashSet<>();
		for (String str : popularAbbrList) {
			popularAbbrSet.add(str);
		}

		// for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
		// String word = token.get(TextAnnotation.class);
		for (String word : words) {
			int wl = word.length();
			int suffixEnd = 2;
			if (wl <= 3) { // US perhaps?
				if (popularAbbrSet.contains(word)) { // we have a country match
					String entityName = word;
					String freeBaseId = freeBaseMapping.get(word.toLowerCase());
					String linkingString = wordOffSet + " " + (wordOffSet + 1)
							+ " " + WordUtils.capitalize(entityName) + " "
							+ freeBaseId + " 1 ";
					entityLinkStringBuilder.append(linkingString);
					
					String typeString = wordOffSet + " " + (wordOffSet + 1)
							+ "/location/country " + freeBaseId + " ";
					typeStringBuilder.append(typeString);
				}
				continue;
			}
			while (suffixEnd < (wl - 5)) {
				String keyWord = word.substring(0, wl - suffixEnd);
				suffixEnd++;
				keyWord = keyWord.toLowerCase(); // map keys are lower case
				if (countryList.contains(keyWord)) { // we have a country match
					String entityName = keyWord
							+ completeNameMapping.get(keyWord);
					String freeBaseId = freeBaseMapping.get(keyWord);
					String linkingString = wordOffSet + " " + (wordOffSet + 1)
							+ " " + WordUtils.capitalize(entityName) + " "
							+ freeBaseId + " 1 ";
					entityLinkStringBuilder.append(linkingString);
					String typeString = wordOffSet + " " + (wordOffSet + 1)
							+ "/location/country " + freeBaseId + " ";
					typeStringBuilder.append(typeString);
					break;
				}
			}
			wordOffSet++;
		}
		res.add(entityLinkStringBuilder.toString());
		res.add(typeStringBuilder.toString());
		return res;
	}

	public static void main(String args[]) throws IOException {
		/**
		 * This is a tester for the country tagger
		 */

		String countriesFile = "meta/country_freebase_mapping";
		CountryMarker cmr = new CountryMarker(countriesFile);
		System.out
				.println(cmr
						.getEntityLinkInformation("Air China is progressing fast with Indians and Portugese music How will UK and USA react to this"));

	}
}
