package org.bigsea.hadoop.manager.submitJob;

import org.json.JSONObject;

public class StatusJob implements Runnable {

	private String jobId;
	private SSHmanager ssh;
	private JSONObject json;

	public StatusJob(String jobId, SSHmanager ssh) {
		super();
		this.ssh = ssh;
	}

	
	@Override
	public void run() {
		json = ssh.statusJob(jobId);
		
	}
		
	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public JSONObject getJson() {
		return json;
	}

}
