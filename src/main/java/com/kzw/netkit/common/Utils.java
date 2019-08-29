package com.kzw.netkit.common;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.kzw.netkit.downloader.DownLoadThread;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Utils {
	public static final synchronized URLConnection newConnection(String uri, Proxy proxy, Map<String, String> headers)
			throws IOException {
		URL url = new URL(uri);
		final URLConnection conn;

		if (null != proxy) {
			conn = url.openConnection(proxy);
		} else {
			conn = url.openConnection();
		}
		if (headers != null && headers.size() > 0) {
			headers.forEach((key, value) -> {
				conn.setRequestProperty(key, value);
			});
		}
		return conn;
	}

	/**
	 * 格式化使用时间，单位：分钟，保留2位小数点
	 * 
	 * @param l
	 * @param i
	 * @param j
	 * @return
	 */
	public static String formatUsedTime(long beginTime, long endTime) {
		DecimalFormat df = new DecimalFormat("0.00");
		if (endTime - beginTime < 1000 * 60) {
			return df.format((float) (endTime - beginTime) / 1000).concat("秒");
		}
		return df.format((float) (endTime - beginTime) / (1000 * 60)).concat("分钟");
	}

	/**
	 * 格式化double，保留2位小数点，如1.24
	 * 
	 * @param d
	 * @return
	 */
	public static String formatDouble(double d) {
		return String.format("%.2f", d);
	}

	/**
	 * 格式化文件大小，如：311.00 kb
	 * 
	 * @param fileSize
	 * @return
	 */
	public static String formatFileSize(long fileSize) {
		double kb = fileSize / 1024;
		if (kb < 1) {
			return formatDouble(fileSize) + "byte";
		}
		double mb = kb / 1024;
		if (mb < 1) {
			return formatDouble(kb) + "kb";
		}
		double gb = mb / 1024;
		if (gb < 1) {
			return formatDouble(mb) + "mb";
		}
		double tb = gb / 1024;
		if (tb < 1) {
			return formatDouble(gb) + "gb";
		}
		return fileSize + "byte";
	}

	public static void validHttpResponseCode(URLConnection conn) throws IOException {
		if (conn instanceof HttpURLConnection) {
			((HttpURLConnection) conn).setRequestMethod("GET");
			int code = ((HttpURLConnection) conn).getResponseCode();

			// 如果响应码不是200 则抛出异常
			if (code != HttpURLConnection.HTTP_OK) {
				throw new IOException("无访问权限 code : " + code);
			}
		}
	}

	public static long getHttpContentLength(String uri, Proxy proxy, Map<String, String> headers) throws IOException {
		URLConnection conn = Utils.newConnection(uri, proxy, headers);
		validHttpResponseCode(conn);
		conn.connect();
		return conn.getContentLength();
	}

	public static String getFileNameFromHttpResponse(String uri, Proxy proxy, Map<String, String> headers)
			throws IOException {
		HttpURLConnection conn = (HttpURLConnection) Utils.newConnection(uri, proxy, headers);
		validHttpResponseCode(conn);
		conn.connect();
		String disposition = conn.getHeaderField("Content-Disposition");
		int index = -1;
		if (!Utils.isEmpty(disposition) && (index = disposition.indexOf("filename=")) != -1) {
			return disposition.substring(index + 10, disposition.length() - 1);
		}

		return null;
	}

	/**
	 * 格式化倒计数，如：2:46:40
	 * 
	 * @param time
	 * @return
	 */
	public static String formatCountdownTimmer(Long time) {
		if (time == -1) {
			return "--:--:--";
		} else if (time == 0) {
			return "00:00:00";
		}
		StringBuilder sb = new StringBuilder();
		int countTime = time.intValue();
		int MINUTE = 60;
		int HOUR = 60 * 60;

		int fHour = (int) countTime / HOUR;
		if (fHour < 10) {
			sb.append("0").append(fHour);
		} else {
			sb.append(fHour);
		}
		sb.append(":");

		int fMinute = (int) countTime % HOUR / MINUTE;
		if (fMinute < 10) {
			sb.append("0").append(fMinute);
		} else {
			sb.append(fMinute);
		}
		sb.append(":");

		int fSecond = countTime % MINUTE;
		if (fSecond < 10) {
			sb.append("0").append(fSecond);
		} else {
			sb.append(fSecond);
		}

		return sb.toString();
	}

	public static String bytesToHex(byte[] bytes) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(bytes[i] & 0xFF);
			if (hex.length() < 2) {
				sb.append(0);
			}
			sb.append(hex);
		}
		return sb.toString();
	}

	public static String encodeURIComponent(String str) {
		try {
			return URLEncoder.encode(str, "UTF-8");
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}

	public static boolean isEmpty(String fileName) {
		return fileName == null || fileName.length() == 0;
	}
}
