# FileHttpServer
基于Java 原生TCPSocket实现的小型服务器Demo，能够访问开放目录下的文件夹及文件内容，支持断点续传。   
文件大小约1M大小哦。

## 特性
* 参照Http协议进行实现，
* 支持Http Basic授权认证（Demo级实现，账户名admin/密码admin，不保证安全）
* 支持Http Chunked传输方式
* 支持文件断点续传（流媒体拖拽播放也是可以的哦~(๑•̀ㅂ•́)و✧）

## 配置
配置从同级目录下的```app.config```中加载。

| 属性  | 值 |
| ------------- | ------------- |
| nicelee.server.port  | 服务器监听端口  |
| nicelee.server.fixedPoolSize  |目前使用fixedThreadPool管理Socket处理线程，可以看作是最大TCP并发连接数  |
| nicelee.server.source  | 目的文件目录 |


示例如下： 
![](https://raw.githubusercontent.com/nICEnnnnnnnLee/FileHttpServer/master/source/config.png)  

## 预览
* 访问路径  
![](https://raw.githubusercontent.com/nICEnnnnnnnLee/FileHttpServer/master/source/preview.png)  

* ```/source```目录下需要授权（Demo，可以是其它逻辑）  
![](https://raw.githubusercontent.com/nICEnnnnnnnLee/FileHttpServer/master/source/preview-auth.png)  

* 认证失败  
![](https://raw.githubusercontent.com/nICEnnnnnnnLee/FileHttpServer/master/source/preview-401.png)  

* 认证成功  
![](https://raw.githubusercontent.com/nICEnnnnnnnLee/FileHttpServer/master/source/preview-auth_ok.png)  

* 禁止读取.txt文件（Demo，可以是其它逻辑）  
![](https://raw.githubusercontent.com/nICEnnnnnnnLee/FileHttpServer/master/source/preview-403.png)  

## 注意  
* 该作品为http协议的学习实现，作者不对程序的安全性做任何保证！  

## 其它  
* **下载地址**: [https://github.com/nICEnnnnnnnLee/FileHttpServer/releases](https://github.com/nICEnnnnnnnLee/FileHttpServer/releases)
* **GitHub**: [https://github.com/nICEnnnnnnnLee/FileHttpServer](https://github.com/nICEnnnnnnnLee/FileHttpServer)  
* **Gitee码云**: [https://gitee.com/NiceLeee/FileHttpServer](https://gitee.com/NiceLeee/FileHttpServer)  
* **LICENSE**: [Apache License v2.0](https://www.apache.org/licenses/LICENSE-2.0.html)




