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

import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.mybatis.spring.boot.autoconfigure.SpringBootVFS;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.*;

public class MybatisMultiDataSourceInitializer implements ResourceLoaderAware {
    private Boolean initialized = false;
    private MultiDataSourceHolder multiDataSourceHolder;
    private BeanFactory beanFactory;
    private ResourceLoader resourceLoader;
    private MybatisProperties properties;
    private Interceptor[] interceptors;
    private DatabaseIdProvider databaseIdProvider;
    private List<ConfigurationCustomizer> configurationCustomizers;

    public MybatisMultiDataSourceInitializer(ObjectProvider<Interceptor[]> interceptorsProvider,
                                             ObjectProvider<DatabaseIdProvider> databaseIdProvider,
                                             ObjectProvider<List<ConfigurationCustomizer>> configurationCustomizersProvider, MultiDataSourceHolder multiDataSourceHolder, BeanFactory beanFactory, MybatisProperties properties) {
        this.interceptors = interceptorsProvider.getIfAvailable();
        this.databaseIdProvider = databaseIdProvider.getIfAvailable();
        this.configurationCustomizers = configurationCustomizersProvider.getIfAvailable();
        this.multiDataSourceHolder = multiDataSourceHolder;
        this.beanFactory = beanFactory;
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        if (initialized) {
            return;
        }
        Set<String> multiDataSourceHolderKeySet = multiDataSourceHolder.getMultiDataSources().keySet();
        Iterator<String> multiDataSourceHolderKeySetItr = multiDataSourceHolderKeySet.iterator();
        while (multiDataSourceHolderKeySetItr.hasNext()) {
            String dataSourceKey = multiDataSourceHolderKeySetItr.next();
            DataSource dataSource = multiDataSourceHolder.getMultiDataSources().get(dataSourceKey);
            try {
                SqlSessionFactory sqlSessionFactory = sqlSessionFactory(dataSource, new String[]{"classpath:mybatis/mapper/" + dataSourceKey + "/*.xml"});
                SqlSessionTemplate sqlSessionTemplate = sqlSessionTemplate(sqlSessionFactory);
                String sqlSessionTemplateBeanName = dataSourceKey + "SqlSessionTemplate";
                String sqlSessionFactoryBeanName = dataSourceKey + "SqlSessionFactory";
                multiDataSourceHolder.addSqlSessionFactory(sqlSessionFactoryBeanName, sqlSessionFactory);
                beanFactory.getBean(sqlSessionFactoryBeanName);
                multiDataSourceHolder.addSqlSessionTemplate(sqlSessionTemplateBeanName, sqlSessionTemplate);
                beanFactory.getBean(sqlSessionTemplateBeanName);
            } catch (Exception e) {
                throw new IllegalStateException("Mybatis for multi datasource failed to be initialized", e);
            }
        }
        initialized = true;
    }

    private SqlSessionFactory sqlSessionFactory(DataSource dataSource, String[] mapperLocations) throws Exception {
        Configuration configuration = new Configuration();
        if (configuration == null && !StringUtils.hasText(this.properties.getConfigLocation())) {
            configuration = new Configuration();
        }
        if (configuration != null && !CollectionUtils.isEmpty(this.configurationCustomizers)) {
            for (ConfigurationCustomizer customizer : this.configurationCustomizers) {
                customizer.customize(configuration);
            }
        }
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setVfs(SpringBootVFS.class);
        if (StringUtils.hasText(this.properties.getConfigLocation())) {
            factory.setConfigLocation(this.resourceLoader.getResource(this.properties.getConfigLocation()));
        }
        factory.setConfiguration(configuration);
        if (this.properties.getConfigurationProperties() != null) {
            factory.setConfigurationProperties(this.properties.getConfigurationProperties());
        }
        if (!ObjectUtils.isEmpty(this.interceptors)) {
            factory.setPlugins(this.interceptors);
        }
        if (this.databaseIdProvider != null) {
            factory.setDatabaseIdProvider(this.databaseIdProvider);
        }
        if (StringUtils.hasLength(this.properties.getTypeAliasesPackage())) {
            factory.setTypeAliasesPackage(this.properties.getTypeAliasesPackage());
        }
        if (StringUtils.hasLength(this.properties.getTypeHandlersPackage())) {
            factory.setTypeHandlersPackage(this.properties.getTypeHandlersPackage());
        }
            factory.setMapperLocations(resolveMapperLocations(mapperLocations));

        return factory.getObject();
    }

    private Resource[] resolveMapperLocations(String[] mapperLocations) {
        ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
        List<Resource> resources = new ArrayList<Resource>();
        if (mapperLocations != null) {
            for (String mapperLocation : mapperLocations) {
                try {
                    Resource[] mappers = resourceResolver.getResources(mapperLocation);
                    resources.addAll(Arrays.asList(mappers));
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return resources.toArray(new Resource[resources.size()]);
    }

    private SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        ExecutorType executorType = this.properties.getExecutorType();
        if (executorType != null) {
            return new SqlSessionTemplate(sqlSessionFactory, executorType);
        } else {
            return new SqlSessionTemplate(sqlSessionFactory);
        }
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
