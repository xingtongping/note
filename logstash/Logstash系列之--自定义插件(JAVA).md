### Logstash系列之--自定义插件(JAVA)

##### 说明

官方文档：https://www.elastic.co/guide/en/logstash/7.2/java-filter-plugin.html

安装版本：7.2.0



1、拉取logstash对应版本代码

```
git clone --branch v7.2.0 --single-branch https://github.com/elastic/logstash.git 
```

2、gradlew.bat assemble ，这一步卡了整整一天，因为不熟悉gradle，要么build失败，要么下载jar失败，下载插件失败，不行试下reimport gradle

![image-20200331123442265](C:\Users\jiang\AppData\Roaming\Typora\typora-user-images\image-20200331123442265.png)

![image-20200331123604762](C:\Users\jiang\AppData\Roaming\Typora\typora-user-images\image-20200331123604762.png)



3、拉取java filter example例子

```
git clone https://github.com/logstash-plugins/logstash-filter-java_filter_example.git
```



4、修改改项目的build.gradle文件，这里我是修改成第一个logstash项目rubbyUtils的绝对路径

![image-20200331123925593](C:\Users\jiang\AppData\Roaming\Typora\typora-user-images\image-20200331123925593.png)



![image-20200331124011377](C:\Users\jiang\AppData\Roaming\Typora\typora-user-images\image-20200331124011377.png)



5、这个时候执行gradlew.bat assemble，报错找不到logstash-filter-java_filter_example/目录下的version.xml,因此我直接复制第一个项目的version.xml到example项目的目录下

![image-20200331124220251](C:\Users\jiang\AppData\Roaming\Typora\typora-user-images\image-20200331124220251.png)



6、执行gradlew.bat assemble，如果没有报错，则可以进行打包

![image-20200331124326483](C:\Users\jiang\AppData\Roaming\Typora\typora-user-images\image-20200331124326483.png)



7、打包命令:gradlew gem，出现BUILD SUCCESSFUL 则成功打包

![image-20200331124417259](C:\Users\jiang\AppData\Roaming\Typora\typora-user-images\image-20200331124417259.png)



8、如图所示，就是我们安装插件用的gem文件

![image-20200331124555157](C:\Users\jiang\AppData\Roaming\Typora\typora-user-images\image-20200331124555157.png)

9、安装自定义插件前，我们先修改服务器logstash的Gemfile文件，不然安装速度很慢，甚至被killed

```
vim /home/hadoop/logstash/logstash-7.2.0/Gemfile

```

修改source链接

![image-20200331124750043](C:\Users\jiang\AppData\Roaming\Typora\typora-user-images\image-20200331124750043.png)



10，然后可以上传gem文件进行安装

```
bin/logstash-plugin install --no-verify --local logstash-filter-java_filter_example-1.0.0.gem
```

![image-20200331124910569](C:\Users\jiang\AppData\Roaming\Typora\typora-user-images\image-20200331124910569.png)



11、测试，编写logstash配置文件

```
input {
  stdin {  }
}
filter {
  java_filter_example {}
}
output {
  stdout { codec => rubydebug }
}
```



启动logstash：

bin/logstash -f agent.conf 



输入hello，可看到相应输出olleh的信息倒序，则证明自定义插件测试安装成功，下面我们就可以修改example项目编写我们需要的自定义插件。













问题记录：



1、jre/lib目录缺少tool.jar

![image-20200331125306760](C:\Users\jiang\AppData\Roaming\Typora\typora-user-images\image-20200331125306760.png)

2、没有使用官网提供的命令gradlew.bat assemble进行build，使用了自己本地的gradle assemble，导致插件找不到

![image-20200331125458407](C:\Users\jiang\AppData\Roaming\Typora\typora-user-images\image-20200331125458407.png)