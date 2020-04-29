### Logstash系列之--实战演练

#### 实现如下功能，以此熟悉一些logstash的语法：

```
**1、接收本机514端口udp数据**

**2、只接收指定IP发过来的日志**

**3、过滤出现“色情”词语的日志**

**4、日志正则匹配抓取字段**

**5、解析sourceIp经纬度、国家省份城市**

**6、输出保存到Elasticsearch**


```

**首先我们看看配置文件的结构：**

```
input {
  ...    #输入
}
​
filter {
  ...   #数据处理转换
}
​
output {
  ...   #输出
}
```





**下面我们开始实现以上功能：**

测试syslong格式：

```
<13>Jan 13 16:09:47 localhost 1578902986 2020/01/13 Mon 16:09:46 ipt_log=ACCEPT IN=vEth0 OUT=enp2s0 MAC=82:4b:ad:48:b6:0e:00:10:f3:71:9b:93:08:00 SRC=172.16.7.170 DST=192.168.0.100 LEN=124 TOS=0x00 PREC=0x00 TTL=122 ID=17313 PROTO=UDP SPT=51424 DPT=54218 LEN=104"|nc -u 172.16.110.243 514
```





##### 1、接收514端口的udp数据

```
input {
    udp {
        port => "514"
    }

}
```



##### 2、只接收指定IP发过来的日志

```

filter {
    #过滤指定IP，如果上报IP在数组中，则丢弃
    if [host] in ["172.16.110.55","10.53.187.101"]{
        drop {}   
    }

}
```



##### 3、过滤出现“色情”词语的日志

```

filter {  
    #关键字过滤,日志中出现色情和傻逼的都丢弃
    if[message]=~"色情|傻逼"{
        drop{}
     }
}
```

##### 4、日志正则匹配抓取字段

###### Grok 正则捕获

```
filter {
     #抓取了日志中的时间、事件名称、IP和协议等字段
     grok {
        match => {
            "message" => ".*\>(?<logtime>.*)\slocalhost.*IN=(?<eventName>.*)\sOUT.*SRC=(?<sourceIp>.*)\sDST=(?<destIp>.*)\sLEN.*PROTO=(?<appproto>\w+)\sSPT=(?<sourcePort>\d+)\sDPT=(?<destPort>\d+)\s.*"
        }
    }

}
output {
  ...   #输出
}
```



##### 5、解析sourceIp经纬度、国家省份城市（使用geoip插件）

```
filter{
	#地理位置
    geoip {
            source => "sourceIp"
            #用《find  / -name 名字》查找mmdb文件在linux的为止
            database => "/../../GeoLite2-City.mmdb"   
            add_field => ["src_country" , "%{[geoip][country_name]}"]
            add_field => ["src_province" , "%{[geoip][region_name]}"]
            add_field => ["src_city" , "%{[geoip][city_name]}"]
            add_field => ["src_latitude" , "%{[geoip][latitude]}"]
            add_field => ["src_longitude" , "%{[geoip][longitude]}"]
           }
}
```



##### 6、输出保存到Elasticsearch

```
output {
   elasticsearch {
        hosts => "10.10.110.110"
        index => "syslog_%{+YYYYMMdd}"
    }

}
```



##### 完整配置文件：

```
input {
    udp {
        port => "514"
    }

}
filter {
    #过滤指定IP，如果上报IP在数组中，则丢弃
    if [host] in ["172.16.110.55","10.53.187.101"]{
        drop {}   
    }
    
    #关键字过滤,日志中出现色情和傻逼的都丢弃
    if[message]=~"色情|傻逼"{
        drop{}
     }
     
     
     #抓取了日志中的时间、事件名称、IP和协议等字段
     grok {
        match => {
            "message" => ".*\>(?<logtime>.*)\slocalhost.*IN=(?<eventName>.*)\sOUT.*SRC=(?<sourceIp>.*)\sDST=(?<destIp>.*)\sLEN.*PROTO=(?<appproto>\w+)\sSPT=(?<sourcePort>\d+)\sDPT=(?<destPort>\d+)\s.*"
        }
    }
    
    #地理位置
    geoip {
            source => "sourceIp"
            #用《find  / -name 名字》查找mmdb文件在linux的为止
            database => "/../../GeoLite2-City.mmdb"   
            add_field => ["src_country" , "%{[geoip][country_name]}"]
            add_field => ["src_province" , "%{[geoip][region_name]}"]
            add_field => ["src_city" , "%{[geoip][city_name]}"]
            add_field => ["src_latitude" , "%{[geoip][latitude]}"]
            add_field => ["src_longitude" , "%{[geoip][longitude]}"]
           }

}
output {
	elasticsearch {
        hosts => "10.10.110.110"
        index => "syslog_%{+YYYYMMdd}"
    }
}
```



##### 启动运行：

bin/logstash  -f  syslog.conf  --config.reload.automatic



如果想删除某些字段

```
 {
 	mutate {
         remove_field => ["src_longitude","appproto"]
   }
 }
```



说明：

上面用的这些都是很基础的，logstash还有很多各种各样的插件，想学好要去官网慢慢看！





