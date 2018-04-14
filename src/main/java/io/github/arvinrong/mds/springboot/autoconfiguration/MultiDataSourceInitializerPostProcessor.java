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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class MultiDataSourceInitializerPostProcessor implements BeanPostProcessor, InitializingBean {

    private static final Log logger = LogFactory
            .getLog(MultiDataSourceInitializerPostProcessor.class);

    private List<String> multiDataSourceBeanNameList = new ArrayList<>();
    private List<String> multiDataSourcePlatformTransactionManagerList = new ArrayList<>();
    private BeanFactory beanFactory;
    private PropertySources propertySources;

    MultiDataSourceInitializerPostProcessor(MultiDataSourceProperties properties, BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        List<MultiDataSourceProperties.CustomDataSource> multiDataSourcePropertiesMap = properties.getDataSources();
        Iterator<MultiDataSourceProperties.CustomDataSource> iterator = multiDataSourcePropertiesMap.iterator();
        while (iterator.hasNext()) {
            MultiDataSourceProperties.CustomDataSource customDataSource = iterator.next();
            multiDataSourceBeanNameList.add(customDataSource.getName());
            multiDataSourcePlatformTransactionManagerList.add(customDataSource.getName() + MultiDataSourceTransactionManagerAutoConfiguration.TRANSACTION_MGR_MAPPER_KEY_SUFFIX);
        }
    }

    @Override
    public void afterPropertiesSet() {
        if (this.propertySources == null) {
            PropertySourcesPlaceholderConfigurer configurer = getSinglePropertySourcesPlaceholderConfigurer();
            if (configurer != null) {
                this.propertySources = new MultiDataSourceInitializerPostProcessor.FlatPropertySources(configurer.getAppliedPropertySources());
            }
        }
    }

    private PropertySourcesPlaceholderConfigurer getSinglePropertySourcesPlaceholderConfigurer() {
        // Take care not to cause early instantiation of all FactoryBeans
        if (this.beanFactory instanceof ListableBeanFactory) {
            ListableBeanFactory listableBeanFactory = (ListableBeanFactory) this.beanFactory;
            Map<String, PropertySourcesPlaceholderConfigurer> beans = listableBeanFactory
                    .getBeansOfType(PropertySourcesPlaceholderConfigurer.class, false,
                            false);
            if (beans.size() == 1) {
                return beans.values().iterator().next();
            }
            if (beans.size() > 1 && logger.isWarnEnabled()) {
                logger.warn("Multiple PropertySourcesPlaceholderConfigurer "
                        + "beans registered " + beans.keySet()
                        + ", falling back to Environment");
            }
        }
        return null;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource) {
            if ("dataSource".equals(beanName)) {
                //Set dataSource bean which defined as default by SrpingBoot to be primary one.
                ((ConfigurableBeanFactory) beanFactory).getMergedBeanDefinition(beanName).setPrimary(true);
            } else if (multiDataSourceBeanNameList.contains(beanName)) {
                MultiDataSourceHolder multiDataSourceHolder = (MultiDataSourceHolder) beanFactory.getBean("multiDataSourceHolder");
                DataSource dataSource = multiDataSourceHolder.getMultiDataSources().get(beanName);
                String prefix = multiDataSourceHolder.getMultiDataSourcePoolPropertyPrefixes().get(beanName);
                bindProperties(dataSource, beanName, prefix);
                if (dataSource == null) {
                    logger.error(beanName + "  datasource bean failed to be created.");
                    throw new IllegalStateException(beanName + " datasource bean failed to be created.");
                }
                return dataSource;
            }
        } else if (bean instanceof PlatformTransactionManager) {
            if ("transactionManager".equals(beanName)) {
                ((ConfigurableBeanFactory) beanFactory).getMergedBeanDefinition(beanName).setPrimary(true);
            } else if (multiDataSourcePlatformTransactionManagerList.contains(beanName)) {
                this.beanFactory.getBean(MultiDataSourceInitializer.class);
                this.beanFactory.getBean(MybatisMultiDataSourceInitializer.class);
                MultiDataSourceHolder multiDataSourceHolder = (MultiDataSourceHolder) beanFactory.getBean("multiDataSourceHolder");
                PlatformTransactionManager platformTransactionManager = multiDataSourceHolder.getMultiPlatformTransactionManager().get(beanName);
                if (platformTransactionManager == null) {
                    logger.error(beanName + "  bean failed to be created.");
                    throw new IllegalStateException(beanName + "  bean failed to be created.");
                }
                return platformTransactionManager;
            }
        }
        return bean;
    }


    private void bindProperties(Object bean, String beanName, String prefix) {
        Object target = bean;
        PropertiesConfigurationFactory<Object> factory = new PropertiesConfigurationFactory<Object>(
                target);
        factory.setPropertySources(this.propertySources);
        // If no explicit conversion service is provided we add one so that (at least)
        // comma-separated arrays of convertibles can be bound automatically
        factory.setConversionService(new DefaultConversionService());
        factory.setIgnoreInvalidFields(false);
        factory.setIgnoreUnknownFields(true);
        factory.setIgnoreNestedProperties(false);
        if (StringUtils.hasLength(prefix)) {
            factory.setTargetName(prefix);
        }
        try {
            factory.bindPropertiesToTarget();
        } catch (Exception ex) {
            String targetClass = ClassUtils.getShortName(target.getClass());
            throw new BeanCreationException(beanName, "Could not bind properties to "
                    + targetClass, ex);
        }
    }

    static class Registrar implements ImportBeanDefinitionRegistrar {

        private static final String BEAN_NAME = "multiDataSourceInitializerPostProcessor";

        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                            BeanDefinitionRegistry registry) {
            if (!registry.containsBeanDefinition(BEAN_NAME)) {
                GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                beanDefinition.setBeanClass(MultiDataSourceInitializerPostProcessor.class);
                beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
                // We don't need this one to be post processed otherwise it can cause a
                // cascade of bean instantiation that we would rather avoid.
                beanDefinition.setSynthetic(true);
                registry.registerBeanDefinition(BEAN_NAME, beanDefinition);
                if (registry.getBeanDefinition("dataSource") != null) {
                    registry.getBeanDefinition("dataSource").setPrimary(true);
                }
            }
        }

    }

    /**
     * Convenience class to flatten out a tree of property sources without losing the
     * reference to the backing data (which can therefore be updated in the background).
     */
    private static class FlatPropertySources implements org.springframework.core.env.PropertySources {

        private org.springframework.core.env.PropertySources propertySources;

        FlatPropertySources(org.springframework.core.env.PropertySources propertySources) {
            this.propertySources = propertySources;
        }

        @Override
        public Iterator<PropertySource<?>> iterator() {
            MutablePropertySources result = getFlattened();
            return result.iterator();
        }

        @Override
        public boolean contains(String name) {
            return get(name) != null;
        }

        @Override
        public PropertySource<?> get(String name) {
            return getFlattened().get(name);
        }

        private MutablePropertySources getFlattened() {
            MutablePropertySources result = new MutablePropertySources();
            for (PropertySource<?> propertySource : this.propertySources) {
                flattenPropertySources(propertySource, result);
            }
            return result;
        }

        private void flattenPropertySources(PropertySource<?> propertySource,
                                            MutablePropertySources result) {
            Object source = propertySource.getSource();
            if (source instanceof ConfigurableEnvironment) {
                ConfigurableEnvironment environment = (ConfigurableEnvironment) source;
                for (PropertySource<?> childSource : environment.getPropertySources()) {
                    flattenPropertySources(childSource, result);
                }
            } else {
                result.addLast(propertySource);
            }
        }

    }
}
