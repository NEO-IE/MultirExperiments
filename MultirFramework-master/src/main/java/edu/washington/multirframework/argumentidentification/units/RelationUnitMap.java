package edu.washington.multirframework.argumentidentification.units;

import java.util.HashMap;

public class RelationUnitMap {
	private static HashMap<String, String> unitMap = null;
	static {
		unitMap = new HashMap<String, String>();
		unitMap.put("AG.LND.TOTL.K2", "square metre");
		unitMap.put("BN.KLT.DINV.CD", "united states dollar");
		unitMap.put("BX.GSR.MRCH.CD", "united states dollar");
		unitMap.put("EG.ELC.PROD.KH", "joule");
		unitMap.put("EN.ATM.CO2E.KT", "kilogram");
		unitMap.put("EP.PMP.DESL.CD", "united states dollar per litre");
		unitMap.put("FP.CPI.TOTL.ZG", "percent");
		unitMap.put("IT.NET.USER.P2", "percent");
		unitMap.put("NY.GDP.MKTP.CD", "united states dollar");
		unitMap.put("SP.DYN.LE00.IN", "second");
		unitMap.put("SP.POP.TOTL", "");

	}

	public static String getUnit(String rel) {

		return unitMap.get(rel);
	}

}
