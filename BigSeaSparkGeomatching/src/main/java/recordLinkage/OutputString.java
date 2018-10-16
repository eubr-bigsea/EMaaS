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
		String timestamp = this.outputString.split(",")[12];
//		System.out.println("Timestamp : " + timestamp);
		if (timestamp.equals("-")) {
			timestamp = "0";
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
