package com.example.RealTime_Attendance.configuration.GraphQlScalar;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class LocalDateScalar {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static GraphQLScalarType createLocalDateScalar() {
        return GraphQLScalarType.newScalar()
                .name("Date")
                .description("Custom scalar for handling LocalDate in format 'yyyy-MM-dd'")
                .coercing(new Coercing<LocalDate, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) {
                        if (dataFetcherResult instanceof LocalDate date) {
                            return FORMATTER.format(date);
                        }
                        throw new CoercingSerializeException("Expected a LocalDate object.");
                    }
                    @Override
                    public LocalDate parseValue(Object input) {
                        if (input instanceof String dateStr) {
                            try {
                                return LocalDate.parse(dateStr, FORMATTER);
                            } catch (DateTimeParseException e) {
                                throw new CoercingParseValueException(
                                        "Invalid LocalDate format. Expected 'yyyy-MM-dd'.", e);
                            }
                        }
                        throw new CoercingParseValueException("Expected a String value for LocalDate.");
                    }
                    @Override
                    public LocalDate parseLiteral(Object input) {
                        if (input instanceof StringValue stringValue) {
                            try {
                                return LocalDate.parse(stringValue.getValue(), FORMATTER);
                            } catch (DateTimeParseException e) {
                                throw new CoercingParseLiteralException(
                                        "Invalid LocalDate literal. Expected 'yyyy-MM-dd'.", e);
                            }
                        }
                        throw new CoercingParseLiteralException("Expected a StringValue for LocalDate literal.");
                    }
                }).build();
    }
}