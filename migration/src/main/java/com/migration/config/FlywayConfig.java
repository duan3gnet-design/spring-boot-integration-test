package com.migration.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flywayTransaction(
            @Value("${transaction.datasource.url}") String url,
            @Value("${transaction.datasource.username}") String username,
            @Value("${transaction.datasource.password}") String password) {
        return Flyway.configure()
                .dataSource(url, username, password)
                .locations("classpath:db/transaction")
                .schemas("public")
                .baselineOnMigrate(true)
                .load();
    }
}
