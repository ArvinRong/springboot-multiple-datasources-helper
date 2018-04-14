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
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.*;


public class MybatisObjectRegistar {


    public static class Registar implements ImportBeanDefinitionRegistrar {
        private static final String BEAN_NAME = "mybatisObjectPostProcessor";

        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            List<String> dataSoruceNames = null;
            try {
                GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                beanDefinition.setBeanClass(MybatisObjectPostProcessor.class);
                beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
                beanDefinition.setSynthetic(true);
                registry.registerBeanDefinition(BEAN_NAME, beanDefinition);
                if (registry.getBeanDefinition("sqlSessionFactory") != null) {
                    registry.getBeanDefinition("sqlSessionFactory").setPrimary(true);
                }
                if (registry.getBeanDefinition("sqlSessionTemplate") != null) {
                    registry.getBeanDefinition("sqlSessionTemplate").setPrimary(true);
                }

                Resource yamlRes = new ClassPathResource("/application.yaml");
                Resource ymlRes = new ClassPathResource("/application.yml");
                boolean yamlExists = yamlRes.exists();
                boolean ymlExists = ymlRes.exists();
                Object ymlProperties = null;
                if (yamlExists) {
                    ymlProperties = new Yaml().load(
                            (new EncodedResource(yamlRes, "UTF-8").getInputStream()));
                } else if (ymlExists) {
                    ymlProperties = new Yaml().load(
                            (new EncodedResource(ymlRes, "UTF-8").getInputStream()));
                } else {
                    throw new IllegalStateException("Using yaml configuration for multi datasources feature.");
                }
                dataSoruceNames = resolveDataBaseNames(ymlProperties);
                if (dataSoruceNames != null && dataSoruceNames.size() > 0) {
                    Iterator<String> iterator = dataSoruceNames.iterator();
                    while (iterator.hasNext()) {
                        String dataSourceKey = iterator.next();
                        createSqlSessionFactoryBeanDef(registry, dataSourceKey);
                        createSqlSessionTemplateBeanDef(registry, dataSourceKey);
                    }
                }

            } catch (IOException e) {
                throw new IllegalStateException("Multi datasources feature failed to initialized.", e);
            }

        }

        private List<String> resolveDataBaseNames(Object ymlProperties) {
            List<String> result = new ArrayList<>();
            try {
                Map<String, Map<String, Map<String, Object>>> dataSourcesProperties = (Map<String, Map<String, Map<String, Object>>>) ymlProperties;
                if (dataSourcesProperties.get("system").get("db").get("data-sources") != null) {
                    List<Map<String, String>> dataSourcesList = (List<Map<String, String>>) dataSourcesProperties.get("system").get("db").get("data-sources");
                    Iterator<Map<String, String>> iterator = dataSourcesList.iterator();
                    while (iterator.hasNext()) {
                        Map<String, String> dataSrouce = iterator.next();
                        if (dataSrouce != null && !StringUtils.isEmpty(dataSrouce.get("name"))) {
                            result.add(dataSrouce.get("name"));
                        }
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException("Multi datasources feature failed to initialized , failed to resolve database name.", e);
            }
            return result;
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
