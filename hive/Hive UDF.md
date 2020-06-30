#### Hive中有三种UDF:

分类

1、用户定义函数(user-defined function)UDF；
2、用户定义聚集函数（user-defined aggregate function，UDAF）；
3、用户定义表生成函数（user-defined table-generating function，UDTF）。

介绍

UDF操作作用于单个数据行，并且产生一个数据行作为输出。大多数函数都属于这一类（比如数学函数和字符串函数）。
UDAF 接受多个输入数据行，并产生一个输出数据行。像COUNT和MAX这样的函数就是聚集函数。
UDTF 操作作用于单个数据行，并且产生多个数据行-------一个表作为输出。lateral view explore()



#### 自定义UDF过程以及例子

自定义UDF过程

导入hive 自定义jar包
继承UDF借口
实现evaluate方法

临时函数的使用:

1. 临时函数的使用

  ```
  进入hive的交互shell中
  1. 上传自定义udf的jar
  hive> add jar /path/to/lower.jar
  2. 创建临时函数
  hive> create temporary function xxoo_lower as 'test.ql.LowerUDF';
  3. 验证
  hive> select xxoo_lower("Hello World!");
  ```

  

2. 永久函数的使用:

  ```
  1. 把自定义函数的jar上传到hdfs中.
  hdfs dfs -put lower.jar 'hdfs:///path/to/hive_func';
  2. 创建永久函数
  hive> create function xxoo_lower as 'test.ql.LowerUDF' using jar 'hdfs:///path/to/hive_func/lower.jar'
  3. 验证
  hive> select xxoo_lower("Hello World");
  hive> show functions;
  ```

  



**需求：**

```
ip格式转换 -192.168.2.1  -> 192.168.002.001
```



```
import org.apache.hadoop.hive.ql.exec.UDF;

/**
 * 自定义hive  UDF函数
 *   hive:2.3.2
 *      打包完成可能报错
 *      linux下执行命令：
 *      zip -d yourjar.jar 'META-INF/.SF' 'META-INF/.RSA' 'META-INF/*SF'
 */
public class MyUDF extends UDF {

    //ip   192.168.2.1  192.168.002.001
    /**
     * ip_location:ip地址库表  起始ip 终止ip 所属国家 所属省份 所属市
     * log:ip
     */
    public String evaluate(String ip){
        String[] datas = ip.split("\\.");//按.切分转义
        StringBuffer sb = new StringBuffer();
        //前边补三个0
        for(String s:datas){
            s="000"+s;
            s=s.substring(s.length()-3);
            sb.append(s).append(".");
        }
        return sb.substring(0,sb.length()-1);
    }

    /*//本地测试
    public static void main(String[] args) {
        String evaluate = evaluate("2.3.5.3");
        System.out.println(evaluate);
    }*/
}
```