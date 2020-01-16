# Spark操作—aggregate、aggregateByKey

1、aggregate函数

​    将每个分区里面的元素进行聚合，然后用combine函数将每个分区的结果和初始值(zeroValue)进行combine操作。这个函数最终返回的类型不需要和RDD中元素类型一致。



```
val array = Array(("tong",25),("wei",27),("ping",23))
    val rdd = ss.sparkContext.parallelize(array)

    //x就是初始值，y是rdd循环每个值

    //格式rdd.aggregate(默认值)(函数1(参数1，参数2)={函数具体实现}，函数2（参数1，参数2）={函数具体实现})

    rdd.aggregate(("test",3))((x,y)=>{   //函数
      println("x._1:"+x._1)   //第一次预计输出为test
      println("x._2:"+x._2)   //第一次预计输出为3
      println("y:"+y)      //第一次预计输出为array(rdd)的第一个元素，即（"tong",25）
      y   //这里返回结果作为下次的输入，即x,第一次进来的输入采用默认值（"test",3）
    },(a,b)=>{
      println("a._1:"+a._1)   //预计为test
      println("a._2:"+a._2)   //预计为3
      println("b:"+b)
      b
    })
  
  输出结果：
    x._1:test
    x._2:3
    y:(tong,25)
    x._1:tong
    x._2:25
    y:(wei,27)
    x._1:wei
    x._2:27
    y:(ping,23)

    a._1:test
    a._2:3
    b:(ping,23)
  
  
 
 执行以上代码，根据输出结果好好理解一番，应该能理解了
```





2、aggregateByKey函数：

```
对PairRDD中相同的Key值进行聚合操作，在聚合过程中同样使用了一个中立的初始值。和aggregate函数类似，aggregateByKey返回值的类型不需要和RDD中value的类型一致。因为aggregateByKey是对相同Key中的值进行聚合操作，所以aggregateByKey'函数最终返回的类型还是PairRDD，对应的结果是Key和聚合后的值，而aggregate函数直接返回的是非RDD的结果。
```