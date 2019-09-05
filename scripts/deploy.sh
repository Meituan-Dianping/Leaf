#!/bin/sh

# start stage 9999
# stop
# reload stage 9999
# status

ARG_NUM=$#
METHOD_NAME=$1
SERVER_NAME="leaf"

#服务启动参数
SERVER_MODE=""
SERVER_PORT=""

#服务配置
JAVA_OPT="-server -Dfile.encoding=UTF-8 -Xms512m -Xmx512m"
JAR_PATH="/data/mall_leaf/leaf.jar"
LOG_PATH="/data/logs/leaf.log"

#检查进程状态
# $1 进程名
# $2 0 停止 1 运行
function checkProcess(){
    PROCESS_NAME=$1
    STATUS=$2
    CUR_TIME=0
    MAX_TIME=10
    while true;do
        if [[ $CUR_TIME -gt $MAX_TIME ]];then
            return 0
        fi

        PID=`ps aux | grep $SERVER_NAME | grep -v 'grep' | grep -v 'sh' | awk '{print $2}'`
        if [[ -z "$PID" && "$STATUS" == "0" ]];then
            return 1
        fi
        if [[ $PID -gt 0 && "$STATUS" == "1" ]];then
            return 1
        fi
        let "CUR_TIME++"
        sleep 1
    done
}

# 停止服务
function stop(){
    PID=`ps aux | grep $SERVER_NAME | grep -v 'grep' | grep -v 'sh' | awk '{print $2}'`
    if [[ -z "$PID" ]];then
        echo "stop   服务已经停止！"
    else
        echo "stop   服务进程ID: $PID"
        kill -15 $PID
        sleep 3
        checkProcess $SERVER_NAME 0 #检查服务是否在停止
        STATUS=$?
        if [[ "$STATUS" == "1" ]];then
            echo "stop   服务停止成功！"
        else
            echo "stop   服务停止失败！"
            exit -1
        fi
        fi

    }

# 启动服务
function start(){
    PID=`ps aux | grep $SERVER_NAME | grep -v 'grep' | grep -v 'sh' | awk '{print $2}'`
    if [[ -z "$PID" ]];then
        nohup java -jar $JAR_PATH $JAVA_OPT --Dspring.profiles.active=$SERVER_MODE --server.port=$SERVER_PORT > $LOG_PATH 2>&1 &

        checkProcess $SERVER_NAME 1  #检查服务是否在运行
        STATUS=$?
        if [[ "$STATUS" == "0" ]];then
            echo "服务运行失败！"
            exit -1
        else
            PID=`ps aux | grep $SERVER_NAME | grep -v 'grep' | grep -v 'sh' | awk '{print $2}'`
            echo "服务运行成功！PID: $PID"
        fi
    else
        echo "服务正在运行！PID: $PID"
        fi
    }

#重启服务
function reload(){
    stop
    start $SERVER_MODE $SERVER_PORT
}

#检查服务状态
function status(){
    PID=`ps aux | grep $SERVER_NAME | grep -v 'grep' | grep -v 'sh' | awk '{print $2}'`
    if [[ -z "$PID" ]];then
        echo "server stop"
    else
        echo "server is running PID: $PID"
    fi
}

#处理用户请求
if [[ "$METHOD_NAME" == "start" && $ARG_NUM -eq 3 ]];then
    SERVER_MODE=$2
    SERVER_PORT=$3
    echo "start server .... mode: $SERVER_MODE port: $SERVER_PORT"
    start $SERVER_MODE $SERVER_PORT
elif [[ "$METHOD_NAME" == "stop" && $ARG_NUM -eq 1 ]];then
    echo "stop server ...."
    stop
elif [[ "$METHOD_NAME" == "reload" && $ARG_NUM -eq 3 ]];then
    SERVER_MODE=$2
    SERVER_PORT=$3
    echo "reload server ...."
    reload $SERVER_MODE $SERVER_PORT
elif [[ "$METHOD_NAME" == "status" && $ARG_NUM -eq 1 ]];then
    echo "check server status ..."
    status
else
    echo "=================================================================="
    echo " you can use next method name:                                    "
    echo " start : start  mode port eg: start stage 9999                    "
    echo " stop  : stop                                                     "
    echo " reload: reload mode port eg: reload stage 9999                   "
    echo " status: status                                                   "
    echo "=================================================================="
fi


