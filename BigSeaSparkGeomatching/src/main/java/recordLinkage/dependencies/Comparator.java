package recordLinkage.dependencies;

import java.io.Serializable;

public class Comparator implements java.util.Comparator<String>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String SEPARATOR = ",";

	public int compare(String key1, String key2) {

		Integer timestamp1 = 0;
		Integer timestamp2 = 0;

		try{
			timestamp1 = Integer.parseInt(key1.split(SEPARATOR)[0].replaceAll(":", ""));
		} catch (Exception e) {
			timestamp1 = 0;
		}
		
		try{
			timestamp2 = Integer.parseInt(key2.split(SEPARATOR)[0].replaceAll(":", ""));
		} catch (Exception e) {
			timestamp2 = 0;
		}
		

//		System.out.println("TIMESTAMP " + timestamp1   + " : " + timestamp2);
		
		if (timestamp1 < timestamp2) {
			return -1;
		}
		if (timestamp1 > timestamp2) {
			return 1;
		}
		return 0;
	}
}
