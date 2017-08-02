package PointDependencies;

public enum Problem {
	
	NO_PROBLEM(0),
	NO_SHAPE(-1),
	TRIP_PROBLEM(-2),
	OUTLIER_POINT(-3);
	
	private Integer code;
	private Problem(Integer code) {
		this.code = code;
	}
	public Integer getCode() {
		return code;
	}
	
	public static String getById(Integer code) {
	    for(Problem e : values()) {
	        if(e.code == code) return e.toString();
	    }
	    return null;
	 }
}
