package com.wirc.config;

import com.wirc.exception.ChatValidationException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class GraphqlExceptionConfig {

    @Bean
    DataFetcherExceptionResolverAdapter chatExceptionResolver() {
        return new DataFetcherExceptionResolverAdapter() {
            @Override
            protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
                if (ex instanceof ChatValidationException validationException) {
                    return GraphqlErrorBuilder.newError(env)
                            .message(validationException.getMessage())
                            .errorType(ErrorType.BAD_REQUEST)
                            .extensions(buildExtensions(validationException))
                            .build();
                }

                if (ex instanceof IllegalArgumentException illegalArgumentException) {
                    return GraphqlErrorBuilder.newError(env)
                            .message(illegalArgumentException.getMessage())
                            .errorType(ErrorType.BAD_REQUEST)
                            .extensions(Map.of("code", "BAD_REQUEST"))
                            .build();
                }

                return null;
            }
        };
    }

    private Map<String, Object> buildExtensions(ChatValidationException exception) {
        Map<String, Object> extensions = new LinkedHashMap<>();
        extensions.put("code", exception.code());
        extensions.put("details", exception.details());
        return extensions;
    }
}
