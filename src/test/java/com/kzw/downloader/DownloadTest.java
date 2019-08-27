package com.kzw.downloader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URLConnection;

import com.kzw.netkit.common.Utils;
import com.kzw.netkit.downloader.Config;
import com.kzw.netkit.downloader.DownloaderManager;
import com.kzw.netkit.downloader.baidupan.BaiduPanUtils;

public class DownloadTest {
//	private static String url = "http://www.baidu.com/img/bd_logo1.png";
//	private static String url = "https://telerik-fiddler.s3.amazonaws.com/fiddler/FiddlerSetup.exe";
//	private static String url = "http://52gyxz.c1578dn.cn/521/rj_lw1/xunixianshixiangji.apk";
	private static String url = "https://dl-sh-ctc-2.pchome.net/03/lt/VMware-workstation-full-15.0.2-10952284.exe?key=a44d589d53ae89a03fd578010b279045&tmp=1566611662152";
//	private static String url = "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1566536883436&di=6a2d5815b57b928ecc6e321432e8f485&imgtype=0&src=http%3A%2F%2Fb-ssl.duitang.com%2Fuploads%2Fitem%2F201303%2F07%2F20130307211303_kr45e.jpeg";
//	private static String url = "https://up.enterdesk.com/edpic_source/8a/f1/1a/8af11a5740292a586209f7e9e6373561.jpg";
//	private static String url = "http://nginx.org/download/nginx-1.16.1.zip";
//	private static String socketUrl = "socket://user:pass@127.0.0.1:1231/#D:/path/file.txt";
	private static String socketUrl = "socket://user:pass@ec2-3-17-24-107.us-east-2.compute.amazonaws.com:8088/#C:/project/my_tv_1.0.0.rar";

	public static void main(String[] args) throws Exception {
		long stime = System.currentTimeMillis();
//		new DownloadTest().testNormal(url);
		new DownloadTest().testBaiduPan("/Android/android-x86-4.0-RC2-tx2500.iso");
//		new Test().testWithCookie();
//		new Test().testWithProxy();

		System.out.println("used : " + (System.currentTimeMillis()-stime));
	}
	
	private void testBaiduPan(String path) throws IOException {
		Config cfg = new Config();
		cfg.setThreadCount(3);
		cfg.addHeader("Cookie", "BIDUPSID=90584202C7FEEA601CCC29EC5914E2ED; PSTM=1565839764; BAIDUID=DB82EB77DF0B7FF8714C4445AFAB5804:FG=1; H_PS_PSSID=1440_21099_29522_29519_29098_29568_29220_26350; PANWEB=1; __51cke__=; Hm_lvt_7a3960b6f067eb0085b7f96ff5e660b0=1564708301,1565149320,1566367039; BDCLND=b%2FwxFFuOiQQUwwxBtpKlBkYpJ6L5oSRuUsw2eBmy4Io%3D; BDSFRCVID=VLuOJeCmH6VwoRJwVzDhomspCgKK0gOTHllvisL-qIm7dgkVJeC6EG0Ptf8g0KubFTPRogKK0gOTH6KF_2uxOjjg8UtVJeC6EG0P3J; H_BDCLCKID_SF=tb4DoC8XJIvbfP0kD5K5q4tHegc-aMRZ5mAqof_byDnEHP3K0h6xhPuyK-bTBq5WbIonaIQqaMbKqRoPDljWBp0yefR7WDr43bRTKPPy5KJvfj6eXMnjhP-UyPvMWh37QmJlMKoaMp78jR093JO4y4Ldj4oxJp8eWJLD_KI5tILabDvnh-rjMIC_hMr8-4CXKKOLVbj_tp7keq8CDRJNDj8-0lJpQpDDaKjeQpOG2PnDKto2y5jHhpLJyP-OhURH22ofhJk-WRjpsIJMyMFWbT8ULf5pB6DLaKviahRjBMb1SqRDBT5h2M4qMxtOLR3pWDTm_q5TtUt5OCFljTubD55LeHRf-b-XKCJ0X458HJOoDDvo0xvcy4LdjGK8J4-e0CQf-fJJQJ7VqPJJb5bPXx7W3-Aq54R0-JRQ54njL4bjVUTk-POlQfbQ0M6OqP-jW5Ta-qI-HR7JOpvsbUnxyhLTQRPH-Rv92DQMVU52QqcqEIQHQT3mDUThDHt8t58qtRKs3bRVKbk_HJRY2Jo_q4tehHRgBCr9WDTm_Do50CD2Jfo63l3jj-4DKp3mL4vOKtjH-pPKKR7ljhvJeb7cK4PN5n6WJfcK3mkjbpnzfn02OpjP3xuKqt4syPRrKMRnWNTrKfA-b4ncjRcTehoM3xI8LNj405OTt2LEoD0hJI_aMItrKPnt5KCehxv02tQba5vXsJrHbIv48-5Oy4oThfvB3lJyQfTfQjQRBn7E--JTepvoD-Jc3Mkjhhj0bMAL-m5L_bbj2MQkeq8CQft20b0EeMtjBbQa3HnTbJ7jWhk2ep72y5jvQlRX5q79atTMfNTJ-qcH0KQpsIJMDUC0-nDSHHKftj-H3J; delPer=0; PSINO=6; locale=zh; BDORZ=B490B5EBF6F3CD402E515D22BCDA1598; yjs_js_security_passport=34b76f68e38b6f0cff3f46a94c560c5008cf4aaa_1566789904_js; BDUSS=hYNjJUM003dmJpYVFmbEg5ZlU1elAxYTUtcFFsUHlRMW1yMURDdnFXcjhCSXRkRVFBQUFBJCQAAAAAAAAAAAEAAAAacs0rcXE4NDEwMjEyMzMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAPx3Y138d2NdT2; pan_login_way=1; STOKEN=c6980b1867f6728a21e70d0c71555dd913d53d827a05f6ce23effc16dc6635a7; SCRC=8979d11dd88e506a7fb01c2d9367286e; cflag=13%3A3; __tins__19988117=%7B%22sid%22%3A%201566799742139%2C%20%22vd%22%3A%2010%2C%20%22expires%22%3A%201566803287211%7D; __51laig__=13; Hm_lpvt_7a3960b6f067eb0085b7f96ff5e660b0=1566801487; PANPSC=15271988661739914446%3AKkwrx6t0uHA138PCbLAhmZgtJeEFa7WQn2093ZBO54R3NgvzfDuyDEci2O0YbEjppgHefbpWnyajax%2Bw%2FvS%2BovAfl5I%2Fit1ce7PrVlGR3H9hBaVJALTtttu1DRmBcCJyvHPKVnDffsp2TQaGfItAk1vKX4DMNpv60PK5REkUBNwmU589kUerfmRQlJOOd4%2FA625AVJzIhX8%3D");
//		cfg.setSaveFileName("vms.exe");

		String bdUrl = BaiduPanUtils.getDownloadLinkWithRESTApi(path, cfg.getHeaders());
		System.out.println("百度网盘下载链接解析为：" + bdUrl);
		cfg.setUrl(bdUrl);
		
		DownloaderManager d = new DownloaderManager(cfg);
		d.start();
		d.printProgressBar();
	}
	
	
	private void testNormal(String url) throws IOException {
		Config cfg = new Config();
		cfg.setThreadCount(3);
		cfg.setUrl(url);
		cfg.setSaveFileName("vms.exe");
		
		DownloaderManager d = new DownloaderManager(cfg);
		d.start();
		d.printProgressBar();
	}
	
	private void testWithCookie() throws IOException {
		Config cfg = new Config();
		cfg.setThreadCount(3);
		cfg.setUrl(url);
		cfg.setSaveFileName("bd_logo_2.png");
		cfg.addHeader("Cookie","BAIDUID=90584202C7FEEA601CCC29EC5914E2ED:FG=1; BIDUPSID=90584202C7FEEA601CCC29EC5914E2ED; PSTM=1541851242; PANWEB=1; BDUSS=4tOEFYWWVnaDlBRnFKS2hPdzN1R05OcDFLWXFMaUthcnBnMDBIcE1nZEIxUGhjRVFBQUFBJCQAAAAAAAAAAAEAAAAacs0rcXE4NDEwMjEyMzMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEFH0VxBR9FceG; MCITY=-%3A; STOKEN=9e231f7aeb18894a55973e70821ca11f68a408999ed1b26c89d2add0c733ca41; SCRC=86f4d1a38916acf0cd02d0b1fc91ae48; BDORZ=B490B5EBF6F3CD402E515D22BCDA1598; yjs_js_security_passport=a43db427bac7a401c0124d8b86c896603fb46944_1562639833_js; H_PS_PSSID=1445_21096_29238_28518_29099_28830_29221_26350_20719; BCLID=11483937944457378017; BDSFRCVID=7R-OJeC627gmFN6w5KjST4j7lM14mynTH6ao8N0Mg2TfFlqwOjsTEG0PHf8g0KubTBBhogKKLgOTHULF_2uxOjjg8UtVJeC6EG0P3J; H_BDCLCKID_SF=tJkOVI0KtIP3eJbGq6_a-n0eqxby26PfLtj9aJ5nJD_ben6DX6jbjfPq0n8JajbQBRCJW4T8QpP-HJA9X63YbTD7b4jQbRb92j7nKl0MLpntbb0xynoD3MDYMMnMBMnr52OnaU513fAKftnOM46JehL3346-35543bRTohFLK-oj-D-GD6AB3e; ZD_ENTRY=empty; delPer=0; PSINO=3; BDRCVFR[dG2JNJb_ajR]=mk3SLVN4HKm; BDRCVFR[-pGxjrCMryR]=mk3SLVN4HKm; BDRCVFR[tox4WRQ4-Km]=mk3SLVN4HKm; Hm_lvt_7a3960b6f067eb0085b7f96ff5e660b0=1560769422,1561337628,1562549260,1562656029; cflag=13%3A3; __51cke__=; __tins__19988117=%7B%22sid%22%3A%201562656057866%2C%20%22vd%22%3A%202%2C%20%22expires%22%3A%201562657857871%7D; __51laig__=2; Hm_lpvt_7a3960b6f067eb0085b7f96ff5e660b0=1562656058; PANPSC=2125602179511439317%3AKkwrx6t0uHA138PCbLAhmZgtJeEFa7WQn2093ZBO54R3NgvzfDuyDEci2O0YbEjp2ux451Gf2yyNzzYrP5S%2FBPAfl5I%2Fit1ce7PrVlGR3H9hBaVJALTtttu1DRmBcCJyvHPKVnDffsp2TQaGfItAk1vKX4DMNpv60PK5REkUBNwmU589kUerfmRQlJOOd4%2FA625AVJzIhX8%3D");

		DownloaderManager d = new DownloaderManager(cfg);
		d.start();
		d.printProgressBar();
	}
	
	private void testWithProxy() throws IOException {
		Config cfg = new Config();
		cfg.setThreadCount(3);
		cfg.setUrl(url);
		cfg.setSaveFileName("bd_logo_3.png");
		cfg.setProxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("host", 80)));

		DownloaderManager d = new DownloaderManager(cfg);
		d.start();
		d.printProgressBar();
	}

}
