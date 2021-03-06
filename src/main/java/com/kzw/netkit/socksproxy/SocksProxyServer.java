package com.kzw.netkit.socksproxy;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * 支持socks4\socks5代理
 * 
 * @author Kuang
 * @date 2019年7月26日 下午5:00:56
 */
@Slf4j
public class SocksProxyServer implements Runnable {

	private static final int SOCKS_PROTOCOL_4 = 0X04;
	private static final int SOCKS_PROTOCOL_5 = 0X05;
	private static final int HTTP_PROTOCOL = 67; //第一个字母：C （ CONNECT www.hbdm.com:80 HTTP/1.1 ...）
	private static final int DEFAULT_BUFFER_SIZE = 1024;
	private static final byte TYPE_IPV4 = 0x01;
	private static final byte TYPE_IPV6 = 0X02;
	private static final byte TYPE_HOST = 0X03;
	private static final byte ALLOW_PROXY = 0X5A;
	private static final byte DENY_PROXY = 0X5B;
	private Socket sourceSocket;
	
	private String remoteAddr = null;
	
	private static ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

	@Override
	public void run() {
		String remoteAddress = sourceSocket.getRemoteSocketAddress().toString();
		remoteAddr = remoteAddress.substring(1);
		InputStream sourceIn = null, proxyIn = null;
		OutputStream sourceOut = null, proxyOut = null;

		Socket proxySocket = null;
		try {
			sourceIn = sourceSocket.getInputStream();
			sourceOut = sourceSocket.getOutputStream();
			// 从协议头中获取socket的类型
			byte[] tmp = new byte[1];
			int n = sourceIn.read(tmp);
			
			if (n == 1) {
				int protocol = tmp[0];
				// socket4
				if (SOCKS_PROTOCOL_4 == protocol) {
					proxySocket = convertToSocket4(sourceIn, sourceOut);
				} else if (SOCKS_PROTOCOL_5 == protocol) {
					proxySocket = convertToSocket5(sourceIn, sourceOut);
				} else if(HTTP_PROTOCOL == protocol) { // http代理 ： 格式如：CONNECT www.hbdm.com:80 HTTP/1.1 
					proxySocket = new Socket("69.171.244.11", 80);
//					proxySocket = convertToHttp(sourceIn, sourceOut);
				} else { 
					log.info("Socket协议错误,不是Socket4或者Socket5或者HTTP协议");
				}
				// socket转换
				if (null != proxySocket) {
					CountDownLatch countDownLatch = new CountDownLatch(1);
					proxyIn = proxySocket.getInputStream();
					proxyOut = proxySocket.getOutputStream();
					transfer(sourceIn, proxyOut, countDownLatch);
					transfer(proxyIn, sourceOut, countDownLatch);
					countDownLatch.await(30, TimeUnit.SECONDS);
					countDownLatch = null;
				}

			} else {
				log.info("SOCKET ERROR: read failt from : {}", remoteAddress);
			}

		} catch (Exception e) {
			log.error("error: {}", e.getMessage());
		} finally {
			closeIO(sourceIn);
			closeIO(sourceOut);
			closeIO(proxyIn);
			closeIO(proxyOut);
			closeIO(proxySocket);
			closeIO(sourceSocket);
		}
	}

	public SocksProxyServer(Socket sourceSocket) {
		this.sourceSocket = sourceSocket;
	}

	private static void startServer(int port) {
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			Socket socket = null;
			while ((socket = serverSocket.accept()) != null) {
				cachedThreadPool.execute(new SocksProxyServer(socket));
			}
			log.info("close socket(this never happen)");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private Socket convertToSocket4(InputStream inputStream, OutputStream outputStream) throws IOException {
		Socket proxySocket = null;
		byte[] tmp = new byte[3];
		inputStream.read(tmp);
		// 请求协议|VN1|CD1|DSTPORT2|DSTIP4|NULL1|
		int port = ByteBuffer.wrap(tmp, 1, 2).asShortBuffer().get() & 0xFFFF;
		String host = getHost((byte) 0x01, inputStream);
		inputStream.read();
		// 返回一个8字节的响应协议: |VN1|CD1|DSTPORT2|DSTIP 4|
		byte[] response = new byte[8];
		try {
			proxySocket = new Socket(host, port);
			response[1] = ALLOW_PROXY;
			log.info("[{}]: {}: {}", remoteAddr, host, port);
		} catch (Exception e) {
			response[1] = DENY_PROXY;
//			log("connect error,host: " + host + " ,port: " + port);
		}
		outputStream.write(response);
		outputStream.flush();

		return proxySocket;
	}

	private Socket convertToSocket5(InputStream inputStream, OutputStream outputStream) throws IOException {
		Socket proxySocket = null;
		// 处理SOCKS5头信息(不支持登录)
		byte[] tmp = new byte[2];
		inputStream.read(tmp);
		byte method = tmp[1];
		if (0x02 == tmp[0]) {
			method = 0x00;
			inputStream.read();
		}
		tmp = new byte[] { 0x05, method };
		outputStream.write(tmp);
		outputStream.flush();

		byte cmd = 0;
		tmp = new byte[4];
		inputStream.read(tmp);
//		log("proxy header is:" + Arrays.toString(tmp));

		cmd = tmp[1];
		String host = getHost(tmp[3], inputStream);
		tmp = new byte[2];
		inputStream.read(tmp);
		int port = ByteBuffer.wrap(tmp).asShortBuffer().get() & 0xFFFF;
		log.info("[{}]: {}: {}", remoteAddr, host, port);
		ByteBuffer rsv = ByteBuffer.allocate(10);
		rsv.put((byte) 0x05);
		Object resultTmp = null;
		try {
			if (0x01 == cmd) {
				resultTmp = new Socket(host, port);
				rsv.put((byte) 0x00);
			} else if (0x02 == cmd) {
				resultTmp = new ServerSocket(port);
				rsv.put((byte) 0x00);
			} else {
				rsv.put((byte) 0x05);
				resultTmp = null;
			}
		} catch (Exception e) {
			rsv.put((byte) 0x05);
			resultTmp = null;
		}
		rsv.put((byte) 0x00);
		rsv.put((byte) 0x01);
		rsv.put(sourceSocket.getLocalAddress().getAddress());
		Short localPort = (short) ((sourceSocket.getLocalPort()) & 0xFFFF);
		rsv.putShort(localPort);
		tmp = rsv.array();

		outputStream.write(tmp);
		outputStream.flush();
		if (null != resultTmp && 0x02 == cmd) {
			ServerSocket ss = (ServerSocket) resultTmp;
			try {
				resultTmp = ss.accept();
			} catch (Exception e) {
			} finally {
				closeIO(ss);
			}
		}
		return (Socket) resultTmp;

	}
	private Socket convertToHttp(InputStream sourceIn , OutputStream sourceOut ) throws IOException {
		BufferedReader bffdReader = new BufferedReader(new InputStreamReader(sourceIn));
		StringBuilder headStr = new StringBuilder("C"); //第一个字符（已经读取）
		String host = null;
		String line = null;
		while (null != (line=bffdReader.readLine())) {
			headStr.append(line + "\r\n");
            if (line.length() == 0) {
                break;
            } else {
                String[] temp = line.split(" ");
                if (temp[0].contains("Host")) {
                    host = temp[1];
                }
            }
		}
		log.info(headStr.toString());
		
		String type = headStr.substring(0, headStr.indexOf(" "));
		//根据host头解析出目标服务器的host和port
        String[] hostTemp = host.split(":");
        host = hostTemp[0];
        int port = 80;
        if (hostTemp.length > 1) {
            port = Integer.valueOf(hostTemp[1]);
        }
        //连接到目标服务器
        Socket proxySocket = new Socket(host, port);
        InputStream proxyInput = proxySocket.getInputStream();
        OutputStream proxyOutput = proxySocket.getOutputStream();
        
        //根据HTTP method来判断是https还是http请求
//        if ("CONNECT".equalsIgnoreCase(type)) {//https先建立隧道
//            sourceOut.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
//            sourceOut.flush();
//        } else {//http直接将请求头转发
//            proxyOutput.write(headStr.toString().getBytes());
//        }
        log.info("write : {}" , headStr.toString());
        proxyOutput.write(headStr.toString().getBytes());
        proxyOutput.flush();
        
        return proxySocket;
	}

	private void transfer(InputStream in, OutputStream out, CountDownLatch latch) {
		cachedThreadPool.execute(() -> {
			byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
			int count = 0;
			try {
				while (0 < (count = in.read(bytes))) {
//			        log.info("transfer : {}" , new String(bytes));
			        
					out.write(bytes, 0, count);
					out.flush();
				}
			} catch (IOException e) {
//				log("转换出现错误");
			}
			if (latch != null) {
				latch.countDown();
			}

		});

	}

	private void closeIO(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
				closeable = null;
			} catch (IOException e) {
//				e.printStackTrace();
			}
		}
	}

	private String getHost(byte type, InputStream inputStream) throws IOException {
		String host = null;
		byte[] tmp = null;
		switch (type) {
		case TYPE_IPV4:
			tmp = new byte[4];
			inputStream.read(tmp);
			host = InetAddress.getByAddress(tmp).getHostAddress();
			break;
		case TYPE_IPV6:
			tmp = new byte[16];
			inputStream.read(tmp);
			host = InetAddress.getByAddress(tmp).getHostAddress();
			break;
		case TYPE_HOST:
			int count = inputStream.read();
			tmp = new byte[count];
			inputStream.read(tmp);
			host = new String(tmp);
		default:
			break;
		}
		return host;
	}

	public static void main(String[] args) {
		
		java.security.Security.setProperty("networkaddress.cache.ttl", "86400");
		int port = 8088;
		try {
			if(args.length == 1) {
				port = Integer.valueOf(args[0]);
			}else {
				showUsage();
			}
		} catch (Exception e) {
			showUsage();
		}
		log.warn("================== server start on : " + port + " ================");
		SocksProxyServer.startServer(port);
	}

	private static void showUsage() {
		System.out.println("usage : java -jar xxx.jar <port>");
	}

}
