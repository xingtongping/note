### 记一次elasticsearch打开文件数过大



问题：公司最近卖出的产品部署上线后，跑了一段时间后ES都会出现问题，Too many open files，进而引发其他程序不正常，太恶心人了。以前天真以为设置了/etc/security/limits.conf，就可以了

```
elasticsearch        hard     nofile         65535
elasticsearch        soft     nofile         65535
```



最近客户又疯狂骚扰，太难顶了，于是继续上网看一些资料，发现能查看ES设置的最大打开文件数，马上打开xshell连接到测试环境，结果一看，跟我配置的值不一样，其实心里挺高兴了，因为感觉找出了病因。。。

curl  es:9200/_nodes/stats?pretty|grep max_file

![image-20200119115409392](C:\Users\jiang\AppData\Roaming\Typora\typora-user-images\image-20200119115409392.png)



后来意外发现使用命令bin/elasticsearch -d 启动后，这个值是和我设定的吻合，于是想到了我们正式环境是使用systemctl  start  elasticsearch 启动的，应该是这个自启动脚本导致的问题。

百度了一番，原来systemctl service可以设置限制，在对应service文件添加

```
[Service]
LimitCORE=infinity
LimitNOFILE=100000
LimitNPROC=100000
```



顺便提一下，CentOS7自带的/etc/security/limits.d/20-nproc.conf文件，里面默认设置了非root用户的最大进程数为4096







查看进程当前设置的max_file_descriptors

cat /proc/进程号/limits



下面文章可能对你有帮助

https://www.jianshu.com/p/ea32136d4074
https://blog.csdn.net/m0_37911384/article/details/102567572
