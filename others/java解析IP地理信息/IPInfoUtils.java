package org.utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Description:
 * User: lilu
 * Date: 2018-07-12
 * Time: 10:53
 * modify by:
 */
public class IPInfoUtils {
    /**
     * slf4j
     */
    private static Logger logger = LoggerFactory.getLogger(IPInfoUtils.class);

    /**
     * 添加ip信息
     */
    public static void addAreaDesc(JSONObject area, String prefix, String field) {
        List<String> areaList = Arrays.asList("country", "province", "city", "latitude", "longitude");
        String ip = area.getString(field);
        if (ip != null && !StringUtils.isEmpty(ip.trim()) && IpUtil.checkIpV4(ip.trim())) {//IPv4地址
            if (IpUtil.isInnerIP(ip.trim())) {//内网ip
                for (int i = 0; i < areaList.size(); i++) {
                    area.put(prefix + areaList.get(i), IpUtil.getInnerIpInfoByNumber(i));
                }
            } else {
                JSONObject data = IPExt.getAreaInfoByIp(ip.trim());
                Object[] keys = data.keySet().toArray();
                for (Object key : keys) {
                    area.put(prefix + key, data.get(key).toString());
                }
            }
        } else {
            for (String s : areaList) {
                area.put(prefix + s, "");
            }
        }
    }

    public static void loadIpFile() {
        IPExt.load(IPInfoUtils.class.getClassLoader().getResourceAsStream("mydata4vipday2.datx"));
    }

    public static void main(String[] args) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sourceip", "1.1.1.1");
        jsonObject.put("destip", "172.16.110.32");
        IPInfoUtils.loadIpFile();
        IPInfoUtils.addAreaDesc(jsonObject, "src", "sourceip");
        IPInfoUtils.addAreaDesc(jsonObject, "dst", "destip");
        System.out.println(jsonObject.toString());
    }
}
