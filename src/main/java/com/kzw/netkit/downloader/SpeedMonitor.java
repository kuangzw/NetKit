package com.kzw.netkit.downloader;

import java.util.TimerTask;

import com.kzw.netkit.common.Utils;

import lombok.extern.slf4j.Slf4j;

/**
 * 下载速度监控
 * 
 * @author Kuang
 * @date 2019年7月10日 下午5:28:30
 */
@Slf4j
public class SpeedMonitor extends TimerTask {
    private long startTime = System.currentTimeMillis();
	private long preReportTime = System.currentTimeMillis();

	private long totelLength;
	private long completedLength;
	
	private long preLength;
	private long preAvgSpeed;
	
	public SpeedMonitor(long totelContentLength) {
		this.totelLength = totelContentLength;
	}
	
	public void setCompletedLength(long completedLength) {
		this.completedLength = completedLength;
	}

	@Override
	public void run() {
		double percent = getDownloadPercent();
		percent = percent > 1 ? 1 : percent;

		System.out.print("[");
		
		int rate = (int)((percent*100)*30/100);
		for (int i = 0; i < 30; i++) {
			if(i == 0 || i < rate) {
				System.out.print(">");
			} else {
				System.out.print(" ");
			}
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("]  【");
		
		sb.append(Utils.formatFileSize(completedLength>totelLength?totelLength:completedLength))
			.append("/")
			.append(Utils.formatFileSize(totelLength))
			.append("  ")
			.append(Utils.formatDouble(percent*100))
			.append("% ")
			.append(Utils.formatCountdownTimmer(getRemainingTime()))
			.append(" ")
			.append(Utils.formatFileSize(getAverageSpeed()))
			.append("/s】     ");
		System.out.print(sb.toString()); 
		System.out.print("\r"); // 回到行首
	}
	
	/**
	 * 更新进度
	 * @param threadName
	 * @param readedLength
	 * @param threadTotalLength
	 */
	public synchronized void updateProccess(long readedLength) {
		// 总进度
		completedLength += readedLength;
	}
	
	// 10s平均速度(字节/秒)=上次取长度/上次耗时
	public long getAverageSpeed() {
		if(System.currentTimeMillis() - preReportTime < 1000) {
			return preAvgSpeed;
		}
		int second = (int)Math.ceil((System.currentTimeMillis() - preReportTime) / 1000);
		long speed = (completedLength-preLength) / second;
		preReportTime = System.currentTimeMillis();
		preLength = completedLength;
		preAvgSpeed = speed;
		return speed;
	}
	
	// 进度百分比=总读取长度/下载文件总长度
	public double getDownloadPercent() {
		return (double)completedLength / totelLength;
	}
	
	// 剩余时间=未下载长度/平均速度
	public long getRemainingTime() {
		long speed = getAverageSpeed();
		if(speed == 0) {
			return -1;
		}
		return (totelLength - completedLength) / speed;
	}
}
