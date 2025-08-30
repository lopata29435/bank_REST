package com.example.bankcards.controller;

import com.example.bankcards.util.LiquibaseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/liquibase")
@PreAuthorize("hasAuthority('ADMIN')")
public class LiquibaseController {

    private final LiquibaseUtil liquibaseUtil;

    public LiquibaseController(LiquibaseUtil liquibaseUtil) {
        this.liquibaseUtil = liquibaseUtil;
    }

    @GetMapping("/status")
    public ResponseEntity<?> getMigrationStatus() {
        try {
            String status = liquibaseUtil.getMigrationStatus();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "migrations", status
            ));
        } catch (Exception e) {
            log.error("Failed to get migration status", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get migration status: " + e.getMessage()));
        }
    }

    @PostMapping("/migrate")
    public ResponseEntity<?> runMigrations() {
        try {
            liquibaseUtil.runMigrations();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Migrations executed successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to run migrations", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to run migrations: " + e.getMessage()));
        }
    }

    @PostMapping("/rollback")
    public ResponseEntity<?> rollbackLastMigration() {
        try {
            liquibaseUtil.rollbackLastMigration();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Rollback executed successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to rollback migration", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to rollback migration: " + e.getMessage()));
        }
    }
}
