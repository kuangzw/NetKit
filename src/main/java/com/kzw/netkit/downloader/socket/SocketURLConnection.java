package com.kzw.netkit.downloader.socket;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kzw.easysocket.EasySocketClient;
import com.kzw.easysocket.protocol.StringOperationProtocol;

public class SocketURLConnection extends URLConnection {

	private static final Logger log = LoggerFactory.getLogger(SocketURLConnection.class);
	private URL u = null;
	private StringOperationProtocol client = null;
	private String range = null;
	private static boolean supported = false;
	

	public static void protocolSupport() throws Exception {
		if(supported) {
			return ;
		}else {
			supported = true;
		}
		
		log.info("support socket downloader protocol !");
        final Field factoryField = URL.class.getDeclaredField("factory");
        factoryField.setAccessible(true);
        final Field lockField = URL.class.getDeclaredField("streamHandlerLock");
        lockField.setAccessible(true);
        // use same lock as in java.net.URL.setURLStreamHandlerFactory
        synchronized (lockField.get(null)) {
            final URLStreamHandlerFactory originalUrlStreamHandlerFactory = (URLStreamHandlerFactory) factoryField.get(null);
            // Reset the value to prevent Error due to a factory already defined
            factoryField.set(null, null);
            URL.setURLStreamHandlerFactory(protocol -> {
                if (protocol.equals("socket")) {
                    return new URLStreamHandler() {
    					@Override
    					protected URLConnection openConnection(URL u) throws IOException {
    						return new SocketURLConnection(u);
    					}
    				};
                } else {
                	if(originalUrlStreamHandlerFactory != null) {
                		return originalUrlStreamHandlerFactory.createURLStreamHandler(protocol);
                	}
                	return null;
                }
            });
        }
    }
	
	public SocketURLConnection(URL url) {
		super(url);
		this.u = url;
	}
	
	@Override
	public void connect() throws IOException {
		log.info("connect >>>");
		client = (StringOperationProtocol) EasySocketClient.protocol(StringOperationProtocol.class)
				.connect(u.getHost(), u.getPort())
				.getProtocol();
		try {
			// 防止client未连接而先执行client.send方法
			Thread.sleep(10);
		} catch (InterruptedException e) { }
	}
	
	@Override
	public InputStream getInputStream() throws IOException {
		return client.sendAndReceive("downloadFile", range + "@" + u.getRef(), InputStream.class);
	}
	
	@Override
	public void setRequestProperty(String key, String value) {
		super.setRequestProperty(key, value);
		
		if("Range".equalsIgnoreCase(key)) {
			this.range  = value;
		}
	}
	
	@Override
	public int getContentLength() {
		try {
			return client.sendAndReceive("getFileLength", u.getRef(), Integer.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return -1;
	}
}
