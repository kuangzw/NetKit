package com.kzw.downloader;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.kzw.netkit.downloader.socket.client.SocketClient;
import com.kzw.netkit.downloader.socket.client.SocketClientDownloaderThread;

public class SocketClientTest {
	public SocketClientTest() {

	}

	public static void main(String[] args) {
		try {
			
//			SocketClientDownloaderThread fc = new SocketClientDownloaderThread("127.0.0.1", 2121);
//			fc.start();
//			fc.join();
			
//			byte[] buffer = 
//			SocketClient.connect("127.0.0.1",2121).cmd("DNFILEN").onReceive()
			
			// FtpClient ftpClient= new FtpClient(); ftpClient.openServer(" 172.168.2.222 "
			// , 21 );
//			fc.login("username", "888888");
//			int ch;
//			File fi = new File("c:\\index.html");
//			RandomAccessFile getFile = new RandomAccessFile(fi, "rw");
//			getFile.seek(0);
//			TelnetInputStream fget = fc.get("index.html");
//			DataInputStream puts = new DataInputStream(fget);
//			while ((ch = puts.read()) >= 0) {
//				getFile.write(ch);
//			}
//			fget.close();
//			getFile.close();
//			fc.closeServer();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}
}
