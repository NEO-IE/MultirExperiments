//sg
package edu.washington.multir.preprocess;
/**
 * This class represents a row in the apache derby corpus
 * @author aman
 *
 */
public class CorpusRow {
	//The columns in the corpus 
	int sentId;
	String docName;
	String tokenInformation;
	String textInformation;
	String sentOffSetInformation;
	String sentdependencyInformation;
	String sentNELinkingInformation;
	String sentFreebaseInformation;
	String tokenNERInformation;
	String tokenOffsetInformation;
	String tokenPOSInformation;
	String tokenChunkInformation;
	public CorpusRow(int sentId, String docName, String tokenInformation,
			String textInformation, String sentOffSetInformation,
			String sentdependencyInformation, String sentNELinkingInformation,
			String sentFreebaseInformation, String tokenNERInformation,
			String tokenOffsetInformation, String tokenPOSInformation,
			String tokenChunkInformation) {
		super();
		this.sentId = sentId;
		this.docName = docName;
		this.tokenInformation = tokenInformation;
		this.textInformation = textInformation;
		this.sentOffSetInformation = sentOffSetInformation;
		this.sentdependencyInformation = sentdependencyInformation;
		this.sentNELinkingInformation = sentNELinkingInformation;
		this.sentFreebaseInformation = sentFreebaseInformation;
		this.tokenNERInformation = tokenNERInformation;
		this.tokenOffsetInformation = tokenOffsetInformation;
		this.tokenPOSInformation = tokenPOSInformation;
		this.tokenChunkInformation = tokenChunkInformation;
	}
	@Override
	public String toString() {
		return  sentId + " \n" + docName
				+ " \n" + tokenInformation
				+ " \n" + textInformation
				+ " \n" + sentOffSetInformation
				+ " \n" + sentdependencyInformation
				+ " \n" + sentNELinkingInformation
				+ " \n" + sentFreebaseInformation
				+ " \n" + tokenNERInformation
				+ " \n" + tokenOffsetInformation
				+ " \n" + tokenPOSInformation
				+ " \n" + tokenChunkInformation + "]";
	}
}
