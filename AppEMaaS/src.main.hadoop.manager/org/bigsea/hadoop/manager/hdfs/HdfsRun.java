package org.bigsea.hadoop.manager.hdfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Properties;

import org.apache.hadoop.fs.http.client.KerberosWebYarnConnection;
import org.apache.hadoop.security.authentication.client.AuthenticationException;

public class HdfsRun {

	public static void main(String[] args) throws MalformedURLException, IOException, AuthenticationException {	
		String HDFS_HOST = null;
		String HDFS_PORT = null;
		String HDFS_USER = null;
		String HDFS_PASSWORD = null;
		
		Properties prop = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream("conf.properties");
			prop.load(input);

			HDFS_HOST = prop.getProperty("HDFS_HOST");
			HDFS_PORT = prop.getProperty("HDFS_PORT");
			HDFS_USER = prop.getProperty("HDFS_USER");
			HDFS_PASSWORD = prop.getProperty("HDFS_PASSWORD");

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		HdfsManager fs = new HdfsManager(HDFS_HOST + ":" + HDFS_PORT, HDFS_USER, HDFS_PASSWORD);
		
		//List files and diretories
		System.out.println(fs.listFiles("RootVeruska"));
		
		//Create directories
//		System.out.println(fs.createDirectory("RootVeruska"));
		
		//Delete files
//		System.out.println(fs.deleteFile("RootTiagooooo"));
		
		//Delete a directory completely
//		System.out.println(fs.deleteDirectory("RootTiago/"));
		System.out.println(fs.deleteDirectory("RootVeruska2/output1/"));
		
		//Upload files
//		FileInputStream is = new FileInputStream(new File("C:/teste.txt"));
//		String pathUpload = "RootVeruska2/teste.txt";
//		System.out.println(fs.uploadFiles(is, pathUpload));
		
		
		
		//Download file
//		System.out.println(fs.downloadFile("C:/hadoop-2.6.7.jar", "RootTiagoooo/hadoop-2.6.7.jar"));
		
//		System.out.println(fs.downloadFile("C:/Users/usuario/Documents/Dataset5PCC.txt", "RootVeruska/Dataset5PC.txt"));
		
		//Open file
//		System.out.println(fs.openFile("RootTiagoooo/hadoop-2.6.7.jar"));
		
		//Rename file or directory
//		System.out.println(fs.renameFile("RootVeruska", "RootVeruska2"));
		
		//Get root
//		System.out.println(fs.getRootDirectory());
	}

}
