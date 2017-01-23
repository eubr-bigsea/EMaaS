package br.edu.BigSeaT44Imp.big.divers.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.json.JSONObject;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.sun.jersey.core.util.MultivaluedMapImpl;

import br.edu.BigSeaT44Imp.big.divers.model.FileNode;

@Service
@Component
public class JobService extends AbstractService {

	private final String MANAGER_JOB_PATH = getApplicationPath() + "/rest/managerJob/";

	public JobService() {
		super();
		super.setManagerPath(MANAGER_JOB_PATH);
	}

	public TreeNode createJars() {
		TreeNode root = new DefaultTreeNode(new FileNode("", "-", "-"), null);
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		params.add("path", "JARS");

		JSONObject jsonEntity = super.get("listFilesInDirectory", params);

		String[] jars = jsonEntity.getString("mesg").split("\n");
		TreeNode jarsNode = new DefaultTreeNode(new FileNode("JARS", "-", "-"), root);

		if (!jars[0].equals("")) {
			for (String jar : jars) {
				new DefaultTreeNode("jar", new FileNode(jar, "-", "-"), jarsNode);
			}
		}
		return root;
	}

	public List<String> getFromJars(String pathToAppJar) throws IOException, ClassNotFoundException {
		FileInputStream jar = new FileInputStream(pathToAppJar);
		ZipInputStream zipSteam = new ZipInputStream(jar);
		ZipEntry ze;
		List<String> classes = new ArrayList<String>();
		URL[] urls = { new URL("jar:file:" + pathToAppJar + "!/") };
		URLClassLoader cl = URLClassLoader.newInstance(urls);

		String wantedMethod = "main";

		while ((ze = zipSteam.getNextEntry()) != null) {

			// Is this a class?
			if (ze.getName().endsWith(".class")) {

				// Relative path of file into the jar.
				String className = ze.getName();

				if (!className.contains("/")) {
					// Complete class name
					className = className.replace(".class", "").replace("/", ".");
					Class<?> klazz = cl.loadClass(className);
					Method[] methodsArray = klazz.getMethods();
					for (Method method : methodsArray) {
						if (method.getName().equals(wantedMethod)) {
							classes.add(className.substring(className.lastIndexOf(".") + 1, className.length()));
						}
					}
				}
			}
		}
		zipSteam.close();
		return classes;
	}
	
	public String getNodePath(TreeNode treeNode, String type) {
		List<String> namesNodes = new ArrayList<String>();
		boolean isFile = type.equals("FILE");
		if (treeNode != null && ((treeNode.getType().equals("document") && isFile) || (!treeNode.getType().equals("document") && !isFile))) {
			namesNodes.add(treeNode.toString());
			treeNode = treeNode.getParent();
			while (treeNode != null) {
				namesNodes.add(0, treeNode.toString());
				treeNode = treeNode.getParent();
			}
		}

		String output = "";
		for (String nameNode : namesNodes) {
			output += nameNode + "/";
		}
		return output;
	}
	
}
