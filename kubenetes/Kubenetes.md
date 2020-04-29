# Kubenetes

### 一、概念

优势：快速部署,方便，超强的横向扩容能力、故障迁移，服务不中断

可移植，可扩展的开源平台，用于管理容器化工作负载和服务，有助于声明性配置和自动化

### 二、安装

1、关闭关闭防火墙、selinux和swap

```
systemctl stop firewalld

systemctl disable firewalld

setenforce 0

sed -i "s/^SELINUX=enforcing/SELINUX=disabled/g" /etc/selinux/config

swapoff -a

sed -i 's/.*swap.*/#&/' /etc/fstab  
```

2、配置内核参数，将桥接的IPv4流量传递到iptables的链

```
cat > /etc/sysctl.d/k8s.conf <<EOF

net.bridge.bridge-nf-call-ip6tables = 1

net.bridge.bridge-nf-call-iptables = 1

EOF

sysctl --system
```

3、配置国内yum源

```
yum install -y wget

mkdir /etc/yum.repos.d/bak && mv /etc/yum.repos.d/*.repo /etc/yum.repos.d/bak

wget -O /etc/yum.repos.d/CentOS-Base.repo http://mirrors.cloud.tencent.com/repo/centos7_base.repo

wget -O /etc/yum.repos.d/epel.repo http://mirrors.cloud.tencent.com/repo/epel-7.repo

yum clean all && yum makecache
```

配置国内Kubernetes源

```
cat <<EOF > /etc/yum.repos.d/kubernetes.repo

[kubernetes]

name=Kubernetes

baseurl=https://mirrors.aliyun.com/kubernetes/yum/repos/kubernetes-el7-x86_64/

enabled=1

gpgcheck=1

repo_gpgcheck=1

gpgkey=https://mirrors.aliyun.com/kubernetes/yum/doc/yum-key.gpg https://mirrors.aliyun.com/kubernetes/yum/doc/rpm-package-key.gpg

EOF
```

配置 docker 源

```
wget https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo -O /etc/yum.repos.d/docker-ce.repo
```

4、安装docker

```
yum install -y docker-ce

systemctl enable docker && systemctl start docker

docker version
```

5、安装kubeadm、kubelet、kubectl

```
yum install -y kubelet kubeadm kubectl

systemctl enable kubelet
```

Kubelet负责与其他节点集群通信，并进行本节点Pod和容器生命周期的管理。Kubeadm是Kubernetes的自动化部署工具，降低了部署难度，提高效率。Kubectl是Kubernetes集群管理工具。

### 1、部署master节点

1、在master进行Kubernetes集群初始化。(修改IP)

```
kubeadm init  --apiserver-advertise-address=172.16.110.175  --image-repository registry.aliyuncs.com/google_containers  --service-cidr=10.1.0.0/16  --pod-network-cidr=10.244.0.0/16 --ignore-preflight-errors=Swap
```

这一步经常出错，查看日志的几个命令:tail -f /var/log/message     journalctl  -f ，注意内核是否达到要求，升级内核命令：yum install -y kernel 

初始化成功后最后会出现如下信息：保存，集群增加node节点的时候执行

```
kubeadm join 172.16.110.243:6443 --token mblo6r.af0hpa7gmjr7n69m \
    --discovery-token-ca-cert-hash sha256:32d4de649452778335bd713f38a4cbfb11c8b41ed816e9168ace19eaa72eaa34 --ignore-preflight-errors=Swap

```

生成不过期的token

```
kubeadm token create --ttl 0 --print-join-command
```

初始化失败，先执行kubeadm reset，重置,删掉/etc/kubenetes相关yaml文件，重新初始化  

2、配置kubectl工具

```
mkdir -p /root/.kube

cp /etc/kubernetes/admin.conf /root/.kube/config

[root@localhost ~]# kubectl get nodes
NAME     STATUS     ROLES    AGE     VERSION
master   NotReady   master   3m24s   v1.14.3
[root@localhost ~]# kubectl get cs
NAME                 STATUS    MESSAGE             ERROR
controller-manager   Healthy   ok                  
scheduler            Healthy   ok                  
etcd-0               Healthy   {"health":"true"}   
```

3、部署flannel网络

```
kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/a70459be0084506e4ec919aa1c114638878db11b/Documentation/kube-flannel.yml
```

如果镜像拉不下来，可以拉取阿里的，然后tag一下

```
docker pull registry.cn-hangzhou.aliyuncs.com/kubernetes
docker tag registry.cn-hangzhou.aliyuncs.com/kubernetes_containers/flann0-amd64
```



### 2、集群加入node节点

1、执行如下命令，使所有node节点加入Kubernetes集群

```
kubeadm join 172.16.110.243:6443 --token mblo6r.af0hpa7gmjr7n69m \
    --discovery-token-ca-cert-hash sha256:32d4de649452778335bd713f38a4cbfb11c8b41ed816e9168ace19eaa72eaa34 --ignore-preflight-errors=Swap
```

即上面master的初始化结果

### 3、部署检测

1.在master节点输入命令检查集群状态，返回Ready则结果则集群状态正常。

```
[root@localhost ~]# kubectl get nodes
NAME     STATUS     ROLES    AGE     VERSION
master   NotReady   master   7m34s   v1.14.3
node1    NotReady   <none>   78s     v1.14.3
node2    NotReady   <none>   70s     v1.14.3
```

刚加入的节点一开始一般都显示NotReady，过一会就好了，如果实在是出错自己检查日志查看具体错误



### 4、部署Dashboard，可视化插件

1、获取dashboard的yaml文件

```
wget https://raw.githubusercontent.com/kubernetes/dashboard/master/aio/deploy/recommended/kubernetes-dashboard.yaml
```

修改访问端口，https 30001访问

```
sed -i '/targetPort:/a\ \ \ \ \ \ nodePort: 30001\n\ \ type: NodePort' kubernetes-dashboard.yaml
```

2、部署Dashboard

```
kubectl create -f kubernetes-dashboard.yaml
```

3、创建完成后，检查相关服务运行状态

```
kubectl get deployment kubernetes-dashboard -n kube-system

kubectl get pods -n kube-system -o wide

kubectl get services -n kube-system

netstat -ntlp|grep 30001
```

一般会下载不下来镜像，我们可以像之前那样下载其他镜像，再做一个tag。要看准哪个node

```
# docker pull mirrorgooglecontainers/kubernetes-dashboard-amd64
[root@node1 ~]# docker tag siriuszg/kubernetes-dashboard-amd64  k8s.gcr.io/kubernetes-dashboard-amd64:v1.10.1
```

4、在Firefox浏览器输入Dashboard访问地址：https://192.168.120.10:30001

![1563531798544](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\1563531798544.png)









### 三、命令

查看配置文件字段说明

kubectl explain  pods.spec.containers

#查看节点

kubectl get node     

#查看该命名空间的pod详细信息

kubectl  get pod -n kube-system -o wide    



#删除node，master上运行

在master节点上执行

```
kubectl drain NODE_NAME --delete-local-data --force --ignore-daemonsets
kubectl delete node node2
```

在node节点上执行

```
kubeadm reset
ifconfig cni0 down
ip link delete cni0
ifconfig flannel.1 down
ip link delete flannel.1
rm -rf /var/lib/cni/
```



kubectl get cs
kubectl explain pods
kubectl -create -f pod-demo.yaml
kubectl logs pode-demo myapp

kubectl delete pod zk-0 --force

#进入pod shell

kubectl exec -it zk-0 /bin/bash



给pod打标签(管理)

kubectl label pods zk-2 xing=ping

kubectl get pods -l xing    查找有xing的标签



k8s运行容器

```
kubectl run my-nginx --image=nginx --replicas=2 --port=80
```



ConfigMap命令：

```
kubectl create configmap special-config --from-literal=flume-ip=172.16.110.173
kubectl get Configmap -o yaml
```





### 四、配置

大部分资源的配置清单：

apiVersion:v1
kind:Pod  资源类别
metadata:元数据  （嵌套）  
spec:规范  资源拥有什么样的特性     containers:{name:    images:    name:    images:     comand}   
status:

示例：

```
apiVersion: v1
kind: Pod
metadata:/
  name: rss-site
  labels:
    app: web
spec:
  containers:
    - name: front-end
      image: nginx
      ports:
        - containerPort: 80
    - name: rss-reader
      image: nickchase/rss-php-nginx:v1
      ports:
        - containerPort: 88
      commond:
      - "/bin/sh" #commond不会运行在shell，需自己指定
      - "-c"
      - "sleep 3600"

```

修改镜像中的默认应用   commond,args    可覆盖dockfile中ENTRYPOINT 和CMD



配置文件参数说明：

```
apiVersion: v1       #必选，版本号，例如v1
kind: Pod       #必选，Pod
metadata:       #必选，元数据
  name: string       #必选，Pod名称
  namespace: string    #必选，Pod所属的命名空间
   s:      #自定义标签
    - name: string     #自定义标签名字
  annotations:       #自定义注释列表
    - name: string
spec:         #必选，Pod中容器的详细定义
  containers:      #必选，Pod中容器列表
  - name: string     #必选，容器名称
    image: string    #必选，容器的镜像名称
    imagePullPolicy: [Always | Never | IfNotPresent] #获取镜像的策略 Alawys表示下载镜像 IfnotPresent表示优先使用本地镜像，否则下载镜像，Nerver表示仅使用本地镜像
    command: [string]    #容器的启动命令列表，如不指定，使用打包时使用的启动命令
    args: [string]     #容器的启动命令参数列表
    workingDir: string     #容器的工作目录
    volumeMounts:    #挂载到容器内部的存储卷配置
    - name: string     #引用pod定义的共享存储卷的名称，需用volumes[]部分定义的的卷名
      mountPath: string    #存储卷在容器内mount的绝对路径，应少于512字符
      readOnly: boolean    #是否为只读模式
    ports:       #需要暴露的端口库号列表
    - name: string     #端口号名称
      containerPort: int   #容器需要监听的端口号
      hostPort: int    #容器所在主机需要监听的端口号，默认与Container相同
      protocol: string     #端口协议，支持TCP和UDP，默认TCP
    env:       #容器运行前需设置的环境变量列表
    - name: string     #环境变量名称
      value: string    #环境变量的值
    resources:       #资源限制和请求的设置
      limits:      #资源限制的设置
        cpu: string    #Cpu的限制，单位为core数，将用于docker run --cpu-shares参数
        memory: string     #内存限制，单位可以为Mib/Gib，将用于docker run --memory参数
      requests:      #资源请求的设置
        cpu: string    #Cpu请求，容器启动的初始可用数量
        memory: string     #内存清楚，容器启动的初始可用数量
    livenessProbe:     #对Pod内个容器健康检查的设置，当探测无响应几次后将自动重启该容器，检查方法有exec、httpGet和tcpSocket，对一个容器只需设置其中一种方法即可
      exec:      #对Pod容器内检查方式设置为exec方式
        command: [string]  #exec方式需要制定的命令或脚本
      httpGet:       #对Pod内个容器健康检查方法设置为HttpGet，需要制定Path、port
        path: string
        port: number
        host: string
        scheme: string
        HttpHeaders:
        - name: string
          value: string
      tcpSocket:     #对Pod内个容器健康检查方式设置为tcpSocket方式
         port: number
       initialDelaySeconds: 0  #容器启动完成后首次探测的时间，单位为秒
       timeoutSeconds: 0   #对容器健康检查探测等待响应的超时时间，单位秒，默认1秒
       periodSeconds: 0    #对容器监控检查的定期探测时间设置，单位秒，默认10秒一次
       successThreshold: 0
       failureThreshold: 0
       securityContext:
         privileged:false
    restartPolicy: [Always | Never | OnFailure]#Pod的重启策略，Always表示一旦不管以何种方式终止运行，kubelet都将重启，OnFailure表示只有Pod以非0退出码退出才重启，Nerver表示不再重启该Pod
    nodeSelector: obeject  #设置NodeSelector表示将该Pod调度到包含这个label的node上，以key：value的格式指定
    imagePullSecrets:    #Pull镜像时使用的secret名称，以key：secretkey格式指定
    - name: string
    hostNetwork:false      #是否使用主机网络模式，默认为false，如果设置为true，表示使用宿主机网络
    volumes:       #在该pod上定义共享存储卷列表
    - name: string     #共享存储卷名称 （volumes类型有很多种）
      emptyDir: {}     #类型为emtyDir的存储卷，与Pod同生命周期的一个临时目录。为空值
      hostPath: string     #类型为hostPath的存储卷，表示挂载Pod所在宿主机的目录
        path: string     #Pod所在宿主机的目录，将被用于同期中mount的目录
      secret:      #类型为secret的存储卷，挂载集群与定义的secre对象到容器内部
        scretname: string  
        items:     
        - key: string
          path: string
      configMap:     #类型为configMap的存储卷，挂载预定义的configMap对象到容器内部
        name: string
        items:
        - key: string
          path: string

```





### 五、问题记录

1、搭建k8s首先检查内核是否达到要求   uname  -r   好像3.10-3.67，如果不升级内核，搭建过程出现错误，但是却几乎不提醒你内核问题，出现一些端口访问不了，api-server那些服务没起来。



2、node加入集群后，执行kubectl命令出现

![1563532147568](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\1563532147568.png)

出现这个问题的原因是kubectl命令需要使用kubernetes-admin来运行，解决方法如下，将主节点中的【/etc/kubernetes/admin.conf】文件拷贝到从节点相同目录下，然后如提示配置环境变量：
  mkdir -p $HOME/.kube
  sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
  sudo chown $(id -u):$(id -g) $HOME/.kube/config
也可以简单方法

echo "export KUBECONFIG=/etc/kubernetes/admin.conf" >> ~/.bash_profile
source ~/.bash_profile



3、k8s部署的kafka集群如何连接，其他机器如何访问

可通过NodePort的方式暴露端口



暴露端口的一些方法：

```
Nodeport、Loadbalancer和Ingress

让你的的pod可以被外网访问:
kubectl expose rc my-nginx --port=80 --type=LoadBalancer

上面是对外网暴露80端口，外放访问的端口可通过kubectl get services查看
说明：rc:replication controller

参考连接：
https://www.cnblogs.com/justmine/p/8628465.html
```



4、增加节点，token过期

```
kubeadm token create --ttl 0 --print-join-command
```

5、zookeeper集群搭建参考

```
https://kubernetes.io/docs/tutorials/stateful-application/zookeeper/
https://www.jianshu.com/p/2633b95c244c
```



6、spark-web-ui代理报错  

7、flume监听端口问题

8、如何固定IP，sts命名有规律，但是其他命名随机



stateful域名：  kafka-0.kafka-svc.default.svc.cluster.local

sts会自动编号，其他需自定义，如zk-0  zk-1  zk-2



### 六、后续

Ingress使用及案例

```
k8s集群内对外暴露服务

apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  name: my-ingress     
spec:
  rules:
  - host: xtp.my.com   --外部访问域名
    http:
      paths:
      - backend:
          serviceName: flume-nc    --暴露的服务名 
          servicePort: 514         --端口
          
          

```





EndPoint使用及案例

```
k8s访问集群外独立的服务最好的方式是采用Endpoint方式，以mysql服务为例：

1、创建mysql-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: mysql-test
spec:
  ports:
    - port: 3306
    
2、创建mysql-endpoints.yaml
kind: Endpoints
apiVersion: v1
metadata:
  name: mysql-test
  namespace: default
subsets:
  - addresses:
      - ip: 192.168.1.25     --外部服务地址
    ports:
      - port: 3306

3、验证
mysql -hmysql-test -u user -p  登陆

查看
 kubectl describe svc mysql-test
```





ConfigMap使用及案例：

```
把本地文件挂载到pod里面配置文件，防止每次修改配置都要重新打包镜像运行

1、创建configmap,指定本地文件
kubectl create configmap flume-config --from-file=./apache-flume-1.8.0-bin/conf/example.conf

2、在yaml使用configmap
```

![1564124316864](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\1564124316864.png)





k8s后台登陆进去出现forbidden问题解决方法：

```
1.获取dashboard secret

kubectl get secret -n kube-system

2.找到刚才创建的角色

获取token

kubectl describe secret dashboard-admin-token-jgpsv -n kube-system
```







1、让Master也当作node使用

kubectl taint node es3 node-role.kubernetes.io/master-



2、将Master恢复成Master Only状态

kubectl taint node es3 node-role.kubernetes.io/master="":NoSchedule