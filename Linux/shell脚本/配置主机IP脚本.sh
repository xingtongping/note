[db]
service_arr=postgresql.service,redis.service,xxl-job-executor.service

[tomcat]
service_arr=xxl-job-admin.service,flume.service,ssatomcat.service,soctomcat.service,nginx.service,mysql.service

[spark]
service_arr=zookeeper.service,kafka.service,spark_master.service,spark_slave.service,sparkjob.service

[es]
service_arr=elasticsearch.service


#==========================================================================================



#!/bin/bash


if [ $# -lt 1 ];then
        echo "please intput hostname ...."
        exit 1
fi

hostname=$1

#修改主机名
function modify_hostname(){
        hostname $hostname
        echo $hostname > /etc/hostname
}


#修改IP
function modify_IP(){
        ip=`cat /etc/hosts|grep $hostname|awk '{print $1}'`
        sed -i '/^IPADDR=/c IPADDR='$ip'' /etc/sysconfig/network-scripts/ifcfg-eth0:1
        systemctl restart network
}


#读取组件配置
function read_ini_file(){
    Section=$1  
    Key=$2  
    Configfile=$3  
    ReadStr=`awk -F '=' '/\['$Section'\]/{a=1}a==1&&$1~/'$Key'/{print $2;exit}' $Configfile`    
    echo "$ReadStr"    
}


#enable service
function enable_service(){
        #service config
        service_list="./service_list.ini"

        #get services of host
        service_arr_str=`read_ini_file "$hostname" "service_arr" "$service_list"`
        service_arr=(`echo $service_arr_str | tr ',' ' '`)
        for service_name in "${service_arr[@]}";do
                #echo $service_name"############"
                systemctl enable $service_name
                systemctl start  $service_name
        done

}

function disable_all_service(){
        hosts=(db tomcat spark es)
        for host in "${hosts[@]}";do
                #echo $host
                service_list="./service_list.ini"

                #get services of host
                service_arr_str=`read_ini_file "$host" "service_arr" "$service_list"`
                service_arr=(`echo $service_arr_str | tr ',' ' '`)
                for service_name in "${service_arr[@]}";do
                        #echo $service_name"############"
                        systemctl disable $service_name
                done
        done

}



modify_hostname

modify_IP

disable_all_service
enable_service

