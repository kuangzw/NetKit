package com.kzw.netkit;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kzw.netkit.common.CmdLineUtil;
import com.kzw.netkit.curl.CURLApplication;
import com.kzw.netkit.downloader.MultiThreadDownloaderApplication;
import com.kzw.netkit.downloader.socket.SocketDownloaderServer;
import com.kzw.netkit.socksproxy.SocksProxyServer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NetKitApplication {

	public static void main(String[] args) {
		CmdLineUtil cmd = new CmdLineUtil("NetKit.jar", args); 

		cmd.addComments("<options> --help -h 显示每个命令帮助");
		
		cmd.addOpt("download", "", "<args>", "多线程下载器，支持socket下载（需要socketServer配合）");
		cmd.addOpt("socketServer","", "<args>","socket下载服务端，提供宿主机器文件多线程下载。");
		cmd.addOpt("proxyServer","", "<args>","socks4/socks5代理服务");
		cmd.addOpt("curl","", "<args>","模拟网络请求，支持自定义请求头，多线程并发、自定义请求次数、cron定时器");
		cmd.addOpt("--logLevel","", "<debug|info|warn|error>","日志级别(默认info)");
		
		if(args.length == 0 || cmd.isShowHelp()) {
			cmd.showUsage();
			return ;
		}
		if(cmd.hasOption("--logLevel")) {
			Logger logger = LogManager.getLogger("com.kzw.netkit");
			String logLevel = cmd.getOptionValue("--logLevel");
			logger.setLevel(Level.toLevel(logLevel));
		}
		
		String[] tarArgs = new String[args.length-1];
		System.arraycopy(args, 1, tarArgs, 0, tarArgs.length);
		
		if(cmd.hasOption("download")) {
			MultiThreadDownloaderApplication.main(tarArgs);
		}else if(cmd.hasOption("socketServer")) {
			SocketDownloaderServer.main(tarArgs);
		}else if(cmd.hasOption("curl")) {
			CURLApplication.main(tarArgs);
		}else if(cmd.hasOption("proxyServer")) {
			SocksProxyServer.main(tarArgs);
		} else {
			log.error("unknown command {} ,use --help show usage .", args[0]);
		}
	}

}
