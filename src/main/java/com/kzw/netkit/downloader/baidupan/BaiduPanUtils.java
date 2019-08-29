package com.kzw.netkit.downloader.baidupan;

import java.util.Date;
import java.util.Map;

import com.kzw.netkit.common.Utils;

public class BaiduPanUtils {
	private static final String restApi = "https://pcs.baidu.com/rest/2.0/pcs/";
//	private static final String clientApi = "https://d.pcs.baidu.com/rest/2.0/pcs/";
	
	public static final String getDownloadLinkWithRESTApi(String path, Map<String, String> headers) {
		StringBuilder sb = new StringBuilder();
		sb.append(restApi).append("file?method=download&path=")
		.append(Utils.encodeURIComponent(path))
		.append("&random=")
		.append(Math.random())
		.append("&app_id=498065");

		headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
		headers.put("Accept-Encoding", "gzip, deflate, br");
		headers.put("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7");
		headers.put("Cache-Control", "no-cache");
		headers.put("Connection", "keep-alive");
		headers.put("Content-Type", "text/plain; charset=utf-8");
		headers.put("Host", "pcs.baidu.com");
		headers.put("Pragma", "no-cache");
		headers.put("Referer", "https://pan.baidu.com/disk/home?");
		headers.put("Upgrade-Insecure-Requests", "1");
		headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36");
		
		return sb.toString();
	}
	

//	public static final String getDownloadLinkWithClientApi(String path) {
//		StringBuilder sb = new StringBuilder();
//		sb.append(clientApi).append("file?method=locatedownload&app_id=")
//		.append(Utils.encodeURIComponent(path))
//		.append("&random=")
//		.append(Math.random())
//		.append("&app_id=498065");
//		return sb.toString();
//	}
}
