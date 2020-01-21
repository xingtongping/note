### Redis几种集群方式：

#### 1、主从

配置：

```
主数据库不用配置，从redis的conf文件中可以加载从数据库的信息，也可以在启动时，
使用 redis-server --port 6380 --slaveof 127.0.0.1 6379
```



复制原理：

```
当从数据库启动时，会向主数据库发送sync命令，主数据库接收到sync后开始在后台保存快照rdb，在保存快照期间收到的命令缓存起来，当快照完成时，主数据库会将快照和缓存的命令一块发送给从**。复制初始化结束。
之后，主每收到1个命令就同步发送给从。
当出现断开重连后，2.8之后的版本会将断线期间的命令传给重数据库。增量复制

主从复制是乐观复制，当客户端发送写执行给主，主执行完立即将结果返回客户端，并异步的把命令发送给从，从而不影响性能。也可以设置至少同步给多少个从主才可写。
无硬盘复制:如果硬盘效率低将会影响复制性能，2.8之后可以设置无硬盘复制，repl-diskless-sync yes
```





#### 2、哨兵

当主数据库遇到异常中断服务后，开发者可以通过手动的方式选择一个从数据库来升格为主数据库，以使得系统能够继续提供服务。**然而整个过程相对麻烦且需要人工介入，难以实现自动化**。 为此，Redis 2.8中提供了哨兵工具来实现自动化的系统监控和故障恢复功能。

**哨兵的作用就是监控redis主、从数据库是否正常运行，主出现故障自动将从数据库转换为主数据库**。

配置：1主2从1哨兵

```
redis-server --port 6379 
redis-server --port 6380 --slaveof 192.168.0.167 6379 
redis-server --port 6381 --slaveof 192.168.0.167 6379

哨兵配置文件 sentinel.conf 
 sentinel monitor mymaster 192.168.0.167 6379  1
这里的1代表1个哨兵
```

*注：*
 配置哨兵监控一个系统时，**只需要配置其监控主数据库即可**，哨兵会自动发现所有复制该主数据库的从数据库

这样哨兵就能监控主6379和从6380、6381，一旦6379挂掉，哨兵就会在2个从中选择一个作为主，**根据优先级选，如果一样就选个id小的**，当6379再起来就作为从存在。



#### 3、分布式集群

`redis`要求至少三主三从共6个节点才能组成`redis`集群。

只需要将每个数据库节点的cluster-enable配置打开即可。



即使使用哨兵，redis每个实例也是全量存储，每个redis存储的内容都是完整的数据，浪费内存且有木桶效应。为了最大化利用内存，可以采用集群，就是分布式存储。即每台redis存储不同的内容。
 集群至少需要3主3从，且每个实例使用不同的配置文件，主从不用配置，集群会自己选。



redis六个配置文件：

```
redis-6379.conf`、`redis-6380.conf`、`redis-6381.conf`、`redis-6382.conf`、`redis-6383.conf`、`redis-6384.conf
```

```
 cluster-enabled yes  --开启集群
```

模板：

```
# redis后台运行
daemonize yes
# 绑定的主机端口
bind 127.0.0.1
# 数据存放目录
dir /usr/local/redis-cluster/data/redis-${自定义}
# 进程文件
pidfile /var/run/redis-cluster/${自定义}.pid
# 日志文件
logfile /usr/local/redis-cluster/log/${自定义}.log
# 端口号
port 6379
# 开启集群模式，把注释#去掉
cluster-enabled yes
# 集群的配置，配置文件首次启动自动生成
cluster-config-file /usr/local/redis-cluster/conf/${自定义}.conf
# 请求超时，设置10秒
cluster-node-timeout 10000
# aof日志开启，有需要就开启，它会每次写操作都记录一条日志
appendonly yes
```

集群的运行

```
#!/bin/bash
path=conf/
cd $path
#启动redis
redis-server redis-6379.conf
redis-server redis-6380.conf
redis-server redis-6381.conf
redis-server redis-6382.conf
redis-server redis-6383.conf
redis-server redis-6384.conf
# 创建cluster集群
redis-cli --cluster create 127.0.0.1:6379 127.0.0.1:6380 127.0.0.1:6381 127.0.0.1:6382 127.0.0.1:6383 127.0.0.1:6384 --cluster-replicas 1
```



```

```

