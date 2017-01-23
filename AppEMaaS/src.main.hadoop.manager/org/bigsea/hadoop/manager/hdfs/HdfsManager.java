package org.bigsea.hadoop.manager.hdfs;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.fs.http.client.KerberosWebHDFSConnection;
import org.apache.hadoop.security.authentication.client.AuthenticationException;
import org.bigsea.hadoop.manager.exceptions.ExceptionsList;
import org.json.JSONObject;

import com.google.gson.Gson;

public class HdfsManager {
	private KerberosWebHDFSConnection conn = null;
	
	public HdfsManager(String hostPathPort, String user, String password) {
		conn = new KerberosWebHDFSConnection(hostPathPort, user, password);
	}

	public JSONObject listFiles(String path){
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			response = conn.listStatus(path);
			if ((Integer)response.get("code") != 200) {
				response.put("error", ExceptionsList.FILE_DIRECTORY_HDFS_NOT_FOUND.getError());
			}
			return convertToJSON(response);
			
		} catch (MalformedURLException e) {
			response.put("error", ExceptionsList.FILE_DIRECTORY_HDFS_NOT_FOUND.getError());
			return convertToJSON(response);
		} catch (IOException e) {
			response.put("error", ExceptionsList.FILE_DIRECTORY_HDFS_NOT_FOUND.getError());
			return convertToJSON(response);
		} catch (AuthenticationException e) {
			response.put("error", ExceptionsList.AUTHENTICATION_PROBLEM.getError());
			return convertToJSON(response);
		}
	}
	
	public JSONObject listNamesFiles(String path){
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			response = conn.listStatus(path);
			if ((Integer)response.get("code") != 200) {
				response.put("error", ExceptionsList.FILE_DIRECTORY_HDFS_NOT_FOUND.getError());
			}
			return convertToJSON(response);
			
		} catch (MalformedURLException e) {
			response.put("error", ExceptionsList.FILE_DIRECTORY_HDFS_NOT_FOUND.getError());
			return convertToJSON(response);
		} catch (IOException e) {
			response.put("error", ExceptionsList.FILE_DIRECTORY_HDFS_NOT_FOUND.getError());
			return convertToJSON(response);
		} catch (AuthenticationException e) {
			response.put("error", ExceptionsList.AUTHENTICATION_PROBLEM.getError());
			return convertToJSON(response);
		}
	}
	
	public JSONObject uploadFiles(FileInputStream fileInput, String pathHdfs){
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			response = conn.create(pathHdfs, fileInput);
			if ((Integer)response.get("code") != 201) {
				response.put("error", ExceptionsList.CANNOT_UPLOAD_FILE.getError());
			}
			return convertToJSON(response);
			
		} catch (MalformedURLException e) {
			response.put("error", ExceptionsList.CANNOT_UPLOAD_FILE.getError());
			return convertToJSON(response);
		} catch (IOException e) {
			response.put("error", ExceptionsList.CANNOT_UPLOAD_FILE.getError());
			return convertToJSON(response);
		} catch (AuthenticationException e) {
			response.put("error", ExceptionsList.AUTHENTICATION_PROBLEM.getError());
			return convertToJSON(response);
		}
	}
	
	public JSONObject deleteFile(String path){
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			response = conn.delete(path);
			if (((StringBuffer)response.get("data")).toString().contains("false")) {
				response.put("error", ExceptionsList.CANNOT_DELETE_FILE.getError());
			}
			return convertToJSON(response);
			
		} catch (MalformedURLException e) {
			response.put("error", ExceptionsList.CANNOT_DELETE_FILE.getError());
			return convertToJSON(response);
		} catch (IOException e) {
			response.put("error", ExceptionsList.CANNOT_DELETE_FILE.getError());
			return convertToJSON(response);
		} catch (AuthenticationException e) {
			response.put("error", ExceptionsList.AUTHENTICATION_PROBLEM.getError());
			return convertToJSON(response);
		}
	}
	
	public JSONObject deleteDirectory(String path){
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			response = conn.deleteRecursive(path);
			if (((StringBuffer)response.get("data")).toString().contains("false")) {
				response.put("error", ExceptionsList.CANNOT_DELETE_DIRECTORY.getError());
			}
			return convertToJSON(response);
			
		} catch (MalformedURLException e) {
			response.put("error", ExceptionsList.CANNOT_DELETE_DIRECTORY.getError());
			return convertToJSON(response);
		} catch (IOException e) {
			response.put("error", ExceptionsList.CANNOT_DELETE_DIRECTORY.getError());
			return convertToJSON(response);
		} catch (AuthenticationException e) {
			response.put("error", ExceptionsList.AUTHENTICATION_PROBLEM.getError());
			return convertToJSON(response);
		}
	}

	public JSONObject createDirectory(String path) {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			response = conn.mkdirs(path);
			if (((StringBuffer)response.get("data")).toString().contains("false")) {
				response.put("error", ExceptionsList.CANNOT_CREATE_DIRECTORY_HDFS.getError());
			}
			return convertToJSON(response);
			
		} catch (MalformedURLException e) {
			response.put("error", ExceptionsList.CANNOT_CREATE_DIRECTORY_HDFS.getError());
			return convertToJSON(response);
		} catch (IOException e) {
			response.put("error", ExceptionsList.CANNOT_CREATE_DIRECTORY_HDFS.getError());
			return convertToJSON(response);
		} catch (AuthenticationException e) {
			response.put("error", ExceptionsList.AUTHENTICATION_PROBLEM.getError());
			return convertToJSON(response);
		}
	}
	
	public OutputStream openFile(String path) {
		OutputStream file = new ByteArrayOutputStream();
		try {
			conn.open(path, file);
			return file;
			
		} catch (MalformedURLException e) {
			return null;
		} catch (IOException e) {
			return null;
		} catch (AuthenticationException e) {
			return null;
		}
	}
	
	public JSONObject downloadFile(String sourcePath, String hadoopPath) {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			OutputStream file = new FileOutputStream(sourcePath);
			response = conn.open(hadoopPath, file);
			if ((Integer)response.get("code") != 200) {
				response.put("error", ExceptionsList.CANNOT_DOWNLOAD_FILE.getError());
			}
			return convertToJSON(response);
			
		} catch (MalformedURLException e) {
			response.put("error", ExceptionsList.CANNOT_DOWNLOAD_FILE.getError());
			return convertToJSON(response);
		} catch (IOException e) {
			response.put("error", ExceptionsList.CANNOT_DOWNLOAD_FILE.getError());
			return convertToJSON(response);
		} catch (AuthenticationException e) {
			response.put("error", ExceptionsList.AUTHENTICATION_PROBLEM.getError());
			return convertToJSON(response);
		}
	}
	
	public JSONObject renameFile(String sourcePath, String remotePath) {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			response = conn.rename(sourcePath, remotePath);
			if ((Integer)response.get("code") != 200) {
				response.put("error", ExceptionsList.CANNOT_RENAME_FILE.getError());
			}
			return convertToJSON(response);
			
		} catch (MalformedURLException e) {
			response.put("error", ExceptionsList.CANNOT_RENAME_FILE.getError());
			return convertToJSON(response);
		} catch (IOException e) {
			response.put("error", ExceptionsList.CANNOT_RENAME_FILE.getError());
			return convertToJSON(response);
		} catch (AuthenticationException e) {
			response.put("error", ExceptionsList.AUTHENTICATION_PROBLEM.getError());
			return convertToJSON(response);
		}
	}
	
	public JSONObject getRootDirectory() {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			response = conn.getHomeDirectory();
			if ((Integer)response.get("code") != 200) {
				response.put("error", ExceptionsList.CANNOT_GET_ROOT_DIRECTORY.getError());
			}
			return convertToJSON(response);
			
		} catch (MalformedURLException e) {
			response.put("error", ExceptionsList.CANNOT_GET_ROOT_DIRECTORY.getError());
			return convertToJSON(response);
		} catch (IOException e) {
			response.put("error", ExceptionsList.CANNOT_GET_ROOT_DIRECTORY.getError());
			return convertToJSON(response);
		} catch (AuthenticationException e) {
			response.put("error", ExceptionsList.AUTHENTICATION_PROBLEM.getError());
			return convertToJSON(response);
		}
	}
	
	/**
	 * This method mapped the Map (json) to JSON format.
	 * @param response
	 * @return
	 */
	private JSONObject convertToJSON(Map<String, Object> response) {
		Gson gson = new Gson();
		String jsonFormat = gson.toJson(response);
		JSONObject json = new JSONObject(jsonFormat);
		return json;
	}
}
