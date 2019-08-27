package com.kzw.netkit.downloader.socket.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;
import java.util.logging.Logger;

import com.kzw.netkit.downloader.Config;
import com.kzw.netkit.downloader.SpeedMonitor;
import com.kzw.netkit.downloader.socket.server.PublicFunc;

public class SocketClientDownloaderThread extends Thread {
	Logger mylog = Logger.getLogger("FtpClient");
	Socket m_clientSocket;
	// 缓冲字节数据的大小
	private int ibufferlength;

	// 缓冲字节数组
	byte[] inputBytes;

	Vector vecServFiles;

	// 用于得到从socket端的输入信息
	InputStream m_inputStream;
	// 用于向socket输出的输出流
	OutputStream m_outputStream;
	// 向本地写文件的文件输出流
	FileOutputStream m_fileOutputStream;
	// 从本地读文件的文件输入流
	FileInputStream m_fileInputStream;
	// 从服务器端传来的指令
	String strServerOrder;
	// 主机的ip地址
	String strServerIP;
	
	String downloadFile;
	
	// 服务器的端口号
	int iServerPort;

	public SocketClientDownloaderThread(String strServIP, int iServPort) {
		strServerIP = strServIP;
		iServerPort = iServPort;
		this.downloadFile = "D:\\log\\config.plist";
	}

	public SocketClientDownloaderThread(long start, long end, Config cfg, SpeedMonitor speedMonitor) {
		// socket://user:pass@ip:port/#D:/path/file.txt
		String uri = cfg.getUrl();
		String str[] = uri.split("@|[/#]");
//		this.strServerIP = ""
		
	}

	public void run() {
		try {
			// 建立连接
			m_clientSocket = new Socket(strServerIP, iServerPort);
			mylog.info("已经连到了主机" + strServerIP + "在端口" + iServerPort);
			m_inputStream = m_clientSocket.getInputStream();
			m_outputStream = m_clientSocket.getOutputStream();
			mylog.fine("客户端得到了socket的输入输出流！");
			ibufferlength = 1024;
			inputBytes = new byte[ibufferlength + 12];
			vecServFiles = new Vector();
			downloadFile(this.downloadFile);
			receiver();
		} catch (UnknownHostException e) {
			mylog.warning("服务器地址未知");
			mylog.warning(e.getMessage());
		} catch (IOException e) {
			mylog.warning(e.getMessage());
		} catch (Exception e) {
			mylog.warning(e.getMessage());
		}
	}

	public void receiver() {
		try {
			int iLength = 0;
			while ((iLength = m_inputStream.read(inputBytes, 0, ibufferlength + 12)) != -1) {
				strServerOrder = new String(inputBytes, 0, 7);
				if (strServerOrder.equals("DISCONN")) { // 断开连接
					mylog.info("在client端得到了DISCONN");
					int length = Integer.parseInt(new String(inputBytes, 7, 5));
					mylog.info("长度是" + length);
				} else if (strServerOrder.equals("LSFILES")) { // 接收服务器当前目录文件列表

					int iDataLength = Integer.parseInt(new String(inputBytes, 7, 5));
					mylog.info("在客户端这个文件名的长度是：" + iDataLength);
					String strFileName = new String(inputBytes, 12, iDataLength);
					mylog.info("客户端正在获取服务器目录信息....." + strFileName);
					vecServFiles.add(strFileName);
				} else if (strServerOrder.equals("ENDFILE")) { // 下载一个文件的结束标记
					mylog.info("收到下载文件结束标志符号");
					m_fileOutputStream.close();
				} else if (strServerOrder.equals("DNDATAS")) { // 表示本包是要下载的数据
					int iDataLength = Integer.parseInt(new String(inputBytes, 7, 5));
					m_fileOutputStream.write(inputBytes, 12, iDataLength);
					m_fileOutputStream.flush();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 客户端上传文件名
	 * 
	 * @param strFileName
	 *            要上传文件的文件名
	 */
	public void downloadFile(String strFileName) {
		try {
			String strLength = PublicFunc.formatLength(strFileName.getBytes().length);
			byte[] outBytes = PublicFunc.makepackage("DNFILEN", strLength, strFileName.getBytes());
			m_outputStream.write(outBytes, 0, outBytes.length);
			m_outputStream.flush();
		} catch (Exception e) {
			e.printStackTrace();
			mylog.warning("在客户端在向服务器写要上传的文件名时发生异常");
			mylog.warning(e.getMessage());
		}
	}

	/**
	 * 客户端上传文件名
	 * 
	 * @param strFileName
	 *            要上传文件的文件名
	 */
	public void upFileName(String strFileName) {
		try {
			String strLength = PublicFunc.formatLength(strFileName.getBytes().length);
			byte[] outBytes = PublicFunc.makepackage("UPFILEN", strLength, strFileName.getBytes());
			m_outputStream.write(outBytes, 0, outBytes.length);
			m_outputStream.flush();
		} catch (Exception e) {
			mylog.warning("在客户端在向服务器写要上传的文件名时发生异常");
			mylog.warning(e.getMessage());
		}
	}

	/**
	 * 讲本地文件strFilePath上传到服务器
	 * 
	 * @param strFilePath
	 *            本地文件路径
	 */
	public void upFileData(String strFilePath) {
		try {
			File file = new File(strFilePath);
			m_fileInputStream = new FileInputStream(file);

			int iInputLength = 0;
			String strInputLength;
			byte[] readBytes = new byte[ibufferlength];
			while ((iInputLength = m_fileInputStream.read(readBytes, 0, ibufferlength)) != -1) {
				strInputLength = PublicFunc.formatLength(iInputLength);
				byte[] outBytes = PublicFunc.makepackage("UPDATAS", strInputLength, readBytes);
				m_outputStream.write(outBytes, 0, outBytes.length);
				m_outputStream.flush();
			}
			// 最后发送一个文件结束标记
			m_outputStream.write(PublicFunc.makepackage("ENDFILE", "00001", new byte[1]));
			m_outputStream.flush();
		} catch (Exception e) {
			mylog.warning("从客户端向服务器传输文件内容是发生异常");
			mylog.warning(e.getMessage());
		}
	}
}
