package com.kzw.netkit.downloader.socket;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.kzw.easysocket.EasySocketServer;
import com.kzw.easysocket.protocol.ProtocolHandler;
import com.kzw.easysocket.protocol.StringOperationProtocol;
import com.kzw.netkit.common.CmdLineUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Unit test for simple App.
 */
@Slf4j
public class SocketDownloaderServer {
	
    public static void main(String[] args) {
		CmdLineUtil cmd = new CmdLineUtil(args); 
		cmd.addOpt("--port", "-p", "<int>", "监听端口，默认8888");
		cmd.showUsage();
    	if(args == null || cmd.isShowHelp()) {
    		return ;
    	}
    	int port = 8888;
    	if(cmd.hasOption("--port")) {
    		port = Integer.valueOf(cmd.getOptionValue("--port"));
    	}
    	
    	log.info("正在启动socketdownload服务端，监听端口：{}", port);
    	EasySocketServer.protocol(StringOperationProtocol.class).listen(port)
			.handler("getFileLength", new ProtocolHandler() {
				@Override
				public void handle(byte[] buffers, long currentLength, long totleLength, boolean isLast) throws IOException {
					String body = new String(buffers ,"utf-8").trim();
					log.info("<<<<< getFileLength :  {}", body);

					StringOperationProtocol protocol = (StringOperationProtocol)getProtocol();
					
					File file = new File(body);
					if(!file.exists()) {
						log.info("file not exists !");
						protocol.send("error", "file "+body+" not exists !");
						return ;
					}
					Long fLength = file.length();
					log.info(">>>> size : {}", fLength);
					
					protocol.send("getFileLength", String.valueOf(fLength));
				}
			})
			.handler("downloadFile", new ProtocolHandler() {
				@Override
				public void handle(byte[] buffers, long currentLength, long totleLength, boolean isLast) throws IOException {
					String body = new String(buffers ,"utf-8").trim();
					System.out.println("downloadFile : received : " + body);
					
					String[] tmp = body.split("@");
					String[] range = tmp[0].split("=|-");
					
					File file = new File(tmp[1]);
					long bodyFrom = Long.valueOf(range[1]);
					long bodyTo = Long.valueOf(range[2]);
					long bodyLength = bodyTo - bodyFrom > file.length() ? file.length() : bodyTo - bodyFrom;
					
					FileInputStream fis = new FileInputStream(file);
					StringOperationProtocol protocol = (StringOperationProtocol)getProtocol();
					
					int len = 0;
					do {
						fis.skip(bodyFrom);
						int bufferLen =(int) bodyLength-len>1024?1024:(int) bodyLength-len;
						byte[] buffer = new byte[bufferLen]; 
						len += fis.read(buffer);
	    				protocol.send("downloadFile", buffer, len, bodyLength);
						
					} while (len != bodyLength);
					
					fis.close();
				}
			})
			.start();
	}
}
