package org.logstashplugins;

import co.elastic.logstash.api.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// class name must match plugin name
@LogstashPlugin(name = "ip_keyword_filter")
public class IpKeywordFilter implements Filter {

//    public static Logger logger = LoggerFactory.getLogger(JavaFilterExample.class);

    public static final PluginConfigSpec<String> SOURCE_CONFIG =
            PluginConfigSpec.stringSetting("source", "message");
    public static final PluginConfigSpec<String> LOG_TYPE =
            PluginConfigSpec.stringSetting("logType", "syslog");


    private String id;
    private String sourceField;
    private String logType;

    public IpKeywordFilter(String id, Configuration config, Context context) {
        // constructors should validate configuration options
        this.id = id;
        this.sourceField = config.get(SOURCE_CONFIG);
        this.logType = config.get(LOG_TYPE);
    }

    @Override
    public Collection<Event> filter(Collection<Event> events, FilterMatchListener matchListener) {
        Pattern pattern;
        Matcher m;
        for (Event e : events) {
            //获取上报IP
            String reportIp = (String) e.getField("host");
            //获取日志内容
            String message = (String) e.getField(sourceField);
            //消息过滤关键字列表
            List<Map<String,String>> filterWord = (List<Map<String, String>>) e.getField("filterMsg");
            //关键字列表,发送到kafka，后续正则匹配
            List<Map<String,String>> keyword = (List<Map<String, String>>) e.getField("keywordList");
            //过滤IP列表
            List<Map<String,String>> filterIp = (List<Map<String, String>>) e.getField("filterIp");

            //过滤上报IP
            for (Map<String,String> ip:filterIp){
                if (ip.get("ip").equals(reportIp)){
                    e.cancel();
                }
            }
            //消息关键字过滤
            for (Map<String,String> msg:filterWord){
                if (message.contains(msg.get("msg"))){
                    e.cancel();
                }
            }

            //处理没有被过滤的消息
            if(!e.isCancelled()){
                //攻击关键字匹配
                for (Map<String,String> word:keyword){
                    if (message.contains(word.get("keyword"))){
                        e.setField("match","1");
                        break;
                    }
                }

                //默认是syslog,其他类型：APT日志，威胁情报等
                if (e.getField("eventType")==null||e.getField("eventType")==""){
                    e.setField("eventType",logType);
                }
                e.remove("filterMsg");
                e.remove("keywordList");
                e.remove("filterIp");
            }
            matchListener.filterMatched(e);
        }
        return events;
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        List<PluginConfigSpec<?>> arrayList = new ArrayList<>();
        arrayList.add(SOURCE_CONFIG);
        arrayList.add(LOG_TYPE);
        // should return a list of all configuration options for this plugin
        return arrayList;
    }

    @Override
    public String getId() {
        return this.id;
    }

    public static void main(String[] args) {

    }
}
