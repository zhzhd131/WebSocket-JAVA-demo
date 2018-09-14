## 环境要求
jdk 1.8

## 运行  

cd 项目路径
mvn clean package -Dmaven.test.skip=true
cd target/
java -jar WebSocket-JAVA-demo-1.jar  

# 使用说明

###配置参数
| 名称| 说明|
|----|----|
|accessKey |用户accessKey|
|    secretKey | 用户secretKey|
|   host      | websocket地址|
|   protocol  | 协议|
|    path     |  请求路径|
|    port     |  端口号|



