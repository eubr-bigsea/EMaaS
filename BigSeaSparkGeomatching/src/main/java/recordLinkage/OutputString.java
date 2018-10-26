package recordLinkage;

import java.io.Serializable;
import java.util.Comparator;

import PointDependencies.GPSPoint;

public class OutputString implements Serializable, Comparable<OutputString>{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String outputString;
	
	public OutputString(String outputString) {		
		this.outputString = outputString;
	}	

	public String getOutputString() {
		return outputString;
	}

	public void setOutputString(String outputString) {
		this.outputString = outputString;
	}

	public String getTimestampField() {
		String gps_date_time = this.outputString.split(",")[12];
		String timestamp;
//		System.out.println("Timestamp : " + timestamp);
		if (gps_date_time.equals("-")) {
			timestamp = "0";
		} else {
			timestamp = gps_date_time.split(" ")[1];
		}
		return timestamp.replaceAll(":", "");
	}


	@Override
	public int compareTo(OutputString otherOut) {
		
		if (Integer.parseInt(this.getTimestampField()) < Integer.parseInt(otherOut.getTimestampField())) {
            return -1;
        }
        if (Integer.parseInt(this.getTimestampField()) > Integer.parseInt(otherOut.getTimestampField())) {
            return 1;
        }
        return 0;
	}

}
