package org.bigsea.hadoop.manager.submitJob;

public class JobID {
	private static JobID instance;
	private String jobId;

	private JobID() {
		super();
	}

	public static JobID getInstance() {
		if (instance == null) {
			inicializaInstancia();

		}
		return instance;
	}
	
	private static synchronized void inicializaInstancia() {
		if (instance == null) {
			instance = new JobID();
		}
	}

	public String getJobId() {
		return jobId.replace("job", "application");
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

}
