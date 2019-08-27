package com.kzw.netkit.downloader;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;

public class Config {
	private long blockSize = 1024 * 1024; // 默认一个block为1mb
	private String url;
	private int threadCount = 5;
	private String saveFileName;
	private Proxy proxy;
	private Map<String, String> headers = new HashMap<>();

	public void setBlockSize(long blockSize) {
		this.blockSize = blockSize;
	}
	public long getBlockSize() {
		return blockSize;
	}
	
	public int getThreadCount() {
		return threadCount;
	}

	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
	}

	public Proxy getProxy() {
		return proxy;
	}

	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setSaveFileName(String saveFileName) {
		this.saveFileName = saveFileName;
	}

	public void addHeader(String key, String value) {
		this.headers.put(key, value);
	}

	public String getSaveFileName() {
		if(this.saveFileName != null) {
			return saveFileName;
		}

		this.url = url.split("\\?", 2)[0];
		int position = this.url.lastIndexOf("/");
		String fileName = this.url.substring(position + 1);
		
		if(fileName.length() > 100 
				|| fileName.indexOf("\\") != -1
				|| fileName.indexOf("/") != -1
				|| fileName.indexOf(":") != -1
				|| fileName.indexOf("?") != -1
				|| fileName.indexOf("\"") != -1
				|| fileName.indexOf("<") != -1
				|| fileName.indexOf(">") != -1
				|| fileName.indexOf("|") != -1
				|| fileName.indexOf("*") != -1) {
//			throw new IllegalArgumentException("无法获取文件名，请使用--saveFileName 或者 -f 指定保存文件名称，或者使用--help查看帮助。");
			return null;
		}
		this.saveFileName = fileName;
		return fileName;
	}

	/**
	 * 获取下载状态文件
	 * 
	 * @param createIfNotExists 
	 * 	<br><b>false : 如果文件不存在返回null
	 *  <br><b>true : 如果文件不存在则创建
	 * @return
	 * @throws IOException
	 */
	public File getMetaFile() throws IOException {
		String tmpDir = System.getProperty("java.io.tmpdir");
		return new File(tmpDir.concat(getSaveFileName()).concat(".meta"));
	}
	
	public File getDownloadFile() {
		return new File(getSaveFileName().concat(".down"));
	}
}
