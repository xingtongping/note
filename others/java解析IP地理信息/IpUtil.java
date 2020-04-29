package org.utils;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * Created by dengxiwen on 2017/2/9.
 */
public class IpUtil {

    private static Logger logger = LoggerFactory.getLogger(IpUtil.class);
    private static String innerIpInfo = null;

    private static void loadInnerIpInfo() {
        //spark 通过class.getClassLoader()才能加载到配置文件
        try (InputStream in = IpUtil.class.getClassLoader().getResourceAsStream("manage.properties");
             InputStreamReader reader = new InputStreamReader(in, "UTF-8")) {
            Properties properties = new Properties();
            properties.load(reader);
            innerIpInfo = properties.getProperty("inner.ip.info");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 验证IP是否属于某个IP段
     *
     * @param ip        所验证的IP号码
     * @param ipSection IP段（以'-'分隔）
     * @return
     */
    public static boolean ipExistsInRange(String ip, String ipSection) {

        ipSection = ipSection.trim();
        ip = ip.trim();
        int idx = ipSection.indexOf('-');
        String beginIP = ipSection.substring(0, idx);
        String endIP = ipSection.substring(idx + 1);
        return getIp2long(beginIP) <= getIp2long(ip) && getIp2long(ip) <= getIp2long(endIP);
    }

    public static long getIp2long(String ip) {

        ip = ip.trim();
        String[] ips = ip.split("\\.");
        long ip2long = 0L;
        for (int i = 0; i < 4; ++i) {
            ip2long = ip2long << 8 | Integer.parseInt(ips[i]);
        }
        return ip2long;
    }

    public static long getIp2long2(String ip) {

        ip = ip.trim();
        String[] ips = ip.split("\\.");
        long ip1 = Integer.parseInt(ips[0]);
        long ip2 = Integer.parseInt(ips[1]);
        long ip3 = Integer.parseInt(ips[2]);
        long ip4 = Integer.parseInt(ips[3]);
        long ip2long = 1L * ip1 * 256 * 256 * 256 + ip2 * 256 * 256 + ip3 * 256 + ip4;
        return ip2long;
    }

    public static boolean isInnerIP(String ip) {
        if (ip.startsWith("172.") || ip.startsWith("192.") || ip.startsWith("10.") || ip.startsWith("114.") || ip.startsWith("8.")) {
            return true;
        } else {
            return false;
        }
    }

    public static String getInnerIpInfoByNumber(int num) {
        if (innerIpInfo == null || innerIpInfo.length() == 0) {
            loadInnerIpInfo();
        }
        String[] s = innerIpInfo.split(",");
        return s[num];
    }

    /**
     * 匹配IPv4地址
     *
     * @param ip
     * @return
     */
    public static boolean checkIpV4(String ip) {
        if (!StringUtils.isEmpty(ip)) {
            // 定义正则表达式
            String regex = "^((25[0-5]|2[0-4]\\d|[0-1]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[0-1]?\\d\\d?)$";
            // 判断ip地址是否与正则表达式匹配
            if (ip.matches(regex)) {
                return true;
            }
        }
        return false;
    }

    /**
     * url截取域名
     *
     * @param urlString url
     * @return
     */
    public static String getHostName(String urlString) {
        int index = urlString.indexOf("://");
        if (index != -1) {
            urlString = urlString.substring(index + 3);
        }
        index = urlString.indexOf("/");
        if (index != -1) {
            urlString = urlString.substring(0, index);
        }
        return urlString;
    }


    /**
     * 判断内外网Ip
     * A类地址：10.0.0.0--10.255.255.255
     * B类地址：172.16.0.0--172.31.255.255
     * C类地址：192.168.0.0--192.168.255.255
     *
     * @param ip 传入的Ip
     * @return true是内网 false是外网
     * @throws Exception
     */
    public static boolean judgeNetworkIp(String ip) throws Exception {
        if (ip != null) {
            if (ip.indexOf(".") != -1) {
                String[] ipArr = ip.split("\\.");
                //    logger.info(Arrays.toString(ipArr));
                //A类 start
                if ("10".equals(ipArr[0])) {
                    return true;
                }
                //A类 end
                //B类 start
                if ("192".equals(ipArr[0]) && "168".equals(ipArr[1])) {
                    return true;
                }
                //B类 end
                //C类 start
                if ("172".equals(ipArr[0])) {
                    if (Integer.valueOf(ipArr[1]) >= 16 && Integer.valueOf(ipArr[1]) <= 31) {
                        return true;
                    }
                }
                //C类 end
            }
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(judgeNetworkIp("172.16.110.88"));
    }

}
