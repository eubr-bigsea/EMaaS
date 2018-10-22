package recordLinkage.dependencies;

import java.io.Serializable;

public class TicketInformation implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4004323352664123659L;

	private String boarding_id;
	private String route;
	private String nameLine;
	private String busCode;
	private String ticketNumber;
	private String birthDate;
	private String gender;
	private String boarding_time;
	private String boarding_date;
	private String string_date_time;
	
	
	public TicketInformation() {
		super();
	}
	
	public TicketInformation(String boarding_id, String route, 
			String nameLine, String busCode,
			String ticketNumber, String birthDate, 
			String gender, String string_date_time) {
		
		this.string_date_time = string_date_time;
		String[] boarding_datetime = string_date_time.split(" ");
	
		this.boarding_id = boarding_id;
		this.route = route;
		this.nameLine = nameLine;
		this.busCode = busCode;
		this.ticketNumber = ticketNumber;
		this.boarding_time = boarding_datetime[1];
		this.boarding_date = boarding_datetime[0];
		this.birthDate = birthDate;
		this.gender = gender;
	}
	
	public String getString_date_time() {
		return string_date_time;
	}

	public void setString_date_time(String string_date_time) {
		this.string_date_time = string_date_time;
	}

	public String getBoarding_id() {
		return boarding_id;
	}

	public void setBoarding_id(String boarding_id) {
		this.boarding_id = boarding_id;
	}

	public String getRoute() {
		return route;
	}

	public void setRoute(String route) {
		this.route = route;
	}

	public String getNameLine() {
		return nameLine;
	}

	public void setNameLine(String nameLine) {
		this.nameLine = nameLine;
	}

	public String getBusCode() {
		return busCode;
	}

	public void setBusCode(String busCode) {
		this.busCode = busCode;
	}

	public String getTicketNumber() {
		return ticketNumber;
	}

	public void setTicketNumber(String ticketNumber) {
		this.ticketNumber = ticketNumber;
	}

	public String getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(String birthDate) {
		this.birthDate = birthDate;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getBoarding_time() {
		return boarding_time;
	}

	public void setBoarding_time(String boarding_time) {
		this.boarding_time = boarding_time;
	}

	public String getBoarding_date() {
		return boarding_date;
	}

	public void setBoarding_date(String boarding_date) {
		this.boarding_date = boarding_date;
	}

	@Override
	public String toString() {
		return route + "," + nameLine + "," + busCode
				+ "," + ticketNumber + "," + boarding_time + "," + boarding_date
				+ "," + birthDate + "," + gender;
	}
	
}

