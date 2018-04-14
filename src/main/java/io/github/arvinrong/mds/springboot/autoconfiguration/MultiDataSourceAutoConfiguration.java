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

import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

@Configuration
@ConditionalOnClass({DataSource.class, EmbeddedDatabaseType.class})
@EnableConfigurationProperties(MultiDataSourceProperties.class)
@AutoConfigureAfter(value = {MybatisAutoConfiguration.class})
@Import({MultiDataSourceBuilder.Tomcat.class, MultiDataSourceBuilder.Hikari.class, MultiDataSourceInitializerPostProcessor.Registrar.class})
public class MultiDataSourceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MultiDataSourceHolder multiDataSourceHolder(MultiDataSourceProperties properties) {
        MultiDataSourceHolder multiDataSourceHolder = new MultiDataSourceHolder(properties);
        return multiDataSourceHolder;
    }

    @Bean
    @ConditionalOnMissingBean
    public MultiDataSourceInitializer multiDataSourceInitializer(MultiDataSourceHolder multiDataSourceHolder, BeanFactory beanFactory) {
        return new MultiDataSourceInitializer(multiDataSourceHolder, beanFactory);
    }

}
