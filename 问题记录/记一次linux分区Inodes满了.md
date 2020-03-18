### 记一次linux分区Inodes满了

过了一个年假后，公司某地产品数据突然断了，远程排查，发现IPS程序起不来了，crontab定时任务也没有执行，通过systemctl status ips查看错误日志

![image-20200318160253382](C:\Users\jiang\AppData\Roaming\Typora\typora-user-images\image-20200318160253382.png)

但是查看文件系统情况没有问题，空间还是充足的，不至于跑不起来程序

![image-20200318160332823](C:\Users\jiang\AppData\Roaming\Typora\typora-user-images\image-20200318160332823.png)

那么空间充足，为什么还有问题，其实使用df -i 就能看出端倪。

![image-20200318160407478](C:\Users\jiang\AppData\Roaming\Typora\typora-user-images\image-20200318160407478.png)



很显然，该分区Inodes满了，在linux中，文件数据都储存在"块"中，我们还必须找到一个地方储存文件的元信息，比如文件的创建者、文件的创建日期、文件的大小等等。这种储存文件元信息的区域就叫做inode，中文译名为"索引节点"。这里因为/var目录存在大量小文件，导致Inodes用满了，所以出现了上述情况。