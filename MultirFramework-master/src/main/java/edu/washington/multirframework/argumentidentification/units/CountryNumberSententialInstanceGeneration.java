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

	@Override
	public List<Pair<Argument, Argument>> generateSententialInstances(
			List<Argument> countryArguments, CoreMap sentence) {
		//get the sentence id
		int sentId = sentence.get(SentGlobalID.class);
		//pull all the numbers for this sentence
		List<NumberArgument> numbers = null;//getAllNumbersForSentence(sentId);
		List< Pair<Argument, Argument> > argPairs = new ArrayList<Pair<NumberArgument, Argument>>();
		for(Argument countryArg : countryArguments) {
			for(Argument number : numbers) {
				argPairs.add(new Pair<Argument, Argument>(countryArg, number));
			}
		}
		return argPairs;
	}
	

}
