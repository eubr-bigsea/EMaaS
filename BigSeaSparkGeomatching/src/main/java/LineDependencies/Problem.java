package LineDependencies;

public enum Problem {
	
	NO_PROBLEM(0),
	NO_TRIP(-1),
	TRIP_PROBLEM(-2),
	OUTLIER_POINT(-3),
	POINT_ABOVE_THRESHOLD(-4);
	
	private int code;
	private Problem(int code) {
		this.code = code;
	}
	public int getCode() {
		return code;
	}
	

}
