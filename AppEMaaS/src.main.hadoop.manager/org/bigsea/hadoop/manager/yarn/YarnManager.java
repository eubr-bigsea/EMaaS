package org.bigsea.hadoop.manager.yarn;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.fs.http.client.KerberosWebYarnConnection;
import org.apache.hadoop.security.authentication.client.AuthenticationException;
import org.bigsea.hadoop.manager.exceptions.ExceptionsList;
import org.json.JSONObject;

import com.google.gson.Gson;

public class YarnManager {
	private KerberosWebYarnConnection conn = null;

	public YarnManager(String hostPathPort, String user, String password) {
		conn = new KerberosWebYarnConnection(hostPathPort, user, password);
	} 

	public JSONObject getClusterInfo() {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			response = conn.cluster();
			if ((Integer)response.get("code") != 200) {
				response.put("error", ExceptionsList.CANNOT_GET_CLUSTER_INFO.getError());
			}
			return convertToJSON(response);
			
		} catch (MalformedURLException e) {
			response.put("error", ExceptionsList.CANNOT_GET_CLUSTER_INFO.getError());
			return convertToJSON(response);
		} catch (IOException e) {
			response.put("error", ExceptionsList.CANNOT_GET_CLUSTER_INFO.getError());
			return convertToJSON(response);
		} catch (AuthenticationException e) {
			response.put("error", ExceptionsList.AUTHENTICATION_PROBLEM.getError());
			return convertToJSON(response);
		}
	}
	
	public JSONObject getListNodes() {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			response = conn.nodes();
			if ((Integer)response.get("code") != 200) {
				response.put("error", ExceptionsList.CANNOT_GET_LIST_NODES.getError());
			}
			return convertToJSON(response);
			
		} catch (MalformedURLException e) {
			response.put("error", ExceptionsList.CANNOT_GET_LIST_NODES.getError());
			return convertToJSON(response);
		} catch (IOException e) {
			response.put("error", ExceptionsList.CANNOT_GET_LIST_NODES.getError());
			return convertToJSON(response);
		} catch (AuthenticationException e) {
			response.put("error", ExceptionsList.AUTHENTICATION_PROBLEM.getError());
			return convertToJSON(response);
		}
	}

	public JSONObject getScheduler() {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			response = conn.scheduler();
			if ((Integer)response.get("code") != 200) {
				response.put("error", ExceptionsList.CANNOT_GET_SCHEDULERS.getError());
			}
			return convertToJSON(response);
			
		} catch (MalformedURLException e) {
			response.put("error", ExceptionsList.CANNOT_GET_SCHEDULERS.getError());
			return convertToJSON(response);
		} catch (IOException e) {
			response.put("error", ExceptionsList.CANNOT_GET_SCHEDULERS.getError());
			return convertToJSON(response);
		} catch (AuthenticationException e) {
			response.put("error", ExceptionsList.AUTHENTICATION_PROBLEM.getError());
			return convertToJSON(response);
		}
	}

	public JSONObject getApplications() {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			response = conn.applications();
			if ((Integer)response.get("code") != 200) {
				response.put("error", ExceptionsList.CANNOT_GET_APPLICATIONS.getError());
			}
			return convertToJSON(response);
			
		} catch (MalformedURLException e) {
			response.put("error", ExceptionsList.CANNOT_GET_APPLICATIONS.getError());
			return convertToJSON(response);
		} catch (IOException e) {
			response.put("error", ExceptionsList.CANNOT_GET_APPLICATIONS.getError());
			return convertToJSON(response);
		} catch (AuthenticationException e) {
			response.put("error", ExceptionsList.AUTHENTICATION_PROBLEM.getError());
			return convertToJSON(response);
		}
	}

	public JSONObject getApplicationInfo(String appID) {
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			response = conn.application(appID);
			if ((Integer)response.get("code") != 200) {
				response.put("error", ExceptionsList.CANNOT_GET_APPLICATION.getError());
			}
			return convertToJSON(response);
			
		} catch (MalformedURLException e) {
			response.put("error", ExceptionsList.CANNOT_GET_APPLICATION.getError());
			return convertToJSON(response);
		} catch (IOException e) {
			response.put("error", ExceptionsList.CANNOT_GET_APPLICATION.getError());
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
