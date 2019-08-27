package com.kzw.netkit.downloader.socket.server;

public class PublicFunc {

	public static String formatLength(int length) {
		// TODO Auto-generated method stub
		return String.valueOf(length);
	}

	public static byte[] makepackage(String cmd, String dataLen, byte[] data) {
		byte[] buffer = new byte[12+1024];
//		int len = Integer.valueOf(dataLen);
		
		System.arraycopy(cmd.getBytes(), 0, buffer, 0, 7);  
		System.arraycopy(dataLen.getBytes(), 0, buffer, 7, dataLen.length());  
		System.arraycopy(data, 0, buffer, 12, data.length);  
		return buffer;
	}

}
