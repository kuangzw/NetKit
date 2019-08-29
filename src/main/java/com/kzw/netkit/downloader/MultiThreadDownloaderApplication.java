package com.kzw.netkit.downloader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.List;

import com.kzw.netkit.common.CmdLineUtil;
import com.kzw.netkit.downloader.baidupan.BaiduPanUtils;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class MultiThreadDownloaderApplication {
	public static void main(String[] args) {
		CmdLineUtil cmd = new CmdLineUtil("NetKit.jar download <uri>", args); 

		cmd.addComments("uri 协议支持 : ");
		cmd.addComments("http|https : 如:http://host/file.txt 注意 : url中有 '&' 整个URL必须使用\\\\\\\"\\\\\\\"包起来，否则dos下不能识别完整的url");
		cmd.addComments("socket : 如:socket://user:pass@ip:port/#D:/path/file.txt D:/path/file.txt为需要下载的目录");
		cmd.addComments("ftp : 如：ftp://user:pass@ip:port/#path/file.txt path/file.txt为需要下载的目录");
		cmd.addComments("");
		cmd.addComments("【百度网盘文件下载步骤：】");
		cmd.addComments("1、登陆百度网盘网页版，找到需要下载的文件路径，比如：/电影/大鱼.mv");
		cmd.addComments("2、网站打开开发者工具，浏览器访问网站：https://pan.baidu.com/disk/home?，复制Request Headers中的Cookie信息");
		cmd.addComments("3、执行命令：java -jar NetKit.jar download \"/电影/大鱼.mv\" --baidupan --cookie \"[输入第2步复制的字符串]\"");
		cmd.addComments("");
		cmd.addComments("");
		
		cmd.addOpt("--baidupan", "-bd", "", "百度网盘内文件高速下载");
		cmd.addOpt("--headers", "-hd", "<headerKey:headerValue>", "添加请求头信息");
		cmd.addOpt("--cookie", "-c", "\"<value>\"", "添加cookie信息");
		cmd.addOpt("--proxy", "", "<proxyhost:port>", "设置socks代理");
		cmd.addOpt("--threadNum", "-t", "<number>", "设置线程数量，默认5线程，最多32个线程");
		cmd.addOpt("--saveFileName", "-f", "<filename>", "文件保存名称");
		
		if(args.length == 0 || cmd.isShowHelp()) {
			cmd.showUsage();
			return ;
		}
		
		Config cfg = new Config();
		
		try {
			cmd.validateArgs();
			
			String url = args[0];
			if(cmd.hasOption("--baidupan")) {
				if(!url.startsWith("/")) {
					throw new IllegalArgumentException("百度网盘内文件路径格式步正确，正确格式如：/我的文件夹/照片.jpg");
				}
			}else if(!url.matches("^(https|http|socket|ftp)?://.+$")) {
				throw new IllegalArgumentException("url格式不正确");
			}
			cfg.setUrl(url);
			
			if(cmd.hasOption("--headers")) {
				List<String> headers = cmd.getMultiOptionValue("--headers");
				for (String each : headers) {
					String[] kv = each.split(":");
					cfg.addHeader(kv[0], kv[1]);
				}
			}
			if(cmd.hasOption("--cookie")) {
				String cookie = cmd.getOptionValue("--cookie");
				if(cookie.length() > 50) {
					log.info("used option cookie {} ...... {}", cookie.substring(0, 20), cookie.substring(cookie.length() - 20, cookie.length()));
				}else {
					log.info("used option cookie {} ", cookie);
				}
				cfg.addHeader("Cookie", cookie);
			}
			if(cmd.hasOption("--proxy")) {
				log.info("used option proxy {}", cmd.getOptionValue("--proxy"));
				String[] kv = cmd.getOptionValue("--proxy").split(":");
				Proxy proxy = new Proxy(Type.SOCKS, new InetSocketAddress(kv[0], Integer.valueOf(kv[1])));
				cfg.setProxy(proxy);
			}
			if(cmd.hasOption("--threadNum")) {
				log.info("used option threadNum {}", cmd.getOptionValue("--threadNum"));
				int threadNum = Integer.valueOf(cmd.getOptionValue("--threadNum"));
				if(threadNum > 32) {
					throw new IllegalArgumentException("线程数量最多32个线程，--help查看帮助。");
				}
				cfg.setThreadCount(threadNum);
			}
			if(cmd.hasOption("--saveFileName")) {
				log.info("used option saveFileName {}", cmd.getOptionValue("--saveFileName"));
				cfg.setSaveFileName(cmd.getOptionValue("--saveFileName"));
			}
			if(cmd.hasOption("--baidupan")) {
				log.info("used option baidupan");
				if(!cfg.getHeaders().containsKey("Cookie")) {
					throw new IllegalArgumentException("百度网盘内文件下载必须指定cookie值，--help查看帮助。");
				}
//				if(cfg.getThreadCount() > 8) {
//					throw new IllegalArgumentException("百度盘文件下载线程数量不能超过8线程，否则容易限速！");
//				}
				String bdUrl = BaiduPanUtils.getDownloadLinkWithRESTApi(cfg.getUrl(), cfg.getHeaders());
				log.info("百度网盘下载链接解析为：{}", bdUrl);
				cfg.setUrl(bdUrl);
			}
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			log.error("输入 --help 查看帮助！");
			return ;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			log.error("输入 --help 查看帮助！");
			return ;
		}

		DownloaderManager download = new DownloaderManager(cfg);
		try {
			download.start();
			download.printProgressBar();
		} catch (IOException e) {
			log.error(e.getMessage());
		}
		
	}
}
