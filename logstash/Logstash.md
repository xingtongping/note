## logstash7.2安装使用说明

官网下载历史把本(太慢，有时候访问不了)：

https://www.elastic.co/cn/downloads/past-releases

云盘安装包提供：

https://blog.csdn.net/weixin_37281289/article/details/101483434



#### 下载7.2版本，解压即可运行

```
tar  -zxvf   logstash-7.2.0.tar.gz
```





#### 启动：

```
#test.conf就是我们需要编写的配置文件
bin/logstash -f test.conf    

#后台运行
nohup  bin/logstash  -f  test.conf  >/dev/null & 

#自动加载配置文件的方式
bin/logstash -f test.conf --config.reload.automatic   

#测试配置文件是否OK
bin/logstash -f test.conf --config.test_and_exit

```



## 配置文件的结构

```

input {
  ...    #输入
}

filter {
  ...   #数据处理转换
}

output {
  ...   #输出
}
```



#### 最简单的例子：

1、vim  test.conf，内容如下

```
input {
   stdin { }
}
filter {
}
output {
    stdout { }
}

```

敲入 **Hello World** ！，回车

```ruby
{
    "@timestamp" => 2020-03-25T11:43:41.365Z,
          "host" => "es3",
       "message" => "hello world !",
      "@version" => "1"
}
```

```

```



####  先看实战，实现如下功能，以此对logstash有个大概了解：

1、接收514端口的udp数据

2、只接收指定IP发过来的日志

3、日志正则匹配抓取字段（解析IP经纬度、国家省份城市，事件类型和其他字段）

4、消息关键字过滤

5、攻击关键字过滤

6、保存到Elasticsearch



实现第一步：

# 保存进 Elasticsearch

```
output {
    elasticsearch {
        host => "192.168.0.2"
        protocol => "http"
        index => "logstash-%{type}-%{+YYYY.MM.dd}"
        index_type => "%{type}"
        workers => 5
        template_overwrite => true
    }
}
```









传输数据JSON例子

```
input {
     stdin {
         type=> "syslog"
         }
}
filter {
     if[type]=="syslog"{
         json {
             source => "message"
         }
     }
}
output {
    stdout { }
}
```









geoip

source => "remote_addr"
                database => "/export/servers/elk/logstash-5.5.2/GeoLite2-City.mmdb"
                target => "geoip"
                add_field => ["[geoip][coordinates]" , "&{[geoip][latitude]}"]
                add_field => ["[geoip][coordinates]" , "&{[geoip][longitude]}"]
                fields => ["country_name" , "region_name" , "city_name" , "latitude" , "longitude"]
                #remove_field => {[geoip][latitude] , []} 





```
input {
    udp {
        port => "514"
        type => "syslog"

    }

}
filter {
    grok {
        match => {
            "message" => ".*IN=(?<eventName>.*)\sOUT.*SRC=(?<sourceIp>.*)\sDST=(?<destIp>.*)\sLEN.*PROTO=(?<appproto>\w+)\sSPT=(?<sourcePort>\d+)\sDPT=(?<destPort>\d+)\s.*"
        }
    }
    geoip {
            source => "sourceIp"
            database => "/home/hadoop/logstash/logstash-7.2.0/vendor/bundle/jruby/2.5.0/gems/logstash-filter-geoip-6.0.1-java/vendor/GeoLite2-City.mmdb"
            add_field => ["country_name" , "%{[geoip][country_name]}"]
            add_field => ["city_name" , "%{[geoip][city_name]}"]
            add_field => ["latitude" , "%{[geoip][latitude]}"]
            add_field => ["longitude" , "%{[geoip][longitude]}"]
            #fields => ["country_name" , , "city_name" , "latitude" , "longitude"]
            remove_field => ["geoip"]
    }
}
output {
   elasticsearch {
        hosts => "172.16.110.199"
        index => "logstash-syslog"
        template_overwrite => true
    }
}
~
```





### 理论知识：

#### 区段

Logstash 用 `{}` 来定义区域，区域内可以包括插件区域定义，你可以在一个区域内定义多个插件

```
input {
    stdin {}
    udp {
       port=>514
    }
}
```



#### 数据类型

Logstash 支持少量的数据值类型：

- bool

```
debug => true
```

- string

```
host => "hostname"
```

- number

```
port => 514
```

- array

```
match => ["datetime", "UNIX", "ISO8601"]
```

- hash

```
options => {
    key1 => "value1",
    key2 => "value2"
}

```

#### 字段引用

字段就像一个键值对，`一个Event` 对象的属性

如果你想在 Logstash 配置中使用字段的值，只需要把字段的名字写在中括号 `[]` 里就行了，这就叫**字段引用**。



**嵌套字段**引用，例如想获取geoip字段里的location的第一个值，可以这样

```
[geoip][location][0]
logstash 的数组也支持倒序下标，即 [geoip][location][-1] 可以获取数组最后一个元素的值。
```



#### 条件判断

通常会在表达式里用到字段引用,如引用message的值

```
if  "病毒" in [message]{

}else{

}
```



#### *type* 和 *tags*

*type* 和 *tags* 是 logstash 事件中两个特殊的字段。通常来说我们会在*输入区段*中通过 *type* 来标记事件类型 —— 我们肯定是提前能知道这个事件属于什么类型的。而 *tags* 则是在数据处理过程中，由具体的插件来添加或者删除的。

最常见的用法是像下面这样：

```
input {
    stdin {
        type => "web"
    }
}
filter {
    if [type] == "web" {
        grok {
            match => ["message", %{COMBINEDAPACHELOG}]
            add_tag => [ "tag1" ]
        }
    }
}
output {
    if "tag1" in [tags] {
        nagios_nsca {
            nagios_status => "1"
        }
    } else {
        elasticsearch {
        }
    }
}
```



#### 使用 UDP 监听器收集日志，用下行命令检查你的 UDP 接收队列大小：

```
# netstat -plnu | awk 'NR==1 || $4~/:514$/{print $2}'
Recv-Q
228096
```



#### 日志过滤

过滤日志中含有病毒或者色情

if[message]=~"病毒|色情"{
         drop{}
        }



过滤日志中出现病、毒、色、情任何一个字

if[message]=~"[病毒色情]"{
         drop{}
        }



#### logstash之codec插件

Logstash不只是一个**`input|filter|output`**的数据流，而是一个**`input|decode|filter|encode|output`**的数据流。

*codec*就是用来decode，encode 事件的。所以codec常用在input和output中

常用的codec插件有plain，json，multiline等

1、plain是最简单的编码插件，你输入什么信息，就返回什么信息，诸如上面的例子中的timestamp、type自动生成的字段等都不会带过来。

```
input {
       stdin {
       }
}
output {
        stdout {
               codec => plain
        }
}
```



2、json,有时候logstash采集的日志是JSON格式，那我们可以在input字段加入codec => json来进行解析，这样就可以根据具体内容生成字段，方便分析和储存。如果想让logstash输出为json格式，可以在output字段加入codec=>json。

```
input {
        stdin {
        }
}

output {
       stdout {
              codec => json
       }
}
```



3、multiline，logstash处理事件默认是单行的，如果我们input(输入)的文件多行才是一条日志，要把多行合并，那么需要使用multiline插件。
multiline可以配置在input当中，也可以配置在filter当中。

```
input {
    file {
        path => "/home/dockermount/jinyiwei/logs/catalina.out" # 日志路径
        codec => multiline {
            pattern => "^\["  # 正则表达式，匹配开头为 "[" 的为一条日志的开始
            negate => true     # true表示若pattern正则匹配失败，则执行合并；false表示若pattern正则匹配失败，则执行合并。默认为false
            what => "previous"   # previous表示跟前面的行合并；next表示跟后面的行合并
        }
    }
```



#### logstash之过滤器（数据转换）

**Grok**

Grok 是 Logstash 最重要的插件。你可以在 grok 里预定义好命名正则表达式，在稍后(grok参数或者其他正则表达式里)引用它。

```
input {stdin{}}
filter {
    grok {
        match => {
            "message" => "\s+(?<request_time>\d+(?:\.\d+)?)\s+"
        }
    }
}
output {stdout{}}
```

**运行 logstash 进程然后输入 "begin 123.456 end"，你会看到123.456被抓取出来了**

```
{
         "message" => "begin 123.456 end",
        "@version" => "1",
      "@timestamp" => "2014-08-09T11:55:38.186Z",
            "host" => "raochenlindeMacBook-Air.local",
    "request_time" => "123.456"
}
```



实际运用中，我们需要处理各种各样的日志文件，如果你都是在配置文件里各自写一行自己的表达式，就完全不可管理了。所以，我们建议是把所有的 grok 表达式统一写入到一个地方。然后用 *filter/grok* 的 `patterns_dir` 选项来指明。

如果你把 "message" 里所有的信息都 grok 到不同的字段了，数据实质上就相当于是重复存储了两份。所以你可以用 `remove_field` 参数来删除掉 *message* 字段，或者用 `overwrite` 参数来重写默认的 *message* 字段，只保留最重要的部分

```
filter {
    grok {
        patterns_dir => "/path/to/your/own/patterns"
        match => {
            "message" => "%{SYSLOGBASE} %{DATA:message}"
        }
        overwrite => ["message"]
    }
}
```



#### **grok 表达式**调试工具

http://grokdebug.herokuapp.com/





#### 时间处理date

```
filter {
    grok {
        match => ["message", "%{HTTPDATE:logdate}"]
    }
    date {
        match => ["logdate", "dd/MMM/yyyy:HH:mm:ss Z"]
    }
}
```

#### 数据修改(Mutate)

**类型转换**

```
filter {
    mutate {
        convert => ["request_time", "float"]
    }
}
```

**字符串处理**

gsub --替换,仅对字符串类型字段有效

```
gsub => ["urlparams", "[\\?#]", "_"]
```

split--仅对字符串类型字段有效

```
filter {
    mutate {
        split => ["message", "|"]
    }
}
```

join--仅对数组类型字段有效

```
filter {
    mutate {
        split => ["message", "|"]
    }
    mutate {
        join => ["message", ","]
    }
}
```

filter 区段之内，是顺序执行的



merge

```
filter {
    mutate {
        split => ["message", "|"]
    }
    mutate {
        merge => ["message", "message"]
    }
}
```

**字段处理**

- rename

重命名某个字段，如果目的字段已经存在，会被覆盖掉：

```
filter {
    mutate {
        rename => ["syslog_host", "host"]
    }
}
```

- update

更新某个字段的内容。如果字段不存在，不会新建。

- replace

作用和 update 类似，但是当字段不存在的时候，它会起到 `add_field` 参数一样的效果，自动添加新的字段。



#### GeoIP 地址

示例：

```
filter {
    geoip {
        source => "message"
    }
}
```

运行结果：

```ruby
{
       "message" => "183.60.92.253",
      "@version" => "1",
    "@timestamp" => "2014-08-07T10:32:55.610Z",
          "host" => "raochenlindeMacBook-Air.local",
         "geoip" => {
                      "ip" => "183.60.92.253",
           "country_code2" => "CN",
           "country_code3" => "CHN",
            "country_name" => "China",
          "continent_code" => "AS",
             "region_name" => "30",
               "city_name" => "Guangzhou",
                "latitude" => 23.11670000000001,
               "longitude" => 113.25,
                "timezone" => "Asia/Chongqing",
        "real_region_name" => "Guangdong",
                "location" => [
            [0] 113.25,
            [1] 23.11670000000001
        ]
    }
}
```



#### split拆分事件

我们通过 multiline 插件将多行数据合并进一个事件里，那么反过来，也可以把一行数据，拆分成多个事件

示例：

```
filter {
    split {
        field => "message"
        terminator => "#"
    }
}
```

我们在 intputs/stdin 的终端中输入一行数据："test1#test2"，结果看到输出两个事件：

```
{
    "@version": "1",
    "@timestamp": "2014-11-18T08:11:33.000Z",
    "host": "web121.mweibo.tc.sinanode.com",
    "message": "test1"
}
{
    "@version": "1",
    "@timestamp": "2014-11-18T08:11:33.000Z",
    "host": "web121.mweibo.tc.sinanode.com",
    "message": "test2"
}
```



geoip插件中文地理位置输出，下载，更新geoip插件

```
https://github.com/wjcxk21/logstash-filter-geoip-cn
```





插件安装卸载

```
# 删除此插件的当前版本
bin/logstash-plugin remove logstash-filter-dissect
# 安装下载的gem包
bin/logstash-plugin install ../logstash-filter-dissect-1.1.1.gem
#查看版本是否是安装的版本
bin/logstash-plugin list --verbose | grep dissect
# logstash-filter-dissect (1.1.1)  可以看到已经安装成功
```



bin/logstash-plugin install --no-verify --local logstash-filter-inner_ip_handle-1.0.0.gem





#### 遇到的问题：

1、时间转换不生效问题,已解决

target 转存，不设置默认timestamp

2、解析IP地理位置中文化

3、内网IP解析地理位置
4、window复制内容去配置文件，导致一直报错找不到原因，这个时候看提示报错哪一行，然后手敲或者在同个窗口复制多一行，删掉原来那行就好了，已解决



5、log是内网IP地址，且数据库没有查到，tag ---geoip错误

6、时间格式匹配问题，要编写所有类型，否则入ES报错

7、加了jdbc_static启动偶尔出现服务器卡顿现象，4核8G

8、运行一段时间被killd

dmesg | tail -20

Out of memory: Kill process 4393 (java) score 111 or sacrifice chi

Killed process 4393 (java) total-vm:4935888kB, anon-rss:909376kB, file-rss:0kB, shmem-rss:0kB