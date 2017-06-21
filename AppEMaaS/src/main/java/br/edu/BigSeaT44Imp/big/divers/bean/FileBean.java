package br.edu.BigSeaT44Imp.big.divers.bean;

import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ViewScoped;
import javax.faces.event.PhaseId;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.primefaces.context.RequestContext;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.model.TreeNode;
import org.primefaces.model.UploadedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sun.jersey.core.util.MultivaluedMapImpl;

import br.edu.BigSeaT44Imp.big.divers.model.FileRow;
import br.edu.BigSeaT44Imp.big.divers.service.FileService;

@Component
@Lazy
@Scope("session")
@ViewScoped
public class FileBean extends AbstractBean {
	
	private TreeNode files;
	private TreeNode folders;
	private String name;
	private String path;
	private String nameDirectory;
	private String newName;
	private String pathDelete;
	private TreeNode selectedNode;
	private String selectedPath;
	private List<FileRow> fileRows;
	private Integer resultLength;
	
	@Autowired
	private FileService service;

	@PostConstruct
	public void init() {
		refreshTreeFiles();
		refreshTreeFolders();
		RequestContext context = RequestContext.getCurrentInstance();
		context.execute("jQuery('#pnlContentBulma').hide()");
	}
	
	public void refreshTreeFiles() {
		files = service.createFiles();
	}
	
	public void submitJob(){
		refreshTreeFiles();
		
		RequestContext context = RequestContext.getCurrentInstance();
		context.execute("jQuery('#pnlContentFile').hide()");
		context.execute("jQuery('#pnlContentBulma').hide()");
	    context.execute("jQuery('#pnlContentJar').show()");
	}
	
	public void refreshTreeFolders() {
		folders = service.createFolders();
	}

	public void onSelectContextMenu(NodeSelectEvent event) {
		try {
		setSelectedNode(event.getTreeNode());
		setSelectedPath(service.getNodePath(selectedNode));
		} catch (Exception e) {}
	}
	
	public void onNodeSelect(NodeSelectEvent event){
		setSelectedNode(event.getTreeNode());
		setSelectedPath(service.getNodePath(selectedNode));		
		fileRows = service.getRowsFile(getSelectedPath().substring(1));
		resultLength = getFileRows().size();
		
		RequestContext context = RequestContext.getCurrentInstance();
		context.execute("jQuery('#pnlContentJar').hide()");
 	    context.execute("jQuery('#pnlContentBulma').hide()");
	    context.execute("jQuery('#pnlContentFile').show()");
	}
	
	public void deleteDirectory() {
		JSONObject jsonEntity = service.delete("deleteDirectory", selectedPath.substring(1, selectedPath.length() - 1));
		String data = jsonEntity.get("data").toString();
		JSONObject dataJson = new JSONObject(data);
		
		try {
			if (jsonEntity.get("code").toString().equals("200") && (boolean) dataJson.get("boolean")) {
				reportMessageSuccessful(jsonEntity.get("mesg").toString());
				refreshTreeFiles();
				refreshTreeFolders();
			} else {
				reportMessageError(jsonEntity.get("mesg").toString());
			}
		} catch (Exception e) {
			reportMessageError("Unexpected error!");
		}
	}

	

	public void uploadFile(FileUploadEvent event) {

		if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
		} else {
			String pathDst = getSelectedPath().substring(1) + event.getFile().getFileName();
			UploadedFile file = event.getFile();
			JSONObject jsonEntity;
			try {

				if (file.getSize() != 0) {
					jsonEntity = service.uploadFile("uploadFile", file, pathDst);

					if (jsonEntity.get("code").toString().equals("201")) {
						reportMessageSuccessful(jsonEntity.get("mesg").toString());
						refreshTreeFiles();
						refreshTreeFolders();

					} else {
						reportMessageError(
								jsonEntity.get("error").toString() + "\n " + jsonEntity.get("mesg").toString());
					}
				} else {
					reportMessageError("It is not possible to send empty file.");
				}

			} catch (IOException e) {
				reportMessageError("Failed : IOException error code : " + e.getMessage());
				e.printStackTrace();
			} catch (Exception e) {
				reportMessageError("Unexpected error!");
			}
		}
	}

	public void downloadFile() {
		String hadoopPath = getSelectedPath().substring(1, getSelectedPath().length() - 1);
		String nameFile = hadoopPath.substring(hadoopPath.lastIndexOf("/"));
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		String home = System.getProperty("user.home");		 
		params.add("destinationPath", home + "/Downloads/" + nameFile);
		params.add("hadoopPath", hadoopPath);
		JSONObject jsonEntity = service.get("downloadFile", params);

		String error = null;
		try {
			error = jsonEntity.get("error").toString();
		} catch (JSONException e) {
		}
		if (error == null) {
			reportMessageSuccessful(jsonEntity.get("mesg").toString());
		} else {
			reportMessageError(jsonEntity.get("error").toString());
		}
	}

	public void deleteFile() {
		JSONObject jsonEntity = service.delete("deleteFile", selectedPath.substring(1, selectedPath.length() - 1));
		String data = jsonEntity.get("data").toString();
		JSONObject dataJson = new JSONObject(data);
		try {
			if (jsonEntity.get("code").toString().equals("200") && (boolean) dataJson.get("boolean")) {
				reportMessageSuccessful(jsonEntity.get("mesg").toString());
				refreshTreeFiles();
			} else {
				reportMessageError(jsonEntity.get("error").toString());
			}
		} catch (Exception e) {
			reportMessageError("Unexpected error!");
		}
	}

	public void renameFile() {
		MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		String newPath = service.getPathRename(selectedPath.substring(1));
		formData.add("currentPath", selectedPath.substring(1));
		formData.add("newPath", newPath + newName);

		JSONObject jsonEntity = service.put("renameFile", formData, MediaType.APPLICATION_FORM_URLENCODED_TYPE);
		
		String error = null;
		try {
			error = jsonEntity.get("error").toString();
		} catch (JSONException e) {
		}

		if (error == null) {
			reportMessageSuccessful(jsonEntity.get("mesg").toString());
			refreshTreeFiles();
			refreshTreeFolders();
		} else {
			reportMessageError(jsonEntity.get("error").toString());
		}
	}

	public void setSelectedPathToRoot() {
		setSelectedPath("/");
	}

	public void createDirectory() {
		if (nameDirectory.trim().equals("")) {
			reportMessageError("Name can not be empty.");
		} else {
			JSONObject jsonEntity = service.put("createDirectory", selectedPath.substring(1) + nameDirectory,
					MediaType.TEXT_PLAIN);

			String error = null;
			try {
				error = jsonEntity.get("error").toString();
			} catch (JSONException e) {
			}

			if (error == null) {
				reportMessageSuccessful(jsonEntity.get("mesg").toString());
				refreshTreeFiles();
				refreshTreeFolders();
			} else {
				reportMessageError(jsonEntity.get("error").toString() + "\n " + jsonEntity.get("mesg").toString());
			}
		}
	}
	
	public Integer getResultLength(){
		return resultLength;
	}
	
	public void setResultLength(Integer resultLength) {
		this.resultLength = resultLength;
	}
	
	public List<FileRow> getFileRows() {
		return fileRows;
	}

	public void setFileRows(List<FileRow> fileRows) {
		this.fileRows = fileRows;
	}

	public String getPathDelete() {
		return pathDelete;
	}

	public void setPathDelete(String pathDelete) {
		this.pathDelete = pathDelete;
	}

	public String getNewName() {
		return newName;
	}

	public void setNewName(String newName) {
		this.newName = newName;
	}

	public String getNameDirectory() {
		return nameDirectory;
	}

	public void setNameDirectory(String nameDirectory) {
		this.nameDirectory = nameDirectory;
	}

	public String getSelectedPath() {
		return selectedPath;
	}

	public void setSelectedPath(String selectedPath) {
		this.selectedPath = selectedPath;
	}

	public TreeNode getSelectedNode() {
		return selectedNode;
	}

	public void setSelectedNode(TreeNode selectedNode) {
		this.selectedNode = selectedNode;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setService(FileService service) {
		this.service = service;
	}

	public TreeNode getFiles() {
		return files;
	}

	public void setFiles(TreeNode files) {
		this.files = files;
	}
	
	public TreeNode getFolders() {
		return folders;
	}

	public void setFolders(TreeNode folders) {
		this.folders = folders;
	}

}