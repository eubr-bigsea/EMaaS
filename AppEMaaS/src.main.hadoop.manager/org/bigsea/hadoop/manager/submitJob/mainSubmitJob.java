package org.bigsea.hadoop.manager.submitJob;

import java.io.File;

public class mainSubmitJob {
	public static void main(String[] args) throws Exception {
		SSHmanager ssh = new SSHmanager("host", "hadoop_user", 22, "password");
		
		System.out.println(ssh.submitSparkJob("spark/examples/jars/spark-examples_2.11-2.0.0.jar", "org.apache.spark.examples.SparkPi", "10", ""));

		ssh.disconnectSSH();
	}

}
