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
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class MybatisObjectPostProcessor implements BeanPostProcessor, BeanFactoryAware, InitializingBean {
    private static final Log logger = LogFactory
            .getLog(MybatisObjectPostProcessor.class);
    private BeanFactory beanFactory;
    private List<String> multiDataSourceBeanNameList = new ArrayList<>();
    private List<String> mybatisSqlSessionTemplateList = new ArrayList<>();
    private List<String> mybatisSqlSessionFactoryList = new ArrayList<>();
    private List<String> multiDataSourcePlatformTransactionManagerList = new ArrayList<>();

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SqlSessionFactory && mybatisSqlSessionFactoryList.contains(beanName)) {
            this.beanFactory.getBean(MultiDataSourceInitializer.class);
            this.beanFactory.getBean(MybatisMultiDataSourceInitializer.class);
            MultiDataSourceHolder multiDataSourceHolder = (MultiDataSourceHolder) beanFactory.getBean("multiDataSourceHolder");
            SqlSessionFactory sqlSessionFactory = multiDataSourceHolder.getSqlSessionFactories().get(beanName);
            if (sqlSessionFactory == null) {
                logger.error(beanName + " mybatis SqlSessionFactory failed to create.");
                throw new IllegalStateException(beanName + " mybatis SqlSessionFactory failed to create.");
            }
            return sqlSessionFactory;
        } else if (bean instanceof SqlSessionTemplate && mybatisSqlSessionTemplateList.contains(beanName)) {
            this.beanFactory.getBean(MultiDataSourceInitializer.class);
            this.beanFactory.getBean(MybatisMultiDataSourceInitializer.class);
            MultiDataSourceHolder multiDataSourceHolder = (MultiDataSourceHolder) beanFactory.getBean("multiDataSourceHolder");
            SqlSessionTemplate sqlSessionTemplate = multiDataSourceHolder.getSqlSessionTemplates().get(beanName);
            if (sqlSessionTemplate == null) {
                logger.error(beanName + " mybatis SqlSessionTemplate failed to create");
                throw new IllegalStateException(beanName + " mybatis SqlSessionTemplate failed to create");
            }
            return sqlSessionTemplate;
        }
        return bean;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        MultiDataSourceProperties properties = beanFactory.getBean(MultiDataSourceProperties.class);
        List<MultiDataSourceProperties.CustomDataSource> multiDataSourcePropertiesMap = properties.getDataSources();
        Iterator<MultiDataSourceProperties.CustomDataSource> iterator = multiDataSourcePropertiesMap.iterator();
        while (iterator.hasNext()) {
            MultiDataSourceProperties.CustomDataSource customDataSource = iterator.next();
            multiDataSourceBeanNameList.add(customDataSource.getName());
            multiDataSourcePlatformTransactionManagerList.add(customDataSource.getName() + MultiDataSourceTransactionManagerAutoConfiguration.TRANSACTION_MGR_MAPPER_KEY_SUFFIX);
            mybatisSqlSessionTemplateList.add(customDataSource.getName() + "SqlSessionTemplate");
            mybatisSqlSessionFactoryList.add(customDataSource.getName() + "SqlSessionFactory");
        }
    }
}
