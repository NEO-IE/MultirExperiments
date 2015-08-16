package edu.washington.multirframework.multiralgorithm;

import java.util.HashMap;
import java.util.Map;


public class FullInferenceAtleastK {
	
	
	public static Parse infer(MILDocument doc,
			Scorer parseScorer, Parameters params) {
		Parse parse = new Parse();
		parse.doc = doc;
		parse.Z = new int[doc.numMentions];
		double K = 0.5;
		//parse.allScores = new double[doc.numMentions][params.model.numRelations];
		
		parseScorer.setParameters(params);
		
		Viterbi viterbi = new Viterbi(params.model, parseScorer);
		
		double[] scores = new double[params.model.numRelations];
		for (int i=0; i < scores.length; i++) scores[i] = Double.NEGATIVE_INFINITY;
		boolean[] binaryYs = new boolean[params.model.numRelations];
		int[] countYs = new int[params.model.numRelations];
		
		int numYs = 0;
		// loop over all mentions of the instance, finding the highest probability
		// relation for each and storing it in scores..
		for (int m = 0; m < doc.numMentions; m++) {
			Viterbi.Parse p = viterbi.parse(doc, m);
			
			parse.Z[m] = p.state;
			if (p.state > 0) {
				countYs[p.state]++;
				numYs++;
			}
			
			if (p.score > scores[parse.Z[m]])
				scores[parse.Z[m]] = p.score;
			//parse.allScores[m] = p.scores;
		}
		for(int i = 1; i < countYs.length; i++) {
			binaryYs[i] = ((double)countYs[i] / doc.numMentions) > K;
		}
		// parse.y is an array the size of the number of relations that
		// were predicted for this instance it stores the relation id
		parse.Y = new int[numYs];
		int pos = 0;
		for (int i=1; i < binaryYs.length; i++)
			if (binaryYs[i]) {
				parse.Y[pos++] = i;
				if (pos == numYs) break;
			}
		
		parse.scores = scores;
		
		// It's important to ignore the _NO_RELATION_ type here, so
		// need to start at 1!
		// final value is avg of maxes
		int sumNum = 0;
		double sumSum = 0;
		for (int i=1; i < scores.length; i++)
			if (scores[i] > Double.NEGATIVE_INFINITY) { 
				sumNum++; sumSum += scores[i]; 
			}
		if (sumNum ==0) parse.score = Double.NEGATIVE_INFINITY;
		else parse.score = sumSum / sumNum;
		
		return parse;		
	}

	
}
