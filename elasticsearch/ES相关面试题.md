ES相关面试题：

https://blog.csdn.net/laoyang360/article/details/86558214

https://zhuanlan.zhihu.com/p/99539109



调优方面：

1、每天凌晨定时对索引做force_merge操作，以释放空间

2、采取冷热分离机制，热数据存储到SSD，提高检索效率；冷数据定期进行shrink（合并分片数）操作，以缩减存储；

3、某些字段设置index:flase，不做检索使用，当该文档被查出来，可以看到该字段

4、refresh_interval参数设置，实时要求不高

5、jvm内存设置

6、最大句柄数



官方给出了各种提高 Bulk 性能的方法，例如：增大刷新时间（index.refresh_interval）；设置副本数为0（index.number_of_replicas）；关闭swapping（sudo swapoff -a）；使用随机id等。



查询调优：

- 1）禁用wildcard；
- 2）禁用批量terms（成百上千的场景）；
- 3）充分利用倒排索引机制，能keyword类型尽量keyword；
- 4）数据量大时候，可以先基于时间敲定索引再检索；
- 5）设置合理的路由机制。



集群：

master防止脑裂，参数设为master/2+1，大于这个数才可以组成集群

master和data节点分开，请求大时数据节点压力也大

master：维护整个集群的状态

client：只处理用户请求，请求转发 负载均衡



#### 倒排索引

倒排索引，是通过分词策略，形成了词和文章的映射关系表，这种词典+映射表即为倒排索引。

倒排索引的底层实现是基于：FST（Finite State Transducer）数据结构

lucene从4+版本后开始大量使用的数据结构是FST。FST有两个优点：

- 1）空间占用小。通过对词典中单词前缀和后缀的重复利用，压缩了存储空间；
- 2）查询速度快。O(len(str))的查询时间复杂度。





写数据：

收到请求，使用文档_id确定文档所属分片，请求转发所属分片，主分片写入成功将请求转发到副本分片，等待结果。所有写入成功后向协调节点报告成功，具体先写入buffer和log文件，定时reflush到文件系统缓存，默认1s，30分钟flush到磁盘。

读数据：

query and  fetch

每个分片进行本地查询，返回有序结果到协调节点，协调节点进行全局排序



集群角色：

master    data    client







### 1、用bulk批量写入

你如果要往es里面灌入数据的话，那么根据你的业务场景来，如果你的业务场景可以支持让你将一批数据聚合起来，一次性写入es，那么就尽量采用bulk的方式，每次批量写个几百条这样子。

bulk批量写入的性能比你一条一条写入大量的document的性能要好很多。但是如果要知道一个bulk请求最佳的大小，需要对单个es node的单个shard做压测。先bulk写入100个document，然后200个，400个，以此类推，每次都将bulk size加倍一次。如果bulk写入性能开始变平缓的时候，那么这个就是最佳的bulk大小。并不是bulk size越大越好，而是根据你的集群等环境具体要测试出来的，因为越大的bulk size会导致内存压力过大，因此最好一个请求不要发送超过10mb的数据量。

先确定一个是bulk size，此时就尽量是单线程，一个es node，一个shard，进行测试。看看单线程最多一次性写多少条数据，性能是比较好的。

### 2、使用多线程将数据写入es

单线程发送bulk请求是无法最大化es集群写入的吞吐量的。如果要利用集群的所有资源，就需要使用多线程并发将数据bulk写入集群中。为了更好的利用集群的资源，这样多线程并发写入，可以减少每次底层磁盘fsync的次数和开销。首先对单个es节点的单个shard做压测，比如说，先是2个线程，然后是4个线程，然后是8个线程，16个，每次线程数量倍增。一旦发现es返回了TOO_MANY_REQUESTS的错误，JavaClient也就是EsRejectedExecutionException。此时那么就说明es是说已经到了一个并发写入的最大瓶颈了，此时我们就知道最多只能支撑这么高的并发写入了。

### 3、增加refresh间隔

默认的refresh间隔是1s，用index.refresh_interval参数可以设置，这样会其强迫es每秒中都将内存中的数据写入磁盘中，创建一个新的segment file。正是这个间隔，让我们每次写入数据后，1s以后才能看到。但是如果我们将这个间隔调大，比如30s，可以接受写入的数据30s后才看到，那么我们就可以获取更大的写入吞吐量，因为30s内都是写内存的，每隔30s才会创建一个segment file。

### 4、禁止refresh和replia

如果我们要一次性加载大批量的数据进es，可以先禁止refresh和replia复制，将index.refresh_interval设置为-1，将index.number_of_replicas设置为0即可。这可能会导致我们的数据丢失，因为没有refresh和replica机制了。但是不需要创建segment file，也不需要将数据replica复制到其他的replica shasrd上面去。此时写入的速度会非常快，一旦写完之后，可以将refresh和replica修改回正常的状态。

### 5、禁止swapping交换内存

如果要将es jvm内存交换到磁盘，再交换回内存，大量磁盘IO，性能很差

### 6、给filesystem cache更多的内存

filesystem cache被用来执行更多的IO操作，如果我们能给filesystemcache更多的内存资源，那么es的写入性能会好很多。

### 7、使用自动生成的id

如果我们要手动给es document设置一个id，那么es需要每次都去确认一下那个id是否存在，这个过程是比较耗费时间的。如果我们使用自动生成的id，那么es就可以跳过这个步骤，写入性能会更好。对于你的业务中的表id，可以作为es document的一个field。

### 8、用性能更好的硬件

我们可以给filesystem cache更多的内存，也可以使用SSD替代机械硬盘，避免使用NAS等网络存储，考虑使用RAID 0来条带化存储提升磁盘并行读写效率，等等。

### 9、index buffer

如果我们要进行非常重的高并发写入操作，那么最好将index buffer调大一些，indices.memory.index_buffer_size，这个可以调节大一些，设置的这个index buffer大小，是所有的shard公用的，但是如果除以shard数量以后，算出来平均每个shard可以使用的内存大小，一般建议，但是对于每个shard来说，最多给512mb，因为再大性能就没什么提升了。es会将这个设置作为每个shard共享的index buffer，那些特别活跃的shard会更多的使用这个buffer。默认这个参数的值是10%，也就是jvm heap的10%，如果我们给jvmheap分配10gb内存，那么这个index buffer就有1gb，对于两个shard共享来说，是足够的了