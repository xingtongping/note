package com.xxl.job.executor.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.util.EntityUtils;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;


@RunWith(SpringRunner.class)
@SpringBootTest
public class ReadEsthreatDataToRedis {

    @Autowired
    RestHighLevelClient restHighLevelClient;
    @Autowired
    JedisPool jedisPool;
    SimpleDateFormat sdf;
    int count =0;


    @org.junit.Test
    public void readEsthreatDataToRedis() throws IOException {
        Jedis jedis = jedisPool.getResource();
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //设定滚动时间间隔,这个时间并不需要长到可以处理所有的数据，仅仅需要足够长来处理前一批次的结果。每个 scroll 请求（包含 scroll 参数）设置了一个新的失效时间
        final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(2L));
        // 新建索引搜索请求
        SearchRequest  searchRequest = new SearchRequest ("threat_ioc_all");
        searchRequest.scroll(scroll);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //设定每次返回多少条数据
        searchSourceBuilder.size(5000);
        searchRequest.source(searchSourceBuilder);
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        SearchHit[] searchHits = response.getHits().getHits();

        for (SearchHit hit : searchHits) {
            count++;
            String sourceAsString = hit.getSourceAsString();
            getDataToRedis(sourceAsString,jedis);
        }

        String scrollId = response.getScrollId();
        //遍历搜索命中的数据，直到没有数据
        while (searchHits != null && searchHits.length > 0) {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(scroll);
            try {
                response = restHighLevelClient.scroll(scrollRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                e.printStackTrace();
            }
            scrollId = response.getScrollId();
            searchHits = response.getHits().getHits();
            if (searchHits != null && searchHits.length > 0) {
                for (SearchHit searchHit : searchHits) {
                    count++;
                    //System.out.println(searchHit.getSourceAsString());
                    String sourceAsString = searchHit.getSourceAsString();
                    getDataToRedis(sourceAsString,jedis);
                }
            }
        }
        System.out.println("插入数量："+count);
        jedis.close();
        jedisPool.close();
        restHighLevelClient.close();
    }


    public void getDataToRedis(String json,Jedis jedis){
        JSONObject jsonObject = JSONObject.parseObject(json);
        JSONObject baseInfo = jsonObject.getJSONObject("basicInfo");
        String attackAction = baseInfo.getJSONArray("attackAction").toString().replace("[","").replace("]","").replace("\"","");
        String dataType = baseInfo.get("dataType").toString();
        String indicator = baseInfo.get("data").toString();
        JSONObject data = new JSONObject();
        data.put("tag",attackAction);
        data.put("category",attackAction);
        data.put("created_time",sdf.format(new Date()));
        data.put("value",indicator);
        data.put("type",dataType);
        data.put("source_ref","");
        data.put("geo","");
        data.put("score","");
        System.out.println(count+":"+data.toString());
        jedis.hset("iocs_test",indicator,data.toString());
    }

    public static void main(String[] args) {
        String json = "{\"attackerInfo\":[],\"whois\":[],\"basicInfo\":{\"lastTime\":\"2020-03-31 20:46:58\",\"firstTime\":\"2020-03-31 20:46:58\",\"total\":28,\"data\":\"blackjoker.newminersage.com\",\"attackAction\":[\"僵尸木马主控端\"],\"dataType\":\"domain\",\"attackInProtocol\":[],\"malwareClass\":[],\"tags\":\"=\"},\"threatIntelligence\":[{\"level\":95,\"activeTime\":\"2020-03-27 00:00:00\",\"channel\":\"domain_reputation\"},{\"level\":95,\"activeTime\":\"2020-03-27 00:00:00\",\"channel\":\"domain_c2\"}],\"linkedAnalysis\":[]}";
        JSONObject jsonObject = JSONObject.parseObject(json);
        System.out.println(jsonObject.getJSONObject("basicInfo").getJSONArray("attackAction").toString());
    }
}
