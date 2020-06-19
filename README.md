# SpringBoot-multiple-datasources-helper

Usually it needs to create custom datasource beans by using "@Configuration" class when you want to indtroduce multiple datasources into your SpringBoot project. Now there is a problem of using @Configuration class means that it will break auto configuration of main datasource bean which should be created by SpringBoot only when none of DataSource type beans exists, so we have to create both main datasource and our custom datasources manually in @Configuration class by ourselves in order to fix it. Besides, beans of type like PlatformTransactionManager and SqlSessionFactory need to be created by ourselves as well.

SpringBoot-multiple-datasources-helper helps us introduce multiple datasources integrated with Mybatis through configuration of type yaml and a little coding works. It's designed to get us take less effort to add any number datasources into our SpringBoot project without effecting creating process of main datasource bean and other relevant beans that be managed by SpringBoot autoconfiguration. 

### Only Tomcat and HikariCP db connection pool supported for custom datasources..

## Installation

SpringBoot-multiple-datasources-helper packages as a jar using Maven.  
1. Download source code and then package it or install it locally using Maven.
2. Add dependency tag into pom.xml of your SpringBoot project likes below:
   ```
   <dependency>
     <groupId>io.github.arvinrong</groupId>
     <artifactId>springboot-multiple-datasources-helper</artifactId>
      <version>0.1.0</version>
   </dependency>
   ```
3. Modify yml file of your project to define custom datasource by adding config coding block like below at root level which defines two datasources. And it's important to define the name and db pool type explicitly.
   ```
   system:
     db:
       data-sources:
         -
           driver-class-name: com.mysql.jdbc.Driver
           name: db1
           url: jdbc:mysql://127.0.0.1:3306/test?useUnicode=true&characterEncoding=utf-8&useSSL=false
           type: org.apache.tomcat.jdbc.pool.DataSource
           username: dbuser
           password: "dkeiFJRU55%"
           tomcat:
             max-active: 20
             max-idle: 10
             max-wait: 20000
             test-while-idle: true
             validation-query: select 1
             validation-query-timeout: 1
             test-on-borrow: true
             test-on-return: false
             remove-abandoned-timeout: 180
             remove-abandoned: true
         -
           driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
           name: db2
           url: jdbc:sqlserver://127.0.0.1:1433;databasename=MASTER
           username: sa
           password: "test@2016%%"
           type: org.apache.tomcat.jdbc.pool.DataSource
           tomcat:
             max-active: 20
             max-idle: 5
             max-wait: 120000
             test-while-idle: true
             validation-query: select 1
             validation-query-timeout: 1
             test-on-borrow: true
             test-on-return: false
             remove-abandoned-timeout: 180
             remove-abandoned: true
   ```
4. Defining @MapperScan for each datasource including main datasource which defined under "spring.datasource". I usually add @MapperScan annotation in **WebApplication class which our main method located in for main datasource, then create config classes for every custom datasources respectively. Below is demo for the two custom datasources defined in yml file above.
   ```markdown
   ### Class Db1MapperScanerConfig
   import org.mybatis.spring.annotation.MapperScan;
   import org.springframework.context.annotation.Configuration;
   
   @Configuration
   @MapperScan(basePackages = "io.github.arvinrong.db1.mapper", sqlSessionTemplateRef = "db1SqlSessionTemplate")
   public class Db1MapperScanerConfig {}
   
   
   ### Class Db2MapperScanerConfig
   import org.mybatis.spring.annotation.MapperScan;
   import org.springframework.context.annotation.Configuration;
   
   @Configuration
   @MapperScan(basePackages = "io.github.arvinrong.db2.mapper", sqlSessionTemplateRef = "db2SqlSessionTemplate")
   public class Db2MapperScanerConfig {}
   ```
Tips: Beans named of **db1SqlSessionTemplate** and **db2SqlSessionTemplate** are created automatically, the name convention is datasource name+"SqlSessionTemplate", and datasource name is defined in yml file above. Through this configuration class, each mapper class can use respective SqlSessionTemplate object with corresponding datasource in it.

5. Startup your SpringBoot project.

