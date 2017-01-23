package org.bigsea.hadoop.manager.yarn;

import java.io.StringWriter;

public class YarnRun {
	public static void main(String[] args) {
		YarnManager yarn = new YarnManager("host/port", "hadoop_user",
				"password");		
		
		//receive the information about the cluster
//		System.out.println(yarn.getClusterInfo());
		
		//return the available nodes
//		System.out.println(yarn.getListNodes());
		
		//return the scheduler
//		System.out.println(yarn.getScheduler());
		
		//return the applications in cluster
//		System.out.println(yarn.getApplications());
		
		//return the applications by id
//		String appID = "02541255";
//		System.out.println(yarn.getApplicationInfo("1461696346387_0001"));
		
		//Submit Job
//		System.out.println(yarn.submitJob());
//		System.out.println(yarn.getApplicationInfo("1461696346387_0019"));
		
	}
}
