package com.kzw.netkit.curl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class CurlUtils {
	private static final int READ_TIMEOUT = 4000;
	private static final int CONNECT_TIMEOUT = 3000;

	public static String get(String url) throws IOException {
		return get(url, null, null);
	}
	
	public static String get(String url, Map<String, String> headers) throws IOException {
		return get(url, null, headers);
	}
	
	public static String post(String url, Map<String, Object> params) throws IOException {
		return post(url, null, params, null);
	}

	public static String post(String url, Proxy proxy, Map<String, Object> params, Map<String, String> headers) throws IOException {
		StringBuilder postData = new StringBuilder();
		
		for (Map.Entry<String, Object> entry : params.entrySet()) {
		    if (postData.length() != 0) {
	            postData.append('&');
	        }
			postData.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
	        postData.append('=');
	        postData.append(URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"));
		}
		return post(url, proxy, postData.toString(), headers);
	}

	public static String get(String url, Proxy proxy, Map<String, String> headers) throws IOException {
		log.info("GET {}", url);
		
		HttpURLConnection conn = createConnection(url, proxy);
		conn.setRequestMethod("GET");
		addHeaders(conn, headers);
		return getResult(conn);
	}

	public static String post(String url, Proxy proxy, String body, Map<String, String> headers) throws IOException {
		log.info("POST {}", url);
		
		HttpURLConnection conn = createConnection(url, proxy);
		conn.setRequestMethod("POST");
		addHeaders(conn, headers);
		
		conn.setDoOutput(true);
		OutputStream out = conn.getOutputStream();
		
	    byte[] postDataBytes = body.toString().getBytes("UTF-8");
		out.write(postDataBytes);
		out.flush();
		out.close();
		
		log.debug("body:");
		log.debug(body);
		
		return getResult(conn);
	}
	
	
	private static void addHeaders(HttpURLConnection conn, Map<String, String> headers) {
		StringBuilder sb = new StringBuilder();
		sb.append("请求：\n");
		headers.forEach((k,v) -> {
			sb.append(k).append(" : ").append(v).append("\n");
			conn.addRequestProperty(k, v);
		});
		log.debug(sb.toString());
	}
	
	private static HttpURLConnection createConnection(String urlStr, Proxy proxy) throws IOException {
		URL url = new URL(urlStr);
		HttpURLConnection conn ;
		if(proxy != null) {
			conn = (HttpURLConnection) url.openConnection(proxy);
		}else {
			conn = (HttpURLConnection) url.openConnection();
		}
		conn.setConnectTimeout(CONNECT_TIMEOUT);
		conn.setReadTimeout(READ_TIMEOUT);
		return conn;
	}
	
	private static String getResult(HttpURLConnection conn) throws IOException {
		int rspCode = conn.getResponseCode();
		StringBuilder sbLog = new StringBuilder();
		sbLog.append("响应：\n");
		if(log.isDebugEnabled()) {
			conn.getHeaderFields().forEach((k, v) -> {
				sbLog.append(k).append(" : ").append(v).append("\n");
			});
		}

		String body = null;
		if(rspCode == 200) {
			String encodeing = conn.getContentEncoding();
			InputStream stream = null;
			if("gzip".equals(encodeing)) {
				stream = new GZIPInputStream(conn.getInputStream()); 
			}else {
				stream = conn.getInputStream();
			} 
			body = IOUtils.toString(stream,"utf-8");
		} else {
			log.warn("返回码错误：{}", rspCode);
			throw new IOException("返回码错误："+rspCode);
		}
		sbLog.append(body);
		log.debug("{}" , sbLog.toString());
		
		return body;
	}
}
