package com.kzw.netkit.socksproxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpProxyServer extends Thread {
	private Socket socket;

    public HttpProxyServer(Socket socket) {
        this.socket = socket;
    }
    
    public static void main(String[] args) {
    	try {
    		ServerSocket serverSocket = new ServerSocket(8088);
  		  for (; ; ) { 
  		    new HttpProxyServer(serverSocket.accept()).start(); 
  		  }
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

    @Override
    public void run() {
        OutputStream clientOutput = null;
        InputStream clientInput = null;
        Socket proxySocket = null;
        InputStream proxyInput = null;
        OutputStream proxyOutput = null;
        try {
            clientInput = socket.getInputStream();
            clientOutput = socket.getOutputStream();
            String line;
            String host = "";
//            LineBuffer lineBuffer = new LineBuffer(1024);
            BufferedReader bffdReader = new BufferedReader(new InputStreamReader(clientInput));
            StringBuilder headStr = new StringBuilder();
            //读取HTTP请求头，并拿到HOST请求头和method
    		while (null != (line=bffdReader.readLine())) {
                System.out.println(line);
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
            String type = headStr.substring(0, headStr.indexOf(" "));
            //根据host头解析出目标服务器的host和port
            String[] hostTemp = host.split(":");
            host = hostTemp[0];
            int port = 80;
            if (hostTemp.length > 1) {
                port = Integer.valueOf(hostTemp[1]);
            }
            //连接到目标服务器
            proxySocket = new Socket(host, port);
            proxyInput = proxySocket.getInputStream();
            proxyOutput = proxySocket.getOutputStream();
            //根据HTTP method来判断是https还是http请求
            if ("CONNECT".equalsIgnoreCase(type)) {//https先建立隧道
                clientOutput.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
                clientOutput.flush();
            } else {//http直接将请求头转发
                proxyOutput.write(headStr.toString().getBytes());
            }
            //新开线程转发客户端请求至目标服务器
            new ProxyHandleThread(clientInput, proxyOutput).start();
            //转发目标服务器响应至客户端
            while (true) {
                clientOutput.write(proxyInput.read());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (proxyInput != null) {
                try {
                    proxyOutput.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (proxyOutput != null) {
                try {
                    proxyOutput.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (proxySocket != null) {
                try {
                    proxySocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (clientInput != null) {
                try {
                    clientInput.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (clientOutput != null) {
                try {
                    clientOutput.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
    
    public static class ProxyHandleThread extends Thread {

        private InputStream input;
        private OutputStream output;

        public ProxyHandleThread(InputStream input, OutputStream output) {
            this.input = input;
            this.output = output;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    output.write(input.read());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
