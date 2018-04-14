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


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.util.ClassUtils;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.*;

public class MultiDataSourceInitializer {
    private static final Log logger = LogFactory.getLog(MultiDataSourceInitializer.class);
    private static final String[] DATA_SOURCE_TYPE_NAMES = {"com.zaxxer.hikari.HikariDataSource", "org.apache.tomcat.jdbc.pool.DataSource"};
    public static final String MULTI_DATA_SOURCE_BUILDER_KEY_PREFIX = "multiDb";
    private MultiDataSourceProperties properties;
    private BeanFactory beanFactory;
    private Map<String, DataSource> dataSources = new HashMap<>();
    private Boolean isInitialized = false;
    private MultiDataSourceHolder multiDataSourceHolder;

    public MultiDataSourceHolder getMultiDataSourceHolder() {
        return multiDataSourceHolder;
    }

    public void setMultiDataSourceHolder(MultiDataSourceHolder multiDataSourceHolder) {
        this.multiDataSourceHolder = multiDataSourceHolder;
    }

    public Map<String, DataSource> getDataSources() {
        return dataSources;
    }

    public void setDataSources(Map<String, DataSource> dataSources) {
        this.dataSources = dataSources;
    }

    MultiDataSourceInitializer(MultiDataSourceHolder multiDataSourceHolder, BeanFactory beanFactory) {
        this.multiDataSourceHolder = multiDataSourceHolder;
        this.beanFactory = beanFactory;
        this.properties = multiDataSourceHolder.getProperties();
    }

    @PostConstruct
    public void init() {
        if (isInitialized) {
            return;
        }
        List<MultiDataSourceProperties.CustomDataSource> customDataSources = properties.getDataSources();
        Iterator<MultiDataSourceProperties.CustomDataSource> iterator = customDataSources.iterator();
        int idx = 0;
        while (iterator.hasNext()) {
            MultiDataSourceProperties.CustomDataSource customDataSourceProperties = iterator.next();
            if (dataSources.containsKey(customDataSourceProperties.getName())) {
                logger.error("=========== Failed to start, duplicate DataSource name found:" + customDataSourceProperties.getName());
                throw new IllegalStateException("=========== Failed to start, duplicate DataSource name found: " + customDataSourceProperties.getName());
            }
            DataSource dataSource = getDataSource(customDataSourceProperties.getType(), customDataSourceProperties);
            if (dataSource == null) {
                throw new IllegalStateException("============ Failed to create DataSource: " + customDataSourceProperties.getName());
            }
            createDataSourceBean(customDataSourceProperties, dataSource, idx);
            idx++;
        }
        isInitialized = true;
    }

    private DataSource getDataSource(Class<? extends DataSource> dataSourceType, MultiDataSourceProperties.CustomDataSource customDataSourceProperties) {
        MultiDataSourceBuilder multiDataSourceBuilder = null;
        String builderName = getBuilderName(dataSourceType, customDataSourceProperties.getClassLoader());
        DataSource dataSource;
        if (builderName != null && beanFactory.containsBean(builderName)) {
            multiDataSourceBuilder = beanFactory.getBean(builderName, MultiDataSourceBuilder.class);
        }
        if (multiDataSourceBuilder == null) {
            logger.error("========== Failed to create custom datasource, dataSourceBuilder returns null：" + customDataSourceProperties.getName());
            throw new IllegalStateException("No supported DataSource type found for " + customDataSourceProperties.getName());
        } else {
            dataSource = multiDataSourceBuilder.buildDataSource(customDataSourceProperties);
        }
        return dataSource;
    }

    private String getBuilderName(Class<? extends DataSource> dataSourceType, ClassLoader classLoader) {
        if (dataSourceType != null) {
            return MULTI_DATA_SOURCE_BUILDER_KEY_PREFIX + dataSourceType.getSimpleName();
        }
        for (String name : DATA_SOURCE_TYPE_NAMES) {
            try {
                Class<? extends DataSource> dataSourceClazz = (Class<? extends DataSource>) ClassUtils.forName(name,
                        classLoader);
                return MULTI_DATA_SOURCE_BUILDER_KEY_PREFIX + dataSourceClazz.getSimpleName();
            } catch (Exception ex) {
                // Swallow and continue
            }
        }
        return null;
    }

    private String getPropertyPrefixPattern(String dataSourceType, int index) {
        if (dataSourceType == null) {
            return "";
        }
        if ("org.apache.tomcat.jdbc.pool.DataSource".equals(dataSourceType)) {
            return "system.db.data-sources[" + String.valueOf(index) + "].tomcat";
        } else if ("com.zaxxer.hikari.HikariDataSource".equals(dataSourceType)) {
            return "system.db.data-sources[" + String.valueOf(index) + "].hikari";
        } else {
            return "";
        }
    }

    private void createDataSourceBean(MultiDataSourceProperties.CustomDataSource customDataSourceProperties, DataSource dataSource, int index) {
        String customDataSourceBeanName = customDataSourceProperties.getName();
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
        if (!registry.containsBeanDefinition(customDataSourceBeanName)) {
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            //  AnnotatedGenericBeanDefinition beanDefinition1 = new AnnotatedGenericBeanDefinition();
            //   beanDefinition1.setPropertyValues();
            beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
            beanDefinition.setRole(BeanDefinition.ROLE_APPLICATION);
            beanDefinition.setBeanClass(dataSource.getClass());
            registry.registerBeanDefinition(customDataSourceBeanName, beanDefinition);
            dataSources.put(customDataSourceBeanName, dataSource);
            multiDataSourceHolder.getMultiDataSources().put(customDataSourceBeanName, dataSource);
            multiDataSourceHolder.getMultiDataSourcePoolPropertyPrefixes().put(customDataSourceBeanName, getPropertyPrefixPattern(customDataSourceProperties.getType().getName(), index));
            beanFactory.getBean(customDataSourceBeanName);
        }
    }
}
