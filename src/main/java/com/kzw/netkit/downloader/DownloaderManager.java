package com.kzw.netkit.downloader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.io.FileUtils;

import com.kzw.netkit.common.Utils;
import com.kzw.netkit.downloader.socket.SocketURLConnection;

import lombok.extern.slf4j.Slf4j;

/**
 * 下载管理
 * 
 * @author Kuang
 * @date 2019年7月10日 下午5:16:26
 */
@Slf4j
public class DownloaderManager {
	private Config cfg;
	private SpeedMonitor speedMonitor;
	private long startTime = System.currentTimeMillis();
	private long contentLength;
	private byte[] metaBytes = null;
	private RandomAccessFile metaDataFile;
	private List<DownLoadThread> tasks = new ArrayList<>();

	private ExecutorService executorService = Executors.newFixedThreadPool(32);
	
	public DownloaderManager(Config cfg) {
		this.cfg = cfg;
	}
	
	/**
	 * 恢复下载任务
	 * @throws IOException
	 */
	private void resumeTask() throws IOException {
		int taskCount = cfg.getThreadCount();
		int blockCount = metaBytes.length / taskCount;
		for (int i = 0; i < taskCount; i++) {
			int blockFrom = i * blockCount;
			int blockEnd = (i==taskCount-1) ? metaBytes.length : (i + 1) * blockCount;
			// 去除2端已下载部分
			while(blockFrom < blockEnd && metaBytes[blockFrom] == 1) {
				blockFrom ++;
			}
			while(blockFrom < blockEnd && metaBytes[blockEnd-1] == 1) {
				blockEnd --;
			}
			if(blockFrom != blockEnd) {
				// 中间部分重新下载
				byte[] mid = new byte[blockEnd - blockFrom];
				System.arraycopy(mid, 0, metaBytes, blockFrom, mid.length);
				
				DownLoadThread task = new DownLoadThread(metaBytes, blockFrom, blockEnd , cfg, speedMonitor);
				tasks.add(task);
			}
		}
	}
	
	/**
	 * 获取任务量最多的task
	 * @return
	 */
	private DownLoadThread getMostTask() {
		DownLoadThread target = null;
		int max = 0; 
		for (DownLoadThread downLoadTask : tasks) {
			if(downLoadTask.getUnDownloadBlockCount() > max) {
				target = downLoadTask;
			}
		}
		return target;
	}
	
	/**
	 * 获取已下载长度
	 * @return
	 */
	private long getCompletedBlock() {
		long blockCount = 0;
		for (int i = 0; i < metaBytes.length; i++) {
			if(metaBytes[i] == 1) {
				blockCount ++;
			}
		}
		return blockCount;
	}
	
	/**
	 * 开始下载
	 * @throws IOException
	 */
	public void start() throws IOException {
		
		if(cfg.getUrl().startsWith("socket:")) {
			try {
				SocketURLConnection.protocolSupport();
			} catch (Exception e) {
				throw new IOException("supports socket protocol failt !");
			}
		}
		
		String fileName = cfg.getSaveFileName();
		if(Utils.isEmpty(fileName)) {
			fileName = Utils.getFileNameFromHttpResponse(cfg.getUrl(), cfg.getProxy(), cfg.getHeaders());
			cfg.setSaveFileName(fileName);
		}
		
		if(Utils.isEmpty(fileName)) {
			throw new IOException("无法获取下载文件名称，请使用 --saveFileName 文件名称") ;
		}else if(new File(fileName).exists()) {
			throw new IOException("文件已经存在：" + new File(cfg.getSaveFileName()).getAbsolutePath()) ;
		}
		
		contentLength = Utils.getHttpContentLength(cfg.getUrl(), cfg.getProxy(), cfg.getHeaders());
		log.info("[GET] {} , ContentLength: {} ({}byte)" , cfg.getUrl(), Utils.formatFileSize(contentLength), contentLength);
		speedMonitor = new SpeedMonitor(contentLength);
		
		if(cfg.getMetaFile().exists() && cfg.getDownloadFile().exists()) { // 继续下载未完成
			log.info("继续下载未完成任务 ..");
			metaBytes = FileUtils.readFileToByteArray(cfg.getMetaFile());
		}else { // 新的下载任务
			if(contentLength < 1024*1024*3) { // 下载内容<3mb
				cfg.setBlockSize(1024);
			}
			metaBytes = new byte[(int)Math.ceil(Double.valueOf(contentLength) / cfg.getBlockSize())];
		}
		resumeTask();
		
		long completeLength = getCompletedBlock()*cfg.getBlockSize();
		speedMonitor.setCompletedLength(completeLength);
		
		metaDataFile = new RandomAccessFile(cfg.getMetaFile(), "rwd");
		
		log.info("启动线程数为：{} 【{}未下载】", tasks.size(), Utils.formatFileSize(contentLength-completeLength));
		
		new Timer("taskTimmer", false).scheduleAtFixedRate(downloadTaskTimmer, 0, 2000);
	}
	
	private TimerTask downloadTaskTimmer = new TimerTask() {
		@Override
		public void run() {
			for (DownLoadThread downLoadTask : tasks) {
				if (downLoadTask.isComplete()) {
					DownLoadThread mostTask = getMostTask();
					if(mostTask != null && mostTask.getUnDownloadBlockCount() >= 2) {
						downLoadTask = mostTask.breakTask();
						executorService.execute(downLoadTask);
					}
				}else if(downLoadTask.isInitial() 
						&& ((ThreadPoolExecutor)executorService).getActiveCount() < tasks.size()){
					executorService.execute(downLoadTask);
				}
			}

			try {
				metaDataFile.seek(0);
				metaDataFile.write(metaBytes, 0, metaBytes.length);
				
				if(getCompletedBlock() == metaBytes.length) { // 下载完成
					onFinished();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	};
	
	private void onFinished()  {
		File tarFile = null;
		try {
			tarFile = new File(cfg.getSaveFileName());
			downloadTaskTimmer.cancel();
			speedMonitor.cancel();

			cfg.getMetaFile().delete();
			cfg.getDownloadFile().renameTo(tarFile);
			metaDataFile.close();
			executorService.shutdown();
			Thread.sleep(1000);
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			int second = (int)Math.ceil((System.currentTimeMillis() - startTime) / 1000);
			long avgSpeed = contentLength / second;

			System.out.print("\n\n");
			log.info("==========================================");
			log.info("下载完成，耗时：{}，平均速度：{}/s ", Utils.formatUsedTime(startTime, System.currentTimeMillis()), Utils.formatFileSize(avgSpeed));
			log.info("文件保存位置：{} ", tarFile.getAbsolutePath());
		}
	}

	public void printProgressBar() {
		Timer mSpeedTimer = new Timer("ProgressBarTimmer", false);
		mSpeedTimer.scheduleAtFixedRate(speedMonitor, 0, 1000);
	}
}
