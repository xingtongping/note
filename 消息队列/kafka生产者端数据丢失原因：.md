kafka生产者端数据丢失原因：

```
java版本produce采用异步机制，send仅仅把消息放到缓冲区中，由一个专属的IO线程负责从缓冲区提取消息并封装进消息batch中，然后发送出去，。若IO发送之前produce崩溃，则缓冲区消息全部丢失
```

在配置文件中设置成不限制阻塞超时的时间，也就说让生产端一直阻塞



kafka生产者端消息乱序：

```
reocrd1和record2发送到相同分区。record1未发送成功，配置了重试机制,以及配置了max.in.flight.requests.per.connection大于1（默认5），那么重试后record1日志位置反而位于record2之后，造成消息乱序
```



producer端无消息丢失配置

```
block.on.buffer.full = true     kafka0.10之后转而设置m a x . b l o c k . m s
acks = all or -1
retries = Integer.MAX_ VALUE
max.in.flight.requests.per.connection= 1     防止同分区下消息乱序问题
使用带回调机制的send发送消息，即KafkaProducer.send(send,callback)
Callback逻辑中显式地立即关闭producer,使用close
```

broker端无消息丢失配置

```
unclean.leader.election. enable=  false
replication.factor = 3
min.insync.replicas = 2
replication.factor > min.insync.replicas
enable.auto.commit= false
```



kafka是把消息写入操作系统的页缓存，同样在读取消息会首先从OS页缓存读取，命中便把消息直接发送到网络socket上，这就是大名鼎鼎的零拷贝技术。



kafka高吞吐、低延时

```
1、大量使用操作系统页缓存，内存操作速度快且命中率高

2、kafka不直接参与物理I/O操作，而是交由擅长此事的操作系统来完成

3、采用追加写入方式，摒弃了缓慢的磁盘随机读写操作

4、使用以sendfile为代表的零拷贝技术加强网络间的数据传输效率
```

