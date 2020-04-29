package com.bluedon.kafka

import java.io.InputStream
import java.util.{Date, Properties}

import com.bluedon.kafka.SyslogAnalyzeServer.log
import com.bluedon.utils.{DateTimeUtil, RedisUtils}
import net.sf.json
import net.sf.json.JSONObject
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.{CanCommitOffsets, HasOffsetRanges, KafkaUtils, OffsetRange}
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.dstream.InputDStream

/**
  * 处理kafka情报数据，存储到redis
  * "base_indicator": {
  * "indicator": "185.144.138.190",
  * "description": "",
  * "title": "",
  * "tags": [
  * "command_and_control"
  * ],
  * "slug": "ip",
  * "type": "IPv4",
  * "id": 2193982682,
  * "name": ""
  * }
  */
object ThreatDataToRedis {
  List
  def main(args: Array[String]): Unit = {
    //加载配置文件
    val properties:Properties = new Properties()
    var ipstream: InputStream = null
    try {
      ipstream = this.getClass.getResourceAsStream("/manage.properties")
      properties.load(ipstream)
    } catch {
      case e: Exception =>
        log.error(e.getMessage, e)
    } finally {
      if (ipstream != null) {
        try {
          ipstream.close()
        } catch {
          case e: Exception =>
            log.error(e.getMessage, e)
        }
      }
    }

    val masterUrl = properties.getProperty("spark.master.url")
    //创建sparkSession
    val spark = SparkSession
      .builder()

      //      .master(masterUrl)
      .master("local")
      .appName("SyslogAnalyzeServer")
      .config("spark.some.config.option", "some-value")
      .config("spark.streaming.unpersist","true")  // 智能地去持久化
      .config("spark.streaming.stopGracefullyOnShutdown","true")  // 优雅的停止服务
      .config("spark.streaming.backpressure.enabled",false)      //开启后spark自动根据系统负载选择最优消费速率
      .config("spark.streaming.backpressure.initialRate",10000)      //限制第一次批处理应该消费的数据，因为程序冷启动队列里面有大量积压，防止第一次全部读取，造成系统阻塞
      .config("spark.streaming.kafka.maxRatePerPartition",3000)      //限制每秒每个消费线程读取每个kafka分区最大的数据量  8个分区总共1w
      .config("spark.streaming.receiver.maxRate",10000)      //设置每次接收的最大数量
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .getOrCreate()

    val ssc = new StreamingContext(spark.sparkContext, Seconds(10))

    val brokers = properties.getProperty("kafka.host.list")
   // val topics = Set(properties.getProperty("kafka.threat.topic"))
    val topics = properties.getProperty("kafka.threat.topic").toString.split(",")
    val kafkaParams = Map[String, String](
      "metadata.broker.list" -> brokers,
      "bootstrap.servers" -> brokers,
      "group.id" -> "xing",
      "key.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer",
      "value.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer",
      "auto.offset.reset" -> "earliest",
      "enable.auto.commit" -> "false"
    )

    val kafkaStream: InputDStream[ConsumerRecord[String, String]] = KafkaUtils.createDirectStream[String, String](ssc, PreferConsistent, Subscribe[String, String](topics, kafkaParams))

    kafkaStream.foreachRDD(threatRDD=>{
      //手动提交kafka offset
      val offsetRanges = threatRDD.asInstanceOf[HasOffsetRanges].offsetRanges
      val rdds = threatRDD.map(_.value())
      val jsonstr = new json.JSONObject()


      rdds.foreachPartition{patition=>{
        patition.foreach(rdd=>{
          val jedis = RedisUtils.getJedis()
          val createTime = DateTimeUtil.format(new Date(),"yyyy-MM-dd HH:mm:ss")
          try{
            val data = JSONObject.fromObject(rdd)
            if (data!=null){
              val key = data.get("indicator").toString
              println(data.toString)
              val types = data.get("type").toString
              val tag = data.get("tags").toString.replace("\"","").replace("[","").replace("]","")
              val tagArray = tag.split(",")
              jsonstr.put("tag",tag)
              jsonstr.put("category",tranform(tagArray))
              jsonstr.put("created_time",createTime)
              jsonstr.put("value",key)
              jsonstr.put("type",types)
              jsonstr.put("source_ref","")
              jsonstr.put("geo","")
              jsonstr.put("score","")

              jedis.hset("iocs_test",key,jsonstr.toString())
            }
          }catch {
            case e=> println(e.getMessage)
          }finally {
            RedisUtils.returnResource(jedis)
          }
        })
      }}

      kafkaStream.asInstanceOf[CanCommitOffsets].commitAsync(offsetRanges)
    })

    ssc.start()
    ssc.awaitTermination()
  }


  def tranform(tagArray:Array[String]):String={
    val attacks = Map("adware" -> "广告软件",
      "backdoor" -> "木马后门",
      "bruteforce" -> "暴力破解",
      "command_and_control" -> "远程控制",
      "delivery_email" -> "电子邮件攻击",
      "document_exploit" -> "文档利用",
      "domain_owner" -> "域所有者",
      "exploit_kit" -> "利用工具包",
      "exploit_source" -> "利用资源",
      "file_scanning" -> "文件扫描",
      "hacking_tool" -> "黑客工具",
      "hunting" -> "威胁狩猎",
      "macro_malware" -> "恶意软件",
      "malvertising" -> "恶意广告",
      "malware_hosting" -> "恶意软件托管",
      "memory_scanning" -> "内存扫描",
      "pcap_scanning" -> "PCAP扫描",
      "phishing" -> "网络钓鱼",
      "rat" -> "远程访问特洛伊木马",
      "ransomware" -> "勒索软件",
      "scanning_host" -> "扫描主机",
      "trojan" -> "特洛伊木马",
      "unknown" -> "未知",
      "web_Attack" -> "WEB攻击",
      "worm" -> "蠕虫")

    var tagString = ""
    tagArray.foreach{tag=>
      tagString += attacks.get(tag).getOrElse(tag)+","
    }
    tagString.substring(0,tagString.length-1)
  }



}
