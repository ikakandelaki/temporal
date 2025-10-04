package com.example.spring_temporal.controller;

import com.example.spring_temporal.domain.CompaniesRequest;
import com.example.spring_temporal.service.MigrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/migration")
@RequiredArgsConstructor
public class MigrationController {
    private final MigrationService migrationService;

    @PostMapping
    public void startMigration(@RequestBody CompaniesRequest request) {
        migrationService.startMigration(request.rootCompany(), request.childrenCompanies());
    }
}
