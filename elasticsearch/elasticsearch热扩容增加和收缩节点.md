### elasticsearch热扩容增加和收缩节点

实战版本：elasticsearch-6.8.3



前置条件：

```
1.保证当前机器能ping通要加入的集群的节点
2.保证当前机器的es使用的端口未添加防火墙
```



说明：

```
集群添加节点不会影响线上已经上线的程序中配置的节点信息，例如
程序中配置的为a、b、c节点，要上线的节点为d、e，当上线节点时，不会影响程序使用
```



#### 步骤

1、修改hosts文件，新增节点sfshadoop4

```
192.168.0.11 sfshadoop1
192.168.0.12 sfshadoop2
192.168.0.13 sfshadoop3
192.168.0.14 sfshadoop4
```



2、复制集群中其中一个节点的elasticsearch(不拷贝data目录,太大了)到新增节点



3、修改elasticsearch.yml 文件，根据实际情况设置新增节点角色（master、data等），这里没设置，即采用默认设置

```
#各个节点集群名字相同
cluster.name: my-application

#修改node.name，不同于集群其他节点
node.name: sfs4

#增加sfshadoop4
discovery.zen.ping.unicast.hosts: ["sfshadoop1", "sfshadoop2","sfshadoop3","sfshadoop4"]
```



4、启动elasticsearch查看集群状态，其他分片会慢慢分配到新增节点

![1593402860735](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\1593402860735.png)

5、修改原集群节点配置后依次重启，集群扩容完成，之后可以修改应用的配置

注意，我这个测试环境，每个节点都可以竞选为master，所以master重启时，可以切换新的master。但是在某些环境，假如只有某一台可以成为master，这时候重启了它，可能导致集群不可用，所以需要确保集群中有多一个可以竞选master的节点。不过如果重启时间很短暂，可能也不太影响，注意下就行了。





##### 集群收缩

直接停止需要取消节点，等待一段时间后恢复正常。如果有索引一个副本都没有，会出现图中情况，因为没有副本的情况下，节点退出会丢失数据

解决办法：重启启动取消的节点，集群状态恢复正常后，增加该索引副本数，完成后再退出该节点，集群状态正常。

![1593403508724](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\1593403508724.png)