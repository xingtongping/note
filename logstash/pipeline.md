### pipeline 

Here is a simple example of this configuration.

```yaml
# config/pipelines.yml
- pipeline.id: upstream
  config.string: input { stdin {} } output { pipeline { send_to => [myVirtualAddress] } }
- pipeline.id: downstream
  config.string: input { pipeline { address => myVirtualAddress } }
```

下面是一个分发服务器模式配置示例。

```yaml
# config/pipelines.yml

- pipeline.id: beats-server
  config.string: |
    input { beats { port => 5044 } }
    output {
        if [type] == apache {
          pipeline { send_to => weblogs }
        } else if [type] == system {
          pipeline { send_to => syslog }
        } else {
          pipeline { send_to => fallback }
        }
    }
- pipeline.id: weblog-processing
  config.string: |
    input { pipeline { address => weblogs } }
    filter {
       # Weblog filter statements here...
    }
    output {
      elasticsearch { hosts => [es_cluster_a_host] }
    }
- pipeline.id: syslog-processing
  config.string: |
    input { pipeline { address => syslog } }
    filter {
       # Syslog filter statements here...
    }
    output {
      elasticsearch { hosts => [es_cluster_b_host] }
    }
- pipeline.id: fallback-processing
    config.string: |
    input { pipeline { address => fallback } }
    output { elasticsearch { hosts => [es_cluster_b_host] } }
```

下面是一个使用输出隔离器模式的场景示例。

```yaml
# config/pipelines.yml
- pipeline.id: intake
  queue.type: persisted
  config.string: |
    input { beats { port => 5044 } }
    output { pipeline { send_to => [es, http] } }
- pipeline.id: buffered-es
  queue.type: persisted
  config.string: |
    input { pipeline { address => es } }
    output { elasticsearch { } }
- pipeline.id: buffered-http
  queue.type: persisted
  config.string: |
    input { pipeline { address => http } }
    output { http { } }
```

下面是分叉路径配置的一个示例。

```yaml
# config/pipelines.yml
- pipeline.id: intake
  queue.type: persisted
  config.string: |
    input { beats { port => 5044 } }
    output { pipeline { send_to => ["internal-es", "partner-s3"] } }
- pipeline.id: buffered-es
  queue.type: persisted
  config.string: |
    input { pipeline { address => "internal-es" } }
    # Index the full event
    output { elasticsearch { } }
- pipeline.id: partner
  queue.type: persisted
  config.string: |
    input { pipeline { address => "partner-s3" } }
    filter {
      # Remove the sensitive data
      mutate { remove_field => 'sensitive-data' }
    }
    output { s3 { } } # Output to partner's bucket
```



下面是收集器模式的一个示例。

```yaml
# config/pipelines.yml
- pipeline.id: beats
  config.string: |
    input { beats { port => 5044 } }
    output { pipeline { send_to => [commonOut] } }
- pipeline.id: kafka
  config.string: |
    input { kafka { ... } }
    output { pipeline { send_to => [commonOut] } }
- pipeline.id: partner
  # This common pipeline enforces the same logic whether data comes from Kafka or Beats
  config.string: |
    input { pipeline { address => commonOut } }
    filter {
      # Always remove sensitive data from all input sources
      mutate { remove_field => 'sensitive-data' }
    }
    output { elasticsearch { } }
```

