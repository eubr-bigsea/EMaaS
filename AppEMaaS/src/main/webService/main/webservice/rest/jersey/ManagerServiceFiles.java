package main.webservice.rest.jersey;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

import org.bigsea.hadoop.manager.hdfs.HdfsManager;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONObject;

@Produces({ MediaType.APPLICATION_JSON })
@Path("managerFiles")
public class ManagerServiceFiles {

	private HdfsManager fs;

	public ManagerServiceFiles() {
		String HDFS_HOST = null;
		String HDFS_PORT = null;
		String HDFS_USER = null;
		String HDFS_PASSWORD = null;
		
		Properties prop = new Properties();
		InputStream fileConfig = null;

		try {
			prop.load(this.getClass().getClassLoader().getResourceAsStream("conf.properties"));

			HDFS_HOST = prop.getProperty("HDFS_HOST");
			HDFS_PORT = prop.getProperty("HDFS_PORT");
			HDFS_USER = prop.getProperty("HDFS_USER");
			HDFS_PASSWORD = prop.getProperty("HDFS_PASSWORD");

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
		
		fs = new HdfsManager(HDFS_HOST + ":" + HDFS_PORT, HDFS_USER, HDFS_PASSWORD);
	}

	@GET	
	@Path("listFiles")
	public Response listFiles(@QueryParam("path") String path) {
		JSONObject json = fs.listFiles(path);
		return Response.ok(json.toString()).build();
	}

	@GET	
	@Path("getRootDirectory")
	public Response getRootDirectory() {
		JSONObject json = fs.getRootDirectory();
		return Response.ok(json.toString()).build();
	}
	
	@GET
	@Path("openFile")
	public Response openFile(@QueryParam("path") String path) {
		OutputStream output = fs.openFile(path);
		JSONObject json = new JSONObject();
		json.accumulate("result", output);
		return Response.ok(json.toString()).build();
	}

	@GET
	@Path("downloadFile")
	public Response downloadFile(@QueryParam("destinationPath") String destinationPath,
			@QueryParam("hadoopPath") String hadoopPath) {
		JSONObject json = fs.downloadFile(destinationPath, hadoopPath);
		return Response.ok(json.toString()).build();
	}

	@POST
	@Consumes({ MediaType.MULTIPART_FORM_DATA })	
	@Path("uploadFile")
	public Response uploadFiles(@FormDataParam("file") File file, @FormDataParam("path") String pathHdfs)
			throws FileNotFoundException {
		FileInputStream is = new FileInputStream(file);
		JSONObject json = fs.uploadFiles(is, pathHdfs);
		return Response.ok(json.toString()).build();
	}
	
	@PUT
	@Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
	@Path("renameFile")
	public Response renameFile(@FormParam("currentPath") String currentPath,
			@FormParam("newPath") String newPath) {
		JSONObject json = fs.renameFile(currentPath, newPath);
		return Response.ok(json.toString()).build();
	}

	@PUT
	@Consumes({ MediaType.TEXT_PLAIN })
	@Path("createDirectory")
	public Response createDirectory(String path) {
		JSONObject json = fs.createDirectory(path);
		return Response.ok(json.toString()).build();
	}

	@DELETE
	@Consumes({ MediaType.TEXT_PLAIN })
	@Path("deleteFile")
	public Response deleteFile(String path) {
		JSONObject json = fs.deleteFile(path);
		return Response.ok(json.toString()).build();
	}

	@DELETE
	@Consumes({ MediaType.TEXT_PLAIN })
	@Path("deleteDirectory")
	public Response deleteDirectory(String path) {
		JSONObject json = fs.deleteDirectory(path);
		return Response.ok(json.toString()).build();
	}
}
