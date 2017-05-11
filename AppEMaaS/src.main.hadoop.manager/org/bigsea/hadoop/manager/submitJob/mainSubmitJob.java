package org.bigsea.hadoop.manager.submitJob;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class mainSubmitJob {
	public static void main(String[] args) throws Exception {
		
		String SSH_HOST = null;
		Integer SSH_PORT = null;
		String SSH_USER = null;
		String SSH_PASSWORD = null;
		
		Properties prop = new Properties();
		InputStream fileConfig = null;

		try {
			fileConfig = new FileInputStream("conf.properties");
			prop.load(fileConfig);

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
		
		SSHmanager ssh = new SSHmanager(SSH_HOST, SSH_USER, SSH_PORT, SSH_PASSWORD);
		
//		System.out.println(ssh.submitSparkJob("spark/examples/jars/spark-examples_2.11-2.0.0.jar", "org.apache.spark.examples.SparkPi", "10", ""));

		System.out.println(ssh.submitSparkJob("JARS/BasicLocal.jar", "approaches.basic.Basic", "/RootVeruska2/Dataset5PC.txt/", "/RootVeruska2/output6"));

		ssh.disconnectSSH();
	}

}
