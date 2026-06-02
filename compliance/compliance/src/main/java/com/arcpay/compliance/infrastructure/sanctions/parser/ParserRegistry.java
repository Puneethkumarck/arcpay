package com.arcpay.compliance.infrastructure.sanctions.parser;

import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ParserRegistry {

    private final Map<SanctionsSource, SanctionsFeedParser> parsersBySource;

    public ParserRegistry(List<SanctionsFeedParser> parsers) {
        this.parsersBySource = parsers.stream()
                .collect(Collectors.toUnmodifiableMap(SanctionsFeedParser::source, Function.identity()));
    }

    public SanctionsFeedParser parserFor(SanctionsSource source) {
        var parser = parsersBySource.get(source);
        if (parser == null) {
            throw new IllegalArgumentException("No sanctions parser registered for source " + source);
        }
        return parser;
    }
}
