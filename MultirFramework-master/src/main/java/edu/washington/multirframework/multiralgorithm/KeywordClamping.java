package edu.washington.multirframework.multiralgorithm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import scala.actors.threadpool.Arrays;
import edu.washington.multirframework.util.KeywordData;
import edu.washington.multirframework.util.StemUtils;

public class KeywordClamping {

	public static HashMap<String, Double> marginMap;
	public static HashMap<MILDocument, Parse> cache;

	static {
		marginMap = new HashMap<String, Double>();
		cache = new HashMap<MILDocument, Parse>();
	}

	public static Parse infer(MILDocument doc, Scorer parseScorer, Parameters params) {
		Parse p = null;
		//if((p = cache.get(doc)) != null) {
		//	System.err.println("Returning from the cache");
	//		return p;
	//	}
		//.err.println("Not found in the cache, calculating");
		p = new Parse();
		p.doc = doc;
		parseScorer.setParameters(params);
		int numMentions = doc.numMentions;
		int z[] = new int[numMentions];

		for (int z_i = 0; z_i < numMentions; z_i++) {
			HashSet<Integer> feats = new HashSet<Integer>();
			for (int id : doc.features[z_i].ids) {
				feats.add(id);
			}

			for (Integer relId : AveragedPerceptron.relNumNameMapping.keySet()) {
				String relName = AveragedPerceptron.relNumNameMapping.get(relId);
				if (hasKeyword(feats, relName)) {
					z[z_i] = relId;
					break;
				}
				// Make the number true if one of the Z nodes attached expresses
				// the relation
			}
		}
		p.Z = z;
		p.Y = doc.Y;
		//cache.put(doc, p);
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
