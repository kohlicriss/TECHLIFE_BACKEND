package com.example.RealTime_Attendance.configuration.GraphQlScalar;


import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class LocalDateTimeScalar {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static GraphQLScalarType createLocalDateTimeScalar() {
        return GraphQLScalarType.newScalar()
                .name("DateTime")
                .description("Custom scalar for handling LocalDateTime in format 'yyyy-MM-dd HH:mm'")
                .coercing(new Coercing<LocalDateTime, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) {
                        if (dataFetcherResult instanceof LocalDateTime dateTime) {
                            return FORMATTER.format(dateTime);
                        }
                        throw new CoercingSerializeException("Expected a LocalDateTime object.");
                    }
                    @Override
                    public LocalDateTime parseValue(Object input) {
                        if (input instanceof String dateTimeStr) {
                            try {
                                return LocalDateTime.parse(dateTimeStr, FORMATTER);
                            } catch (DateTimeParseException e) {
                                throw new CoercingParseValueException(
                                        "Invalid LocalDateTime format. Expected 'yyyy-MM-dd HH:mm'.", e);
                            }
                        }
                        throw new CoercingParseValueException("Expected a String value for LocalDateTime.");
                    }
                    @Override
                    public LocalDateTime parseLiteral(Object input) {
                        if (input instanceof StringValue stringValue) {
                            try {
                                return LocalDateTime.parse(stringValue.getValue(), FORMATTER);
                            } catch (DateTimeParseException e) {
                                throw new CoercingParseLiteralException(
                                        "Invalid LocalDateTime literal. Expected 'yyyy-MM-dd HH:mm'.", e);
                            }
                        }
                        throw new CoercingParseLiteralException("Expected a StringValue for LocalDateTime literal.");
                    }
                }).build();
    }
}