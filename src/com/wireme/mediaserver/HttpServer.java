package com.wireme.mediaserver;

import java.io.*;
import java.net.*;
import java.util.*;

import android.R.integer;
import android.util.Log;

public class HttpServer extends Thread {

	static final String HTML_START = "<html>"
			+ "<title>HTTP Server in java</title>" + "<body>";

	static final String HTML_END = "</body>" + "</html>";
	
	private static final String LOGTAG = "GNaP-HttpServer";
	
	private static final int BUFSIZE = 2 * 1024 * 1024;

	Socket connectedClient = null;
	BufferedReader inFromClient = null;
	DataOutputStream outToClient = null;

	public HttpServer(Socket client) {
		connectedClient = client;
	}

	public void run() {

		try {

			System.out.println("The Client " + connectedClient.getInetAddress()
					+ ":" + connectedClient.getPort() + " is connected");

			inFromClient = new BufferedReader(new InputStreamReader(
					connectedClient.getInputStream()));
			outToClient = new DataOutputStream(
					connectedClient.getOutputStream());

			String requestString = inFromClient.readLine();
			String headerLine = requestString;

			StringTokenizer tokenizer = new StringTokenizer(headerLine);
			String httpMethod = tokenizer.nextToken();
			String httpQueryString = tokenizer.nextToken();

			StringBuffer responseBuffer = new StringBuffer();
			responseBuffer
					.append("<b> This is the HTTP Server Home Page.... </b><BR>");
			responseBuffer.append("The HTTP Client request is ....<BR>");

			System.out.println("The HTTP request string is ....");
			while (inFromClient.ready()) {
				// Read the HTTP complete HTTP Query
				responseBuffer.append(requestString + "<BR>");
				System.out.println(requestString);
				requestString = inFromClient.readLine();
			}

			if (httpMethod.equals("GET")) {
				if (httpQueryString.equals("/")) {
					// The default home page
					sendResponse(200, responseBuffer.toString(), false);
				} else {
					// This is interpreted as a file name
					String id = httpQueryString.replaceFirst("/", "");
					id = URLDecoder.decode(id);

					if (ContentTree.hasNode(id)) {
						ContentNode node = ContentTree.getNode(id);
						if (node.isItem()) {
							String filePath = node.getFullPath();
							if (new File(filePath).isFile()) {
								Log.v(LOGTAG, "sending file " + filePath);
								sendResponse(200, filePath, true);
								return;
							}
						}
					}
				}
			}

			sendResponse(
					404,
					"<b>The Requested resource not found ...."
							+ "Usage: http://127.0.0.1:5000 or http://127.0.0.1:5000/</b>",
					false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendResponse(int statusCode, String responseString,
			boolean isFile) throws Exception {

		String statusLine = null;
		String serverdetails = "Server: Java HTTPServer";
		String contentLengthLine = null;
		String fileName = null;
		String contentTypeLine = "Content-Type: text/html" + "\r\n";
		FileInputStream fin = null;

		if (statusCode == 200)
			statusLine = "HTTP/1.1 200 OK" + "\r\n";
		else
			statusLine = "HTTP/1.1 404 Not Found" + "\r\n";

		if (isFile) {
			fileName = responseString;
			fin = new FileInputStream(fileName);
			contentLengthLine = "Content-Length: "
					+ Integer.toString(fin.available()) + "\r\n";
			if (!fileName.endsWith(".htm") && !fileName.endsWith(".html"))
				contentTypeLine = "Content-Type: \r\n";
		} else {
			responseString = HttpServer.HTML_START + responseString
					+ HttpServer.HTML_END;
			contentLengthLine = "Content-Length: " + responseString.length()
					+ "\r\n";
		}

		outToClient.writeBytes(statusLine);
		outToClient.writeBytes(serverdetails);
		outToClient.writeBytes(contentTypeLine);
		outToClient.writeBytes(contentLengthLine);
		outToClient.writeBytes("Connection: close\r\n");
		outToClient.writeBytes("\r\n");

		if (isFile)
			sendFile(fin, outToClient);
		else
			outToClient.writeBytes(responseString);

		outToClient.close();
	}

	public void sendFile(FileInputStream fin, DataOutputStream out)
			throws Exception {
		byte[] buffer = new byte[BUFSIZE];
		int bytesRead;

		Log.v(LOGTAG, "reading local file");
		while ((bytesRead = fin.read(buffer)) != -1) {
			Log.v(LOGTAG, "read " + bytesRead + " bytes");
			out.write(buffer, 0, bytesRead);
		}
		fin.close();
	}
}