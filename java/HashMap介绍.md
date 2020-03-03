### HashMap介绍

##### 1、hashMap的数据结构

哈希表结构（链表散列：数组+链表）实现，结合数组和链表的优点。当链表长度超过8时，链表转换为红黑树。

![image-20200219095142791](C:\Users\jiang\AppData\Roaming\Typora\typora-user-images\image-20200219095142791.png)



##### 2、hashMap的工作原理

HashMap底层是hash数组和单向链表实现，数组中的每个元素都是链表，由Node内部类（实现Map.Entry<k,V>接口）实现，HashMap通过put&get方法存储和获取。

**存储对象时，将K/V键值对传给put（）方法；**

①、调用hash（K）方法计算K的hash值，然后结合数组长度，计算得数组下标；

②、调整数组大小（当容器中得元素个数大于capacity*loadFactor时，容器会进行resize为2n）

③、

i、如果K的hash值在HashMap不存在，则执行插入；若存在，则发生碰撞；

ii、如果K的hash值在HashMap存在，且它们两者equals返回true，则更新键值对；

iii、如果K的hash值在HashMap存在，且它们两者equals返回false，则插入链表的尾部（尾插法）或者红黑树（树的添加方式）

（JDK1.7 之前使用头插法、JDK 1.8 使用尾插法）

（注意：当碰撞导致链表大于TREEIFY_THRESHOLD = 8时，就把链表转换为红黑树）

**获取对象时，将K传给get（）方法：**

①、调用hash（K）方法（计算K的hash值）从而获取该键值对所在链表的数组下标；

②、顺序遍历链表，equals（）方法查找相同Node链表K值对应的V值

hashCode是定位的，存储位置；

equals是定性的，比较两者是否相等。





##### hashMap其他相关知识：

https://www.cnblogs.com/Roni-i/p/10404829.html