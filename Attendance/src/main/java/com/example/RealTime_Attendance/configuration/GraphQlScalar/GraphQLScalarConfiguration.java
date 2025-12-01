package com.example.RealTime_Attendance.configuration.GraphQlScalar; 

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;

@Configuration
public class GraphQLScalarConfiguration {

    @Bean
    public GraphQLScalarType localDate() {
        return LocalDateScalar.createLocalDateScalar();
    }

    @Bean
    public GraphQLScalarType time() {
        return TimeScalar.createTimeScalar();
    }

    @Bean
    public GraphQLScalarType localDateTime() {
        return LocalDateTimeScalar.createLocalDateTimeScalar();
    }

    @Bean
    public GraphQLScalarType duration() {
        return DurationScalar.createDurationScalar();
    }

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return builder -> builder
            .scalar(ExtendedScalars.Date)
            .scalar(ExtendedScalars.DateTime)
            .scalar(ExtendedScalars.LocalTime)
            .scalar(DurationScalar.createDurationScalar())
            .scalar(ExtendedScalars.GraphQLLong);
    }
}
