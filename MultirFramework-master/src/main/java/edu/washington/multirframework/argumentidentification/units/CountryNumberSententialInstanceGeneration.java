//sg
package edu.washington.multirframework.argumentidentification.units;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.NumberArgument;
/**
 * This overriding implementation returns a list of all the country number pairs. It essentially takes a cross product of
 * all the countries and all the numbers
 * @author aman
 *
 */
public class CountryNumberSententialInstanceGeneration implements SententialInstanceGeneration {

	private static CountryNumberSententialInstanceGeneration instance = null;
	private CountryNumberSententialInstanceGeneration() {
		
	}
	public static CountryNumberSententialInstanceGeneration getInstance() {
		if(null == instance) {
			instance = new CountryNumberSententialInstanceGeneration();
			return instance;
		}
		return instance;
	}
	
	@Override
	public List<Pair<Argument, Argument>> generateSententialInstances(
			List<Argument> countryArguments, CoreMap sentence) {
		//throw new NotImplementedException();
		ArrayList< Pair<Argument, Argument> > argPairs = new ArrayList<Pair<Argument, Argument>>();
		
		System.out.println("Hi!");
		//get the sentence id
		int sentId = sentence.get(SentGlobalID.class);
		System.out.println(sentId);
		//pull all the numbers for this sentence
		ArrayList<NumberArgument> numbers = SentenceNumbersMap.getNumbersForSentId(sentId);
		if((null != numbers) && numbers.size() > 0) {
			System.out.println(numbers);
		} else {
			return argPairs;
		}
		
		for(Argument countryArg : countryArguments) {
			for(NumberArgument number : numbers) {
				argPairs.add(new Pair<Argument, Argument>(countryArg, number));
			}
		}
		return argPairs;
		
	}
	

}
