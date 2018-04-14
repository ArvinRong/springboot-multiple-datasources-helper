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
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.springframework.beans.factory.BeanFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;


@org.springframework.context.annotation.Configuration
@ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
@ConditionalOnBean({MultiDataSourceInitializer.class})
@EnableConfigurationProperties(MybatisProperties.class)
@AutoConfigureAfter({DataSourceAutoConfiguration.class, MultiDataSourceAutoConfiguration.class})
@Import({MybatisObjectRegistar.Registar.class})
public class MybatisMultiDataSourceAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MybatisMultiDataSourceAutoConfiguration.class);

    @Bean
    public Configuration mybatisMultiDataSourceConfiguration(MybatisProperties properties, ObjectProvider<List<ConfigurationCustomizer>> configurationCustomizersProvider) {
        Configuration configuration = properties.getConfiguration();
        List<ConfigurationCustomizer> configurationCustomizers = configurationCustomizersProvider.getIfAvailable();
        if (configuration == null && !StringUtils.hasText(properties.getConfigLocation())) {
            configuration = new Configuration();
        }
        if (configuration != null && !CollectionUtils.isEmpty(configurationCustomizers)) {
            for (ConfigurationCustomizer customizer : configurationCustomizers) {
                customizer.customize(configuration);
            }
        }
        return configuration;
    }

    @Bean
    public MybatisMultiDataSourceInitializer mybatisMultiDataSourceInitializer(ObjectProvider<Interceptor[]> interceptorsProvider,
                                                                               ObjectProvider<DatabaseIdProvider> databaseIdProvider,
                                                                               ObjectProvider<List<ConfigurationCustomizer>> configurationCustomizersProvider, MultiDataSourceHolder multiDataSourceHolder, BeanFactory beanFactory, MybatisProperties properties) {
        return new MybatisMultiDataSourceInitializer(interceptorsProvider, databaseIdProvider, configurationCustomizersProvider, multiDataSourceHolder, beanFactory, properties);
    }

}
