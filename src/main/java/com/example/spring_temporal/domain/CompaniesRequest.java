package com.example.spring_temporal.domain;

import java.util.Set;

public record CompaniesRequest(Company rootCompany, Set<Company> childrenCompanies) {
}
