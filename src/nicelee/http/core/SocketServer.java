package nicelee.http.core;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nicelee.http.core.runnable.SocketDealer;

public class SocketServer {
	
	boolean isRun = true;
	int portServerListening;
	ExecutorService httpThreadPool;
	String sourceLocation;
	
	public SocketServer(int portServerListening, int threadPoolSize, String sourceLocation) {
		this.portServerListening = portServerListening;
		httpThreadPool = Executors.newFixedThreadPool(threadPoolSize);
		this.sourceLocation = sourceLocation;
	}
	public void startServer() {
		ServerSocket serverSocket = null;
		Socket socket = null;
		System.out.println("SocketServer: 服务器监听开始... ");
		try {
			serverSocket = new ServerSocket(portServerListening);
			//5min 空闲判断一次是否需要继续监听, 不需要则结束程序.
			serverSocket.setSoTimeout(3000);
			while (isRun) {
				try {
					socket = serverSocket.accept();
				}catch (Exception e) {
					continue;
				}
				//System.out.println("收到新连接: " + socket.getInetAddress() + ":" + socket.getPort());
				SocketDealer dealer = new SocketDealer(socket, sourceLocation);
				httpThreadPool.execute(dealer);
				
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
				}
			}
			if (serverSocket != null) {
				try {
					serverSocket.close();
				} catch (IOException e) {
				}
			}
		}
	}
}
