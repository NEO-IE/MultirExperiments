package edu.washington.multirframework.multiralgorithm;

import iitb.rbased.meta.KeywordData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import edu.washington.multirframework.util.StemUtils;

public class KeywordClamping {

	public static HashMap<String, Double> marginMap;

	static {
		marginMap = new HashMap<String, Double>();
	}

	public static Parse infer(MILDocument doc, Scorer parseScorer, Parameters params) {
		Parse p = new Parse();
		p.doc = doc;
		parseScorer.setParameters(params);
		int numMentions = doc.numMentions;
		int z[] = new int[numMentions];

		for (int z_i = 0; z_i < numMentions; z_i++) {
			HashSet<Integer> feats = new HashSet<>();
			for (int id : doc.features[z_i].ids) {
				feats.add(id);
			}

			for (Integer relId : AveragedPerceptron.relNumNameMapping.keySet()) {
				String relName = AveragedPerceptron.relNumNameMapping.get(relId);
				if (hasKeyword(feats, relName)) {
					z[z_i] = relId;
				}
				// Make the number true if one of the Z nodes attached expresses
				// the relation
			}
		}
		p.Z = z;
		p.Y = doc.Y;
		return p;
	}

	public static boolean hasKeyword(HashSet<Integer> feats, String rel) {
		List<String> relKey = KeywordData.REL_KEYWORD_MAP.get(rel);
		for (String key : relKey) {
			String stemKey = StemUtils.getStemWord(key.toLowerCase());
			Integer featID = AveragedPerceptron.featNameNumMapping.get("key: " + stemKey);
			if (featID != null) {
				if (feats.contains(featID)) {
					return true;
				}
			}
		}
		return false;
	}
}
