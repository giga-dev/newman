package com.gigaspaces.newman.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.Properties;

@Configuration
@ComponentScan(basePackages = {"com.gigaspaces.newman"})
@EnableJpaRepositories(basePackages = {"com.gigaspaces.newman"})
public class JpaConfig {

    @Bean
    public DataSource dataSource() {
        String username = System.getProperty("newman.postgres.username", "admin");
        String password = System.getProperty("newman.postgres.password", "password");

        String host = System.getProperty("newman.postgres.db.host", "localhost:5432");
        String dbName = System.getProperty("newman.postgres.db.name", "newman-db");

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(String.format("jdbc:postgresql://%s/%s", host, dbName));
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(dataSource());
        factoryBean.setPackagesToScan(
                "com.gigaspaces.newman.entities",
                "com.gigaspaces.newman.beans",
                "com.gigaspaces.newman.usermanagment"
        );
        factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factoryBean.setJpaProperties(jpaProperties());
        return factoryBean;
    }

    @Bean
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(Objects.requireNonNull(emf.getObject()));
    }

    private Properties jpaProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.hbm2ddl.auto", "update"); // Or 'create'/'validate'
        properties.put("hibernate.show_sql", "false");
        properties.put("hibernate.format_sql", "false");
        properties.put("hibernate.use_sql_comments", "false");
        return properties;
    }
}