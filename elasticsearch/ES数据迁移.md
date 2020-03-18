### ES数据迁移

#### 场景

1、当你的数据量过大，而你的索引最初创建的分片数量不足，导致数据入库较慢的情况，此时需要扩大分片的数量，此时可以尝试使用Reindex。

2、当数据的mapping需要修改，但是大量的数据已经导入到索引中了，重新导入数据到新的索引太耗时；但是在ES中，一个字段的mapping在定义并且导入数据之后是不能再修改的，



所以这种情况下也可以考虑尝试使用Reindex。

常规的如果我们只是进行少量的数据迁移利用普通的reindex就可以很好的达到要求,但是当我们发现我们需要迁移的数据量过大时,我们会发现reindex的速度会变得很慢”

```
POST _reindex
{
  "source": {
    "index": "old"
    "size": 5000
  },
  "dest": {
    "index": "new"
  }}
```

curl _XPOST 'ES地址:9200/_reindex' -d{"source":{"index":"old_index"},"dest":{"index":"new_index"}}

