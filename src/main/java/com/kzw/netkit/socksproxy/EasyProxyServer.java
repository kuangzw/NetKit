package com.kzw.netkit.socksproxy;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.extern.slf4j.Slf4j;

/**
 * 支持socks4\socks5代理
 * 
 * @author Kuang
 * @date 2019年7月26日 下午5:00:56
 */
@Slf4j
public class EasyProxyServer implements Runnable {

	private static final int SOCKS_PROTOCOL_4 = 0X04;
	private static final int SOCKS_PROTOCOL_5 = 0X05;
	private static final int DEFAULT_BUFFER_SIZE = 1024;
	private static final byte TYPE_IPV4 = 0x01;
	private static final byte TYPE_IPV6 = 0X02;
	private static final byte TYPE_HOST = 0X03;
	private static final byte ALLOW_PROXY = 0X5A;
	private static final byte DENY_PROXY = 0X5B;
	private Socket sourceSocket;
	
	Selector selector=null;
	ServerSocketChannel ssc = null;
			
	private String remoteAddr = null;
	
	private static ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

	public EasyProxyServer(Selector selector, ServerSocketChannel ssc) {
		this.selector = selector;
		this.ssc = ssc;
	}

	@Override
	public void run() {
		Socket proxySocket = null;
		InputStream sourceIn = null, proxyIn = null;
		OutputStream sourceOut = null, proxyOut = null;
		
		try {
			SocketChannel socket = ssc.accept();
			socket.configureBlocking(false);
			SelectionKey keyclient = socket.register(selector, SelectionKey.OP_READ);

			if(keyclient.isReadable()) {
				SocketChannel sc = (SocketChannel) keyclient.channel();
				sourceIn = sc.socket().getInputStream();
				sourceOut = sc.socket().getOutputStream();
				byte[] tmp = new byte[1];
				int n = sourceIn.read(tmp);
				if (n == 1) {
					int protocol = tmp[0];
					// socket4
					if (SOCKS_PROTOCOL_4 == protocol) {
						proxySocket = convertToSocket4(sourceIn, sourceOut);
					} else if (SOCKS_PROTOCOL_5 == protocol) {
						proxySocket = convertToSocket5(sourceIn, sourceOut);
					} else {
						log.info("Socket协议错误,不是Socket4或者Socket5");
					}
				}
			} 
			if(keyclient.isWritable()) {
				if (null != proxySocket) {
					CountDownLatch countDownLatch = new CountDownLatch(1);
					proxyIn = proxySocket.getInputStream();
					proxyOut = proxySocket.getOutputStream();
					transfer(sourceIn, proxyOut, countDownLatch);
					transfer(proxyIn, sourceOut, countDownLatch);
					try {
						countDownLatch.await();
					} catch (InterruptedException e) {
//						e.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			closeIO(sourceIn);
			closeIO(proxyIn);
			closeIO(proxyOut);
			closeIO(proxyIn);
			closeIO(proxySocket);
			closeIO(sourceSocket);
		}
	}
	
	private static void startServer(int port) throws IOException {
		//初始化容器和服务器
		Selector selector=Selector.open();
		ServerSocketChannel ssc = ServerSocketChannel.open();
		//绑定事件 和 端口 设置异步
		ssc.bind(new InetSocketAddress(port));
		ssc.configureBlocking(false);
		SelectionKey key = ssc.register(selector, SelectionKey.OP_ACCEPT);
		//创建accept事件,以及创建处理的线程
		key.attach(new EasyProxyServer(selector,ssc));
				
		while(true){
			try {
				int size = selector.select();
				if(size==0){
					continue;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
 
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = keys.iterator();
			while(iterator.hasNext()){
				SelectionKey next = iterator.next();
				
				Runnable r = (Runnable) next.attachment();
				new Thread(r).start();
				
				iterator.remove();
			}
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

	private void transfer(InputStream in, OutputStream out, CountDownLatch latch) {
		cachedThreadPool.execute(() -> {
			byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
			int count = 0;
			try {
				while (0 < (count = in.read(bytes))) {
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
		int port = 80;
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
		try {
			EasyProxyServer.startServer(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void showUsage() {
		System.out.println("usage : java -jar xxx.jar <port>");
	}

}
