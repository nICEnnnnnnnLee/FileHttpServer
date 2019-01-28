package nicelee.http.core.runnable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nicelee.http.core.transfer.HttpDataTransfer;
import nicelee.http.core.transfer.HttpHeaderTransfer;
import nicelee.http.core.transfer.util.StreamReader;
import nicelee.http.model.HttpRequest;
import nicelee.http.model.HttpResponse;

public class SocketDealer implements Runnable {
	final static Pattern patternFileRange = Pattern.compile("^bytes=([0-9]+)-([0-9]*)$");
	final static Pattern patternURL = Pattern.compile("^/([^?^#]*)#?[^#]*\\??[^?^#]*$");
	final static Pattern patternParent = Pattern.compile("^(/.*)/[^/]+/$|^(/)[^/]+/$");
	final static Pattern patternSessionid = Pattern
			.compile("jsessionid *[=|:]{1} *YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo");
	final static SimpleDateFormat aDateFormat = new SimpleDateFormat("yyyy/MM/dd  HH:mm:ss");
	final static byte[] BREAK_LINE = "\r\n".getBytes();
	final static byte[] PAGE_401 = "<html><head><title>Error</title></head><body><center><h1>401 Authorization Required</h1></center><hr><center>Copyright @Nicelee.top</center></body></html>"
			.getBytes();
	final static byte[] PAGE_403 = "<html><head><title>Error</title></head><body><center><h1>403 Forbidden</h1></center><hr><center>Copyright @Nicelee.top</center></body></html>"
			.getBytes();
	final static byte[] PAGE_404 = "<html><head><title>Error</title></head><body><center><h1>404 Not Found</h1></center><hr><center>Copyright @Nicelee.top</center></body></html>"
			.getBytes();
	final static char[] WHITE_SPACE = "                                                ".toCharArray();
	Socket socketClient;
	File srcFolder;
	SocketMonitor monitor;

	StreamReader streamReader = new StreamReader();
	ByteBuffer byteBuffer = ByteBuffer.allocate(2560);

	public SocketDealer(Socket socketClient, SocketMonitor monitor, String source) {
		this.socketClient = socketClient;
		this.srcFolder = new File(source);
		this.monitor = monitor;
	}

	@Override
	public void run() {
		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		String url = "";
		try {
			in = new BufferedInputStream(socketClient.getInputStream());
			out = new BufferedOutputStream(socketClient.getOutputStream());
			// writer = new BufferedWriter(new
			// OutputStreamWriter(socketClient.getOutputStream()));
			HttpRequest httpRequest;
			while ((httpRequest = getHttpRequestStructrue(in)) != null) {
				url = httpRequest.url;
				// TODO do something with the httpRequest. Put 'X-forward...', for example.

				// TODO do URLFilter

				// TODO give the client a response, proxy or http
				doResponse(httpRequest, in, out);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (IndexOutOfBoundsException e1) {
			e1.printStackTrace();
		} finally {
			System.out.println(url + " -线程结束...");
			try {
				in.close();
			} catch (Exception e) {
			}
			try {
				out.close();
			} catch (Exception e) {
			}
			try {
				socketClient.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * 返回HttpRequest结构, 不符合协议标准将会抛出异常
	 * 
	 * @param reader return
	 * @throws IOException
	 */
	private HttpRequest getHttpRequestStructrue(BufferedInputStream in)
			throws NullPointerException, IOException, IndexOutOfBoundsException {
		// refresh the monitor
		monitor.put(socketClient);
		// System.out.println("Headers 提取中... 可能会阻塞或抛出异常...");
		HttpRequest httpRequest = new HttpRequest();

		// 第一行
		String firstLine = streamReader.readLineFromInputStream(byteBuffer, in);
		System.out.println(firstLine);
		String firstLines[] = firstLine.split(" ");
		httpRequest.method = firstLines[0];
		httpRequest.url = firstLines[1];
		httpRequest.version = firstLines[2];
		// 第一行结束

		// 获取其他属性
		String key_value = streamReader.readLineFromInputStream(byteBuffer, in);
		while (key_value != null && key_value.length() > 0) {
			// System.out.println(key_value);
			// System.out.println("获取数据中...");
			String[] objs = key_value.split(":");
			objs[0] = objs[0].toLowerCase().trim();
			objs[1] = objs[1].trim();
			// 获取目的host
			if (objs[0].toLowerCase().startsWith("host")) {
				httpRequest.host = objs[1];
			}
			// 判断是否有数据
			if (objs[0].toLowerCase().startsWith("content-length")) {
				httpRequest.dataLength = Integer.parseInt(objs[1]);
			}
			httpRequest.headers.put(objs[0], objs[1]);
			key_value = streamReader.readLineFromInputStream(byteBuffer, in);
		}
		// System.out.println("获取data数据中...");
		// 获取post数据
		if (httpRequest.dataLength > 0) {
			httpRequest.data = new byte[httpRequest.dataLength];
			int pos = byteBuffer.position();
			if (httpRequest.dataLength > pos + 1) {
				System.arraycopy(byteBuffer.array(), 0, httpRequest.data, 0, pos + 1);
				in.read(httpRequest.data, pos + 1,
						httpRequest.dataLength - pos - 1);
			} else {
				System.arraycopy(byteBuffer.array(), 0, httpRequest.data, 0, httpRequest.dataLength);
				byteBuffer.position(0);
				byteBuffer.put(byteBuffer.array(), httpRequest.dataLength, pos - httpRequest.dataLength + 1);
			}

		}
		// System.out.println("获取httpRequest完毕...");

		// 内容传输不计入时间, 则从监控队列删除
		// monitor.remove(socketClient);
		return httpRequest;
	}

	/**
	 * 根据请求返回
	 * 
	 * @param httpRequest
	 * @param reader
	 * @param writer
	 */
	private void doResponse(HttpRequest httpRequest, BufferedInputStream in, BufferedOutputStream out)
			throws IOException {
		httpRequest.print();
		HttpResponse httpResponse = new HttpResponse();
		// 盘点是否禁止访问/未授权访问
		/**
		 * 未授权检验 这里以admin:admin为例, 固定生成jsessionid=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo
		 * 路径为/source
		 */
		if (httpRequest.url.startsWith("/source")) {
			// System.out.println("访问权限目录...");
			// 如果jsessionid正确, 跳过认证
			// boolean sessionCorrect = false;
			String cookies = httpRequest.headers.get("cookie");
			if (cookies == null || !patternSessionid.matcher(cookies).find()) {
				// System.out.println("未找到匹配session");
				String auth = httpRequest.headers.get("authorization");
				httpRequest.print();
				// 这里用contains不对, 仅作示范用, 表示鉴权通过
				if (auth != null && auth.contains("YWRtaW46YWRtaW4")) {
					// System.out.println("未找到匹配session,但是鉴权通过");
					httpRequest.headers.put("Set-cookie",
							"jsessionid=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo;path=/;httponly");
				} else {
					// System.out.println("未找到匹配session,且鉴权未通过");
					doResponseWithFileNotAuth(httpResponse, out);
					return;
				}
				// System.out.println("存在匹配session");
			}
		}
		/**
		 * 禁止访问检验 路径为*.txt
		 */
		if (httpRequest.url.endsWith(".txt")) {
			doResponseWithFileForbidden(httpResponse, out);
			return;
		}
		if (isPathExists(httpRequest)) {
			// System.out.println("URL Resouce is at: " + httpRequest.url);
			File file = new File(srcFolder, httpRequest.url);
			if (file.isDirectory()) {
				doResponseWithFolderOK(file, httpRequest, httpResponse, out);
			} else {
				doResponseWithFileOK(file, httpRequest, httpResponse, out);
			}
		} else {
			doResponseWithFileNotFound(httpResponse, out);
		}
	}

	/**
	 * 若返回true, 则httpRequest.url已经做了修改,且已经变成了绝对路径
	 * 
	 * @param httpRequest
	 * @return
	 * @throws IOException
	 */
	boolean isPathExists(HttpRequest httpRequest) throws IOException {
		// 去掉锚# 和参数? , 获取path
		String path = httpRequest.url;
		Matcher matcher = patternURL.matcher(path);
		if (matcher.find()) {
			path = matcher.group(1);
		} else {
			System.out.println("path路径不匹配");
			return false;
		}

		/**
		 * 优先顺序, 0. 文件存在 1. 不做任何修饰, 文件存在 2. 文件夹下, index.html/index.htm存在 3.
		 * path加上.html后缀,文件存在
		 */
		File file = new File(srcFolder, path);
		if (file.exists() && file.isDirectory()) {
			return true;
		}
		if (file.exists()) {
			// 匹配 1.
			if (file.isFile()) {
				return true;
			}
		} else {
			// 匹配 3.
			String[] suffixs = { ".html", ".htm" };
			for (String suffix : suffixs) {
				File filDst = new File(file.getParent(), file.getName() + suffix);
				if (filDst.exists()) {
					httpRequest.url += suffix;
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 若URL对应的文件不允许访问, 使用该方法返回
	 * 
	 * @param httpResponse
	 * @param writer
	 * @throws IOException
	 */
	private void doResponseWithFileForbidden(HttpResponse httpResponse, BufferedOutputStream out) throws IOException {
		HttpHeaderTransfer headerTrans = new HttpHeaderTransfer();

		// 403
		httpResponse.do403();
		httpResponse.dataLength = PAGE_403.length;
		headerTrans.transferCommonHeader(httpResponse, out);

		// out date-length & data
		out.write("Content-Length: ".getBytes());
		out.write(("" + httpResponse.dataLength).getBytes());
		out.write(BREAK_LINE);
		out.write(BREAK_LINE);
		out.write(PAGE_403);
		out.flush();
	}

	/**
	 * 若URL对应的文件未经授权认证, 使用该方法返回
	 * 
	 * @param httpResponse
	 * @param writer
	 * @throws IOException
	 */
	private void doResponseWithFileNotAuth(HttpResponse httpResponse, BufferedOutputStream out) throws IOException {
		HttpHeaderTransfer headerTrans = new HttpHeaderTransfer();

		// 401
		httpResponse.do401();
		httpResponse.dataLength = PAGE_401.length;
		httpResponse.headers.put("WWW-Authenticate", "Basic realm=\"NiceLee's Site\"");
		headerTrans.transferCommonHeader(httpResponse, out);

		// out date-length & data
		out.write("Content-Length: ".getBytes());
		out.write(("" + httpResponse.dataLength).getBytes());
		out.write(BREAK_LINE);
		out.write(BREAK_LINE);
		out.write(PAGE_401);
		out.flush();
	}

	/**
	 * 若URL对应的文件不存在, 使用该方法返回
	 * 
	 * @param httpResponse
	 * @param writer
	 * @throws IOException
	 */
	private void doResponseWithFileNotFound(HttpResponse httpResponse, BufferedOutputStream out) throws IOException {
		HttpHeaderTransfer headerTrans = new HttpHeaderTransfer();

		// 404
		httpResponse.do404();
		httpResponse.dataLength = PAGE_404.length;
		headerTrans.transferCommonHeader(httpResponse, out);

		// out date-length & data
		out.write("Content-Length: ".getBytes());
		out.write(("" + httpResponse.dataLength).getBytes());
		out.write(BREAK_LINE);
		out.write(BREAK_LINE);
		out.write(PAGE_404);
		out.flush();
	}

	/**
	 * 若文件夹解析成功, 按此返回目录
	 * 
	 * @param fileFolder
	 * @param httpRequest
	 * @param httpResponse
	 * @param out
	 * @throws IOException
	 */
	public void doResponseWithFolderOK(File fileFolder, HttpRequest httpRequest, HttpResponse httpResponse,
			BufferedOutputStream out) throws IOException {
		if (!httpRequest.url.endsWith("/")) {
			httpRequest.url += "/";
		}
		// 200
		HttpHeaderTransfer headerTrans = new HttpHeaderTransfer();
		httpResponse.do200();
		headerTrans.transferCommonHeader(httpResponse, out);

		// out date-length & data
		out.write("Transfer-Encoding: chunked".getBytes());
		out.write(BREAK_LINE);
		out.write(BREAK_LINE);
		out.flush();

		byte[] head = ("<html><head><meta http-equiv=\"content-type\" content=\"text/html;charset=utf-8;\"/><title>Nicelee.top提供</title></head><body>")
				.getBytes();
		out.write(String.format("%x", head.length).getBytes());
		out.write(BREAK_LINE);
		out.write(head);
		out.write(BREAK_LINE);

		// System.out.println("当前url 为: " +httpRequest.url);
		byte[] title = String.format("<h1>Index Of %s</h1><hr><pre>", httpRequest.url).getBytes();
		out.write(String.format("%x", title.length).getBytes());
		out.write(BREAK_LINE);
		out.write(title);
		out.write(BREAK_LINE);

		StringBuilder sb = new StringBuilder();
		// 列出父级目录
		Matcher matcher = patternParent.matcher(httpRequest.url);
		if (matcher.find()) {
			String parent = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
			sb.append("<a href=\"").append(parent).append("\">../</a><br>");

		}
		// 列出文件夹
		for (File childFolder : fileFolder.listFiles()) {
			if (childFolder.isDirectory()) {
				sb.append("<a href=\"").append(httpRequest.url + childFolder.getName()).append("\">")
						.append(childFolder.getName()).append("/</a><br>");

			}
		}
		// 列出文件
		for (File childFile : fileFolder.listFiles()) {
			if (childFile.isFile()) {
				String fSize = String.valueOf(childFile.length());
				sb.append("<a href=\"").append(httpRequest.url + childFile.getName()).append("\">")
						.append(childFile.getName()).append("</a>")
						.append(WHITE_SPACE, 0, WHITE_SPACE.length - childFile.getName().length())
						.append(aDateFormat.format(childFile.lastModified()))
						.append(WHITE_SPACE, 0, WHITE_SPACE.length - fSize.length() - 30).append(fSize).append("<br>");
			}
		}
		sb.append("</pre><hr></body></html>");
		out.write(String.format("%x", sb.length()).getBytes());
		out.write(BREAK_LINE);
		out.write(sb.toString().getBytes());
		out.write(BREAK_LINE);

		out.write(48);
		out.write(BREAK_LINE);
		out.write(BREAK_LINE);
		out.write(BREAK_LINE);
		out.flush();
	}

	/**
	 * 若URL对应的文件存在, 使用该方法返回
	 * 
	 * @param file
	 * @param httpResponse
	 * @param writer
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	private void doResponseWithFileOK(File file, HttpRequest httpRequest, HttpResponse httpResponse,
			BufferedOutputStream out) throws FileNotFoundException, UnsupportedEncodingException, IOException {
		httpResponse.do200();
		httpResponse.dataLength = (int) file.length();
		HttpHeaderTransfer headerTrans = new HttpHeaderTransfer();

		String fName = file.getName().toLowerCase();
		headerTrans.setContentType(httpResponse, fName);

		// out date-length & data
		HttpDataTransfer dataTrans = new HttpDataTransfer();
		// decide file begin and end if range acquired
		String range;
		if ((range = httpRequest.headers.get("range")) != null) {
			// System.out.println("Range Required: " +range);
			Matcher matcher = patternFileRange.matcher(range);
			if (matcher.find()) {
				long begin = Long.parseLong(matcher.group(1));
				long end = file.length() - 1;
				try {
					end = Long.parseLong(matcher.group(2));
					end = end < (file.length() - 1) ? end : (file.length() - 1);
				} catch (Exception e) {
				}
				if (begin > 0) {
					System.out.println("206");
					httpResponse.do206();
				}
				headerTrans.transferCommonHeader(httpResponse, out);
				dataTrans.transferFileWithRange(begin, end, out, file);
			} else {
				headerTrans.transferCommonHeader(httpResponse, out);
				dataTrans.transferFileCommon(out, file);
			}
		} else {
			headerTrans.transferCommonHeader(httpResponse, out);
			dataTrans.transferFileCommon(out, file);
		}
		out.flush();
	}

}
