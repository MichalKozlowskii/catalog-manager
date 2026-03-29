package com.example.catalog_manager.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.springframework.stereotype.Component;

@Component
public class JsonbFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry().registerPattern(
                "jsonb_contains",
                "(?1 @> ?2::jsonb)"
        );
    }
}
