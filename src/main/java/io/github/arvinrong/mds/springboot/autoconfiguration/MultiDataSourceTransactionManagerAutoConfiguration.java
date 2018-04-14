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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.Iterator;
import java.util.Set;


@Configuration
@ConditionalOnClass({JdbcTemplate.class, PlatformTransactionManager.class})
@AutoConfigureAfter(MultiDataSourceAutoConfiguration.class)
public class MultiDataSourceTransactionManagerAutoConfiguration {

    public final static String TRANSACTION_MGR_MAPPER_KEY_SUFFIX = "TransactionManager";

    private static Boolean isInitialized = false;

    @Configuration
    static class MultiDataSourceTransactionManagerConfiguration {

        private final MultiDataSourceHolder multiDataSourceHolder;

        private final TransactionManagerCustomizers transactionManagerCustomizers;

        private BeanFactory beanFactory;

        MultiDataSourceTransactionManagerConfiguration(MultiDataSourceHolder multiDataSourceHolder,
                                                       ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers, BeanFactory beanFactory) {
            this.multiDataSourceHolder = multiDataSourceHolder;
            this.beanFactory = beanFactory;
            this.transactionManagerCustomizers = transactionManagerCustomizers
                    .getIfAvailable();
        }

        @PostConstruct
        public void init() {
            if (isInitialized) {
                return;
            }
            Set<String> multiDataSourceHolderKeySet = multiDataSourceHolder.getMultiDataSources().keySet();
            Iterator<String> multiDataSourceHolderKeySetItr = multiDataSourceHolderKeySet.iterator();
            while (multiDataSourceHolderKeySetItr.hasNext()) {
                String dataSourceKey = multiDataSourceHolderKeySetItr.next();
                DataSource dataSource = multiDataSourceHolder.getMultiDataSources().get(dataSourceKey);
                DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
                if (this.transactionManagerCustomizers != null) {
                    this.transactionManagerCustomizers.customize(transactionManager);
                }
                createPlatformTransactionManagerBean(dataSourceKey + TRANSACTION_MGR_MAPPER_KEY_SUFFIX, transactionManager);
            }
            isInitialized = true;
        }

        private void createPlatformTransactionManagerBean(String transactionManagerBeanName, PlatformTransactionManager transactionManager) {
            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
            if (!registry.containsBeanDefinition(transactionManagerBeanName)) {
                GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
                beanDefinition.setRole(BeanDefinition.ROLE_APPLICATION);
                beanDefinition.setBeanClass(transactionManager.getClass());
                registry.registerBeanDefinition(transactionManagerBeanName, beanDefinition);
                multiDataSourceHolder.addPlatformTransactionManager(transactionManagerBeanName, transactionManager);
                beanFactory.getBean(transactionManagerBeanName);
            }
        }

    }
}
