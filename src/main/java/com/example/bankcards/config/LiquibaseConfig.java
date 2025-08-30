package com.example.bankcards.config;

import liquibase.integration.spring.SpringLiquibase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@Configuration
@EnableConfigurationProperties(LiquibaseProperties.class)
public class LiquibaseConfig {
    @Bean
    public SpringLiquibase liquibase(DataSource dataSource, LiquibaseProperties properties) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(properties.getChangeLog());

        if (properties.getContexts() != null && !properties.getContexts().isEmpty()) {
            liquibase.setContexts(String.join(",", properties.getContexts()));
        }

        liquibase.setDefaultSchema(properties.getDefaultSchema());
        liquibase.setDropFirst(properties.isDropFirst());
        liquibase.setShouldRun(properties.isEnabled());

        log.info("Configuring Liquibase with changelog: {}", properties.getChangeLog());

        return liquibase;
    }
}
