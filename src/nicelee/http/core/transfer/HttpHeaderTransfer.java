package nicelee.http.core.transfer;

import java.io.IOException;
import java.io.BufferedOutputStream;
import java.util.Date;
import java.util.Map.Entry;

import nicelee.http.model.HttpResponse;

public class HttpHeaderTransfer {
	final static byte[] BREAK_LINE = "\r\n".getBytes();
	
	/**
	 * 设置内容类型
	 * @param httpResponse
	 * @param fName
	 */
	public void setContentType(HttpResponse httpResponse, String fName) {
		if(fName.endsWith(".html") || fName.endsWith(".htm") || fName.endsWith(".txt") || fName.endsWith(".md")) {
			httpResponse.contentType = HttpResponse.HTTP_CONTENT_TYPE_HTML+"; charset=UTF-8";
		}else if(fName.endsWith(".js")) {
			httpResponse.contentType = HttpResponse.HTTP_CONTENT_TYPE_JS;
		}else if(fName.endsWith(".css")) {
			httpResponse.contentType = HttpResponse.HTTP_CONTENT_TYPE_CSS;
		}else if(fName.endsWith(".json")) {
			httpResponse.contentType = HttpResponse.HTTP_CONTENT_TYPE_JSON;
		}else if(fName.endsWith(".jpg") || fName.endsWith(".png")) {
			httpResponse.contentType = HttpResponse.HTTP_CONTENT_TYPE_JPG;
		}else if(fName.endsWith(".mp3")) {
			httpResponse.contentType = HttpResponse.HTTP_CONTENT_TYPE_MP3;
		}else if(fName.endsWith(".mp4")) {
			httpResponse.contentType = HttpResponse.HTTP_CONTENT_TYPE_MP4;
			httpResponse.headers.put("Accept-Ranges", "bytes");
		}else if(fName.endsWith(".ico")) {
			httpResponse.contentType = HttpResponse.HTTP_CONTENT_TYPE_ICO;
		}else{
			httpResponse.contentType = HttpResponse.HTTP_CONTENT_TYPE_FILE;
			httpResponse.headers.put("Accept-Ranges", "bytes");
		}
	}
	/**
	 * 
	 * @param httpResponse
	 * @param writer
	 * @throws IOException
	 */
	public void transferCommonHeader(HttpResponse httpResponse, BufferedOutputStream out) throws IOException {
		httpResponse.date = new Date().toGMTString();
		httpResponse.print();
		//System.out.println("doResponse start~");
		
		String line = String.format("%s %d %s", httpResponse.version, httpResponse.status, httpResponse.statusDescript);
		out.write(line.getBytes());
		out.write(BREAK_LINE);
		out.write("Connection:keep-alive".getBytes());
		out.write(BREAK_LINE);
		out.write("Date: ".getBytes());
		out.write(httpResponse.date.getBytes());
		out.write(BREAK_LINE);
		out.write("Content-Type: ".getBytes());
		out.write(httpResponse.contentType.getBytes());
		out.write(BREAK_LINE);
		for(Entry<String, String> entry : httpResponse.headers.entrySet()) {
			out.write((entry.getKey() + ": " + entry.getValue()).getBytes());
			out.write(BREAK_LINE);
		}
		out.flush();
	}
}
