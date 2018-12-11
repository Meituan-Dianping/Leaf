


# 程序执行的根路径
PROJ_DIR=./target

# 服务的lib库路径
PROJ_LIB_DIR=${PROJ_DIR}/lib
PROJ_CLASSES_DIR=${PROJ_DIR}/classes
PROJ_CLASSPATH=${PROJ_CLASSES_DIR}
LIBS=`ls ${PROJ_LIB_DIR}`
for LIB_JAR in ${LIBS}
do
    PROJ_CLASSPATH+=":${PROJ_LIB_DIR}/${LIB_JAR}"
done
MAIN_CLASS="com.sankuai.inf.leaf.server.LeafServerApplication"
JAVA_CMD=java
JVM_ARGS="-server"

CMD="${JAVA_CMD} ${JVM_ARGS} -cp ${PROJ_CLASSPATH} ${MAIN_CLASS}"
echo "Leaf Start--------------"
echo "JVM ARGS   ${JVM_ARGS}"
echo "->"
echo "CLASS_PATH ${PROJ_CLASSPATH}"
echo "->"
echo "MAIN_CLASS ${MAIN_CLASS}"
echo "------------------------"
$CMD
