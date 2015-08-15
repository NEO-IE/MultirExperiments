package edu.washington.multirframework.multiralgorithm;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

public class AveragedPerceptron {

	public int maxIterations = 50;
	public boolean computeAvgParameters = true;
	public boolean updateOnTrueY = true;
	public double delta = 1;

	private Scorer scorer;
	private Model model;
	private Random random;

	/**
	 * Data structures to store information about keyword features
	 */
	public static HashMap<Integer, String> relNumNameMapping;
	public static HashMap<String, Integer> featNameNumMapping;
	public static HashMap<Integer, String> featureList;
	Integer numRelation;
	Integer numFeatures;
	
	boolean readMapping = false;
	
	public AveragedPerceptron(Model model, Random random, String mappingFile) {
		scorer = new Scorer();
		this.model = model;
		this.random = random;
		if(readMapping) {
			relNumNameMapping = new HashMap<Integer, String>();
			featNameNumMapping = new HashMap<String, Integer>();
			featureList = new HashMap<Integer, String>();
			BufferedReader featureReader;
			try {
				featureReader = new BufferedReader(new FileReader(
						mappingFile));
		
			Integer numRel = Integer.parseInt(featureReader.readLine());
			for (int i = 0; i < numRel; i++) {
				// skip relation names
				String rel = featureReader.readLine().trim();
				relNumNameMapping.put(i, rel);
			}
			int numFeatures = Integer.parseInt(featureReader.readLine());
			String ftr = null;
			featureList = new HashMap<Integer, String>();
			int fno = 0;
			while (fno < numFeatures) {
				ftr = featureReader.readLine().trim();
				String parts[] = ftr.split("\t");
				featNameNumMapping.put(parts[1], Integer.parseInt(parts[0]));
				featureList.put(fno, ftr);
				fno++;
			}
			featureReader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// the following two are actually not storing weights:
	// the first is storing the iteration in which the average weights were
	// last updated, and the other is storing the next update value
	private Parameters avgParamsLastUpdatesIter;
	private Parameters avgParamsLastUpdates;

	private Parameters avgParameters;
	private Parameters iterParameters;

	public Parameters train(Dataset trainingData) {

		if (computeAvgParameters) {
			avgParameters = new Parameters();
			avgParameters.model = model;
			avgParameters.init();

			avgParamsLastUpdatesIter = new Parameters();
			avgParamsLastUpdates = new Parameters();
			avgParamsLastUpdatesIter.model = avgParamsLastUpdates.model = model;
			avgParamsLastUpdatesIter.init();
			avgParamsLastUpdates.init();
		}

		iterParameters = new Parameters();
		iterParameters.model = model;
		iterParameters.init();

		for (int i = 0; i < maxIterations; i++)
			trainingIteration(i, trainingData);

		if (computeAvgParameters) finalizeRel();
		
		return (computeAvgParameters) ? avgParameters : iterParameters;
	}

	int avgIteration = 0;

	public void trainingIteration(int iteration, Dataset trainingData) {
		System.out.println("iteration " + iteration);

		MILDocument doc = new MILDocument();

		trainingData.shuffle(random);

		trainingData.reset();

		while (trainingData.next(doc)) {

			// compute most likely label under current parameters
			Parse predictedParse = FullInference.infer(doc, scorer,
					iterParameters);

			if (updateOnTrueY || !YsAgree(predictedParse.Y, doc.Y)) {
				// if this is the first avgIteration, then we need to initialize
				// the lastUpdate vector
				if (computeAvgParameters && avgIteration == 0)
					avgParamsLastUpdates.sum(iterParameters, 1.0f);

				Parse trueParse = ConditionalInference.infer(doc, scorer,
					iterParameters);
				update(predictedParse, trueParse);
			}

			if (computeAvgParameters) avgIteration++;
		}
	}

	private boolean YsAgree(int[] y1, int[] y2) {
		if (y1.length != y2.length)
			return false;
		for (int i = 0; i < y1.length; i++)
			if (y1[i] != y2[i])
				return false;
		return true;
	}

	// a bit dangerous, since scorer.setDocument is called only inside inference
	public void update(Parse pred, Parse tru) {
		int numMentions = tru.Z.length;

		// iterate over mentions
		for (int m = 0; m < numMentions; m++) {
			int truRel = tru.Z[m];
			int predRel = pred.Z[m];

			if (truRel != predRel) {
				SparseBinaryVector v1a = scorer.getMentionRelationFeatures(
						tru.doc, m, truRel);
				updateRel(truRel, v1a, delta, computeAvgParameters);

				SparseBinaryVector v2a = scorer.getMentionRelationFeatures(
						tru.doc, m, predRel);
				updateRel(predRel, v2a, -delta, computeAvgParameters);
			}
		}
	}

	private void updateRel(int toState, SparseBinaryVector features,
			double delta, boolean useIterAverage) {
		iterParameters.relParameters[toState].addSparse(features, delta);

		if (useIterAverage) {
			DenseVector lastUpdatesIter = (DenseVector) avgParamsLastUpdatesIter.relParameters[toState];
			DenseVector lastUpdates = (DenseVector) avgParamsLastUpdates.relParameters[toState];
			DenseVector avg = (DenseVector) avgParameters.relParameters[toState];
			DenseVector iter = (DenseVector) iterParameters.relParameters[toState];
			for (int j = 0; j < features.num; j++) {
				int id = features.ids[j];
				if (lastUpdates.vals[id] != 0)
					avg.vals[id] += (avgIteration - lastUpdatesIter.vals[id])
							* lastUpdates.vals[id];

				lastUpdatesIter.vals[id] = avgIteration;
				lastUpdates.vals[id] = iter.vals[id];
			}
		}
	}

	private void finalizeRel() {
		for (int s = 0; s < model.numRelations; s++) {
			DenseVector lastUpdatesIter = (DenseVector) avgParamsLastUpdatesIter.relParameters[s];
			DenseVector lastUpdates = (DenseVector) avgParamsLastUpdates.relParameters[s];
			DenseVector avg = (DenseVector) avgParameters.relParameters[s];
			for (int id = 0; id < avg.vals.length; id++) {
				if (lastUpdates.vals[id] != 0) {
					avg.vals[id] += (avgIteration - lastUpdatesIter.vals[id])
							* lastUpdates.vals[id];
					lastUpdatesIter.vals[id] = avgIteration;
				}
			}
		}
	}
}
