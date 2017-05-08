package main.webservice.rest.jersey;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.bigsea.hadoop.manager.submitJob.ExecuteJob;
import org.bigsea.hadoop.manager.submitJob.SSHmanager;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONObject;

@Produces({ MediaType.APPLICATION_JSON })
@Path("managerJob")
public class ManagerServiceJob {

	private SSHmanager ssh;

	public ManagerServiceJob() {
		String SSH_HOST = null;
		Integer SSH_PORT = null;
		String SSH_USER = null;
		String SSH_PASSWORD = null;
		
		Properties prop = new Properties();
		InputStream fileConfig = null;

		try {
			prop.load(this.getClass().getClassLoader().getResourceAsStream("conf.properties"));

			SSH_HOST = prop.getProperty("SSH_HOST");
			SSH_PORT = Integer.valueOf(prop.getProperty("SSH_PORT"));
			SSH_USER = prop.getProperty("SSH_USER");
			SSH_PASSWORD = prop.getProperty("SSH_PASSWORD");

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (fileConfig != null) {
				try {
					fileConfig.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		ssh = new SSHmanager(SSH_HOST, SSH_USER, SSH_PORT, SSH_PASSWORD);
	}

	@GET
	@Path("listFilesInDirectory")
	public Response listFilesinDirectory(@QueryParam("path") String path) {
		JSONObject json = ssh.listFilesInDirectory(path);
		return Response.ok(json.toString()).build();
	}

	@GET
	@Path("openFileJar")
	public Response openFile(@QueryParam("path") String path) {
		JSONObject json = new JSONObject();
		try {
			File file = ssh.openFileJar(path);
			json.put("filePath", file.getAbsolutePath());
			json.put("code", "200");
		} catch (Exception e) {
			json.put("code", "500");
			json.put("mesg", "Unexpeted Error!");
			e.printStackTrace();
		}

		return Response.ok(json.toString()).build();
	}

	@GET
	@Path("downloadFile")
	public Response downloadFile(@QueryParam("destinationPath") String destinationPath,
			@QueryParam("hdPath") String hdPath) {
		JSONObject json = new JSONObject();
		try {
			ssh.downloadFile(destinationPath, hdPath);
			json.put("mesg", "File has been downloaded.");
		} catch (Exception e) {
			json.put("error", "The file cannot be downloaded.");
		}
		return Response.ok(json.toString()).build();
	}
	
	@GET
	@Path("getStatusJob")
	public Response getStatus() {
		String jobId = ssh.getLastJobId();
		while (jobId == null) {
			jobId = ssh.getLastJobId();
		}		
		JSONObject json = ssh.statusJob(jobId);			
		
		return Response.ok(json.toString()).build();
	}
	
	@POST
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Path("sendFile")
	public Response sendFile(@FormDataParam("file") File file, @FormDataParam("path") String destinationPath) {
		JSONObject json = new JSONObject();
		try {
			ssh.sendFile(file, destinationPath);
			json.put("code", "0");
			json.put("mesg", "File has been uploaded.");
		} catch (Exception e) {
			json.put("code", "500");
			json.put("error", "The file cannot be uploaded.");
		}
		return Response.ok(json.toString()).build();
	}
	
	@PUT
	@Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
	@Path("submitHadoopJob")
	public Response submitHadoopJob(@FormParam("jarPath") String jarPath, @FormParam("mainClass") String mainClass,
			@FormParam("inputHdfs") String inputHdfs, @FormParam("outputHdfs") String outputHdfs) throws InterruptedException {
		ExecuteJob exec = new ExecuteJob(jarPath, mainClass, inputHdfs, outputHdfs, ssh);
		Thread threadDoPdf = new Thread(exec);
		threadDoPdf.start();
		
		while (exec.getJson() == null) {
			Thread.sleep(1000);
		}
		//JSONObject json = ssh.submitJob(jarPath, mainClass, inputHdfs, outputHdfs);
		return Response.ok(exec.getJson().toString()).build();

	}
	
	@PUT
	@Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
	@Path("submitSparkJob")
	public Response submitSparkJob(@FormParam("jarPath") String jarPath, @FormParam("mainClass") String mainClass,
			@FormParam("inputHdfs") String inputHdfs, @FormParam("outputHdfs") String outputHdfs) throws InterruptedException {
		
		JSONObject json = ssh.submitSparkJob(jarPath, mainClass, inputHdfs, outputHdfs);
		return Response.ok(json.toString()).build();

	}

	@PUT
	@Consumes({ MediaType.TEXT_PLAIN })
	@Path("createDirectory")
	public Response createDirectory(String path) {
		JSONObject json = ssh.createDirectory(path);
		return Response.ok(json.toString()).build();
	}

	@DELETE
	@Consumes({ MediaType.TEXT_PLAIN })
	@Path("deleteDirectory")
	public Response deleteDirectory(String path) {
		JSONObject json = ssh.deleteDirectory(path);
		return Response.ok(json.toString()).build();
	}

}
