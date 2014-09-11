//sg
package edu.washington.multirframework.argumentidentification.units;

import java.util.List;

import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.data.Argument;
/**
 * This overriding implementation returns a list of all the country number pairs. It essentially takes a cross product of
 * all the countries and all the numbers
 * @author aman
 *
 */
public class CountryNumberSententialInstanceGeneration implements SententialInstanceGeneration {

	@Override
	public List<Pair<Argument, Argument>> generateSententialInstances(
			List<Argument> arguments, CoreMap sentence) {
		// TODO Auto-generated method stub
		return null;
	}
	

}
