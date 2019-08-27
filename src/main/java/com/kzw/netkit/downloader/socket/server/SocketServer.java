package com.kzw.netkit.downloader.socket.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SocketServer {
	// 客户端socket对象
	private ServerSocket m_servSocket;

	// ftp服务器的端口号
	private int SERVER_PORT;

	// ftp服务器所允许的最大连接数
	private int MAX_CONN;

	// 连入的客户端处理对象管理器
	private Vector vecClient;

	// 设定一个log日志对象
	private Logger mylog;

	private ConsoleHandler handler;

	String strServHome;

	public SocketServer(int servPort, int maxConn) {
		SERVER_PORT = servPort;
		MAX_CONN = maxConn;
		strServHome = "c:\\";
		vecClient = new Vector();
		/*------------初始化log------------*/
		try {
			handler = new ConsoleHandler();
			handler.setLevel(Level.ALL);
			mylog = Logger.getLogger("FtpServer");
			mylog.addHandler(handler);
			mylog.setLevel(Level.ALL);
		} catch (SecurityException e) {
			mylog.warning("在设置程序日志级别时出现异常");
			mylog.warning(e.getMessage());
		}

		/*--------------初始化服务器，端口2121----------------------*/
		try {
			m_servSocket = new ServerSocket(SERVER_PORT, MAX_CONN);
			while (true) {
				mylog.finest("FtpServer开始在端口2121监听");
				Socket clientSocket = m_servSocket.accept();
				vecClient.add(clientSocket);
				mylog.info("#" + vecClient.size() + "号客户端连入");
				new TransHandler(this, clientSocket, vecClient.size()).start();
			}
		} catch (IOException e) {
			mylog.warning("在初始化FtpServ时出现错误");
			mylog.warning(e.getMessage());
		}
	}

	public void deleteClient(TransHandler handler) {
		try {
			vecClient.remove(handler);
			vecClient.setSize(vecClient.size() - 1);
			mylog.info("第#" + handler.iClientNum + "号客户端断开了与服务器的连接！");
		} catch (Exception e) {
			mylog.warning("在删除第#" + handler.iClientNum + "号客户端时出现异常");
			mylog.warning(e.getMessage());
		}
	}

	public static void main(String[] args) {
		new SocketServer(2121, 50);
	}
}