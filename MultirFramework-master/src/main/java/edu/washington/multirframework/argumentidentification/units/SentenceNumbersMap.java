/**
 * The unit tagger is run a priori on the corpus and the results are stored in the following format :
 * SentenceId	number:unit:startOff:endOff;number2:unit:startOff:endOff;number3:unit:startOff:endOff
 * This class parses this file and stores the information in a map
 */
package edu.washington.multirframework.argumentidentification.units;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import edu.washington.multirframework.data.NumberArgument;

public class SentenceNumbersMap {
	static HashMap<Integer, ArrayList<NumberArgument>> sentNumMap;
	SentenceNumbersMap instance = null;
	private static String SENTIDSEPERATOR = "\t";
	private static String NUMSEPERATOR = ";";
	private static String FIELDSEPERATOR = ":";

	private static int SENTID_INDEX = 0;
	private static int NUMBER_INDEX = 0;
	private static int UNIT_INDEX = 1;
	private static int STARTOFF_INDEX = 2;
	private static int ENDOFF_INDEX = 3;

	private static final String numberMapFile = "data/sentenceNumberMapFile";
	static {
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(
					numberMapFile)));
			String line = null;
			sentNumMap = new HashMap<Integer, ArrayList<NumberArgument>>();
			
			while ((line = br.readLine()) != null) {
				ArrayList<NumberArgument> numsOnLine = new ArrayList<NumberArgument>();
				String lineSplit[] = line.split(SENTIDSEPERATOR);
				Integer sentId = Integer.parseInt(lineSplit[SENTID_INDEX]);
				String nums[] = lineSplit[SENTID_INDEX + 1].split(NUMSEPERATOR);
				for (String num : nums) {
					String numSplit[] = num.split(FIELDSEPERATOR);
					numsOnLine.add(new NumberArgument(numSplit[NUMBER_INDEX],
							Integer.parseInt(numSplit[STARTOFF_INDEX]), Integer.parseInt(numSplit[ENDOFF_INDEX]),
							numSplit[UNIT_INDEX]));
				}
				sentNumMap.put(sentId, numsOnLine);
			}
		} catch (IOException ioe) {
			System.err.println(ioe);
		}

	}

	private SentenceNumbersMap() {
		// TODO Auto-generated constructor stub
	}

	public SentenceNumbersMap getInstance() {
		if (null == instance) {
			instance = new SentenceNumbersMap();
			return instance;
		}
		return instance;
	}

}
