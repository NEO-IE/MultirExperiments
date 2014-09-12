package edu.washington.multirframework.argumentidentification.units;

import java.util.HashMap;

public class RelationUnitMap {
	private static HashMap<String, String>  unitMap = null;
	static {
		unitMap = new HashMap<String, String>();
		unitMap.put("AG.LND.TOTL.K2","");
		unitMap.put("BN.KLT.DINV.CD","");
		unitMap.put("BX.GSR.MRCH.CD","");
		unitMap.put("EG.ELC.PROD.KH","");
		unitMap.put("EN.ATM.CO2E.KT","");
		unitMap.put("EP.PMP.DESL.CD","");
		unitMap.put("FP.CPI.TOTL.ZG","");
		unitMap.put("IT.NET.USER.P2","");
		unitMap.put("NY.GDP.MKTP.CD","");
		unitMap.put("SP.DYN.LE00.IN","");
		unitMap.put("SP.POP.TOTL","");

	}
	public static String getUnit(String rel) {
		
		return unitMap.get(rel);
	}

}
