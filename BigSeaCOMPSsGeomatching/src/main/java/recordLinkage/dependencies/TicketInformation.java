package recordLinkage.dependencies;

import java.io.Serializable;

public class TicketInformation implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4004323352664123659L;
	private String codLine;
	private String nameLine;
	private String busCode;
	private String ticketNumber;
	private String timeOfUse;
	private String dateOfUse;
	private String birthDate;
	private String gender;
	
	public TicketInformation() {
		super();
	}
	
	public TicketInformation(String codLine, String nameLine, String busCode, String ticketNumber, String timeOfUse,
			String dateOfUse, String birthDate, String gender) {
		this.codLine = codLine;
		this.nameLine = nameLine;
		this.busCode = busCode;
		this.ticketNumber = ticketNumber;
		this.timeOfUse = timeOfUse;
		this.dateOfUse = dateOfUse;
		this.birthDate = birthDate;
		this.gender = gender;
	}

	public String getCodLine() {
		return codLine;
	}

	public void setCodLine(String codLine) {
		this.codLine = codLine;
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

	public String getTimeOfUse() {
		return timeOfUse;
	}

	public void setTimeOfUse(String timeOfUse) {
		this.timeOfUse = timeOfUse;
	}

	public String getDateOfUse() {
		return dateOfUse;
	}

	public void setDateOfUse(String dateOfUse) {
		this.dateOfUse = dateOfUse;
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
	
	@Override
	public String toString() {
		return codLine + "," + nameLine + "," + busCode
				+ "," + ticketNumber + "," + timeOfUse + "," + dateOfUse
				+ "," + birthDate + "," + gender;
	}
	
}

