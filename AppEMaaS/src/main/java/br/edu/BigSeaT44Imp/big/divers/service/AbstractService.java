package br.edu.BigSeaT44Imp.big.divers.service;

import java.io.IOException;
import java.util.Properties;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.json.JSONObject;
import org.primefaces.model.UploadedFile;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;

public abstract class AbstractService {
	
	private WebResource wr;
	private Client client;
	private String managerPath;
	
	public AbstractService() {
		client = Client.create();
	}
	
	public String getApplicationPath() {
		Properties properties = new Properties();
		try {
			properties.load(this.getClass().getClassLoader().getResourceAsStream("/config.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		return properties.getProperty("applicationPath");
	}
	
	public JSONObject get(String method, MultivaluedMap<String, String> params) {
		wr = client.resource(managerPath + method);

		if (params != null) {
			wr = wr.queryParams(params);
		}

		ClientResponse response = wr.get(ClientResponse.class);

		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}
		String entity = response.getEntity(String.class);
		return new JSONObject(entity);

	}

	public JSONObject delete(String method, String path) {
		wr = client.resource(managerPath + method);

		ClientResponse response = wr.type(MediaType.TEXT_PLAIN).delete(ClientResponse.class, path);

		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}

		String entity = response.getEntity(String.class);
		return new JSONObject(entity);
	}

	public JSONObject uploadFile(String method, UploadedFile file, String pathDestinationUpload) throws IOException {
		String fileName = file.getFileName();
		FormDataMultiPart fdmp = new FormDataMultiPart();
		FormDataBodyPart fdbp = new FormDataBodyPart(FormDataContentDisposition.name("file").fileName(fileName).build(),
				file.getInputstream(), MediaType.APPLICATION_OCTET_STREAM_TYPE);
		fdmp.bodyPart(fdbp);
		fdmp.bodyPart(new FormDataBodyPart("path", pathDestinationUpload));

		wr = client.resource(managerPath + method);
		ClientResponse response = wr.accept("application/json").type(MediaType.MULTIPART_FORM_DATA)
				.post(ClientResponse.class, fdmp);

		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed service call: HTTP error code : " + response.getStatus());
		}

		String entity = response.getEntity(String.class);
		return new JSONObject(entity);
	}

	public JSONObject put(String method, String param, String mediaType) {
		wr = client.resource(managerPath + method);

		ClientResponse response = wr.type(mediaType).put(ClientResponse.class, param);

		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}

		String entity = response.getEntity(String.class);
		return new JSONObject(entity);
	}

	public JSONObject put(String method, MultivaluedMap<String, String> formData, MediaType mediaType) {
		wr = client.resource(managerPath + method);

		ClientResponse response = wr.type(mediaType).put(ClientResponse.class, formData);

		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}

		String entity = response.getEntity(String.class);
		return new JSONObject(entity);
	}
	
	public String getManagerPath() {
		return managerPath;
	}

	public void setManagerPath(String managerPath) {
		this.managerPath = managerPath;
	}
		
	public JSONObject submitJob(MultivaluedMap<String, String> formData, String typeOfSubmit) {		
		
		wr = client.resource(managerPath + typeOfSubmit);

		ClientResponse response = wr.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).put(ClientResponse.class, formData);

		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}

		String entity = response.getEntity(String.class);
		return new JSONObject(entity);
	}
	
}
