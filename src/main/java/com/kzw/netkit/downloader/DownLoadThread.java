package com.kzw.netkit.downloader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URLConnection;

import com.kzw.netkit.common.Utils;

import lombok.extern.slf4j.Slf4j;

/**
 * 下载线程
 * 
 * @author Kuang
 * @date 2019年7月10日 下午2:10:12
 */
@Slf4j
public class DownLoadThread implements Runnable {
	public static final int STATUS_COMPLETE = 1;
	public static final int STATUS_RUNNING = 0;
	public static final int STATUS_INITIAL = -1;

	private int taskStatus = STATUS_INITIAL;
	private long beginTime = System.currentTimeMillis();
	
	private Config cfg;
	private SpeedMonitor speedMonitor;
	private byte[] metaData = null;
	
	private int blockFrom;
	private int blockEnd;

	private int totleRetryTime = 10;
	private int crtRetryTime = 0;

	public DownLoadThread(byte[] metaData,int blockFrom, int blockEnd, Config cfg, SpeedMonitor speedMonitor) {
		this.metaData = metaData;
		this.blockFrom = blockFrom;
		this.blockEnd = blockEnd;
		this.cfg = cfg;
		this.speedMonitor = speedMonitor;
	}
	
	@Override
	public void run() {
		this.taskStatus = STATUS_RUNNING;
		
		RandomAccessFile raf = null;
		URLConnection conn = null;
		BufferedInputStream bis = null;
		long totalReadLength = 0;
		
		long pstStart = blockFrom * cfg.getBlockSize();
		long pstEnd = blockEnd * cfg.getBlockSize();
		int preBlock = blockFrom;
		
		try {
			conn = Utils.newConnection(cfg.getUrl(), cfg.getProxy(), cfg.getHeaders());
			conn.setRequestProperty("Range", "bytes=" + pstStart + "-" + pstEnd);
			conn.connect();
			
			log.info(" Range bytes[ " + pstStart + " - " + pstEnd + " ]");
			
			if(conn instanceof HttpURLConnection) {
				int code = ((HttpURLConnection)conn).getResponseCode();
				if (code != HttpURLConnection.HTTP_OK && code != 206) {
					throw new IllegalStateException(" GET "+cfg.getUrl()+" , Rsp code : " + code);
				}
			}
			
			raf = new RandomAccessFile(cfg.getDownloadFile(), "rwd");
			raf.seek(pstStart); // 制定存放的位置
			
			bis = new BufferedInputStream(conn.getInputStream());
			int len = 0;
			byte[] buf = new byte[1024*1024];
			while ((len = bis.read(buf, 0, buf.length)) != -1) {
				totalReadLength += len;
				
				raf.write(buf, 0, len);
				speedMonitor.updateProccess(len);

				int cBlock = (int)(totalReadLength / cfg.getBlockSize()) + blockFrom;

				for (int i = preBlock; i < cBlock; i++) {
					metaData[i] = 1;
				}
				preBlock = cBlock;
				
				if(cBlock >= metaData.length 
						|| metaData[cBlock] == 1) { // 下一个block已下载
					break;
				} 
			}
			if(blockEnd == metaData.length) { // 最后一个Block完成
				metaData[blockEnd-1] = 1;
			}
			// 防止进度条字符缓冲，使用空格覆盖
			log.info("线程结束,下载长度：{}, 耗时： {}           ", Utils.formatFileSize(totalReadLength), Utils.formatUsedTime(beginTime,System.currentTimeMillis()));
		} catch (IllegalStateException | IOException e) {
			closeAllQuiet(raf, conn, bis);

			crtRetryTime ++;
			if(crtRetryTime < totleRetryTime) {
				log.info("下载失败 ,正在重试 (次数：{}) : {}", crtRetryTime, e.getCause()!=null?e.getCause().getMessage():e.getMessage());
				try {
					Thread.sleep(crtRetryTime * 3000);
				} catch (InterruptedException e1) { }
				// 重新下载
				this.blockFrom += (int)(totalReadLength / cfg.getBlockSize());
				run();
			}else {
				log.error("download failt : {} ", e.getMessage());
			}
		} finally {
			closeAllQuiet(raf, conn, bis);
		}
	}
	
	private void closeAllQuiet(RandomAccessFile raf, URLConnection conn, BufferedInputStream bis) {
		try {
			if(conn != null && conn instanceof HttpURLConnection) {
				((HttpURLConnection)conn).disconnect();
				conn = null;
			}
			if(bis != null) {
				bis.close();
				bis = null;
			}
			if(raf != null) {
				raf.close();
				raf = null;
			}
		} catch (IOException e1) { }
	}
	
	/**
	 * 分解任务
	 * @return
	 */
	public DownLoadThread breakTask() {
		int crtBlock = this.blockEnd - getUnDownloadBlockCount();
		int newEnd = crtBlock+((this.blockEnd-crtBlock)/2);
		DownLoadThread task = new DownLoadThread(metaData, newEnd, blockEnd, cfg, speedMonitor);
		this.blockEnd = newEnd;
		return task;
	}

	public int getUnDownloadBlockCount() {
		int count = 0;
		for (int i = blockFrom; i < blockEnd; i++) {
			if(metaData[i] != 1) {
				count ++;
			}
		};
		return count;
	}

	public boolean isRunning() {
		return this.taskStatus == STATUS_RUNNING;
	}

	public boolean isInitial() {
		return this.taskStatus == STATUS_INITIAL;
	}

	public boolean isComplete() {
		for (int i = blockFrom; i < blockEnd; i++) {
			if(metaData[i] != 1) {
				return false;
			}
		}
		this.taskStatus = STATUS_COMPLETE;
		return true;
	}
}
