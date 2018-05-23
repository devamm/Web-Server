import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.io.File;
import java.io.FileInputStream;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.io.InputStreamReader;

public class HTTP1ServerASP {

	public static void main(String[] args) {
		//get port number from command line argument & check validity
		if (args.length != 1){
			System.out.println("Invalid number of inputs");
			return;
		}
		int port;
		try {
			port = Integer.parseInt(args[0]);
		} catch (NumberFormatException e){
			System.out.println("Port number not a number");
			return;
		}
		if (port < 0 || port > 65535){
			System.out.println("Invalid port number");
		}
				
		//create server & listen for incoming requests
		//each request is handled by the creation of a socket & running service in thread
		ServerSocket listener = null;

		try{
			listener = new ServerSocket(port);
			System.out.println("Listening on port "+port);
					
			//loop to handle requests
			while(true){
				Socket cSocket = listener.accept();
				cSocket.setSoTimeout(3000);
				// spawn thread here
				ServiceThread request = new ServiceThread(cSocket);
				request.start();	
			}
		} catch (IOException e){
			System.out.println("Error encountered creating/closing socket");
		}

	}

}

class ServiceThread extends Thread{
	Socket socket;

	
	public ServiceThread(Socket s){
		socket = s;
		
	}

	public void run(){
		//run here
		BufferedReader in = null;
		PrintWriter out = null;
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream());
		} catch (IOException e){
			//something went wrong
			System.out.println("Error getting input from client");
			CloseConnections(socket,in,out);
			return;
		}
		
		try {
			//run service here
			System.out.println("new request accepted");
			//get input from socket
			
			String request = "";
			int msgSize = socket.getInputStream().available();
			if (msgSize > 0){
				char[] buf = new char[msgSize];
				in.read(buf);
				request = new String(buf);
			}
			//remove any new line if testing with AutoGrader
			if (request.endsWith("\r\n")){
				request = request.substring(0,request.length()-2);
			}
	 		

			//System.out.println(request);
			//System.out.println("-end-");
			//parse input from client socket & process
			ParseRequest(request, socket, out, in);
			
		} catch (SocketTimeoutException e){
			//catch timeout event and send back 408 error
			//System.out.println("timeout");
			out.println("HTTP/1.0 408 Request Timeout\r\n");
			out.flush();
			CloseConnections(socket,in,out); 
			return;
		} catch (IOException e){
			//something went wrong
			out.println("HTTP/1.0 500 Internal Server Error\r\n");
			out.flush();
			CloseConnections(socket,in,out);
			return;
		}
	}
	
public static void ParseRequest (String string, Socket cSocket, PrintWriter out, BufferedReader in ){
		//isolates first line to be parsed, remaining header lines to be used later
		String[] lines = string.split("\\r?\\n",-1);
		String s = lines[0]; 
		String split[] = s.split("\\s+");

		
		//System.out.println(string);
		

		if (split.length < 3){
			//checks for incorrect spacing or missing items from request
			out.println("HTTP/1.0 400 Bad Request\r\n");
			out.flush();
			CloseConnections(cSocket,in,out);
			return;
		}
		
		String com = split[0];		//isolated command of request 
		String rec = split[1];		//isolated resource requested
		String ver = split[2];		//isolated HTTP Version number
		
		if ( com.length() <= 1 || rec.length() <=1 || !rec.startsWith("/") || ver.length() < 8){
			out.println("HTTP/1.0 400 Bad Request\r\n");
			out.flush();
			CloseConnections(cSocket,in,out);
			return;
		}

		//check if command is properly formatted
		for (int i = 0; i < com.length(); i++){
			if (Character.isLowerCase(com.charAt(i)) || !Character.isLetter(com.charAt(i))){
				out.println("HTTP/1.0 400 Bad Request\r\n");
				out.flush();
				CloseConnections(cSocket,in,out);
				return;
			}
		}

		if (ver.substring(0, 5).equals("HTTP/")){
			//check if HTTP version is a number, and if server can implement version #
			try {
				 float version = Float.parseFloat(ver.substring(5));
				 if (version > 1.2){
					 out.println("HTTP/1.0 505 HTTP Version Not Supported\r\n");
					 out.flush();
					 CloseConnections(cSocket,in,out);
					 return;
				 }
			} catch (NumberFormatException e){
				//HTTP version not a number, invalid request
				out.println("HTTP/1.0 400 Bad Request\r\n");
				out.flush();
				CloseConnections(cSocket,in,out);
				return;
			}
		} else {
			//improper format 
			out.println("HTTP/1.0 400 Bad Request\r\n");
			out.flush();
			CloseConnections(cSocket,in,out);
			return;
		}

		
		// parse header section
		HashMap<String,String> map = new HashMap<String,String>();
		//map.put("REQUEST_METHOD",com);
		
		Boolean err = parseHeader(lines,com,map,cSocket,out,in);
		if (err == true){
			out.println("HTTP/1.0 500 Internal Server Error\r\n");
			out.flush();
			CloseConnections(cSocket,in,out);
			return;
		}

		String url = "";
		//get payload from request
		
		boolean add = false;
		if (com.equals("POST")){
			//finds blank line indicating end of header, add all contents after line
			int n = string.indexOf("\r\n\r\n");
			url = string.substring(n+4);
		}
		 

		
		//System.out.println("Payload:\n"+url);
		//request is correctly formatted
		switch (com){
		case "POST":
			PostReq(split,map,url,cSocket,in,out);
			break;
		case "GET":
			// perform POST if GET is requested on a cgi file 
			if (rec.endsWith(".cgi")){
				PostReq(split,map,url,cSocket,in,out);
				break;
			}
		case "HEAD":
			ResourceOps(split,map,cSocket,out,in);
			break;
		case "DELETE":
		case "PUT":
		case "LINK":
		case "UNLINK":
			out.println("HTTP/1.0 501 Not Implemented\r\n");

			out.flush();
			CloseConnections(cSocket,in,out);
			break;
		default:
			out.println("HTTP/1.0 400 Bad Request\r\n");
			//System.out.println("HTTP/1.0 501 Not Implemented\r\n");
			out.flush();
			CloseConnections(cSocket,in,out);
			break;
		}
		
		return;
	}

public static void PostReq(String[] split, HashMap<String,String> map, String url, Socket cSocket, BufferedReader in, PrintWriter out){
	//this method runs the cgi script

	//check if script exists
	String path = split[1];
	path = path.substring(1);
	//System.out.println("path: "+path);
	if(!Files.exists(Paths.get(path))){
		out.println("HTTP/1.0 404 Not Found\r\n");
		System.out.println("404 Not Found\r\n");
		out.flush();
		CloseConnections(cSocket,in,out);
		return;
	}
	//check if header exists
	if(split[0].equals("POST")){
		//bypass this check is GET is requested on a cgi script
		String length = map.get("CONTENT_LENGTH");
		//System.out.println("length:"+length);
		if (length == null || length.equals("")){
			out.println("HTTP/1.0 411 Length Required\r\n");
			System.out.println("411 Length Req\r\n");
			out.flush();
			CloseConnections(cSocket,in,out);
			return;
		}
		//check if content length is a number
		length = length.trim();
		for (int x = 0; x < length.length(); x++){
			char c = length.charAt(x);
			if (!Character.isDigit(c)){
				out.println("HTTP/1.0 411 Length Required\r\n");
				System.out.println("411 Length Required\r\n");
				out.flush();
				CloseConnections(cSocket,in,out);
				return;
			} else {
				//do nothing
			}
		}
	}
		
	//check if script is a cgi script
	String rec = split[1];
	//rec = rec.trim();
	//System.out.println(rec);
	if(!rec.endsWith(".cgi")){
		out.println("HTTP/1.0 405 Method Not Allowed\r\n");
		out.flush();
		CloseConnections(cSocket,in,out);
		return;
	}
	//check if executable
	File file = new File(path);
	if (!file.canExecute()){
		out.println("HTTP/1.0 403 Forbidden\r\n");
		//System.out.println("403 Forbiden\r\n");
		out.flush();
		CloseConnections(cSocket,in,out);
		return;
	}
	
	//parse URL
	try {
	//System.out.println("URL: "+url);
	String decoded = url;
	if(!path.contains("service.cgi")){
		//decode URL for all scripts except service.cgi, which needs unaltered payload
		decoded = URLDecoder.decode(url, "UTF-8");
	} else {
		//do nothing
	}
	//run script


		ProcessBuilder scriptExec = new ProcessBuilder("./"+path);
		Map<String,String> eMap = scriptExec.environment();
		String setCookie = "";
		
		if (path.contains("store.cgi")){
			//manage cookies
			if(url.contains("user=")){
				//payload contains login info
				String[] cookies = url.split("&");
				cookies[0] = URLDecoder.decode(cookies[0], "UTF-8");
				//gets time 3 mins from now
				String cExpr = getDate(System.currentTimeMillis()+180000);

				setCookie = "Set-Cookie: "+cookies[0]+"; Expires="+cExpr+"\r\n";
				//setCookie+="Set-Cookie: cart=\r\n";
			}
			
			if(url.contains("add=")){
				url=url.substring(4);
				setCookie ="Set-Cookie: "+url+"="+url+"\r\n";
			}
			
			
		}
		
		eMap.put("REQUEST_METHOD", split[0]);
		if (split[0].equals("POST")){
			try {
				eMap.put("CONTENT_LENGTH", String.valueOf(decoded.length()));
				eMap.put("HTTP_COOKIE", map.get("HTTP_COOKIE"));
				eMap.put("SCRIPT_NAME", "/"+path);
				eMap.put("HTTP_FROM", map.get("HTTP_FROM"));
				eMap.put("HTTP_USER_AGENT", map.get("HTTP_USER_AGENT"));
				eMap.put("SERVER NAME", "127.0.0.1");

				eMap.put("SERVER PORT", String.valueOf(cSocket.getPort()));
			} catch (NullPointerException e){
				//do nothing
			}
		}
		//eMap.put("Payload", "name=GOOG&price=$632.74&CEO=Larry+Page&email=larry@google.com");
		System.out.println("running script: "+path);
		//System.out.println("Passing into script:\n"+decoded);
		Process scriptRunner = scriptExec.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(scriptRunner.getInputStream()));
		OutputStream stdin = scriptRunner.getOutputStream();
		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(stdin));
		w.write(decoded);
		w.flush();

	//capture output
		String s = "";
		String payload = "";
		while( (s = reader.readLine()) != null ) {
			s+="\n";
			payload+=s;
		}
		//checks if CGI script returned payload
		if (payload.equals("")){
			out.println("HTTP/1.0 204 No Content\r\n");
			System.out.println("204 No Content\r\n");
			out.flush();
			CloseConnections(cSocket,in,out);
			return;
		}

		//removes trailing \n character from payload
		payload = payload.substring(0, payload.length()-1);
		String len = String.valueOf(payload.length());
		scriptRunner.waitFor();
		reader.close();
	
		//return output to client
		String header = "HTTP/1.0 200 OK\r\nContent-Type: text/html\r\nContent-Encoding: identity\r\nAllow: GET, POST, HEAD\r\n";
		header+=setCookie;
		
		String expr = "Expires: "+getDate(System.currentTimeMillis()+(24L*3600*1000));
		out.write(header+expr+"\r\n"+"Content-Length: "+len+"\r\n\r\n"+payload);
		out.flush();
		System.out.println("ran script");
		CloseConnections(cSocket,in,out);

	} catch (Exception e){
		out.println("HTTP/1.0 500 Internal Error\r\n");
		System.out.println("error encountered");
		out.flush();
		CloseConnections(cSocket,in,out);
		return;
	}
}


public static boolean parseHeader(String[] header, String com, HashMap<String, String> eMap, Socket cSocket, PrintWriter out, BufferedReader in){
		//parses header and stores into Map for easier retrival later on
		//stores environment variables if com = "POST", stores date if GET request
		//returns true if a 500 error needs to be sent

		if (header.length <= 1){
			if (com.equals("POST")){
				//does not contain content-type header
				return true;
			}
			return false;
		}

		if (com.equals("HEAD")){
			//no need for header in HEAD request
			return false;
		} else if(com.equals("POST")) {
			boolean quit = true;
			//takes values from header to be used as environment variables later
			for (int i = 1; i < header.length; i++){
				//System.out.println("checking current header line:"+header[i]);
				if (header[i].contains("Content-Type: application/x-www-form-urlencoded") || 
					header[i].contains("Content-Type: multipart/form-data")){
					quit = false;
					//System.out.println("Found");
				}

				if (header[i].startsWith("Content-Length:")){
					int x = header[i].indexOf(":");
					String var = header[i].substring(x+2);
					eMap.put("CONTENT_LENGTH", var);
				} else if (header[i].startsWith("From:")){
					int x = header[i].indexOf(":");
					String var = header[i].substring(x+2);
					eMap.put("HTTP_FROM", var);
				} else if (header[i].startsWith("User-Agent:")){
					int x = header[i].indexOf(":");
					String var = header[i].substring(x+2);
					eMap.put("HTTP_USER_AGENT", var);
				} else if (header[i].startsWith("Cookie:")) {
					int x = header[i].indexOf(":");
					String var = header[i].substring(x+2);
					eMap.put("HTTP_COOKIE", var);
					//System.out.println("Put in cookies!")
				} else {
					//do nothing
				}
			}
			//variables stored in map, 
			if (quit){
				//does not contain content-type header
				return true; 
			}
			return false;

		} else if(com.equals("GET")){
			//search header lines for "If-Modified since"
			//make sure date is in right format 
			String date = null;
			for (int i = 1; i < header.length; i++){
				if(header[i].contains("If-Modified-Since:")){
					int x = header[i].indexOf("If-Modified-Since:");
					if (x >= 0){
						x+=19;
						int end = header[i].indexOf("GMT",x); //check if end of date is in proper format
						if (end < 0){
							//date not in proper format 
							break;
						} else {
							end+=3;
							date = header[i].substring(x, end);
							break;
						}
		
					}
				}
			}
			eMap.put("date",date);
			//System.out.println("Date after parsing: "+date);

		} else {
			//command is not implemented
			return false;
		}
		//System.out.println("header parsed");
		return false;
	}

public static void ResourceOps(String[] split,HashMap<String,String> map,Socket cSocket, PrintWriter out, BufferedReader in){
	//this method performs GET/HEAD operations on a given resource
	//takes in string array containing command, resource, & http version
	//takes in date/ environments from header if it exists (contained in HashMap)
	//System.out.println("performing operation on resource");
	String com = split[0];
	String path = split[1];
	//System.out.println("Resource requested: "+path);
	path = path.substring(1);
	//check if file exists first
	if(!Files.exists(Paths.get(path)) ){
		out.println("HTTP/1.0 404 Not Found\r\n");
		out.flush();
		CloseConnections(cSocket,in,out);
		return;
	}
	//check if file is accessible 
	File file = new File(path); //file object that points to resource from request

	try {	
		if(!file.canRead()){
			out.println("HTTP/1.0 403 Forbidden\r\n");
			out.flush();
			CloseConnections(cSocket,in,out);
			return;
		}
	} catch (SecurityException e){
		out.println("HTTP/1.0 500 Internal Server Error\r\n");
		out.flush();
		CloseConnections(cSocket,in,out);
		return;
	}

		
		//create proper response header first
		String date; 
		date = map.get("date"); //date from header stored in hashmap
		//System.out.println("date: "+date);
		if (date != null && !com.equals("HEAD")){
			//request has a header containing "if-modified-since", compare date from header to date file was modified 
			// parse time to time from epoch
			SimpleDateFormat f = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
			java.util.Date time = null;
			try {
				time = f.parse(date);
			} catch (Exception e){
				//error encountered parsing date
				out.println("HTTP/1.0 500 Internal Server Error\r\n");
				//System.out.println("date error");
				out.flush();
				CloseConnections(cSocket,in,out);
				return;
			}
			long timeReq = time.getTime();
			//compare time in request to time file was last modified
			long lastMod = file.lastModified();
			//System.out.println("Comparing "+lastMod+" with "+timeReq);
			if (lastMod < timeReq){	
				//get current time & add 24hrs (expressed as millisec), convert to string 
				String expr = "Expires: "+getDate(System.currentTimeMillis()+(24L*3600*1000));
				out.println("HTTP/1.0 304 Not Modified\r\n"+expr+"\r\n");
			
				out.flush();
				CloseConnections(cSocket,in,out);
				return;
			}
		}

		//System.out.println("!");
		//file, exists, can be read, passes last modified test (or no header), send 200 OK to client
		//get all information for headers first:
		String allow = "Allow: GET, POST, HEAD";
		String mod = "Last-Modified: "+getDate(file.lastModified());
		String length = "Content-Length: "+String.valueOf(file.length());
		String encoding = "Content-Encoding: identity";
		String type = MrMime(path);
		//get current time as millis since epoch and add 24 hours 
		//parse time to proper format and send to client as expires time 
		String expires ="Expires: "+ getDate(System.currentTimeMillis()+( 24L*3600*1000));
		//"Expires: Tue, 15 Aug 2017 14:03:00 GMT";
		String header = allow+"\r\n"+mod+"\r\n"+length+"\r\n"+encoding+"\r\n"+type+"\r\n"+expires+"\r\n";
		String payload;
		if (com.equals("HEAD")){
			//client only requests header
			out.println("HTTP/1.0 200 OK\r\n"+header);
			out.flush();
			CloseConnections(cSocket,in,out);
			return;
		} else {
			//send payload to client 
			sendPayload(header,type,file,path,cSocket,in,out);
			CloseConnections(cSocket,in,out);
			return;
		}
	
}


public static void sendPayload (String header, String type, File file, String path, Socket cSocket, BufferedReader in, PrintWriter out){
	//given a file and its mime type, this method sends file data to the client
	//System.out.println("Sending payload");
	if (type.equals("Content-Type: text/html")){
		//file is txt readable, return as string
		String payload;
		try {
			payload = new String(Files.readAllBytes(Paths.get(path)));
			out.println("HTTP/1.0 200 OK\r\n"+header+"\r\n"+payload+"\r\n");
			out.flush();
			return;

		} catch (IOException e){
			out.println("HTTP/1.0 500 Internal Server Error\r\n");
			out.flush();
			return;
		}
	} else {
		try {
			//send byte[] to client
			int len = (int)file.length();
			byte[] fileData = new byte[len];
			fileData = Files.readAllBytes(Paths.get(path));
			//send header
			out.println("HTTP/1.0 200 OK\r\n"+header);
			out.flush();

			// send payload
			DataOutputStream dOut = new DataOutputStream(cSocket.getOutputStream());
			dOut.write(fileData,0,len);
			dOut.flush();
			dOut.close();
		} catch (IOException e){
			//something went wrong
			out.println("HTTP/1.0 500 Internal Server Error\r\n");
			out.flush();
		}
		return;
	}
}


public static String getDate(long time){
	//converts millisec from epoch into a string with dd MMM yyyy time format 
	Date date = new Date(time);
	SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
	format.setTimeZone(TimeZone.getTimeZone("GMT"));
	//System.out.println(format.format(date));
	return (format.format(date));
}

public static String MrMime(String pathName){
	//strips file extension off string containing path for resource
	//returns the respective mime type
	int x = pathName.length();
	x--;
	for (int i =x; i > 0; i--){
		//starts at end of string and works backwards until a "." is encountered
		if (pathName.charAt(i) == '.'){
			x = i;
			break;
		}
	}
	String mime = "";
	if (x != 0){
		mime = pathName.substring(x+1);
	}
	//System.out.println("Evaluating: "+mime);
	switch (mime){
		case "html":
		case "txt":
			return "Content-Type: text/html";
		case "gif":
		case "jpeg":
		case "png":
			return "Content-Type: image/png";
		case "pdf":
			return "Content-Type: application/pdf";
		case "zip":
			return "Content-Type: application/zip";
		case "x-gzip":
		default:
			return "Content-Type: application/octet-stream";
	}
}

public static void CloseConnections(Socket socket, BufferedReader in, PrintWriter out){
	//closes client socket and readers/writers
	try {
		//pauses thread for a quarter second after flushing PrintWriter
		Thread.sleep(250);

	} catch(InterruptedException e){
		System.out.println("Thread Error");
	}

	System.out.println("Completed request, closing socket");
	// closes reader, writer, and socket
	try{
		if (in != null && out != null){
			in.close();
			out.close();
		}
		socket.close();
	} catch (IOException e){
		System.out.println("Error closing connection");
	}
	return;
}
	

}