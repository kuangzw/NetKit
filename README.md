# MultiThreadDownloader

#### 介绍
> 写这个项目的初衷：百度云盘下载限速，使用IDM下载可以提高速度但需要注册码（当然可以破解，但嫌麻烦），于是干脆自己写个多线程下载工具。

+ java多线程下载器（类似与IDM）
+ 可自定义线程数量，最多自从32个线程
+ 可设定cookie，这样就可以自由下载百度云里面的资源
+ 使用方法如下：

```
 =======================================================

 usage : java -jar MultiThreadDownloader.jar "<url>" <options>

 注意 : url中有 '&' 整个URL必须使用""包起来，否则dos下不能识别完整的url
 options :
 --help  帮助
 --headers <headerKey:headerValue>  添加请求头信息
 --cookie "<value>"  添加cookie信息
 --proxy <proxyhost:port>  设置socks代理
 --threadNum <number>  设置线程数量，默认3线程，最多32个线程
 --saveFileName <filename>  文件保存名称
 --printProgressBarDetail 控制台打印每个线程进度条

 =======================================================
 
```

+ 举例：

```
java -jar MultiThreadDownloader.jar https://download-ssl.firefox.com.cn/releases-sha2/full/zh-CN/Firefox-full-latest.exe
```

#### 截图
> 使用多线程下载和chrome浏览器下载速度对比

![Image text](https://gitee.com/kuangzw2/MultiThreadDownloader/raw/master/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20190710175005.png)

![Image text](https://gitee.com/kuangzw2/MultiThreadDownloader/raw/master/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20190710174934.png)
