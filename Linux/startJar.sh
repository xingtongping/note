#!/bin/bash
cd `dirname $0`
CUR_SHELL_DIR=`pwd`
CUR_SHELL_NAME=`basename ${BASH_SOURCE}`
#修改这里jar包名即可
JAR_NAME="tb-web-1.0.0-RELEASE.jar"
PROJECT_NAME="tb-api"
JAR_PATH=$CUR_SHELL_DIR/$JAR_NAME
JAVA_MEM_OPTS=" -server -Xms512m -Xmx1024m -XX:PermSize=512m"
#JAVA_MEM_OPTS=""
SPRING_PROFILES_ACTIV="-Dspring.profiles.active=release"
SPRING_CONFIG_NAME="--spring.config.name=application"
SPRING_CONFIG_LOCATION="--spring.config.location=./config/"
JVM_PORT="6081"
#SPRING_PROFILES_ACTIV=""
#LOG_DIR=$CUR_SHELL_DIR/logs
PROJECT_DIR=/data/logs/tb-api-nohup/$PROJECT_NAME
LOG_DIR=$PROJECT_DIR/nohup
LOG_PATH=$LOG_DIR/nohup.out

echo_help()
{
    echo -e "syntax: sh $CUR_SHELL_NAME start|stop"
}
if [ -z $1 ];then
    echo_help
    exit 1
fi
if [ ! -d "$PROJECT_DIR" ];then
    mkdir "$PROJECT_DIR"
fi
if [ ! -d "$LOG_DIR" ];then
    mkdir "$LOG_DIR"
fi
if [ ! -f "$LOG_PATH" ];then
    touch "$LOG_DIR"
fi
if [ "$1" == "start" ];then
# check server
    PIDS=`ps --no-heading -C java -f --width 1000 | grep $JAR_NAME | awk '{print $2}'`
    if [ -n "$PIDS" ]; then
        echo -e "ERROR: The $JAR_NAME already started and the PID is ${PIDS}."
        exit 1
    fi
echo "Starting the $JAR_NAME..."
 
    # startnohup java $JAVA_MEM_OPTS -jar -Dloader.path="lib/" $SPRING_PROFILES_ACTIV $JAR_PATH --server.port=8081 >> $LOG_PATH 2>&1 &
    nohup java $JAVA_MEM_OPTS -jar $SPRING_PROFILES_ACTIV $JAR_PATH $SPRING_CONFIG_NAME $SPRING_CONFIG_LOCATION --server.port=$JVM_PORT >> $LOG_PATH 2>&1 &
    COUNT=0
    while [ $COUNT -lt 1 ]; do
        sleep 1
        COUNT=`ps  --no-heading -C java -f --width 1000 | grep "$JAR_NAME" | awk '{print $2}' | wc -l`
        if [ $COUNT -gt 0 ]; then
            break
        fi
    done
    PIDS=`ps  --no-heading -C java -f --width 1000 | grep "$JAR_NAME" | awk '{print $2}'`
    echo "${JAR_NAME} Started and the PID is ${PIDS}."
    echo "You can check the log file in ${LOG_PATH} for details."
 
elif [ "$1" == "stop" ];then
 
    PIDS=`ps --no-heading -C java -f --width 1000 | grep $JAR_NAME | awk '{print $2}'`
    if [ -z "$PIDS" ]; then
        echo "ERROR:The $JAR_NAME does not started!"
        exit 1
    fi
echo -e "Stopping the $JAR_NAME..."
 
    for PID in $PIDS; do
        kill $PID > /dev/null 2>&1
    done
 
    COUNT=0
    while [ $COUNT -lt 1 ]; do
        sleep 1
        COUNT=1
        for PID in $PIDS ; do
            PID_EXIST=`ps --no-heading -p $PID`
            if [ -n "$PID_EXIST" ]; then
                COUNT=0
                break
            fi
        done
    done
 
    echo -e "${JAR_NAME} Stopped and the PID is ${PIDS}."
else
    echo_help
    exit 1
fi
[gama@tb-app tbapi]$ 
[gama@tb-app tbapi]$ cat appStart-data-change.sh 
#!/bin/bash
cd `dirname $0`
CUR_SHELL_DIR=`pwd`
CUR_SHELL_NAME=`basename ${BASH_SOURCE}`
#修改这里jar包名即可
JAR_NAME="tb-web-1.0.0-RELEASE.jar"
PROJECT_NAME="tb-api"
JAR_PATH=$CUR_SHELL_DIR/$JAR_NAME
JAVA_MEM_OPTS=" -server -Xms512m -Xmx1024m -XX:PermSize=512m"
#JAVA_MEM_OPTS=""
SPRING_PROFILES_ACTIV="-Dspring.profiles.active=release"
SPRING_CONFIG_NAME="--spring.config.name=application"
SPRING_CONFIG_LOCATION="--spring.config.location=./config/"
JVM_PORT="6081"
#SPRING_PROFILES_ACTIV=""
#LOG_DIR=$CUR_SHELL_DIR/logs
PROJECT_DIR=/data/logs/tb-api-nohup/$PROJECT_NAME
LOG_DIR=$PROJECT_DIR/nohup
LOG_PATH=$LOG_DIR/nohup.out

echo_help()
{
    echo -e "syntax: sh $CUR_SHELL_NAME start|stop"
}
if [ -z $1 ];then
    echo_help
    exit 1
fi
if [ ! -d "$PROJECT_DIR" ];then
    mkdir "$PROJECT_DIR"
fi
if [ ! -d "$LOG_DIR" ];then
    mkdir "$LOG_DIR"
fi
if [ ! -f "$LOG_PATH" ];then
    touch "$LOG_DIR"
fi
if [ "$1" == "start" ];then
# check server
    PIDS=`ps --no-heading -C java -f --width 1000 | grep $JAR_NAME | awk '{print $2}'`
    if [ -n "$PIDS" ]; then
        echo -e "ERROR: The $JAR_NAME already started and the PID is ${PIDS}."
        exit 1
    fi
echo "Starting the $JAR_NAME..."
 
    # startnohup java $JAVA_MEM_OPTS -jar -Dloader.path="lib/" $SPRING_PROFILES_ACTIV $JAR_PATH --server.port=8081 >> $LOG_PATH 2>&1 &
    nohup java $JAVA_MEM_OPTS -jar $SPRING_PROFILES_ACTIV $JAR_PATH $SPRING_CONFIG_NAME $SPRING_CONFIG_LOCATION --server.port=$JVM_PORT >> $LOG_PATH 2>&1 &
    COUNT=0
    while [ $COUNT -lt 1 ]; do
        sleep 1
        COUNT=`ps  --no-heading -C java -f --width 1000 | grep "$JAR_NAME" | awk '{print $2}' | wc -l`
        if [ $COUNT -gt 0 ]; then
            break
        fi
    done
    PIDS=`ps  --no-heading -C java -f --width 1000 | grep "$JAR_NAME" | awk '{print $2}'`
    echo "${JAR_NAME} Started and the PID is ${PIDS}."
    echo "You can check the log file in ${LOG_PATH} for details."
 
elif [ "$1" == "stop" ];then
 
    PIDS=`ps --no-heading -C java -f --width 1000 | grep $JAR_NAME | awk '{print $2}'`
    if [ -z "$PIDS" ]; then
        echo "ERROR:The $JAR_NAME does not started!"
        exit 1
    fi
echo -e "Stopping the $JAR_NAME..."
 
    for PID in $PIDS; do
        kill $PID > /dev/null 2>&1
    done
 
    COUNT=0
    while [ $COUNT -lt 1 ]; do
        sleep 1
        COUNT=1
        for PID in $PIDS ; do
            PID_EXIST=`ps --no-heading -p $PID`
            if [ -n "$PID_EXIST" ]; then
                COUNT=0
                break
            fi
        done
    done
 
    echo -e "${JAR_NAME} Stopped and the PID is ${PIDS}."
else
    echo_help
    exit 1
fi
