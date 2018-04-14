/*
  Copyright 2018 Arvin Rong

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package io.github.arvinrong.mds.springboot.autoconfiguration;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import javax.sql.DataSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MultiDataSourceHolder {
    private MultiDataSourceProperties properties;
    private Map<String, DataSource> multiDataSources = Collections.synchronizedMap(new HashMap<>());
    private Map<String, PlatformTransactionManager> multiPlatformTransactionManagers = Collections.synchronizedMap(new HashMap<>());
    private Map<String, SqlSessionFactory> sqlSessionFactories = Collections.synchronizedMap(new HashMap<>());
    private Map<String, SqlSessionTemplate> sqlSessionTemplates = Collections.synchronizedMap(new HashMap<>());
    private Map<String, String> multiDataSourcePoolPropertyPrefixes = Collections.synchronizedMap(new HashMap<>());

    public void addSqlSessionFactory(String sqlSessionFactoryName, SqlSessionFactory sqlSessionFactory) {
        if (sqlSessionFactoryName != null && sqlSessionFactory != null && !sqlSessionFactories.containsKey(sqlSessionFactoryName)) {
            sqlSessionFactories.put(sqlSessionFactoryName, sqlSessionFactory);
        }
    }

    public void addSqlSessionTemplate(String sqlSessionTemplateName, SqlSessionTemplate sqlSessionTemplate) {
        if (sqlSessionTemplateName != null && sqlSessionTemplate != null && !sqlSessionTemplates.containsKey(sqlSessionTemplateName)) {
            sqlSessionTemplates.put(sqlSessionTemplateName, sqlSessionTemplate);
        }
    }

    public void addDataSource(String dataSourceName, DataSource dataSource) {
        if (dataSourceName == null && dataSource == null && !multiDataSources.containsKey(dataSourceName)) {
            multiDataSources.put(dataSourceName, dataSource);
        }
    }

    public void addPlatformTransactionManager(String platformTransactionManagerName, PlatformTransactionManager platformTransactionManager) {
        if (platformTransactionManagerName != null && platformTransactionManager != null
                && !multiPlatformTransactionManagers.containsKey(platformTransactionManagerName)) {
            multiPlatformTransactionManagers.put(platformTransactionManagerName, platformTransactionManager);
        }
    }

    public MultiDataSourceHolder(MultiDataSourceProperties properties) {
        this.properties = properties;
    }

    public Map<String, DataSource> getMultiDataSources() {
        return multiDataSources;
    }

    public void setMultiDataSources(Map<String, DataSource> multiDataSources) {
        this.multiDataSources = Collections.synchronizedMap(multiDataSources);
    }

    public Map<String, PlatformTransactionManager> getMultiPlatformTransactionManager() {
        return multiPlatformTransactionManagers;
    }

    public void setMultiPlatformTransactionManager(Map<String, PlatformTransactionManager> multiPlatformTransactionManagers) {
        this.multiPlatformTransactionManagers = multiPlatformTransactionManagers;
    }

    public MultiDataSourceProperties getProperties() {
        return properties;
    }

    public void setProperties(MultiDataSourceProperties properties) {
        this.properties = properties;
    }


    public Map<String, SqlSessionFactory> getSqlSessionFactories() {
        return sqlSessionFactories;
    }

    public void setSqlSessionFactories(Map<String, SqlSessionFactory> sqlSessionFactories) {
        this.sqlSessionFactories = sqlSessionFactories;
    }

    public Map<String, SqlSessionTemplate> getSqlSessionTemplates() {
        return sqlSessionTemplates;
    }

    public void setSqlSessionTemplates(Map<String, SqlSessionTemplate> sqlSessionTemplates) {
        this.sqlSessionTemplates = sqlSessionTemplates;
    }

    public Map<String, String> getMultiDataSourcePoolPropertyPrefixes() {
        return multiDataSourcePoolPropertyPrefixes;
    }

    public void setMultiDataSourcePoolPropertyPrefixes(Map<String, String> multiDataSourcePoolPropertyPrefixes) {
        this.multiDataSourcePoolPropertyPrefixes = multiDataSourcePoolPropertyPrefixes;
    }
}