package BULMADependences;

public enum Problem {
	
	NO_PROBLEM(0),
	NO_SHAPE(-1),
	TRIP_PROBLEM(-2),
	OUTLIER_POINT(-3);
	
	private int code;
	private Problem(int code) {
		this.code = code;
	}
	public int getCode() {
		return code;
	}
	
	public static String getById(Integer code) {
	    for(Problem e : values()) {
	        if(e.code == code) return e.toString();
	    }
	    return null;
	 }
}
