package org.bigsea.hadoop.manager.submitJob;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.bigsea.hadoop.manager.exceptions.ExceptionsList;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

public class SSHmanager {
	private JSch jsch;
	private Session session;
	private static String HOME_HADOOP;
	private static String HOME_SPARK;
	private String applicationId = null;
//	private String ENV = "export HADOOP_CONF_DIR=/home/hadoop/hadoop/etc/hadoop/ \n"
//			+ "export HADOOP_HOME=/home/hadoop/hadoop \n" + "export HADOOP_INSTALL=$HADOOP_HOME \n"
//			+ "export PATH=$PATH:$HADOOP_INSTALL/bin \n" + "export PATH=$PATH:$HADOOP_INSTALL/sbin \n"
//			+ "export HADOOP_HOME=$HADOOP_INSTALL \n" + "export HADOOP_MAPRED_HOME=$HADOOP_INSTALL \n"
//			+ "export HADOOP_COMMON_HOME=$HADOOP_INSTALL \n" + "export HADOOP_HDFS_HOME=$HADOOP_INSTALL \n"
//			+ "export HADOOP_YARN_HOME=$HADOOP_INSTALL \n" + "export HADOOP_CONF_DIR=$HADOOP_INSTALL/etc/hadoop \n"
//			+ "export SPARK_YARN_DIST_FILES=$(ls $HADOOP_CONF_DIR* | sed 's#^#file://#g' | tr '\n' ',' | sed 's/,$//') \n"
//			+ "export YARN_CONF_DIR=$HADOOP_INSTALL/etc/hadoop/*.xml \n" + "export SPARK_HOME=/home/hadoop/spark \n"
//			+ "export PATH=$PATH:$HADOOP_HOME/bin:$SPARK_HOME/bin \n";
	
	private String ENV;

	public SSHmanager(String host, String user, int port, String password) {
		super();
		setConfig();
		this.jsch = new JSch();
		connectSSH(host, user, port, password);
	}

	private void setConfig(){
		Properties prop = new Properties();
		InputStream fileConfig = null;

		try {
			prop.load(this.getClass().getClassLoader().getResourceAsStream("conf.properties"));
			
			HOME_HADOOP = prop.getProperty("HADOOP_HOME");
			HOME_SPARK = prop.getProperty("SPARK_HOME");
			ENV = prop.getProperty("ENV");

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
	}
	
	/**
	 * Method to connect with remote node by ssh.
	 * 
	 * @param host
	 * @param user
	 * @param port
	 * @param tunnelRemotePort
	 * @param tunnelRemoteHost
	 * @param tunnelLocalPort
	 */
	private void connectSSH(String host, String user, int port, String password) {
		try {
			session = jsch.getSession(user, host, port);
			session.setPassword(password);
			UserInfo ui = new MyUserInfo();
			ui.promptPassword(password);
			session.setUserInfo(ui);
			session.connect();

		} catch (JSchException e) {
			JOptionPane.showMessageDialog(null, "Connection Failed.");
		}

	}

	/**
	 * Method to finalize the connection with remote node by ssh.
	 * 
	 */
	public void disconnectSSH() {
		session.disconnect();
	}

	/**
	 * This method receives the command (String) and execute in the remote node.
	 * The output is shared in terminal
	 * 
	 * @param command
	 */
	public void executeCommandinTerminal(String command) {
		try {
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			channel.setInputStream(null);
			((ChannelExec) channel).setErrStream(System.err);

			InputStream in = channel.getInputStream();
			channel.connect();

			byte[] tmp = new byte[1024];
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
					System.out.print(new String(tmp, 0, i));
				}
				if (channel.isClosed()) {
					if (in.available() > 0)
						continue;
					System.out.println("exit-status: " + channel.getExitStatus());
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (Exception ee) {
				}
			}
			channel.disconnect();
		} catch (IOException | JSchException e) {
			JOptionPane.showMessageDialog(null, "The command cannot be sent.");
		}
	}

	/**
	 * This method opens the jar files in pathRemote.
	 * 
	 * @param pathLocal
	 * @param pathRemote
	 * @return
	 * @throws Exception
	 */
	public File openFileJar(String pathRemote) throws Exception {
		FileOutputStream fos = null;
		File downloadedFile = null;

		try {
			// exec 'scp -f rfile' remotely
			String command = "scp -f " + pathRemote;
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			byte[] buf = new byte[1024];

			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();

			while (true) {
				int c = checkAck(in);
				if (c != 'C') {
					break;
				}

				// read '0644 '
				in.read(buf, 0, 5);

				long filesize = 0L;
				while (true) {
					if (in.read(buf, 0, 1) < 0) {
						// error
						break;
					}
					if (buf[0] == ' ')
						break;
					filesize = filesize * 10L + buf[0] - '0';
				}

				String file = null;
				for (int i = 0;; i++) {
					in.read(buf, i, 1);
					if (buf[i] == (byte) 0x0a) {
						file = new String(buf, 0, i);
						break;
					}
				}

				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();

				// read a content of pathLocal
				String pathTmp = extractNameFile(pathRemote);
				downloadedFile = File.createTempFile(pathTmp, ".jar");
				fos = new FileOutputStream(downloadedFile);
				int foo;
				while (true) {
					if (buf.length < filesize)
						foo = buf.length;
					else
						foo = (int) filesize;
					foo = in.read(buf, 0, foo);
					if (foo < 0) {
						// error
						break;
					}
					fos.write(buf, 0, foo);
					filesize -= foo;
					if (filesize == 0L)
						break;
				}
				fos.close();
				fos = null;

				if (checkAck(in) != 0) {
					System.exit(0);
				}

				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();
			}

			session.disconnect();

			try {
				if (fos != null)
					fos.close();
			} catch (Exception ee) {
			}

			return downloadedFile;

		} catch (Exception e) {
			throw new Exception("The file cannot be opened.");
		}
	}

	private String extractNameFile(String path) {
		String[] splitedName = path.split("/");
		String nameFile = splitedName[splitedName.length - 1];
		return nameFile.substring(0, nameFile.length() - 4);
	}

	/**
	 * This method receives the command (String) and execute in the remote node.
	 * The output is shared into the OutputStream
	 * 
	 * @param command
	 * @return
	 * @throws SSHmanagerException
	 */
	public Map<String, Object> executeCommand(String command) {
		setApplicationId(null);
		try {
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			OutputStream hadoopOutput = new ByteArrayOutputStream();
			channel.setInputStream(null);
			((ChannelExec) channel).setErrStream(hadoopOutput);

			InputStream in = channel.getInputStream();
			channel.connect();

			ByteArrayOutputStream writer = new ByteArrayOutputStream();

			byte[] tmp = new byte[1024];
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
					writer.write(tmp, 0, i);
					// System.out.print(new String(tmp, 0, i));
				}
				if (channel.isClosed()) {
					if (in.available() > 0) {
						continue;
					} else {
						// System.out.println("exit-status: " +
						// channel.getExitStatus());
						break;
					}
				}

				String line = hadoopOutput.toString();
				if (command.contains("hadoop jar") && line.contains("job_")) {
					int init = line.lastIndexOf("job_");
					int end = init;
					for (int i = init; i < line.length(); i++) {
						if (line.charAt(i) == '\n' || line.charAt(i) == ' ') {
							break;
						}
						end++;
					}
					JobID.getInstance().setJobId(line.substring(init, end));
					setApplicationId(line.substring(init, end));
				}

				try {
					Thread.sleep(1000);
				} catch (Exception ee) {
				}

			}

			Map<String, Object> result = new HashMap<String, Object>();
			result.put("code", channel.getExitStatus());
			if (writer.size() == 0) {
				result.put("mesg", hadoopOutput.toString());
			} else {
				result.put("mesg", writer.toString());
			}

			return result;
		} catch (IOException | JSchException e) {
			JOptionPane.showMessageDialog(null, "The command cannot be sent.");
		}
		return null;
	}

	/**
	 * This method mapped the Map (json) to JSON format.
	 * 
	 * @param response
	 * @return
	 */
	private JSONObject convertToJSON(Map<String, Object> response) {
		Gson gson = new Gson();
		String jsonFormat = gson.toJson(response);
		JSONObject json = new JSONObject(jsonFormat);
		return json;
	}

	/**
	 * This method receives the command (String) to remove a file in the remote
	 * node.
	 * 
	 * @param command
	 * @return
	 */
	public JSONObject deleteFile(String pathFile) {
		String command = "rm " + pathFile;
		Map<String, Object> response = executeCommand(command);
		if ((Integer) response.get("code") != 0) {
			response.put("error", ExceptionsList.FILE_DIRECTORY_NOT_FOUND.getError());
		}
		return convertToJSON(response);
	}

	/**
	 * This method receives the command (String) to remove a directory
	 * (completely - cascade) in the remote node.
	 * 
	 * @param command
	 * @return
	 */
	public JSONObject deleteDirectory(String pathDirectory) {
		String command = "rm -r " + pathDirectory;
		Map<String, Object> response = executeCommand(command);
		if ((Integer) response.get("code") != 0) {
			response.put("error", ExceptionsList.FILE_DIRECTORY_NOT_FOUND.getError());
		}
		return convertToJSON(response);
	}

	/**
	 * This method receives the command (String) to remove a directory
	 * (completely - cascade) in the remote node.
	 * 
	 * @param command
	 * @return
	 */
	public JSONObject createDirectory(String pathDirectory) {
		String command = "mkdir -p " + pathDirectory;
		Map<String, Object> response = executeCommand(command);
		if ((Integer) response.get("code") != 0) {
			response.put("error", ExceptionsList.CANNOT_CREATE_DIRECTORY.getError());
		}
		return convertToJSON(response);
	}

	/**
	 * This method receives the command (String) and list the files into
	 * directory path. If the pathDirectory is empty, will be list the files in
	 * root directory.
	 * 
	 * @param command
	 * @return
	 */
	public JSONObject listFilesInDirectory(String pathDirectory) {
		String command = "ls " + pathDirectory;
		Map<String, Object> response = executeCommand(command);
		if ((Integer) response.get("code") != 0) {
			response.put("error", ExceptionsList.FILE_DIRECTORY_NOT_FOUND.getError());
		}
		return convertToJSON(response);
	}

	public Map<String, Object> executeSparkCommand(String command) {
		try {
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			OutputStream hadoopOutput = new ByteArrayOutputStream();
			channel.setInputStream(null);
			((ChannelExec) channel).setErrStream(hadoopOutput);

			InputStream in = channel.getInputStream();
			channel.connect();

			ByteArrayOutputStream writer = new ByteArrayOutputStream();

			byte[] tmp = new byte[1024];
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
					writer.write(tmp, 0, i);
				}
				if (channel.isClosed()) {
					if (in.available() > 0) {
						continue;
					} else {
						break;
					}
				}

				try {
					Thread.sleep(1000);
				} catch (Exception ee) {
				}

			}

			Map<String, Object> result = new HashMap<String, Object>();
			result.put("code", channel.getExitStatus());
			if (writer.size() == 0) {
				result.put("mesg", hadoopOutput.toString());
			} else {
				result.put("mesg", writer.toString());
				result.put("output", "The Spark job was submitted with sucess.");
			}

			return result;
		} catch (IOException | JSchException e) {
			JOptionPane.showMessageDialog(null, "The command cannot be sent.");
		}
		return null;
	}

	public JSONObject submitSparkJob(String jarPath, String mainClass, String inputHdfs, String outputHdfs) {
		String command = HOME_SPARK + "spark-submit --class " + mainClass
				+ " --master yarn --deploy-mode client " + jarPath + " " + inputHdfs + " " + outputHdfs;
		Map<String, Object> response = executeSparkCommand(ENV + command);
		if ((Integer) response.get("code") != 0) {
			response.put("error", ExceptionsList.CANNOT_SUBMIT_JOB.getError());
		}
		return convertToJSON(response);
	}

	/**
	 * This method receives the commands to compose a job submission (String)
	 * and execute in the remote node.
	 * 
	 * @param command
	 * @param string3
	 * @param string2
	 * @param string
	 * @return
	 */
	public JSONObject submitHadoopJob(String jarPath, String mainClass, String inputHdfs, String outputHdfs) {
		String command = HOME_HADOOP + "hadoop jar " + jarPath + " " + mainClass + " " + inputHdfs + " "
				+ outputHdfs;
		Map<String, Object> response = executeCommand(command);
		if ((Integer) response.get("code") != 0) {
			response.put("error", ExceptionsList.CANNOT_SUBMIT_JOB.getError());
		}
		return convertToJSON(response);
	}

	// public JSONObject getRunningJobs() {
	// String command = "yarn application --list";
	// Map<String, Object> response = executeCommand(command);
	// if ((Integer)response.get("code") != 0) {
	// response.put("error", ExceptionsList.CANNOT_SUBMIT_JOB.getError());
	// }
	// return convertToJSON(response);
	// }

	public JSONObject statusJob(String jobId) {
		String command = HOME_HADOOP + "yarn application -status " + jobId;
		Map<String, Object> response = executeCommand(command);
		if ((Integer) response.get("code") != 0) {
			response.put("error", ExceptionsList.CANNOT_GET_STATUS.getError());
		}
		return convertToJSON(response);
	}

	public void shell() {
		try {
			Channel channel = session.openChannel("shell");
			channel.setInputStream(System.in);
			channel.setOutputStream(System.out);
			channel.connect(3 * 1000);
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Channel configureChannel(Session session) throws JSchException {
		Channel channel = session.openChannel("exec");

		// channel.setOutputStream(out);
		channel.setOutputStream(System.out);

		((ChannelExec) channel).setErrStream(System.err);

		return channel;
	}

	/**
	 * This method receives the path of local file and send to remote node (into
	 * the remote path).
	 * 
	 * @param uploadedFile
	 * @param pathRemote
	 * @throws Exception
	 */
	public void sendFile(File uploadedFile, String pathRemote) throws Exception {
		boolean ptimestamp = true;
		FileInputStream fis = null;
		try {
			// exec 'scp -t rfile' remotely
			String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + pathRemote;
			Channel channel;

			channel = session.openChannel("exec");

			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			if (checkAck(in) != 0) {
				System.exit(0);
			}

			File _lfile = uploadedFile;

			if (ptimestamp) {
				command = "T" + (_lfile.lastModified() / 1000) + " 0";
				// The access time should be sent here,
				// but it is not accessible with JavaAPI ;-<
				command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
				out.write(command.getBytes());
				out.flush();
				if (checkAck(in) != 0) {
					System.exit(0);
				}
			}

			// send "C0644 filesize filename", where filename should not include
			long filesize = _lfile.length();
			command = "C0644 " + filesize + " " + uploadedFile.getName() + "\n";
			// if (uploadedFile.getName().lastIndexOf('/') > 0) {
			// command +=
			// uploadedFile.getName().substring(uploadedFile.getName().lastIndexOf('/')
			// + 1);
			// } else {
			// command += uploadedFile.getName();
			// }
			// command += "\n";
			out.write(command.getBytes());
			out.flush();
			if (checkAck(in) != 0) {
				System.exit(0);
			}

			// send a content of pathLocal
			fis = new FileInputStream(uploadedFile);
			byte[] buf = new byte[1024];
			while (true) {
				int len = fis.read(buf, 0, buf.length);
				if (len <= 0)
					break;
				out.write(buf, 0, len);
			}

			fis.close();
			fis = null;

			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			if (checkAck(in) != 0) {
				System.exit(0);
			}
			out.close();

			channel.disconnect();

			try {
				if (fis != null)
					fis.close();
			} catch (Exception ee) {
			}
		} catch (JSchException | IOException e) {
			throw new Exception("The file cannot be sent.");
			// JOptionPane.showMessageDialog(null, "The file cannot be sent.");
		}
	}

	/**
	 * This method downloads the file (in pathRemote) to local node (into the
	 * local path).
	 * 
	 * @param pathLocal
	 * @param pathRemote
	 * @return
	 * @throws Exception
	 */
	public File downloadFile(String pathLocal, String pathRemote) throws Exception {
		FileOutputStream fos = null;
		File downloadedFile = null;
		// String prefix = null;
		// if (new File(pathLocal).isDirectory()) {
		// prefix = pathLocal + File.separator;
		// }

		try {
			// exec 'scp -f rfile' remotely
			String command = "scp -f " + pathRemote;
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			byte[] buf = new byte[1024];

			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();

			while (true) {
				int c = checkAck(in);
				if (c != 'C') {
					break;
				}

				// read '0644 '
				in.read(buf, 0, 5);

				long filesize = 0L;
				while (true) {
					if (in.read(buf, 0, 1) < 0) {
						// error
						break;
					}
					if (buf[0] == ' ')
						break;
					filesize = filesize * 10L + buf[0] - '0';
				}

				String file = null;
				for (int i = 0;; i++) {
					in.read(buf, i, 1);
					if (buf[i] == (byte) 0x0a) {
						file = new String(buf, 0, i);
						break;
					}
				}

				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();

				// read a content of pathLocal
				downloadedFile = new File(pathLocal + file);
				fos = new FileOutputStream(downloadedFile);
				int foo;
				while (true) {
					if (buf.length < filesize)
						foo = buf.length;
					else
						foo = (int) filesize;
					foo = in.read(buf, 0, foo);
					if (foo < 0) {
						// error
						break;
					}
					fos.write(buf, 0, foo);
					filesize -= foo;
					if (filesize == 0L)
						break;
				}
				fos.close();
				fos = null;

				if (checkAck(in) != 0) {
					System.exit(0);
				}

				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();
			}

			session.disconnect();

			try {
				if (fos != null)
					fos.close();
			} catch (Exception ee) {
			}

			return downloadedFile;

		} catch (Exception e) {
			throw new Exception("The file cannot be downloaded.");
			// JOptionPane.showMessageDialog(null, "The file cannot be
			// downloaded.");
		}
	}

	/**
	 * Static Class to store information about the user (connection).
	 * 
	 * @author Brasileiro
	 *
	 */
	public static class MyUserInfo implements UserInfo, UIKeyboardInteractive {
		String passwd;

		@Override
		public String getPassword() {
			return passwd;
		}

		@Override
		public boolean promptYesNo(String str) {
			// Object[] options = { "yes", "no" };
			// int foo = JOptionPane.showOptionDialog(null, str, "Warning",
			// JOptionPane.DEFAULT_OPTION,
			// JOptionPane.WARNING_MESSAGE, null, options, options[0]);
			// return foo == 0;
			return true;
		}

		JTextField passwordField = new JPasswordField(20);

		@Override
		public String getPassphrase() {
			return null;
		}

		@Override
		public boolean promptPassphrase(String message) {
			return true;
		}

		@Override
		public boolean promptPassword(String password) {
			passwd = password;
			return true;
			// Object[] ob = { passwordField };
			// int result = JOptionPane.showConfirmDialog(null, ob, message,
			// JOptionPane.OK_CANCEL_OPTION);
			// if (result == JOptionPane.OK_OPTION) {
			// passwd = passwordField.getText();
			// return true;
			// } else {
			// return false;
			// }
		}

		@Override
		public void showMessage(String message) {
			JOptionPane.showMessageDialog(null, message);
		}

		final GridBagConstraints gbc = new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.NORTHWEST,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
		private Container panel;

		@Override
		public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt,
				boolean[] echo) {
			panel = new JPanel();
			panel.setLayout(new GridBagLayout());

			gbc.weightx = 1.0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.gridx = 0;
			panel.add(new JLabel(instruction), gbc);
			gbc.gridy++;

			gbc.gridwidth = GridBagConstraints.RELATIVE;

			JTextField[] texts = new JTextField[prompt.length];
			for (int i = 0; i < prompt.length; i++) {
				gbc.fill = GridBagConstraints.NONE;
				gbc.gridx = 0;
				gbc.weightx = 1;
				panel.add(new JLabel(prompt[i]), gbc);

				gbc.gridx = 1;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.weighty = 1;
				if (echo[i]) {
					texts[i] = new JTextField(20);
				} else {
					texts[i] = new JPasswordField(20);
				}
				panel.add(texts[i], gbc);
				gbc.gridy++;
			}

			if (JOptionPane.showConfirmDialog(null, panel, destination + ": " + name, JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
				String[] response = new String[prompt.length];
				for (int i = 0; i < prompt.length; i++) {
					response[i] = texts[i].getText();
				}
				return response;
			} else {
				return null; // cancel
			}
		}

	}

	/**
	 * TODO
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	static int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (b == 0)
			return b;
		if (b == -1)
			return b;

		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				System.out.print(sb.toString());
			}
			if (b == 2) { // fatal error
				System.out.print(sb.toString());
			}
		}
		return b;
	}

	public String getLastJobId() {
		return applicationId.replace("job", "application");
	}

	private void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

}
