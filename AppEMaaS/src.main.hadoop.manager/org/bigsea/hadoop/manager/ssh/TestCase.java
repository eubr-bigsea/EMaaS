package org.bigsea.hadoop.manager.ssh;

public class TestCase {
	public static void main(String[] args) {
		SSHInterface ssh = new SSHInterface("host", "hadoop_user", 22, "password");
//		ssh.executeCommand("ls");
		ssh.sendFile("C:/wordcountH.jar", "/home/hadoop/hadoop/");
//		ssh.downloadFile("/home/hadoop/wordcount.jar", "C:/");
		
		
		ssh.disconnectSSH();
		
	}
}
