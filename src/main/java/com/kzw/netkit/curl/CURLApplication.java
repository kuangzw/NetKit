package com.kzw.netkit.curl;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kzw.netkit.common.CmdLineUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CURLApplication {
	public static void main(String[] args) {
		CmdLineUtil cmd = new CmdLineUtil("NetKit.jar curl <options>", args); 

		cmd.addOpt("--get", "-g", "<url>", "使用get请求");
		cmd.addOpt("--post", "-p", "<url>", "使用post请求");
		cmd.addOpt("--body", "-b", "<params>", "post请求参数, 格式如：key1=v1&key2=v2");
		cmd.addOpt("--userAgent", "-u", "<string>", "设置userAgent，默认为 QuBuKeJi/3.1.0 (iPhone; iOS 12.0.1; Scale/2.00) ");
		cmd.addOpt("--header","-hd", "<key:value>","请求同信息，可以拥有多个--header参数");
		cmd.addOpt("--cookie","-c", "<string>","设置cookie信息，可以拥有多个--cookie参数");
		cmd.addOpt("--contentType","-ct", "<string>","设置contentType信息，默认 application/x-www-form-urlencoded;charset=utf8");
		cmd.addOpt("--proxy","-p", "<ip:port>","使用socks5代理");
		cmd.addOpt("--thread","-td", "<int>","设置并发线程数");
		cmd.addOpt("--debug","", "","打印debug信息");

		if(args.length == 0 || cmd.isShowHelp()) {
			cmd.showUsage();
			return ;
		}
		
		try {
			Map<String, String> headers = new HashMap<String, String>();
			setDefaultHeaders(headers);
			Proxy proxy = null;
			
			if(cmd.hasOption("--proxy")) {
				String[] kv = cmd.getOptionValue("--proxy").split(":");
				proxy = new Proxy(Type.SOCKS, new InetSocketAddress(kv[0], Integer.valueOf(kv[1])));
			}
			
			if(cmd.hasOption("--header")) {
				List<String> headersList = cmd.getMultiOptionValue("--header");
				for (String header : headersList) {
					String[] kv = header.split(":");
					headers.put(kv[0], kv[1].trim());
				}
			}

			if(cmd.hasOption("--cookie")) {
				List<String> headersList = cmd.getMultiOptionValue("--cookie");
				for (String cookie : headersList) {
					headers.put("Cookie", cookie);
				}
			}
			
			if(cmd.hasOption("--userAgent")) {
				headers.put("User-Agent", cmd.getOptionValue("--userAgent"));
			} 
			
			if(cmd.hasOption("--debug")) {
				Logger logger = LogManager.getLogger("com.kzw.netkit.curl");
				logger.setLevel(Level.toLevel("DEBUG"));
			} 
			
			String rst = null;
			if(cmd.hasOption("--get")) {
				String url = cmd.getOptionValue("--get");
				
				if(cmd.hasOption("--body")) {
					url += "?" + cmd.getOptionValue("--body");
				} 
				
				rst = CurlUtils.get(url, proxy, headers);
			}else if(cmd.hasOption("--post")){
				String body = null;
				
				if(cmd.hasOption("--body")) {
					body = cmd.getOptionValue("--body");
				} 
				
				rst = CurlUtils.post(cmd.getOptionValue("--post"), proxy, body, headers);
			} else {
				log.warn("必须有--get 或者 --post 参数，使用 --help 命令查看帮助");
				return ;
			}
			
			log.info(rst);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	private static void setDefaultHeaders(Map<String, String> headers) {
		headers.put("Content-Type", "application/x-www-form-urlencoded;charset=utf8");
		headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
		headers.put("User-Agent", "QuBuKeJi/3.1.0 (iPhone; iOS 12.0.1; Scale/2.00)");
		headers.put("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7");
		headers.put("Accept-Encoding", "gzip, deflate, br");
		headers.put("Connection", "keep-alive");
	}
}
