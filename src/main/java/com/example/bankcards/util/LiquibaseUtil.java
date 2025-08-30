package com.example.bankcards.util;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class LiquibaseUtil {

    private static final Logger log = LoggerFactory.getLogger(LiquibaseUtil.class);

    private final DataSource dataSource;

    @Value("${spring.liquibase.change-log:db/migration/db.changelog-master.yml}")
    private String changeLogFile;

    public LiquibaseUtil(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void runMigrations() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));

            Liquibase liquibase = new Liquibase(changeLogFile,
                    new ClassLoaderResourceAccessor(), database);

            log.info("Running Liquibase migrations...");
            liquibase.update(new Contexts(), new LabelExpression());
            log.info("Liquibase migrations completed successfully");
        }
    }

    public String getMigrationStatus() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));

            Liquibase liquibase = new Liquibase(changeLogFile,
                    new ClassLoaderResourceAccessor(), database);

            var unrunChangeSets = liquibase.listUnrunChangeSets(new Contexts(), new LabelExpression());

            if (unrunChangeSets.isEmpty()) {
                return "Database is up to date. No pending migrations.";
            } else {
                StringBuilder status = new StringBuilder();
                status.append("Pending migrations (").append(unrunChangeSets.size()).append("):\n");
                for (var changeSet : unrunChangeSets) {
                    status.append("- ").append(changeSet.getId())
                            .append(" by ").append(changeSet.getAuthor())
                            .append(" (").append(changeSet.getFilePath()).append(")\n");
                }
                return status.toString();
            }
        }
    }

    public void rollbackLastMigration() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));

            Liquibase liquibase = new Liquibase(changeLogFile,
                    new ClassLoaderResourceAccessor(), database);

            log.warn("Rolling back last migration...");
            liquibase.rollback(1, new Contexts(), new LabelExpression());
            log.info("Rollback completed");
        }
    }
}
