package com.kzw.downloader;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.kzw.netkit.NetKitApplication;

public class QuBuSteps {
	private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd@HH#mm#ss");
	private static long lastStepsTime = 0;
	private static String todayIs = null;
	private static int totalStep = 0;
	private static int initStep = 0;
	
	public static void main(String[] args) {
		if(args.length == 1) {
			initStep = Integer.valueOf(args[0]);
		}
		while (true) {
			try {
				if(todayIs != null) {
					Thread.sleep((long)(random(1000*60*20, 1000*60*100)));
				}
			} catch (InterruptedException e) { } 
			
			String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
			if(!today.equals(todayIs)) {
				lastStepsTime = 0;
				totalStep = 0;
			}
			todayIs = today;
			
			int crrtHours = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
			if(crrtHours < 9 || crrtHours > 21) {
				continue;
			}else if(System.currentTimeMillis() - lastStepsTime < 1000*60*30) { // 上次提交小于30分钟
				continue;
			}else if(totalStep > 20000) { //大于2w就不要提交了
				continue;
			}
			
			StringBuilder url = new StringBuilder();
			int[] steps = randomSteps();
			if(steps[1] > 10000 || steps[1] == 0) {
				continue;
			}
			url.append("https://www.51qub.com/task/getcoinforsteps?addtimes=");
			url.append(formatter.format(new Date()).replace("@", "%20").replaceAll("#", "%3A"));
			url.append("&bracelet_distanse=0&bracelet_steps=0&idcode=fa0cf1187883466d867362e1016b0613&memberid=54780657&");
			url.append("mobile_distanse=");
			url.append(steps[0]);
			url.append("&mobile_steps=");
			url.append(steps[1]);
			url.append("&token=4cdc4cfcca8942b2a79de89a22e26c4b");
			
			args = new String[] {"curl"
			        ,"--get" , url.toString()
			        ,"-hd" ,"Content-Type: application/x-www-form-urlencoded;charset=utf8"
			        ,"-hd", "wToken: KIUR_2RbXt8eFA/n2UNDU303vl6Rq+lwavS2sXsadboXHBLIEAxWbAR81y1ZGght1/4qu9WhR3bSmxp1F6ScNG7K9AUQ1LA64+RydIRGngHh23DCWZb4AeSPLRglFL9EAuu22tTadEVCjhQg13ZuQbW3sYPNnLANVPNJW3/FP3e3t/GyY4YCQDH0aGMhp9c4llG55hJjnu8OHyLmOkw4FKtH+BZnRIqsuabQjdQp10dwtJBj9fl119fi7G0xe3RrilobUKU0N8bSa76Io7L/6II/cqv1gCCPiErZHx+DYUdjKEQvgANaKwsoKuQqVV59UG72xc7lOD3R7YOfwlOAaMNwBPOwSV+0yNluFO61RMW8VPzKg08wAqO5Zo9jO0DdAcTTTA5FWFxbcsg13qykY/CT6hQAtTrY9o3jmEM52GJLVtEUCPfY7au2isf4XyY2o817R&IMHW_i0013925a2eae4cd27d08394238b8862ddc4fc9f7abef"
			        ,"-hd", "Cookie: SERVERID=3c384dc32873c7fc8e33306cd8cd6544|1563801294|1563801291" 
	        		,"-hd" ,"Accept: */*" 
			        ,"-hd", "User-Agent: QuBuKeJi/3.1.0 (iPhone; iOS 12.0.1; Scale/2.00)" 
			        ,"-hd", "Accept-Language: zh-Hans-CN;q=1" 
			        ,"-hd", "Accept-Encoding: br, gzip, deflate" 
			        ,"-hd", "Connection: keep-alive"
			        ,"--debug"};
			try {
				NetKitApplication.main(args);
			} catch (Exception e) { }

		}
	}
	
	private static int[] randomSteps() {
		int stepByHours = random(10, 40); // 每分钟走多少步
		int step = 0;
		if(lastStepsTime == 0) { //新的一天
			step = initStep != 0 ? initStep : random(2800, 3500);
		}else {
			step = ((int)(System.currentTimeMillis() - lastStepsTime) / 1000 / 60)  * stepByHours;
		}
		
		lastStepsTime = System.currentTimeMillis();
		totalStep += step;
		
		int distanse = (int) (totalStep * 0.75);
		return new int[] {distanse, totalStep};
	}
	
	private static int random(int Min, int Max) {
		return Min + (int)(Math.random() * ((Max - Min) + 1));
	}
}
