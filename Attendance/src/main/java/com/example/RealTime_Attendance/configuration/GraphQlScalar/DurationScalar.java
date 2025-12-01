package com.example.RealTime_Attendance.configuration.GraphQlScalar;

import graphql.language.StringValue;
import graphql.schema.*;
import java.time.Duration;
import java.time.format.DateTimeParseException;

public class DurationScalar {

    public static GraphQLScalarType createDurationScalar() {
        return GraphQLScalarType.newScalar()
                .name("Duration")
                .description("Custom scalar for Java Duration in ISO-8601 format (e.g., 'PT1H30M' or 'PT45S')")
                .coercing(new Coercing<Duration, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) {
                        if (dataFetcherResult instanceof Duration duration) {
                            return duration.toString(); // e.g., PT1H30M
                        }
                        throw new CoercingSerializeException("Expected a Duration object.");
                    }

                    @Override
                    public Duration parseValue(Object input) {
                        if (input instanceof String str) {
                            try {
                                return Duration.parse(str);
                            } catch (DateTimeParseException e) {
                                throw new CoercingParseValueException(
                                        "Invalid Duration format. Expected ISO-8601 (e.g., 'PT1H30M').", e);
                            }
                        }
                        throw new CoercingParseValueException("Expected a String value for Duration.");
                    }

                    @Override
                    public Duration parseLiteral(Object input) {
                        if (input instanceof StringValue stringValue) {
                            try {
                                return Duration.parse(stringValue.getValue());
                            } catch (DateTimeParseException e) {
                                throw new CoercingParseLiteralException(
                                        "Invalid Duration literal. Expected ISO-8601 format (e.g., 'PT1H30M').", e);
                            }
                        }
                        throw new CoercingParseLiteralException("Expected a StringValue for Duration literal.");
                    }
                })
                .build();
    }
}
