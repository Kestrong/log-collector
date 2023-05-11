# log-collector 

一款轻量级日志收集框架，可以通过注解自动或者编程方式手动将日志信息存储到日志文件、数据库、elasticsearch、
http和feign方式转发到第三方服务等，支持定制自己的收集器。同时还提供全链路id和用户登录信息在微服务间传递的功能。

## quick start

 - 添加依赖 

    gradle:
    ```
    compile 'com.xjbg:log-collector-spring-boot-starter:${version}'
    ```

    maven:
    ```
        <dependency>
            <groupId>com.xjbg</groupId>
            <artifactId>log-collector-spring-boot-starter</artifactId>
            <version>${version}</version>
        </dependency>
    ```
   
 - 最小化配置

   以database收集器为例：开启收集器aop注解，设置默认收集器为database，然后初始化database收集器（线程池大小10，丢弃策略为丢弃最早的），开启过滤器传递链路id（请求头中的X-Request-Id）
    ```
      log:
        collector:
          enable: true
          application: test-log-collector
          defaultCollectorType: database
          filter:
            enable: true
            request-id-head-name: X-Request-Id       
          database:
            enable: true
            poolSize: 10
            rejectPolicy: discard_oldest
    ```
    
    日志收集：
    ```
        //①编程式收集日志
        LogCollectors.defaultCollector().log(LogInfo.builder().build());
        //②通过注解方式收集日志
        @CollectorLog(businessNo = "#SensitiveStrategy.ACCESS_KEY.apply(#userInfo.userName)")
        public void addUser(UserInfo userInfo) {

        }
        //③通过配置属性中的pointcut属性统一收集指定路径的日志
        //更多内容查看test下面的测试用例
    ```
   
## 拓展插件功能

 - 开发新的插件
   

   通过实现`com.xjbg.log.collector.api.LogCollector`接口或者继承`com.xjbg.log.collector.api.impl.AbstractLogCollector`抽象类。


 - 自定义日志转换器


   通过实现`com.xjbg.log.collector.transformer.LogTransformer`接口，默认实现有`DefaultLogTransformer`。


 - 自定义队列实现


   通过继承`com.xjbg.log.collector.channel.Channel`抽象类，默认实现有`MemoryChannel`。


 - 自定义http请求头


   通过实现`com.xjbg.log.collector.token.IHttpTokenCreator`接口，默认实现有`DefaultHttpTokenCreator`和`BasicAuthHttpTokenCreator`。


 - 自定义用户token解析器


   通过继承`com.xjbg.log.collector.spring.retriever.UserIdRetriever`抽象类，默认实现有`SimpleUserIdRetriever`和`Base64JsonTokenUserIdRetriever`。


 - 如何限流


   通过设置`channel`对象的`capacity`和`byteCapacity`来限制队列的容量，通过`byteSpeed`和`recordSpeed`来分别限制每秒字节数bps和每秒记录数tps。


 - 如何透传全链路id和用户id


   通过开启`filter`来生成和传递链路id、用户信息。


 - 如何对日志内容脱敏和过滤属性


   通过`ignore-properties`属性忽略或者`@CollectorLog.Sensitive`注解来自定义脱敏策略。


 - 定时清理日志


   通过开启`clean-up`属性和`max-history`来清理过期的日志信息，内置的收集器仅`database`和`es`支持。


## 附录

### 表模型和索引结构

查看**log-collector-spring-boot-starter**模块单元测试resources目录下的`log_info_es_mapping.json`、`log_info_mysql_ddl.sql`文件

### 配置说明

```
log:
  collector:
    enable: true #是否启用 默认true
    application: ${spring.application.name} #应用的名称
    defaultCollectorType: feign #默认收集器的类型 内置默认可选类型[noop|common|database|http|es|feign]
    ignore-properties: #json序列化时忽略的属性
    clean-up: true #是否定时清理日志 仅databse和es支持 默认false
    max-history: 30 #日志保留天数 大于0时生效 默认30
    filter:
      enable: true #是否开启全局过滤器 生成链路id 默认false
      request-id-head-name: X-Request-Id #链路id的请求头名称 默认X-Request-Id
      user-token-head-name: Authorization #用户信息的请求头名称 默认Authorization
      user-property-name: userId #用户id的属性名称 默认userId
      user-id-retriever: com.xjbg.log.collector.retriever.Base64JsonTokenUserIdRetriever #提取用户信息中的用户id值的类，默认NULL
      allowed-headers: #允许传递给其他服务的请求头 默认空 
        - User-Agent
      order: -1 #过滤器优先级 如果是网关需要小于-1才能拿到返回值
      log-paths: #配置之后对匹配的路径自动记录日志 默认为空不启用
        - Ant:/user/**
        - Rex:.*/user/.*
      exclude-paths: #忽略的请求路径 优先于上面的log-paths
        - Ant:/user/download
        - Ant:/user/file
      consume-media-type: #过滤器如果配置记录日志 此配置用于判断content-type符合才记录request和response的body 
        - application/json
    pointcut: execution(public * com.xjbg.log.collector.starter.example.*.*(..)) #统一拦截的切面 默认空不生效
    database:
      #共同属性
      enable: true #是否启用 默认false
      channel:
        channelClass: #自定义队列的全限定类名（需要有默认构造方法）
        capacity: #队列容量的记录大小 默认10000
        byteCapacity: #队列容量的内存大小 默认8M
        byteSpeed: #bps 每秒字节数 默认1M
        recordSpeed: 100 #tps 每秒记录数 默认1000
        flowControlInterval: 1000 #间隔多久检查一次流量 默认3000 单位：毫秒
        threshold: 0.8 # 队列容量的记录大小阈值 到达阈值时将触发拒绝策略 默认0.8
      fallbackCollector: #当收集器发送错误时降级处理的处理器类型
      poolSize: 10 #开启线程池 默认0不开启
      batchSize: 10 #异步写入时的批次大小 大于0时生效 默认1
      rejectPolicy: discard_oldest #队列拒绝策略 默认noop不丢失阻塞等待 可选[noop|fallback|caller_runs|discard|discard_oldest]
      logTransformerImpl: #自定义日志信息转换器 可以是spring的beanName 或者 全限定类名（需要有默认构造方法） 默认不处理
      properties:
        key: value
      #个性化属性
      tableName: log_info #日志表名 默认log_info
      wrapper: '`' #用来包裹表名或字段名的符号 默认空
    http:
      enable: true
      #省略共同属性
      url: https://www.12306.cn/mormhweb/?a=b #请求的地址
      charset: utf-8 #编码
      method: post #请求方法 默认post 可选[post|put]
      tokenHeaderCreator: #自定义http收集器的请求头 可以是spring的beanName 或者 全限定类名（需要有默认构造方法） 默认不处理
      json:
        naming-strategy: camel_case #属性命名策略 默认camel_case小写驼峰 可选[camel_case|snake_case|upper_snake_case|upper_camel_case|kebab_case]
        dateFormat: #时间格式化 默认yyyy-MM-dd HH:mm:ss
      connection:
        connectTimeout: 5000
        socketTimeout: 30000
        requestTimeout: 5000
        maxConnect: 60
        maxConnectPerRoute: 30
        ignoreHttps: true #忽略https不提倡生产使用 默认false
        certFile: #服务器证书路径
    es:
      enable: true
      #省略共同属性
      version: 7 #版本 默认7 可选[7|8]
      index: log_info_index #索引名称 默认log_info_index
      hosts: ${es.hosts} #ip端口信息 多个用,分开
      schema: http
      clusterName: es-cluster
      username: ${es.userName}
      password: ${es.password}
      json:
        naming-strategy: upper_snake_case
        dateFormat:
      connection:
        connectTimeout: 5000
        socketTimeout: 30000
        requestTimeout: 5000
        maxConnect: 60
        maxConnectPerRoute: 30
        ignoreHttps: false
        certFile:
    feign:
      enable: true
      #省略共同属性
      method: post #请求方法 默认post 可选[post|put]
      name: ${spring.application.name} #微服务注册中心实例名称
      path: /user/log-info #接口
      url: http://localhost:8080 #请求地址 默认为空使用name去解析
```

### 依赖组件信息

仅列出开发时使用的版本信息参考，高版本或低版本兼容性未测试。

#### 必选


  * org.openjdk.jol:jol-core:0.17
  * jakarta.json:jakarta.json-api:2.1.1
  * com.fasterxml.jackson:jackson-bom:2.14.2
  * org.apache.commons:commons-lang3:3.9
  * commons-collections:commons-collections:3.2


#### 可选

  * org.apache.httpcomponents:httpclient:4.5.12
  * org.elasticsearch:elasticsearch:\[7.5.1-8.5.1]
  * org.elasticsearch.client:elasticsearch-rest-client:8.5.1  
  * co.elastic.clients:elasticsearch-java:8.5.1
  * org.elasticsearch.client:elasticsearch-rest-high-level-client:7.5.1
  * io.github.openfeign:feign-httpclient:10.10.1
  * com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2.1.4.RELEASE
  * com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-config:2.1.4.RELEASE
  * org.springframework.cloud:spring-cloud-dependencies:Hoxton.SR9
  * org.springframework.boot:spring-boot-dependencies:2.3.2.RELEASE
  * org.springframework:spring-framework-bom:5.2.23.RELEASE