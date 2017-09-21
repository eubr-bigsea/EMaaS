package recordLinkage.dependencies;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ComparatorTmpOutput implements java.util.Comparator<String>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String SEPARATOR = ",";
	
	public ComparatorTmpOutput() {
		super();
	}

	public int compare(String key1, String key2) {
		
			SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
			 Date date1;
			 Date date2;
			try {
				date1 = sdf.parse(key1.split(SEPARATOR)[12]);		
				date2 = sdf.parse(key2.split(SEPARATOR)[12]);
			} catch (ParseException e) {
				date1 = null;
				date2 = null;
			}
			
			if (date1 == null) {
				return 1;
			}
					
			if (date2 != null && date1.equals(date2)) {
				return 0;
			}
			
			return date1.before(date2)? -1: 1;

		
	}

}
