### 安装Elasticsearch-Head
git下载Elasticsearch-Head
#安装git,若机器环境已存在，不需要再次安装
yum install git
#下载
git clone https://github.com/mobz/elasticsearch-head.git

使用npm安装
#安装nodejs环境，若机器环境已存在，不需要再次安装
yum install nodejs

#安装 (elasticsearch-head目录下执行)
npm install

修改Elasticsearch配置,允许跨域访问，修改后重新启动Elasticsearch
vi elasticsearch-7.1.1/config/elasticsearch.yml

#添加如下配置，支持跨域访问
http.cors.enabled: true
http.cors.allow-origin: "*"

启动

elasticsearch-head目录下执行

npm run start



或者试下：elasticsearch-head/elasticsearch-head/node_modules/grunt/bin/grunt server &