package org.bigsea.hadoop.manager.exceptions;

public enum ExceptionsList {
	/* SSH Exceptions*/
	FILE_DIRECTORY_NOT_FOUND("File or directory not found."), 
	CANNOT_CREATE_DIRECTORY("The directory cannot be created."), 
	CANNOT_SUBMIT_JOB("The Hadoop job cannot be submitted to cluster."),
	
	/* HDFS Exceptions*/
	FILE_DIRECTORY_HDFS_NOT_FOUND("File or directory not found."),
	CANNOT_UPLOAD_FILE("The file cannot be uploaded to HDFS."), 
	CANNOT_DELETE_FILE("The file cannot be removed from HDFS."),
	CANNOT_DELETE_DIRECTORY("The directory cannot be removed from HDFS."),
	CANNOT_CREATE_DIRECTORY_HDFS("The directory cannot be created in HDFS."),
	CANNOT_DOWNLOAD_FILE("The file cannot be downloaded from HDFS."), 
	CANNOT_RENAME_FILE("The file cannot be renamed in HDFS."), 
	CANNOT_GET_ROOT_DIRECTORY("The root directory cannot be recovered."),
	AUTHENTICATION_PROBLEM("Occurred some authentication error, check the credentials."),
	
	
	/* Yarn Exceptions*/
	CANNOT_GET_CLUSTER_INFO("The information about cluster cannot be recovered."),
	CANNOT_GET_LIST_NODES("The list of nodes cannot be recovered."),
	CANNOT_GET_SCHEDULERS("The schedulers cannot be recovered."),
	CANNOT_GET_APPLICATIONS("The jobs (applications) cannot be recovered."),
	CANNOT_GET_APPLICATION("The job (application) cannot be recovered."),
	CANNOT_GET_STATUS("The job status cannot be recovered.");
	
	
	
	/* Methods */
	
	private final String errorMsg;
	ExceptionsList(String error){
		errorMsg = error;
	}
	public String getError(){
		return errorMsg;
	}
}
