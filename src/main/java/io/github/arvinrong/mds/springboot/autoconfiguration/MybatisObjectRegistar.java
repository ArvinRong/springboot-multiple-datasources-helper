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

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.mybatis.spring.MyBatisExceptionTranslator;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class MybatisObjectRegistar {


    public static class Registar implements ImportBeanDefinitionRegistrar, EnvironmentAware {
        private static final String BEAN_NAME = "mybatisObjectPostProcessor";
        private Environment environment;

        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(MybatisObjectPostProcessor.class);
            beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            beanDefinition.setSynthetic(true);
            registry.registerBeanDefinition(BEAN_NAME, beanDefinition);
            if (registry.containsBeanDefinition("sqlSessionFactory")) {
                registry.getBeanDefinition("sqlSessionFactory").setPrimary(true);
            }
            if (registry.containsBeanDefinition("sqlSessionTemplate")) {
                registry.getBeanDefinition("sqlSessionTemplate").setPrimary(true);
            }

            List<String> dataSoruceNames = resolveDataSourceNames();
            if (!dataSoruceNames.isEmpty()) {
                Iterator<String> iterator = dataSoruceNames.iterator();
                while (iterator.hasNext()) {
                    String dataSourceKey = iterator.next();
                    createSqlSessionFactoryBeanDef(registry, dataSourceKey);
                    createSqlSessionTemplateBeanDef(registry, dataSourceKey);
                }
            }

        }

        private List<String> resolveDataSourceNames() {
            List<String> result = new ArrayList<>();
            if (environment == null) {
                return result;
            }
            int misses = 0;
            for (int i = 0; i < 1000 && misses < 50; i++) {
                String name = environment.getProperty("system.db.data-sources[" + i + "].name");
                if (StringUtils.hasText(name)) {
                    result.add(name);
                    misses = 0;
                } else {
                    misses++;
                }
            }
            return result;
        }

        @Override
        public void setEnvironment(Environment environment) {
            this.environment = environment;
        }

        private void createSqlSessionTemplateBeanDef(BeanDefinitionRegistry registry, String sqlSessionTemplateKey) {
            String sqlSessionTemplateBeanName = sqlSessionTemplateKey + "SqlSessionTemplate";
            if (!registry.containsBeanDefinition(sqlSessionTemplateBeanName)) {
                GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_NO);
                beanDefinition.setRole(BeanDefinition.ROLE_APPLICATION);
                beanDefinition.setBeanClass(SqlSessionTemplate.class);
                ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
                constructorArgumentValues.addGenericArgumentValue(new DefaultSqlSessionFactory(new Configuration()));
                constructorArgumentValues.addGenericArgumentValue(ExecutorType.SIMPLE);
                constructorArgumentValues.addGenericArgumentValue(new MyBatisExceptionTranslator(null, true));
                beanDefinition.setConstructorArgumentValues(constructorArgumentValues);
                beanDefinition.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
                registry.registerBeanDefinition(sqlSessionTemplateBeanName, beanDefinition);
            }
        }

        private void createSqlSessionFactoryBeanDef(BeanDefinitionRegistry registry, String sqlSessionFactoryKey) {
            String sqlSessionFactoryBeanName = sqlSessionFactoryKey + "SqlSessionFactory";
            if (!registry.containsBeanDefinition(sqlSessionFactoryBeanName)) {
                GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
                beanDefinition.setRole(BeanDefinition.ROLE_APPLICATION);
                beanDefinition.setBeanClass(DefaultSqlSessionFactory.class);
                beanDefinition.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
                registry.registerBeanDefinition(sqlSessionFactoryBeanName, beanDefinition);
            }
        }
    }
}
