package com.kzw.netkit.downloader.socket.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Logger;

class TransHandler extends Thread {
	// 服务器句柄，用于最后销毁TransHandler对象时用
	SocketServer main = null;
	// 客户端的socket
	private Socket m_clientSocket = null;

	// 日志对象
	private Logger mylog;

	// 要上传的文件路径
	private String strUpFilePath = null;

	// 要下载的文件路径
	private String strDnFilePath = null;

	// 本客户端在的序号
	int iClientNum = -1;

	// 缓冲字节数据的大小
	private int ibufferlength;

	// 缓冲字节数组
	byte[] inputBytes;

	// 从客户端传来的指令
	String strClientOrder;
	// 用于得到从socket端的输入信息
	InputStream m_inputStream;
	// 用于向socket输出的输出流
	OutputStream m_outputStream;
	// 用于上传文件的输出流
	FileOutputStream m_fileOutputStream;
	// 用于下载文件的输入流
	FileInputStream m_fileInputStream;

	// 构造函数
	public TransHandler(SocketServer fserver, Socket s, int iNum) {
		try {
			main = fserver;
			// 将客户端socket句柄付给本地对象
			m_clientSocket = s;
			// 初始化log对象
			mylog = Logger.getLogger("TransHandler");
			// 初始化本客户端序号
			iClientNum = iNum;
			// 用于得到从socket端的输入信息
			m_inputStream = m_clientSocket.getInputStream();
			m_outputStream = m_clientSocket.getOutputStream();
			ibufferlength = 1024;
			inputBytes = new byte[ibufferlength + 12];

		} catch (Exception e) {
			mylog.warning("在初始化TransHandler时发生异常！");
			mylog.warning(e.getMessage());
		}
	}

	public void run() {
		try {
			int ilength;
			while ((ilength = m_inputStream.read(inputBytes, 0, 12 + ibufferlength)) != -1) {
				strClientOrder = new String(inputBytes, 0, 7);
				if (strClientOrder.equals("DISCONN")) { // 断开连接
					mylog.info("得到了DISCONN");
					exit();
				} else if (strClientOrder.equals("LSFILES")) { // 发送当前目录文件列表
					mylog.info("服务器端接收到了LSFILES命令");
					File flHome = new File(main.strServHome);
					String[] strFileNames = flHome.list();
					strFileNames = AdjustStrings(strFileNames);
					for (int i = 0; i < strFileNames.length; i++) {
						String strFileNameLength = PublicFunc.formatLength(strFileNames[i].getBytes().length);
						byte[] fileNameBytes = strFileNames[i].getBytes();
						byte[] outBytes = PublicFunc.makepackage("LSFILES", strFileNameLength, fileNameBytes);
						m_outputStream.write(outBytes, 0, outBytes.length);
						m_outputStream.flush();
					}
				} else if (strClientOrder.equals("ENDFILE")) { // 上传一个文件的结束标记
					mylog.info("收到文件结束标志符号");
					m_fileOutputStream.close();
				} else if (strClientOrder.equals("UPFILEN")) { // 表示要上传一个新的文件，并且此包中包含了文件名
					int iFileNameLength = Integer.parseInt(new String(inputBytes, 7, 5));
					mylog.info("要上传的文件名的长度为" + iFileNameLength);
					String strFileName = new String(inputBytes, 12, iFileNameLength);
					mylog.info("要上传的文件名是：" + strFileName);
					// 初始化上传文件路径
					strUpFilePath = main.strServHome + strFileName;
					File upFile = new File(strUpFilePath);
					m_fileOutputStream = new FileOutputStream(upFile);
				} else if (strClientOrder.equals("UPDATAS")) { // 表示本包是要上传的数据
																// 本次数据包的长度
					mylog.info("正在接收文件...");
					int iDataLength = Integer.parseInt(new String(inputBytes, 7, 5));
					m_fileOutputStream.write(inputBytes, 12, iDataLength);
					m_fileOutputStream.flush();
				} else if (strClientOrder.equals("DNFILEN")) { // 表示要下载的文件名，服务器要执行向客户端传输文件的操作
					int iFileNameLength = Integer.parseInt(new String(inputBytes, 7, 5).trim());
					mylog.info("要下载的文件名的长度为" + iFileNameLength);
					String strFileName = new String(inputBytes, 12, iFileNameLength);
					mylog.info("要下载的文件名是：" + strFileName);
					// 初始化上传文件路径
//					strDnFilePath = main.strServHome + strFileName;
					File dnFile = new File(strFileName);
					// 初始化了文件输出流
					m_fileInputStream = new FileInputStream(dnFile);

					// 开始向客户端传输文件
					mylog.info("开始向客户端传输文件" + strFileName + "...");
					int iInputLength = 0;
					String strInputLength;
					byte[] readBytes = new byte[ibufferlength];
					while ((iInputLength = m_fileInputStream.read(readBytes, 0, ibufferlength)) != -1) {
						strInputLength = PublicFunc.formatLength(iInputLength);
						byte[] outBytes = PublicFunc.makepackage("DNDATAS", strInputLength, readBytes);
						m_outputStream.write(outBytes, 0, outBytes.length);
						m_outputStream.flush();
					}

					// 最后发送一个文件结束标记
					m_outputStream.write(PublicFunc.makepackage("ENDFILE", "00001", new byte[1]));
					m_outputStream.flush();
				}
			}
		} catch (Exception e) {
			mylog.warning(e.getMessage());
		}
	}

	public void exit() {
		try {
			m_outputStream.write(PublicFunc.makepackage("DISCONN", "00001", new byte[1]));
			m_inputStream.close();
			m_outputStream.close();
			main.deleteClient(this);
			main = null;
		} catch (Exception e) {
			mylog.warning("在断开客户端#" + this.iClientNum + "连接时出现异常！");
			mylog.warning(e.getMessage());
		}
	}

	public String[] AdjustStrings(String[] strFileNames) {
		String[] strItemNames = new String[strFileNames.length + 1];
		strItemNames[0] = "返回上一级";
		int j = 1;
		for (int i = 0; i < strFileNames.length; i++) {
			File upFile = new File(main.strServHome + strFileNames[i]);
			if (!upFile.isFile()) {
				strItemNames[j++] = "[文件夹]" + strFileNames[i];
			}
		}
		for (int i = 0; i < strFileNames.length; i++) {
			File upFile = new File(main.strServHome + strFileNames[i]);
			if (upFile.isFile()) {
				strItemNames[j++] = strFileNames[i];
			}
		}

		return strItemNames;
	}

}