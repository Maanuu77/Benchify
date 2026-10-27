package com.benchify.support;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Omite el test cuando no hay Docker ni una base de datos externa configurada. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(EnabledIfTestDatabase.Condition.class)
public @interface EnabledIfTestDatabase {

    class Condition implements ExecutionCondition {

        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            return TestDatabase.isAvailable()
                    ? ConditionEvaluationResult.enabled("PostGIS available")
                    : ConditionEvaluationResult.disabled("No Docker and no IT_DATABASE_URL: skipping integration test");
        }
    }
}
