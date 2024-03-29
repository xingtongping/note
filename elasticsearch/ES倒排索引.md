## ES倒排索引

**由于不是由记录来确定属性值，而是由属性值来确定记录的位置，因而称为倒排索引**

## 原始记录

> 文章1的内容为:Tom lives in Guangzhou, I live in Guangzhou too.
>
> 文章2的内容为:He once lived in Shanghai.

## 取得关键词（字符过滤器->分词器->Token 过滤器）

获取关键词的步骤如下： 

1.使用空格对文章内容进行分隔 

2.去掉文章中没有实际意义的词，如：“in”，“once”，“too”，如果是在中文中，那么可能是“的”，“是”，这些不代表概念的词是可以过滤的 

3.统一大小写：用户希望查询“He”和“he”是都能查询出结果 

4.去掉时态，如：“lives”，“lived” 还原成“live” 

5.文章中的标点符号通常不代表某种概念，也可以去掉

通过上诉步骤分词得到的结果如下：

> 文章1的内容为:[tom] [live] [guangzhou] [i [live] [guangzhou]
>
> 文章2的内容为:[he] [live] [hanghai]
>
> 

**字符过滤器**

首先，字符串按顺序通过每个 *字符过滤器* 。他们的任务是在分词前整理字符串。一个字符过滤器可以用来去掉HTML，或者将 `&` 转化成 `and`。

**分词器**

其次，字符串被 *分词器* 分为单个的词条。一个简单的分词器遇到空格和标点的时候，可能会将文本拆分成词条。

**Token 过滤器**

最后，词条按顺序通过每个 *token 过滤器* 。这个过程可能会改变词条（例如，小写化 `Quick` ），删除词条（例如， 像 `a`， `and`， `the` 等无用词），或者增加词条（例如，像 `jump` 和 `leap` 这种同义词）。



## 建立倒排索引

上面的对应关系是："文章号"对"文章中的所有关键词"，倒排索引就是将这个关系倒过来，变成："关键词"对"拥有该关键词的文章号"，文章1和文章2经过倒排后得到的关系如下：

| 关键词    | 文章号 |
| --------- | ------ |
| guangzhou | 1      |
| he        | 2      |
| i         | 1      |
| live      | 1,2    |
| shanghai  | 1      |
| tom       | 1      |

知道了关键词在哪些文章中出现，我们还要知道关键词在文章中出现的次数和位置，通常有两种位置：

- 字符位置：该词是文章中的第几个字符（有点是显示并定位关键词快）
- 关键词**位置**：记录该词是**文章中的第几个关键词**（节约索引空间，词组查询块）

如下表是：加上“出现频率”和“出现位置”的结果

| 关键词    | 文章号[出现频率] | 出现位置 |
| --------- | ---------------- | -------- |
| guangzhou | 1 [2]            | 3,6      |
| he        | 2 [1]            | 1        |
| i         | 1 [1]            | 4        |
| live      | 1 [2]  2[1]      | 2,5 2    |
| shanghai  | 1 [1]            | 3        |
| tom       | 1 [1]            | 1        |

以live为例，在文章1中出现了2次，在文章2中出现了1次，在文章1中出现的2次分别为：文章1的第2个关键词和第5个关键词；在文章2中出现的1次为：文章2中的第2个关键词

## 实现

实现时，Lucene将上面的三列分别作为term Dictionary（词典文件） 和 frequencies（频率文件）和positions（位置文件）保存，其中字典文件不仅保存了每个关键词，**还保存了指向频率文件和位置文件的指针**

Lucene中使用了field的概念，用于表达信息所在的位置（如标题中，文章中，URL中，即：title=xxxxx;article=xxxx..）,在建立索引时，该field信息也记录在词典文件中，每个关键词都属于一个或多个field

## 压缩算法

为了减小索引文件的大小，Lucene还使用了压缩技术

首先，对词典中的关键词进行了压缩，关键词压缩为<前缀长度，后缀>，例如：当前词为“阿拉伯语”，上一个词为“阿拉伯”，那么“阿拉伯语”压缩为<3,语>

其次大量用到是对数字的压缩，数字只是保存与上一个值的差值（这样可以减少数字的长度，进而减少保存该数字需要的字节数），例如当前文章号为16389，不压缩要用3个字节保存，上一个文章的文章号为16382，压缩后保存7（只要用1个字节）

## 应用场景

下面我们通过对索引的查询来说明一下为什么要建立索引

假设我们要查询“live”，Lucene先对索引文件进行二元查找，找到该词，然后通过指向频率文件的指针读出所有的文章号，然后返回结果，词典文件通常非常小，因而整个过程的时间通常是毫秒级别的

## 总结

倒排索引就是经过过滤、分词、转换 拆分成一个个单词，记录每个单词的频率和所在位置。



查看索引分词器：

http://192.168.10.22:9200/_mapping/event_20200311/