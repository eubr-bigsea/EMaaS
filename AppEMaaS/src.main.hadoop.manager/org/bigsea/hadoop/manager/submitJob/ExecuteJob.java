package org.bigsea.hadoop.manager.submitJob;

import org.json.JSONObject;

public class ExecuteJob implements Runnable{

	private String jarPath, mainClass, inputHdfs, outputHdfs;
	private SSHmanager ssh;
	private JSONObject json;

	public ExecuteJob(String jarPath, String mainClass, String inputHdfs, String outputHdfs, SSHmanager ssh) {
		super();
		this.jarPath = jarPath;
		this.mainClass = mainClass;
		this.inputHdfs = inputHdfs;
		this.outputHdfs = outputHdfs;
		this.ssh = ssh;
	}

	@Override
	public void run() {
		json = ssh.submitHadoopJob(jarPath, mainClass, inputHdfs, outputHdfs);		
	}
	
	public String getJarPath() {
		return jarPath;
	}


	public void setJarPath(String jarPath) {
		this.jarPath = jarPath;
	}


	public String getMainClass() {
		return mainClass;
	}


	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}


	public String getInputHdfs() {
		return inputHdfs;
	}


	public void setInputHdfs(String inputHdfs) {
		this.inputHdfs = inputHdfs;
	}


	public String getOutputHdfs() {
		return outputHdfs;
	}


	public void setOutputHdfs(String outputHdfs) {
		this.outputHdfs = outputHdfs;
	}
	
	public JSONObject getJson() {
		return json;
	}


}
