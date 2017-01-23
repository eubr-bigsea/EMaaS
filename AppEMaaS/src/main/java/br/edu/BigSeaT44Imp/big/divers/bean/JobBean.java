package br.edu.BigSeaT44Imp.big.divers.bean;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.event.PhaseId;
import javax.ws.rs.core.MultivaluedMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.model.TreeNode;
import org.primefaces.model.UploadedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sun.jersey.core.util.MultivaluedMapImpl;

import br.edu.BigSeaT44Imp.big.divers.service.FileService;
import br.edu.BigSeaT44Imp.big.divers.service.JobService;

@Component
@Lazy
@Scope("session")
public class JobBean extends AbstractBean implements Serializable {

	private static final long serialVersionUID = -1799880219475923891L;
	private String mainClass;
	private List<String> classes;
	private String inputHdfs;
	private String outputHdfs;
	private String fileResult;
	private TreeNode jars;
	private TreeNode selectedNode;
	private String selectedPath;
	private String optionSubmit;

	private final String HOME_PATH_HD = "/home/hadoop";

	@Autowired
	private JobService jobService;
	@Autowired
	private FileService fileService;

	@PostConstruct
	public void init() {
		jars = listJars();
		setOptionSubmit("submitSparkJob");
	}

	public void onNodeSelect(NodeSelectEvent event) {
		try {
			setSelectedNode(event.getTreeNode());
			setSelectedPath(fileService.getNodePath(selectedNode));
		} catch (Exception e) {
			// TODO
		}
	}

	public void onFileSelected(NodeSelectEvent event) {
		setSelectedNode(event.getTreeNode());
		setInputHdfs(jobService.getNodePath(selectedNode, "FILE"));

	}

	public void onFolderSelected(NodeSelectEvent event) {
		setSelectedNode(event.getTreeNode());
		setOutputHdfs(jobService.getNodePath(selectedNode, "FOLDER"));
	}

	private TreeNode listJars() {
		return jobService.createJars();
	}

	public String openFile(String path) {
		MultivaluedMap<String, String> param = new MultivaluedMapImpl();
		param.add("path", path);
		JSONObject jsonEntity = fileService.get("openFile", param);
		return jsonEntity.getString("result");
	}

	public String openFileJar(String path) {
		MultivaluedMap<String, String> param = new MultivaluedMapImpl();
		param.add("path", path);
		JSONObject jsonEntity = jobService.get("openFileJar", param);
		String localPath = null;

		if (jsonEntity.getString("code").equals("200")) {
			localPath = jsonEntity.getString("filePath");
		}

		return localPath;
	}

	public void searchClasses() {
		String localPath = openFileJar(HOME_PATH_HD + selectedPath);

		try {
			classes = jobService.getFromJars(localPath);
		} catch (Exception e) {
			reportMessageError("Unexpected error while trying to read file .jar!");
			e.printStackTrace();
		}
	}

	public void submitJob() {

		MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		formData.add("jarPath", HOME_PATH_HD + selectedPath);
		formData.add("mainClass", this.mainClass);
		if (getOptionSubmit().equals("submitSparkJob")) {
			formData.add("inputHdfs", this.inputHdfs);
		} else {
			formData.add("inputHdfs", checkPathSubmitJob(inputHdfs));
		}
		
		String outputDirectory = checkPathSubmitJob(outputHdfs) + "output";
		formData.add("outputHdfs", outputDirectory);

		removeDirectoryExistent(outputDirectory.substring(1));

		JSONObject jsonEntity = jobService.submitJob(formData, getOptionSubmit());
		try {

			if (jsonEntity.get("code").toString().equals("0")) {
				reportMessageSuccessful("Job has been submitted.");
				setFileResult(convertSpace(jsonEntity.get("mesg").toString()));

			} else {
				reportMessageError(jsonEntity.get("mesg").toString());
			}
		} catch (JSONException e) {
			reportMessageError("Unexpected error!");
		}
	}

	private void removeDirectoryExistent(String pathDirectory) {
		try {
			fileService.delete("deleteDirectory", pathDirectory);
		} catch (Exception e) {
			// TODO
		}
	}

	private String checkPathSubmitJob(String path) {
		if (!path.startsWith("/") && !path.startsWith("\\")) {
			return "/" + checkPath(path);
		}
		return checkPath(path);
	}

	public void sendFile(FileUploadEvent event) {
		if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
		} else {
			String pathDst = HOME_PATH_HD + getSelectedPath() + event.getFile().getFileName();
			UploadedFile file = event.getFile();
			JSONObject jsonEntity;
			try {

				if (file.getSize() != 0) {
					jsonEntity = jobService.uploadFile("sendFile", file, pathDst);

					if (jsonEntity.get("code").toString().equals("0")) {
						reportMessageSuccessful(jsonEntity.get("mesg").toString());
						jars = jobService.createJars();

					} else {
						reportMessageError(jsonEntity.get("error").toString());
					}
				} else {
					reportMessageError("It is not possible to send empty file.");
				}

			} catch (IOException e) {
				reportMessageError("Failed : IOException error code : " + e.getMessage());
				e.printStackTrace();

			} catch (JSONException e) {
				reportMessageError("Unexpected error!");
			}
		}
	}

	public void downloadFile() {
		String hadoopPath = getSelectedPath();
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		String home = System.getProperty("user.home");
		params.add("destinationPath", home + "/Downloads/");
		params.add("hdPath", hadoopPath.substring(1, hadoopPath.length() - 1));
		JSONObject jsonEntity = jobService.get("downloadFile", params);

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

	public void deleteDirectory() {
		String pathJAR = getSelectedPath().substring(1, getSelectedPath().length() - 1);
		JSONObject jsonEntity = jobService.delete("deleteDirectory", pathJAR);

		try {
			if (jsonEntity.get("code").toString().equals("0")) {
				reportMessageSuccessful("Success.");
				jars = listJars();
			} else {
				reportMessageError(jsonEntity.get("error").toString());
			}

		} catch (JSONException e) {
			reportMessageError("Unexpected error!");
		}
	}

	public void setJobService(JobService jobService) {
		this.jobService = jobService;
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

	public void setFileService(FileService fileService) {
		this.fileService = fileService;
	}

	public String getOutputHdfs() {
		return outputHdfs;
	}

	public void setOutputHdfs(String outputHdfs) {
		this.outputHdfs = outputHdfs;
	}

	public String getFileResult() {
		return fileResult;
	}

	public void setFileResult(String fileResult) {
		this.fileResult = fileResult;
	}

	public TreeNode getJars() {
		return jars;
	}

	public void setJars(TreeNode jars) {
		this.jars = jars;
	}

	public TreeNode getSelectedNode() {
		return selectedNode;
	}

	public void setSelectedNode(TreeNode selectedNode) {
		this.selectedNode = selectedNode;
	}

	public String getSelectedPath() {
		return selectedPath;
	}

	public void setSelectedPath(String selectedPath) {
		this.selectedPath = selectedPath;
	}

	public List<String> getClasses() {
		return classes;
	}

	public void setClasses(List<String> classes) {
		this.classes = classes;
	}

	public String getOptionSubmit() {
		return optionSubmit;
	}

	public void setOptionSubmit(String optionSubmit) {
		this.optionSubmit = optionSubmit;
	}
}
