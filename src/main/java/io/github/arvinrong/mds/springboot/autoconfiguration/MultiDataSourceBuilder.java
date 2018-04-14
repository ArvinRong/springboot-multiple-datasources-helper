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

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.annotation.Bean;
import javax.sql.DataSource;

abstract class MultiDataSourceBuilder {
    /**
     * Tomcat Pool DataSource configuration.
     */
    @ConditionalOnClass(org.apache.tomcat.jdbc.pool.DataSource.class)
    static class Tomcat extends MultiDataSourceBuilder {
        @Bean("multiDbDataSource")
        public MultiDataSourceBuilder.Tomcat tomcatBuilder() {
            return new MultiDataSourceBuilder.Tomcat();
        }

        @Override
        public DataSource buildDataSource(MultiDataSourceProperties.CustomDataSource customDataSourceProperties) {
            org.apache.tomcat.jdbc.pool.DataSource dataSource = createDataSource(
                    customDataSourceProperties, org.apache.tomcat.jdbc.pool.DataSource.class);
            DatabaseDriver databaseDriver = DatabaseDriver
                    .fromJdbcUrl(customDataSourceProperties.determineUrl());
            String validationQuery = databaseDriver.getValidationQuery();
            if (validationQuery != null) {
                dataSource.setTestOnBorrow(true);
                dataSource.setValidationQuery(validationQuery);
            }
            return dataSource;
        }
    }

    /**
     * Hikari DataSource configuration.
     */
    @ConditionalOnClass(HikariDataSource.class)
    static class Hikari extends MultiDataSourceBuilder {
        @Bean(name = "multiDbHikariDataSource")
        public MultiDataSourceBuilder.Hikari hikariBuilder() {
            return new MultiDataSourceBuilder.Hikari();
        }

        @Override
        public DataSource buildDataSource(MultiDataSourceProperties.CustomDataSource customDataSourceProperties) {
            return createDataSource(customDataSourceProperties, HikariDataSource.class);
        }
    }

    public abstract DataSource buildDataSource(MultiDataSourceProperties.CustomDataSource customDataSourceProperties);

    @SuppressWarnings("unchecked")
    protected <T> T createDataSource(MultiDataSourceProperties.CustomDataSource properties,
                                     Class<? extends DataSource> type) {
        return (T) properties.initializeDataSourceBuilder().type(type).build();
    }
}
