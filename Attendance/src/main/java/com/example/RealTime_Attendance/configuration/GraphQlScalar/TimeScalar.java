package com.example.RealTime_Attendance.configuration.GraphQlScalar;

import graphql.language.StringValue;
import graphql.schema.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class TimeScalar {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static GraphQLScalarType createTimeScalar() {
        return GraphQLScalarType.newScalar()
                .name("Time")
                .description("Custom scalar for LocalTime in format 'HH:mm'")
                .coercing(new Coercing<LocalTime, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) {
                        if (dataFetcherResult instanceof LocalTime time) {
                            return FORMATTER.format(time);
                        }
                        throw new CoercingSerializeException("Expected a LocalTime object.");
                    }

                    @Override
                    public LocalTime parseValue(Object input) {
                        if (input instanceof String timeStr) {
                            try {
                                return LocalTime.parse(timeStr, FORMATTER);
                            } catch (DateTimeParseException e) {
                                throw new CoercingParseValueException(
                                        "Invalid Time format. Expected 'HH:mm'.", e);
                            }
                        }
                        throw new CoercingParseValueException("Expected a String value for Time.");
                    }

                    @Override
                    public LocalTime parseLiteral(Object input) {
                        if (input instanceof StringValue stringValue) {
                            try {
                                return LocalTime.parse(stringValue.getValue(), FORMATTER);
                            } catch (DateTimeParseException e) {
                                throw new CoercingParseLiteralException(
                                        "Invalid Time literal. Expected 'HH:mm'.", e);
                            }
                        }
                        throw new CoercingParseLiteralException("Expected a StringValue for Time literal.");
                    }
                })
                .build();
    }
}
