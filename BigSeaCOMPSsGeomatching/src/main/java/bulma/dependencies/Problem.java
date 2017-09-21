package bulma.dependencies;

public enum Problem {
	
	NO_PROBLEM(0),
	NO_SHAPE(-1),
	TRIP_PROBLEM(-2),
	OUTLIER_POINT(-3);
	
	private int code;
	
	Problem() {
	}
	
	Problem(int code) {
		this.code = code;
	}
	public int getCode() {
		return code;
	}

}
