package edu.washington.multirframework.argumentidentification;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.washington.multirframework.corpus.TokenOffsetInformation.SentenceRelativeCharacterOffsetBeginAnnotation;
import edu.washington.multirframework.corpus.TokenOffsetInformation.SentenceRelativeCharacterOffsetEndAnnotation;
import edu.washington.multirframework.data.Argument;

/**
 * Implements <code>ArgumentIdentification</code> method
 * <code>identifyArguments</code> to get all Arguments with an NER type.
 * 
 * @author jgilme1
 * 
 */
public class NERAndNumberArgumentIdentification implements
		ArgumentIdentification {

	// only NER Types considered
	private static String[] relevantNERTypes = { "ORGANIZATION", "PERSON",
			"LOCATION" };

	private static NERAndNumberArgumentIdentification instance = null;
	
	static Pattern numberPat;
	static BufferedWriter numberWriter;
	HashSet<String> countryList;

	private static final String countriesFileName = "data/numericalkb/countries_list";

	private NERAndNumberArgumentIdentification() {

		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(
					NERAndNumberArgumentIdentification.countriesFileName));
			numberWriter = new BufferedWriter(new FileWriter("numbers"));
			String countryName = null;
			countryList = new HashSet<>();
			while ((countryName = br.readLine()) != null) {
				countryList.add(countryName.toLowerCase());
			}
			br.close();
			numberPat = Pattern.compile("^[\\+-]?\\d+([,\\.]\\d+)?([eE]-?\\d+)?$");
		} catch (IOException e) {
			System.err.println(e);
		}
	}

	public static NERAndNumberArgumentIdentification getInstance() {
		if (instance == null)
			instance = new NERAndNumberArgumentIdentification();
		return instance;
	}

	@Override
	/**
	 * Returns a List of Argument for all of the unique arguments in
	 * the sentence.
	 */
	public List<Argument> identifyArguments(Annotation d, CoreMap s) {
		List<Argument> arguments = new ArrayList<Argument>();
		List<CoreLabel> tokens = s.get(CoreAnnotations.TokensAnnotation.class);
		List<List<CoreLabel>> argumentTokenSpans = new ArrayList<List<CoreLabel>>();

		// add candidate token spans
		for (int i = 0; i < tokens.size();) {
			if (isRelevant(tokens.get(i))) {
				List<CoreLabel> tokenSequence = getRelevantTokenSequence(
						tokens, i);
				argumentTokenSpans.add(tokenSequence);
				i += tokenSequence.size();
			} else {
				i++;
			}
		}

		// for each candidate string check in the KB for all ids that
		// share that string and return as possible arguments
		for (List<CoreLabel> argumentTokenSpan : argumentTokenSpans) {
			StringBuilder argumentSB = new StringBuilder();
			for (CoreLabel token : argumentTokenSpan) {
				argumentSB.append(token.value());
				argumentSB.append(" ");
			}
			String argumentString = argumentSB.toString().trim();
			int tokenStartOffset = argumentTokenSpan.get(0).get(
					SentenceRelativeCharacterOffsetBeginAnnotation.class);
			int tokenEndOffset = argumentTokenSpan.get(
					argumentTokenSpan.size() - 1).get(
					SentenceRelativeCharacterOffsetEndAnnotation.class);

			Argument arg = new Argument(argumentString, tokenStartOffset,
					tokenEndOffset);
			arguments.add(arg);
		}
		return arguments;
	}

	// get contiguous sequences of tokens that share a relevant named entity
	// type
	private List<CoreLabel> getRelevantTokenSequence(List<CoreLabel> tokens,
			int i) {
		List<CoreLabel> tokenSequence = new ArrayList<CoreLabel>();
		tokenSequence.add(tokens.get(i));
		String ner = tokens.get(i).get(
				CoreAnnotations.NamedEntityTagAnnotation.class);
		i++;
		while (i < tokens.size()) {
			String nextNer = tokens.get(i).get(
					CoreAnnotations.NamedEntityTagAnnotation.class);
			if (ner.equals(nextNer)) {
				tokenSequence.add(tokens.get(i));
			} else {
				break;
			}
			i++;
		}
		return tokenSequence;
	}

	private boolean isCountry(String s) {
		return countryList.contains(s.toLowerCase());
	}
	
	private boolean isRelevant(CoreLabel token) {
		String ner = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
		for (String relevantNER : relevantNERTypes) {
			if (relevantNER.equals(ner)) {
				return true;
			}
		}
		String tokenString = token.toString();
		boolean isNumber =  numberPat.matcher(tokenString).matches();
		if (isNumber) {
			try {
			numberWriter.write(tokenString + "\n");
			} catch(IOException ioe) {
				System.out.println(ioe);
			}
			return (tokenString.length() > 1);
		}
		if (isCountry(token.toString())) {
			return true;
		}
		return false;
	}
}
