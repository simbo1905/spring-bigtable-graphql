package com.github.simbo1905.bigquerygraphql;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import graphql.schema.idl.RuntimeWiring;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@Component
@Slf4j
public class BigTableDataFetchers {

    @Autowired
    BigTableRunner bigTableRunner;

    @Value("${wirings.json}")
    String wirings;

    @SneakyThrows
    public RuntimeWiring wire(RuntimeWiring.Builder wiring) {
        log.info("Loading wirings file: "+ wirings);
        URL url = Resources.getResource(wirings);
        if( url == null ){
            throw new IllegalStateException("could not resolve "+ wirings +" from resources");
        }

        String jsonCarArray = Resources.toString(url, Charsets.UTF_8);
        ObjectMapper objectMapper = new ObjectMapper();
        List<WiringMetadata> mappings = objectMapper.readValue(jsonCarArray, new TypeReference<>() {});

        for (WiringMetadata wiringMetadata : mappings) {
            log.info("wiring: {}", wiringMetadata.toString());
            final Set<String> qualifiers =
                    Arrays.stream(wiringMetadata.qualifiesCsv.split(",")).collect(Collectors.toSet());
            wiring = wiring
                    .type(newTypeWiring(wiringMetadata.typeName)
                            .dataFetcher(wiringMetadata.fieldName,
                                    bigTableRunner.queryForOne(
                                            wiringMetadata.table,
                                            wiringMetadata.family,
                                            qualifiers,
                                            wiringMetadata.gqlAttr
                                    )));
        }

        return wiring.build();
    }

    @Getter @Setter @ToString
    static class WiringMetadata {
        /**
         * Wiring typeName e.g. "Query", "Book"
         */
        String typeName;

        /**
         * Wiring fieldName e.g. "bookById", "author"
         */
        String fieldName;

        /**
         * The BigTable table
         */
        String table;

        /**
         * The BigTable column family
         */
        String family;

        /**
         * The BigTable cell qualifiers
         */
        String qualifiesCsv;

        /**
         * The source parameter (if query) or attribute (if entity) on the GraphQL side e.g., "authorId"
         */
        String gqlAttr;
    }
}
