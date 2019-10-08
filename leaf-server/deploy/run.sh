#!/usr/bin/env bash



# 程序执行的根路径
PROJ_DIR=./target

# 程序目标
PROJ_TARGET_JAR=${PROJ_DIR}/leaf.jar
JAVA_CMD=java
JVM_ARGS="-jar"

CMD="${JAVA_CMD} ${JVM_ARGS}  ${PROJ_TARGET_JAR}"
echo "Leaf Start--------------"
echo "JVM ARGS   ${JVM_ARGS}"
echo "->"
echo "PROJ_TARGET_JAR ${PROJ_TARGET_JAR}"
echo "------------------------"
$CMD
