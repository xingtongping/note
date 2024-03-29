### kafka读书笔记

#### 1、操作系统选型

Linux上部署kafka要比Windows部署能够得到更高效的IO处理性能

kafka应用必然要大量通过网络与磁盘进行数据传输，而大部分这样的操作都是通过java的FileChannel.transferTo方法实现，在linux平台该方法会调用sendfile系统调用，即采用了linux提供的零拷贝技术。这种技术可以减少甚至完全规避不必要的CPU数据拷贝操作。





#### 2、磁盘容量规划

假设每天产生1E条消息，每条消息保存两份并且保留一周时间，平均一条消息大小是1KB。

1E * 2 * 1KB /1000 /1000 =200G的磁盘空间，最好额外10%磁盘空间用于保存其他数据文件，如索引文件等。

因为每天大概210G，七天即大概1.5T，如果启用消息压缩，估计一个平均压缩比，如0.5，那么整体大概就是0.75TB。



磁盘规划因素：

新增消息数

消息留存时间

平均消息大小

副本数

是否启用压缩





#### 3、内存规划

kafka消息缓存的地方是操作系统的页缓存。consumer读取消息时会首先在该区域读，命中则完全不消耗IO，

内存规划建议：

尽量分配更多的内存给操作系统页缓存

不要为broker设置过大的堆内存，最好不要超过6GB

页缓存大小至少要大于一个日志段的大小



#### 4、宽带规划

1GB/s    10GB/s   千兆和万兆 

1Gb/s  *  0.7 = 710Mb/s       kafka专属使用，70%的宽带资源，这是kafka使用的最高宽带

保守估计再截取1/3 = 240Mb/s    一般宽带不会用满

如果要1小时处理1TB数据，即每秒处理292MB左右，也就是2336Mb，那么2336/240=10台，若副本数为2，即20台broker机器。







#### 5、吞吐量测试

kafka提供性能吞吐量测试脚本

kafka-producer-perf-test

kafka-consumer-perf-test





#### 6、broker端参数

broker.id   

唯一标识每个broker



log.dirs    

kafka消息持久化目录，默认/tmp/kafka-logs



listeners    

broker监听的csv列表，该参数主要用于客户端连接broker使用，如果不指定，则表示绑定网卡，如果主机名是0.0.0.0,则表示绑定所有网卡



advertised.listeners   

该参数主要用于IAAS环境，比如云上的机器通常配有多块网卡（私有和共有网卡），listeners绑定的是默认网卡，默认网卡通常绑定私网IP的。



unclean.leader.election.enable  

默认false，kafka不允许从剩下存活的非ISR副本选择一个当leader，如果为true，可让kafka继续提供服务，但是会造成数据丢失。



delete.topic.enable  

是否允许kafka删除topic



log.retention.  {hours l minutes l ms}    

消息留存时间



log.retention.bytes          

默认-1，不会根据消息大小来删除数据。



min.insync.replics      

配合ack=-1时才有意义，它指定了broker端必须成功响应client消息发送的最少副本数。



num.network.threads  

 默认3，控制broker在后台用于处理网络请求的线程数



num.io.threads     

控制broker端实际处理网络请求的线程数



message.max.bytes    

 能够接收最大消息大小，默认977KB，不到1MB





#### 7、topic级别参数

delete.retention.ms   

每个topic可以设置自己日志留存事件



max.message.bytes

每个topic最大消息尺寸



retention.bytes

每个topic的日志留存尺寸



#### 8、JVM参数

kafka broker主要使用堆外内存，即大量使用操作系统页缓存，因此其实并不需要为JVM分配太多内存，实际中，一般broker设置不超过6GB的堆空间





### kafka设计原理

消息设计

集群管理

副本与ISR机制

日志存储

请求处理协议

controller设计

broker状态机

broker通信原理





#### 消息设计

kafka的实现方式本质上是使用Java NIO的ByteBuffer来保存消息，同时依赖文件系统提供的页缓存机制，而非依赖java堆内存。



#### 集群管理

kafka支持自动化的服务发现与成员管理，依赖zookeeper实现。每当一个broker启动时，它会将自己注册到zookeeper下的一个节点。kafka利用zookeeper的临时节点来管理broker的生命周期。broker启动时在zookeeper中创建临时节点，同时还会创建一个监听器监听该临时节点的状态，一旦启动，监听器会自动同步整个集群信息到broker上，而一旦broker崩溃，临时节点被删除。



#### 副本与ISR设计



1、follow副本同步

副本均匀分配到所有的broker上，leader对外提供服务，follow被动向leader请求数据，保持与leader同步。

对于落后leader太多的follow，他们没有资格竞选leader，如果允许，将造成数据丢失。鉴于这个原因，kafka引入了ISR概念。



ISR:即kafka集群动态维护的一组同步副本集合，每个topic分区都有自己的ISR列表





分区日志几个概念：

起始位移：表示该副本当前所包含第一条消息的offset

高水印值（HW）：最新一条已提交消息的位移，只有leader副本的HW值才能决定client能看到的消息数量。

日志末端位移（LED）：副本日志下一条待写入消息的offset,事实上当所有副本都更新了对应的LEO之后，leader副本才会向右移动HW值表明消息写入成功。

![1566813820431](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\1566813820431.png)



2、ISR设计

0.900版本之前，kafka提供一个参数replica.lag.max.message,用于控制follow副本落后leader副本。一旦超过这个消息树，则视为不同步状态，提出ISR



导致follow与leader不同步的原因：

1、请求速度跟不上

2、进程卡住，频繁GC或程序bug，follow无法向leader请求数据

3、新创建的副本，追赶时间不同步



0.99版本之前存在缺陷：假设producer突然发起一波消息生产瞬时高峰流量，比如producer一次性发送4条消息，这时replica.lag.max.messages设置为4，leader收到消息后，follow还为开始拉去，此时，这两个follow副本会认为与leader副本不同步，从而被踢出ISR，会导致follow副本一直循环踢出，加入ISR.。而且，这个参数的值是全局的，所有的topic都会受影响。



0.900版本之后，kafka去掉了replica.lag.max.messages参数，改用replic.lag.time.max.ms,即follow落后leader副本的时间间隔，默认为10s。这段时间内follow落后leader的持续性超过这个参数值，就认为不同步。





LEO和HW

LEO：指向下一条代写入的消息，也就是说，LEO指向的位置是没有消息的。

HW：HW指向的是实实在在的消息。

消费者无法消费分区leader副本上那些位移大于分区HW的消息。分区HW就是leader副本的HW值。



LEO更新机制：

follow副本会不停地向leader副本所在的broker发送FETCH请求。

kafka设计了两套follow副本的LEO属性。一套保存在follow副本的broker缓存上，另一份保存在leader副本的缓存上，leader副本上缓存了该分区下所有follow副本的LEO属性（包括自己）。

1、follow副本上LEO何时更新？

follow FETCH请求拿到数据后，向底层log写数据，从而自动更新LEO值，每写入一条，LEO值+1。

2、leader端follow副本LEO何时更新？

leader接收到follow FETCH请求，在返回数据给follow之前先去更新LEO。

3、leader自己的LEO何时更新？

leader写log时就会自动更新自己的LEO值。