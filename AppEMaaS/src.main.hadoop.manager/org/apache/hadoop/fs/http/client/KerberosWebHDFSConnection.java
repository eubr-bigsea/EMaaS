package org.apache.hadoop.fs.http.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.security.authentication.client.AuthenticatedURL;
import org.apache.hadoop.security.authentication.client.AuthenticatedURL.Token;
import org.apache.hadoop.security.authentication.client.AuthenticationException;
import org.apache.hadoop.security.authentication.client.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * 
===== HTTP GET <br/>
<li>OPEN (see FileSystem.open)
<li>GETFILESTATUS (see FileSystem.getFileStatus)
<li>LISTSTATUS (see FileSystem.listStatus)
<li>GETCONTENTSUMMARY (see FileSystem.getContentSummary)
<li>GETFILECHECKSUM (see FileSystem.getFileChecksum)
<li>GETHOMEDIRECTORY (see FileSystem.getHomeDirectory)
<li>GETDELEGATIONTOKEN (see FileSystem.getDelegationToken)
<li>GETDELEGATIONTOKENS (see FileSystem.getDelegationTokens)
<br/>
===== HTTP PUT <br/>
<li>CREATE (see FileSystem.create)
<li>MKDIRS (see FileSystem.mkdirs)
<li>CREATESYMLINK (see FileContext.createSymlink)
<li>RENAME (see FileSystem.rename)
<li>SETREPLICATION (see FileSystem.setReplication)
<li>SETOWNER (see FileSystem.setOwner)
<li>SETPERMISSION (see FileSystem.setPermission)
<li>SETTIMES (see FileSystem.setTimes)
<li>RENEWDELEGATIONTOKEN (see FileSystem.renewDelegationToken)
<li>CANCELDELEGATIONTOKEN (see FileSystem.cancelDelegationToken)
<br/>
===== HTTP POST <br/>
APPEND (see FileSystem.append)
<br/>
===== HTTP DELETE <br/>
DELETE (see FileSystem.delete)

 */
public class KerberosWebHDFSConnection implements WebHDFSConnection {

	protected static final Logger logger = LoggerFactory.getLogger(KerberosWebHDFSConnection.class);

	private String httpfsUrl = WebHDFSConnectionFactory.DEFAULT_PROTOCOL 
			+ WebHDFSConnectionFactory.DEFAULT_HOST + ":" + WebHDFSConnectionFactory.DEFAULT_PORT;
	private String principal = WebHDFSConnectionFactory.DEFAULT_USERNAME;
	private String password = WebHDFSConnectionFactory.DEFAULT_PASSWORD;

	private Token token = new AuthenticatedURL.Token();
	private AuthenticatedURL authenticatedURL = new AuthenticatedURL(new KerberosAuthenticator2(principal, password));

	public KerberosWebHDFSConnection() {
	}

	public KerberosWebHDFSConnection(String httpfsUrl, String principal, String password) {
		this.httpfsUrl = httpfsUrl;
		this.principal = principal;
		this.password = password;
		this.authenticatedURL = new AuthenticatedURL(new KerberosAuthenticator2(principal, password));
	}

	public static synchronized Token generateToken(String srvUrl, String princ, String passwd) {
		AuthenticatedURL.Token newToken = new AuthenticatedURL.Token();
		try {

			HttpURLConnection conn = new AuthenticatedURL(new KerberosAuthenticator2(princ, passwd)).openConnection(
					new URL(new URL(srvUrl), "/webhdfs/v1/?op=GETHOMEDIRECTORY"), newToken);

			conn.connect();

			conn.disconnect();

		} catch (Exception ex) {
			logger.error(ex.getMessage());
			logger.error("[" + princ + ":" + passwd + "]@" + srvUrl, ex);
			// WARN
			// throws MalformedURLException, IOException,
			// AuthenticationException, InterruptedException
		}

		return newToken;

	}

	protected static long copy(InputStream input, OutputStream result) throws IOException {
		byte[] buffer = new byte[12288]; // 8K=8192 12K=12288 64K=
		long count = 0L;
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			result.write(buffer, 0, n);
			count += n;
			result.flush();
		}
		result.flush();
		return count;
	}

	/**
	 * Report the result in JSON way
	 * 
	 * @param conn
	 * @param input
	 * @return
	 * @throws IOException
	 */
	private static Map<String, Object> result(HttpURLConnection conn, boolean input) throws IOException {
		StringBuffer sb = new StringBuffer();
		if (input) {
			InputStream is = conn.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
			String line = null;

			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			reader.close();
			is.close();
		}
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("code", conn.getResponseCode());
		result.put("mesg", conn.getResponseMessage());
		result.put("type", conn.getContentType());
		result.put("data", sb);
		
		return result;
		//
		// Convert a Map into JSON string.
		//
//		Gson gson = new Gson();
//		String json = gson.toJson(result);
//		logger.info("json = " + json);
//
//		//
//		// Convert JSON string back to Map.
//		//
//		// Type type = new TypeToken<Map<String, Object>>(){}.getType();
//		// Map<String, Object> map = gson.fromJson(json, type);
//		// for (String key : map.keySet()) {
//		// System.out.println("map.get = " + map.get(key));
//		// }
//
//		return json;
	}

	public void ensureValidToken() {
		if (!token.isSet()) { // if token is null
			token = generateToken(httpfsUrl, principal, password);
		} else {
			long currentTime = new Date().getTime();
			long tokenExpired = Long.parseLong(token.toString().split("&")[3].split("=")[1]);
			logger.info("[currentTime vs. tokenExpired] " + currentTime + " " + tokenExpired);

			if (currentTime > tokenExpired) { // if the token is expired
				token = generateToken(httpfsUrl, principal, password);
			}
		}

	}

	/*
	 * ========================================================================
	 * GET
	 * ========================================================================
	 */
	/**
	 * <b>GETHOMEDIRECTORY</b>
	 * 
	 * curl -i "http://<HOST>:<PORT>/webhdfs/v1/?op=GETHOMEDIRECTORY"
	 * 
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws AuthenticationException
	 */
	public Map<String, Object> getHomeDirectory() throws MalformedURLException, IOException, AuthenticationException {
		ensureValidToken();

		HttpURLConnection conn = authenticatedURL.openConnection(new URL(new URL(httpfsUrl),
				"/webhdfs/v1/?op=GETHOMEDIRECTORY"), token);
		conn.connect();

		Map<String, Object> resp = result(conn, true);
		conn.disconnect();
		return resp;
	}

	/**
	 * <b>OPEN</b>
	 * 
	 * curl -i -L "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=OPEN
	 * [&offset=<LONG>][&length=<LONG>][&buffersize=<INT>]"
	 * 
	 * @param path
	 * @param os
	 * @throws AuthenticationException
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public Map<String, Object> open(String path, OutputStream os) throws MalformedURLException, IOException, AuthenticationException {
		ensureValidToken();

		HttpURLConnection conn = authenticatedURL.openConnection(
				new URL(new URL(httpfsUrl), MessageFormat.format("/webhdfs/v1/{0}?op=OPEN", URLUtil.encodePath(path))),
				token);
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Content-Type", "application/octet-stream");
		//
		String redirect = conn.getURL().toString().replace("50070", "50075") + "&namenoderpcaddress=0.0.0.0:9000&overwrite=false";
		conn.disconnect();
		conn = authenticatedURL.openConnection(new URL(redirect), token);
		//
		conn.connect();
		InputStream is = conn.getInputStream();
		copy(is, os);
		is.close();
		os.close();
		Map<String, Object> resp = result(conn, false);
		conn.disconnect();

		return resp;
	}

	/**
	 * <b>GETCONTENTSUMMARY</b>
	 * 
	 * curl -i "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=GETCONTENTSUMMARY"
	 * 
	 * @param path
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws AuthenticationException
	 */
	public Map<String, Object> getContentSummary(String path) throws MalformedURLException, IOException, AuthenticationException {
		ensureValidToken();

		HttpURLConnection conn = authenticatedURL.openConnection(
				new URL(new URL(httpfsUrl), MessageFormat.format("/webhdfs/v1/{0}?op=GETCONTENTSUMMARY",
						URLUtil.encodePath(path))), token);
		conn.setRequestMethod("GET");
		// conn.setRequestProperty("Content-Type", "application/octet-stream");
		conn.connect();
		Map<String, Object> resp = result(conn, true);
		conn.disconnect();

		return resp;
	}

	/**
	 * <b>LISTSTATUS</b>
	 * 
	 * curl -i "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=LISTSTATUS"
	 * 
	 * @param path
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws AuthenticationException
	 */
	public Map<String, Object> listStatus(String path) throws MalformedURLException, IOException, AuthenticationException {
		ensureValidToken();

		HttpURLConnection conn = authenticatedURL.openConnection(
				new URL(new URL(httpfsUrl), MessageFormat.format("/webhdfs/v1/{0}?op=LISTSTATUS",
						URLUtil.encodePath(path))), token);
		conn.setRequestMethod("GET");
		conn.connect();
		Map<String, Object> resp = result(conn, true);
		conn.disconnect();

		return resp;
	}

	/**
	 * <b>GETFILESTATUS</b>
	 * 
	 * curl -i "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=GETFILESTATUS"
	 * 
	 * @param path
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws AuthenticationException
	 */
	public Map<String, Object> getFileStatus(String path) throws MalformedURLException, IOException, AuthenticationException {
		ensureValidToken();

		HttpURLConnection conn = authenticatedURL.openConnection(
				new URL(new URL(httpfsUrl), MessageFormat.format("/webhdfs/v1/{0}?op=GETFILESTATUS",
						URLUtil.encodePath(path))), token);
		conn.setRequestMethod("GET");
		conn.connect();
		Map<String, Object> resp = result(conn, true);
		conn.disconnect();

		return resp;
	}

	/**
	 * <b>GETFILECHECKSUM</b>
	 * 
	 * curl -i "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=GETFILECHECKSUM"
	 * 
	 * @param path
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws AuthenticationException
	 */
	public Map<String, Object> getFileCheckSum(String path) throws MalformedURLException, IOException, AuthenticationException {
		Map<String, Object> resp = null;
		ensureValidToken();

		HttpURLConnection conn = authenticatedURL.openConnection(
				new URL(new URL(httpfsUrl), MessageFormat.format("/webhdfs/v1/{0}?op=GETFILECHECKSUM",
						URLUtil.encodePath(path))), token);

		conn.setRequestMethod("GET");
		conn.connect();
		resp = result(conn, true);
		conn.disconnect();

		return resp;
	}

	/*
	 * ========================================================================
	 * PUT
	 * ========================================================================
	 */
	/**
	 * <b>CREATE</b>
	 * 
	 * curl -i -X PUT "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=CREATE
	 * [&overwrite=<true|false>][&blocksize=<LONG>][&replication=<SHORT>]
	 * [&permission=<OCTAL>][&buffersize=<INT>]"
	 * 
	 * @param path
	 * @param is
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws AuthenticationException
	 */
	public Map<String, Object> create(String path, InputStream is) throws MalformedURLException, IOException,
			AuthenticationException {
		Map<String, Object> resp = null;
		ensureValidToken();

		String redirectUrl = null;
		HttpURLConnection conn = authenticatedURL
				.openConnection(
						new URL(new URL(httpfsUrl), MessageFormat.format("/webhdfs/v1/{0}?op=CREATE",
								URLUtil.encodePath(path))), token);
		conn.setRequestMethod("PUT");
		conn.setInstanceFollowRedirects(false);
		conn.connect();
		logger.info("Location:" + conn.getHeaderField("Location"));
		resp = result(conn, true);
		if (conn.getResponseCode() == 307)
//			redirectUrl = conn.getHeaderField("Location");
			redirectUrl = conn.getURL().toString().replace("50070", "50075") + "&namenoderpcaddress=0.0.0.0:9000&overwrite=false";
			conn.disconnect();

		if (redirectUrl != null) {
			conn = authenticatedURL.openConnection(new URL(redirectUrl), token);
			conn.setRequestMethod("PUT");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setUseCaches(false);
			conn.setRequestProperty("Content-Type", "application/octet-stream");
			// conn.setRequestProperty("Transfer-Encoding", "chunked");
			final int _SIZE = is.available();
			conn.setRequestProperty("Content-Length", "" + _SIZE);
			conn.setFixedLengthStreamingMode(_SIZE);
			conn.connect();
			OutputStream os = conn.getOutputStream();
			copy(is, os);
			// Util.copyStream(is, os);
			is.close();
			os.close();
			resp = result(conn, false);
			conn.disconnect();
		}

		return resp;
	}

	/**
	 * <b>MKDIRS</b>
	 * 
	 * curl -i -X PUT
	 * "http://<HOST>:<PORT>/<PATH>?op=MKDIRS[&permission=<OCTAL>]"
	 * 
	 * @param path
	 * @return
	 * @throws AuthenticationException
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public Map<String, Object> mkdirs(String path) throws MalformedURLException, IOException, AuthenticationException {
		Map<String, Object> resp = null;
		ensureValidToken();

		HttpURLConnection conn = authenticatedURL
				.openConnection(
						new URL(new URL(httpfsUrl), MessageFormat.format("/webhdfs/v1/{0}?op=MKDIRS",
								URLUtil.encodePath(path))), token);
		conn.setRequestMethod("PUT");
		conn.connect();
		resp = result(conn, true);
		conn.disconnect();

		return resp;
	}

	/**
	 * <b>CREATESYMLINK</b>
	 * 
	 * curl -i -X PUT "http://<HOST>:<PORT>/<PATH>?op=CREATESYMLINK
	 * &destination=<PATH>[&createParent=<true|false>]"
	 * 
	 * @param path
	 * @return
	 * @throws AuthenticationException
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public Map<String, Object> createSymLink(String srcPath, String destPath) throws MalformedURLException, IOException,
			AuthenticationException {
		Map<String, Object> resp = null;
		ensureValidToken();

		HttpURLConnection conn = authenticatedURL.openConnection(
				new URL(new URL(httpfsUrl), MessageFormat.format("/webhdfs/v1/{0}?op=CREATESYMLINK&destination={1}",
						URLUtil.encodePath(srcPath), URLUtil.encodePath(destPath))), token);
		conn.setRequestMethod("PUT");
		conn.connect();
		resp = result(conn, true);
		conn.disconnect();

		return resp;
	}

	/**
	 * <b>RENAME</b>
	 * 
	 * curl -i -X PUT "http://<HOST>:<PORT>/<PATH>?op=RENAME
	 * &destination=<PATH>[&createParent=<true|false>]"
	 * 
	 * @param path
	 * @return
	 * @throws AuthenticationException
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public Map<String, Object> rename(String srcPath, String destPath) throws MalformedURLException, IOException,
			AuthenticationException {
		Map<String, Object> resp = null;
		ensureValidToken();

		HttpURLConnection conn = authenticatedURL.openConnection(
				new URL(new URL(httpfsUrl), MessageFormat.format("/webhdfs/v1/{0}?op=RENAME&destination=/{1}",
						URLUtil.encodePath(srcPath), URLUtil.encodePath(destPath))), token);
		conn.setRequestMethod("PUT");
		conn.connect();
		resp = result(conn, true);
		conn.disconnect();

		return resp;
	}

	/**
	 * <b>SETPERMISSION</b>
	 * 
	 * curl -i -X PUT "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=SETPERMISSION
	 * [&permission=<OCTAL>]"
	 * 
	 * @param path
	 * @return
	 * @throws AuthenticationException
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public Map<String, Object> setPermission(String path) throws MalformedURLException, IOException, AuthenticationException {
		Map<String, Object> resp = null;
		ensureValidToken();

		HttpURLConnection conn = authenticatedURL.openConnection(
				new URL(new URL(httpfsUrl), MessageFormat.format("/webhdfs/v1/{0}?op=SETPERMISSION",
						URLUtil.encodePath(path))), token);
		conn.setRequestMethod("PUT");
		conn.connect();
		resp = result(conn, true);
		conn.disconnect();

		return resp;
	}

	/**
	 * <b>SETOWNER</b>
	 * 
	 * curl -i -X PUT "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=SETOWNER
	 * [&owner=<USER>][&group=<GROUP>]"
	 * 
	 * @param path
	 * @return
	 * @throws AuthenticationException
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public Map<String, Object> setOwner(String path) throws MalformedURLException, IOException, AuthenticationException {
		Map<String, Object> resp = null;
		ensureValidToken();

		HttpURLConnection conn = authenticatedURL.openConnection(
				new URL(new URL(httpfsUrl), MessageFormat.format("/webhdfs/v1/{0}?op=SETOWNER",
						URLUtil.encodePath(path))), token);
		conn.setRequestMethod("PUT");
		conn.connect();
		resp = result(conn, true);
		conn.disconnect();

		return resp;
	}

	/**
	 * <b>SETREPLICATION</b>
	 * 
	 * curl -i -X PUT "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=SETREPLICATION
	 * [&replication=<SHORT>]"
	 * 
	 * @param path
	 * @return
	 * @throws AuthenticationException
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public Map<String, Object> setReplication(String path) throws MalformedURLException, IOException, AuthenticationException {
		Map<String, Object> resp = null;
		ensureValidToken();

		HttpURLConnection conn = authenticatedURL.openConnection(
				new URL(new URL(httpfsUrl), MessageFormat.format("/webhdfs/v1/{0}?op=SETREPLICATION",
						URLUtil.encodePath(path))), token);
		conn.setRequestMethod("PUT");
		conn.connect();
		resp = result(conn, true);
		conn.disconnect();

		return resp;
	}

	/**
	 * <b>SETTIMES</b>
	 * 
	 * curl -i -X PUT "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=SETTIMES
	 * [&modificationtime=<TIME>][&accesstime=<TIME>]"
	 * 
	 * @param path
	 * @return
	 * @throws AuthenticationException
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public Map<String, Object> setTimes(String path) throws MalformedURLException, IOException, AuthenticationException {
		Map<String, Object> resp = null;
		ensureValidToken();

		HttpURLConnection conn = authenticatedURL.openConnection(
				new URL(new URL(httpfsUrl), MessageFormat.format("/webhdfs/v1/{0}?op=SETTIMES",
						URLUtil.encodePath(path))), token);
		conn.setRequestMethod("PUT");
		conn.connect();
		resp = result(conn, true);
		conn.disconnect();

		return resp;
	}

	/*
	 * ========================================================================
	 * POST
	 * ========================================================================
	 */
	/**
	 * curl -i -X POST
	 * "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=APPEND[&buffersize=<INT>]"
	 * 
	 * @param path
	 * @param is
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws AuthenticationException
	 */
	public Map<String, Object> append(String path, InputStream is) throws MalformedURLException, IOException,
			AuthenticationException {
		Map<String, Object> resp = null;
		ensureValidToken();

		String redirectUrl = null;
		HttpURLConnection conn = authenticatedURL.openConnection(
				new URL(new URL(httpfsUrl), MessageFormat.format("/webhdfs/v1/{0}?op=APPEND", path)), token);
		conn.setRequestMethod("POST");
		conn.setInstanceFollowRedirects(false);
		conn.connect();
		logger.info("Location:" + conn.getHeaderField("Location"));
		resp = result(conn, true);
		if (conn.getResponseCode() == 307)
			redirectUrl = conn.getHeaderField("Location");
		conn.disconnect();

		if (redirectUrl != null) {
			conn = authenticatedURL.openConnection(new URL(redirectUrl), token);
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setUseCaches(false);
			conn.setRequestProperty("Content-Type", "application/octet-stream");
			// conn.setRequestProperty("Transfer-Encoding", "chunked");
			final int _SIZE = is.available();
			conn.setRequestProperty("Content-Length", "" + _SIZE);
			conn.setFixedLengthStreamingMode(_SIZE);
			conn.connect();
			OutputStream os = conn.getOutputStream();
			copy(is, os);
			// Util.copyStream(is, os);
			is.close();
			os.close();
			resp = result(conn, true);
			conn.disconnect();
		}

		return resp;
	}

	/*
	 * ========================================================================
	 * DELETE
	 * ========================================================================
	 */
	/**
	 * <b>DELETE</b>
	 * 
	 * curl -i -X DELETE "http://<host>:<port>/webhdfs/v1/<path>?op=DELETE
	 * [&recursive=<true|false>]"
	 * 
	 * @param path
	 * @return
	 * @throws AuthenticationException
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public Map<String, Object> delete(String path) throws MalformedURLException, IOException, AuthenticationException {
		Map<String, Object> resp = null;
		ensureValidToken();

		HttpURLConnection conn = authenticatedURL
				.openConnection(
						new URL(new URL(httpfsUrl), MessageFormat.format("/webhdfs/v1/{0}?op=DELETE",
								URLUtil.encodePath(path))), token);
		conn.setRequestMethod("DELETE");
		conn.setInstanceFollowRedirects(false);
		conn.connect();
		resp = result(conn, true);
		conn.disconnect();

		return resp;
	}
	
	/*
	 * ========================================================================
	 * DELETE
	 * ========================================================================
	 */
	/**
	 * <b>DELETE all sub directories</b>
	 * 
	 * curl -i -X DELETE "http://<host>:<port>/webhdfs/v1/<path>?op=DELETE&recursive=true"
	 * 
	 * @param path
	 * @return
	 * @throws AuthenticationException
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public Map<String, Object> deleteRecursive(String path) throws MalformedURLException, IOException, AuthenticationException {
		Map<String, Object> resp = null;
		ensureValidToken();

		//http://150.165.75.67:50070/webhdfs/v1/RootVeruska2/output1/?op=DELETE&recursive=true
		HttpURLConnection conn = authenticatedURL
				.openConnection(
						new URL(new URL(httpfsUrl), MessageFormat.format("/webhdfs/v1/{0}?op=DELETE&recursive=true",
								URLUtil.encodePath(path))), token);
		conn.setRequestMethod("DELETE");
		conn.setInstanceFollowRedirects(false);
		conn.connect();
		resp = result(conn, true);
		conn.disconnect();

		return resp;
	}

	// Begin Getter & Setter
	public String getHttpfsUrl() {
		return httpfsUrl;
	}

	public void setHttpfsUrl(String httpfsUrl) {
		this.httpfsUrl = httpfsUrl;
	}

	public String getPrincipal() {
		return principal;
	}

	public void setPrincipal(String principal) {
		this.principal = principal;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	// End Getter & Setter
}
