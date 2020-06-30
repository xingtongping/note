# HBase集群环境配置

[一、集群规划](https://github.com/heibaiying/BigData-Notes/blob/master/notes/installation/HBase集群环境搭建.md#一集群规划)
[二、前置条件](https://github.com/heibaiying/BigData-Notes/blob/master/notes/installation/HBase集群环境搭建.md#二前置条件)
[三、集群搭建](https://github.com/heibaiying/BigData-Notes/blob/master/notes/installation/HBase集群环境搭建.md#三集群搭建)
    [3.1 下载并解压](https://github.com/heibaiying/BigData-Notes/blob/master/notes/installation/HBase集群环境搭建.md#31-下载并解压)
    [3.2 配置环境变量](https://github.com/heibaiying/BigData-Notes/blob/master/notes/installation/HBase集群环境搭建.md#32-配置环境变量)
    [3.3 集群配置](https://github.com/heibaiying/BigData-Notes/blob/master/notes/installation/HBase集群环境搭建.md#33-集群配置)
    [3.4 HDFS客户端配置](https://github.com/heibaiying/BigData-Notes/blob/master/notes/installation/HBase集群环境搭建.md#34-HDFS客户端配置)
    [3.5 安装包分发](https://github.com/heibaiying/BigData-Notes/blob/master/notes/installation/HBase集群环境搭建.md#35-安装包分发)
[四、启动集群](https://github.com/heibaiying/BigData-Notes/blob/master/notes/installation/HBase集群环境搭建.md#四启动集群)
    [4.1 启动ZooKeeper集群](https://github.com/heibaiying/BigData-Notes/blob/master/notes/installation/HBase集群环境搭建.md#41-启动ZooKeeper集群)
    [4.2 启动Hadoop集群](https://github.com/heibaiying/BigData-Notes/blob/master/notes/installation/HBase集群环境搭建.md#42-启动Hadoop集群)
    [4.3 启动HBase集群](https://github.com/heibaiying/BigData-Notes/blob/master/notes/installation/HBase集群环境搭建.md#43-启动HBase集群)
    [4.5 查看服务](https://github.com/heibaiying/BigData-Notes/blob/master/notes/installation/HBase集群环境搭建.md#45-查看服务)

## 一、集群规划

这里搭建一个 3 节点的 HBase 集群，其中三台主机上均为 `Regin Server`。同时为了保证高可用，除了在 hadoop001 上部署主 `Master` 服务外，还在 hadoop002 上部署备用的 `Master` 服务。Master 服务由 Zookeeper 集群进行协调管理，如果主 `Master` 不可用，则备用 `Master` 会成为新的主 `Master`。

[![img](https://camo.githubusercontent.com/b72f7a1904af343ecf5ce521abf33286332b132d/68747470733a2f2f67697465652e636f6d2f68656962616979696e672f426967446174612d4e6f7465732f7261772f6d61737465722f70696374757265732f6862617365e99b86e7bea4e8a784e588922e706e67)](https://camo.githubusercontent.com/b72f7a1904af343ecf5ce521abf33286332b132d/68747470733a2f2f67697465652e636f6d2f68656962616979696e672f426967446174612d4e6f7465732f7261772f6d61737465722f70696374757265732f6862617365e99b86e7bea4e8a784e588922e706e67)

## 二、前置条件

HBase 的运行需要依赖 Hadoop 和 JDK(`HBase 2.0+` 对应 `JDK 1.8+`) 。同时为了保证高可用，这里我们不采用 HBase 内置的 Zookeeper 服务，而采用外置的 Zookeeper 集群。相关搭建步骤可以参阅：

- [Linux 环境下 JDK 安装](https://github.com/heibaiying/BigData-Notes/blob/master/notes/installation/Linux下JDK安装.md)
- [Zookeeper 单机环境和集群环境搭建](https://github.com/heibaiying/BigData-Notes/blob/master/notes/installation/Zookeeper单机环境和集群环境搭建.md)
- [Hadoop 集群环境搭建](https://github.com/heibaiying/BigData-Notes/blob/master/notes/installation/Hadoop集群环境搭建.md)

## 三、集群搭建

### 3.1 下载并解压

下载并解压，这里我下载的是 CDH 版本 HBase，下载地址为：http://archive.cloudera.com/cdh5/cdh/5/

```
# tar -zxvf hbase-1.2.0-cdh5.15.2.tar.gz
```

### 3.2 配置环境变量

```
# vim /etc/profile
```

添加环境变量：

```
export HBASE_HOME=usr/app/hbase-1.2.0-cdh5.15.2
export PATH=$HBASE_HOME/bin:$PATH
```

使得配置的环境变量立即生效：

```
# source /etc/profile
```

### 3.3 集群配置

进入 `${HBASE_HOME}/conf` 目录下，修改配置：

#### 1. hbase-env.sh

```
# 配置JDK安装位置
export JAVA_HOME=/usr/java/jdk1.8.0_201
# 不使用内置的zookeeper服务
export HBASE_MANAGES_ZK=false
```

#### 2. hbase-site.xml

```
<configuration>
    <property>
        <!-- 指定 hbase 以分布式集群的方式运行 -->
        <name>hbase.cluster.distributed</name>
        <value>true</value>
    </property>
    <property>
        <!-- 指定 hbase 在 HDFS 上的存储位置 -->
        <name>hbase.rootdir</name>
        <value>hdfs://hadoop001:8020/hbase</value>
    </property>
    <property>
        <!-- 指定 zookeeper 的地址-->
        <name>hbase.zookeeper.quorum</name>
        <value>hadoop001:2181,hadoop002:2181,hadoop003:2181</value>
    </property>
</configuration>
```

#### 3. regionservers

```
hadoop001
hadoop002
hadoop003
```

#### 4. backup-masters

```
hadoop002
```

`backup-masters` 这个文件是不存在的，需要新建，主要用来指明备用的 master 节点，可以是多个，这里我们以 1 个为例。

### 3.4 HDFS客户端配置

这里有一个可选的配置：如果您在 Hadoop 集群上进行了 HDFS 客户端配置的更改，比如将副本系数 `dfs.replication` 设置成 5，则必须使用以下方法之一来使 HBase 知道，否则 HBase 将依旧使用默认的副本系数 3 来创建文件：

> 1. Add a pointer to your `HADOOP_CONF_DIR` to the `HBASE_CLASSPATH` environment variable in *hbase-env.sh*.
> 2. Add a copy of *hdfs-site.xml* (or *hadoop-site.xml*) or, better, symlinks, under *${HBASE_HOME}/conf*, or
> 3. if only a small set of HDFS client configurations, add them to *hbase-site.xml*.

以上是官方文档的说明，这里解释一下：

**第一种** ：将 Hadoop 配置文件的位置信息添加到 `hbase-env.sh` 的 `HBASE_CLASSPATH` 属性，示例如下：

```
export HBASE_CLASSPATH=usr/app/hadoop-2.6.0-cdh5.15.2/etc/hadoop
```

**第二种** ：将 Hadoop 的 `hdfs-site.xml` 或 `hadoop-site.xml` 拷贝到 `${HBASE_HOME}/conf `目录下，或者通过符号链接的方式。如果采用这种方式的话，建议将两者都拷贝或建立符号链接，示例如下：

```
# 拷贝
cp core-site.xml hdfs-site.xml /usr/app/hbase-1.2.0-cdh5.15.2/conf/
# 使用符号链接
ln -s   /usr/app/hadoop-2.6.0-cdh5.15.2/etc/hadoop/core-site.xml
ln -s   /usr/app/hadoop-2.6.0-cdh5.15.2/etc/hadoop/hdfs-site.xml
```

> 注：`hadoop-site.xml` 这个配置文件现在叫做 `core-site.xml`

**第三种** ：如果你只有少量更改，那么直接配置到 `hbase-site.xml` 中即可。

### 3.5 安装包分发

将 HBase 的安装包分发到其他服务器，分发后建议在这两台服务器上也配置一下 HBase 的环境变量。

```
scp -r /usr/app/hbase-1.2.0-cdh5.15.2/  hadoop002:usr/app/
scp -r /usr/app/hbase-1.2.0-cdh5.15.2/  hadoop003:usr/app/
```

## 四、启动集群

### 4.1 启动ZooKeeper集群

分别到三台服务器上启动 ZooKeeper 服务：

```
 zkServer.sh start
```

### 4.2 启动Hadoop集群

```
# 启动dfs服务
start-dfs.sh
# 启动yarn服务
start-yarn.sh
```

### 4.3 启动HBase集群

进入 hadoop001 的 `${HBASE_HOME}/bin`，使用以下命令启动 HBase 集群。执行此命令后，会在 hadoop001 上启动 `Master` 服务，在 hadoop002 上启动备用 `Master` 服务，在 `regionservers` 文件中配置的所有节点启动 `region server` 服务。

```
start-hbase.sh
```

### 4.5 查看服务

访问 HBase 的 Web-UI 界面，这里我安装的 HBase 版本为 1.2，访问端口为 `60010`，如果你安装的是 2.0 以上的版本，则访问端口号为 `16010`。可以看到 `Master` 在 hadoop001 上，三个 `Regin Servers` 分别在 hadoop001，hadoop002，和 hadoop003 上，并且还有一个 `Backup Matser` 服务在 hadoop002 上。

[![img](https://camo.githubusercontent.com/40f38a6f79ba0648f08d68be74d6b2168b7ff5f5/68747470733a2f2f67697465652e636f6d2f68656962616979696e672f426967446174612d4e6f7465732f7261772f6d61737465722f70696374757265732f68626173652de99b86e7bea4e690ade5bbba312e706e67)](https://camo.githubusercontent.com/40f38a6f79ba0648f08d68be74d6b2168b7ff5f5/68747470733a2f2f67697465652e636f6d2f68656962616979696e672f426967446174612d4e6f7465732f7261772f6d61737465722f70696374757265732f68626173652de99b86e7bea4e690ade5bbba312e706e67)



hadoop002 上的 HBase 出于备用状态：



[![img](https://camo.githubusercontent.com/76e750f5bfc92a4f2ebaf97d12747bdd12f0ed0b/68747470733a2f2f67697465652e636f6d2f68656962616979696e672f426967446174612d4e6f7465732f7261772f6d61737465722f70696374757265732f68626173652de99b86e7bea4e690ade5bbba322e706e67)](https://camo.githubusercontent.com/76e750f5bfc92a4f2ebaf97d12747bdd12f0ed0b/68747470733a2f2f67697465652e636f6d2f68656962616979696e672f426967446174612d4e6f7465732f7261772f6d61737465722f70696374757265732f68626173652de99b86e7bea4e690ade5bbba322e706e67)