package org.utils;


import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IPExt {
    public static String getRandomIp() {
        //ip范围
        int[][] range = {{607649792, 608174079},//36.56.0.0-36.63.255.255
                {1038614528, 1039007743},//61.232.0.0-61.237.255.255
                {1783627776, 1784676351},//106.80.0.0-106.95.255.255
                {2035023872, 2035154943},//121.76.0.0-121.77.255.255
                {2078801920, 2079064063},//123.232.0.0-123.235.255.255
                {-1950089216, -1948778497},//139.196.0.0-139.215.255.255
                {-1425539072, -1425014785},//171.8.0.0-171.15.255.255
                {-1236271104, -1235419137},//182.80.0.0-182.92.255.255
                {-770113536, -768606209},//210.25.0.0-210.47.255.255
                {-569376768, -564133889}, //222.16.0.0-222.95.255.255
        };

        Random rdint = new Random();
        int index = rdint.nextInt(10);
        String ip = num2ip(range[index][0] + new Random().nextInt(range[index][1] - range[index][0]));
        return ip;
    }

    public static String num2ip(int ip) {
        int[] b = new int[4];
        String x = "";

        b[0] = (int) ((ip >> 24) & 0xff);
        b[1] = (int) ((ip >> 16) & 0xff);
        b[2] = (int) ((ip >> 8) & 0xff);
        b[3] = (int) (ip & 0xff);
        x = Integer.toString(b[0]) + "." + Integer.toString(b[1]) + "." + Integer.toString(b[2]) + "." + Integer.toString(b[3]);

        return x;
    }

    public static void main(String[] args) throws Exception {
        IPExt.load(IPExt.class.getClassLoader().getResourceAsStream("mydata4vipday2.datx"));
        Long st = System.nanoTime();
        //返回是一个数组
        //System.out.println(Arrays.toString(IPExt.find("104.244.14.252")));
        //System.out.println(Arrays.toString(IPExt.find("14.18.249.35")));
        //System.out.println(Arrays.toString(IPExt.find("114.114.114.114")));
        //  System.out.println(Arrays.toString(IPExt.find("37.79.87.234")));
        //  System.out.println(Arrays.toString(IPExt.find("ns1.toprealinfo.ru")));
        JSONObject obj = getAreaInfoByIp("37.79.87.234");
        System.out.println(obj.toString());
        Long et = System.nanoTime();
        System.out.println((et - st) / 1000 / 1000);

    }

    public static boolean enableFileWatch = false;

    private static int offset;
    private static int[] index = new int[65536];
    private static ByteBuffer dataBuffer;
    private static ByteBuffer indexBuffer;
    private static int size = 0;
    private static InputStream inputStream;
    private static ReentrantLock lock = new ReentrantLock();

    public static void load(InputStream in) {
        inputStream = in;
        load();
        if (enableFileWatch) {
            watch();
        }
    }

    public static void load(InputStream in, boolean strict) throws Exception {
        if (strict) {
            int contentLength = Long.valueOf(in.available()).intValue();
            if (contentLength < 512 * 1024) {
                throw new Exception("ip data file error.");
            }
        }
        load();
        if (enableFileWatch) {
            watch();
        }
    }

    public static String[] find(String ip) {
        boolean isIp = isIP(ip);
        if (isIp) {
            String[] ips = ip.split("\\.");
            int prefix_value = (Integer.valueOf(ips[0]) * 256 + Integer.valueOf(ips[1]));
            long ip2long_value = ip2long(ip);
            int start = index[prefix_value];
            int max_comp_len = offset - 262144 - 4;
            long tmpInt;
            long index_offset = -1;
            int index_length = -1;
            byte b = 0;
            for (start = start * 9 + 262144; start < max_comp_len; start += 9) {
                tmpInt = int2long(indexBuffer.getInt(start));
                if (tmpInt >= ip2long_value) {
                    index_offset = bytesToLong(b, indexBuffer.get(start + 6), indexBuffer.get(start + 5), indexBuffer.get(start + 4));
                    index_length = (0xFF & indexBuffer.get(start + 7) << 8) + (0xFF & indexBuffer.get(start + 8));
                    break;
                }
            }

            byte[] areaBytes;

            lock.lock();
            try {
                dataBuffer.position(offset + (int) index_offset - 262144);
                areaBytes = new byte[index_length];
                dataBuffer.get(areaBytes, 0, index_length);
            } finally {
                lock.unlock();
            }
            return new String(areaBytes, Charset.forName("UTF-8")).split("\t", -1);
        } else {
            return new String[0];
        }

    }

    public static JSONObject getAreaInfoByIp(String ip) {
        JSONObject result = new JSONObject();
        if (StringUtils.isEmpty(ip)) {
            result.put("country", "");
            result.put("province", "");
            result.put("city", "");
            result.put("latitude", "");
            result.put("longitude", "");
        } else {
            String[] info = IPExt.find(ip);
            if (info.length > 0) {
                result.put("country", info[0]);
                result.put("province", info[1]);
                result.put("city", info[2]);
                result.put("latitude", info[5]);
                result.put("longitude", info[6]);
            } else {
                result.put("country", "");
                result.put("province", "");
                result.put("city", "");
                result.put("latitude", "");
                result.put("longitude", "");
            }

        }
        return result;
    }


    private static void watch() {
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                int currentSize = 0;
                try {
                    currentSize = inputStream.available();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (currentSize > size) {
                    load();
                }
            }
        }, 1000L, 5000L, TimeUnit.MILLISECONDS);
    }

    private static void load() {
        try {
            size = inputStream.available();
        } catch (IOException e) {
            e.printStackTrace();
        }
        lock.lock();
        try {
            dataBuffer = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
            dataBuffer.position(0);
            offset = dataBuffer.getInt(); // indexLength
            byte[] indexBytes = new byte[offset];
            dataBuffer.get(indexBytes, 0, offset - 4);
            indexBuffer = ByteBuffer.wrap(indexBytes);
            indexBuffer.order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < 256; i++) {
                for (int j = 0; j < 256; j++) {
                    index[i * 256 + j] = indexBuffer.getInt();
                }
            }
            indexBuffer.order(ByteOrder.BIG_ENDIAN);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    private static byte[] getBytesByFile(File file) {
        FileInputStream fin = null;
        byte[] bs = new byte[new Long(file.length()).intValue()];
        try {
            fin = new FileInputStream(file);
            int readBytesLength = 0;
            int i;
            while ((i = fin.available()) > 0) {
                fin.read(bs, readBytesLength, i);
                readBytesLength += i;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if (fin != null) {
                    fin.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return bs;
    }

    private static long bytesToLong(byte a, byte b, byte c, byte d) {
        return int2long((((a & 0xff) << 24) | ((b & 0xff) << 16) | ((c & 0xff) << 8) | (d & 0xff)));
    }

    private static int str2Ip(String ip) {
        String[] ss = ip.split("\\.");
        int a, b, c, d;
        a = Integer.parseInt(ss[0]);
        b = Integer.parseInt(ss[1]);
        c = Integer.parseInt(ss[2]);
        d = Integer.parseInt(ss[3]);
        return (a << 24) | (b << 16) | (c << 8) | d;
    }

    private static long ip2long(String ip) {
        return int2long(str2Ip(ip));
    }

    private static long int2long(int i) {
        long l = i & 0x7fffffffL;
        if (i < 0) {
            l |= 0x080000000L;
        }
        return l;
    }

    /**
     * 判断是否为ip
     *
     * @param addr
     * @return
     */
    public static boolean isIP(String addr) {
        if (addr.length() < 7 || addr.length() > 15 || "".equals(addr)) {
            return false;
        }
        /**
         * 判断IP格式和范围
         */
        String rexp = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        Pattern pat = Pattern.compile(rexp);
        Matcher mat = pat.matcher(addr);
        boolean ipAddress = mat.find();
        return ipAddress;
    }

}
