package org.bigsea.hadoop.manager.hdfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.hadoop.fs.http.client.KerberosWebYarnConnection;
import org.apache.hadoop.security.authentication.client.AuthenticationException;

public class HdfsRun {

	public static void main(String[] args) throws MalformedURLException, IOException, AuthenticationException {
		HdfsManager fs = new HdfsManager("http://150.165.75.67:60070", "hadoop",
				"lqd@bigsea");
		
		
		//List files and diretories
//		System.out.println(fs.listFiles("RootTiago"));
		
		//Create directories
//		System.out.println(fs.createDirectory("RootTiago"));
		
		//Delete files
//		System.out.println(fs.deleteFile("RootTiagooooo"));
		
		//Delete a directory completely
//		System.out.println(fs.deleteDirectory("RootTiago/"));
			
		//Upload files
//		FileInputStream is = new FileInputStream(new File("C:/hadoop-mapreduce-examples-2.2.0.jar"));
//		String pathUpload = "RootTiagoooo/hadoop-2.6.7.jar";
//		System.out.println(fs.uploadFiles(is, pathUpload));
		
		//Download file
//		System.out.println(fs.downloadFile("C:/hadoop-2.6.7.jar", "RootTiagoooo/hadoop-2.6.7.jar"));
		
		//Open file
//		System.out.println(fs.openFile("RootTiagoooo/hadoop-2.6.7.jar"));
		
		//Rename file or directory
//		System.out.println(fs.renameFile("RootTiagoooo2", "RootTiagoooo3"));
		
		//Get root
//		System.out.println(fs.getRootDirectory());
	}

}
