package com.xjbg.log.collector.sensitive;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * @author kesc
 * @since 2023-04-12 11:25
 */
public enum SensitiveStrategy {

    REAL_NAME(new Function<String, String>() {
        @Override
        public String apply(String o) {
            return patterns.computeIfAbsent("(\\S)\\S(\\S*)", Pattern::compile).matcher(o).replaceAll("$1*$2");
        }
    }),

    ACCESS_KEY(new Function<String, String>() {
        @Override
        public String apply(String o) {
            return patterns.computeIfAbsent("(?<=\\S)\\S(?=\\S)", Pattern::compile).matcher(o)
                    .replaceAll("*");
        }
    }),

    SECRET_KEY(new Function<String, String>() {
        @Override
        public String apply(String o) {
            return patterns.computeIfAbsent("[\\s\\S]", Pattern::compile).matcher(o)
                    .replaceAll("*");
        }
    }),

    ID_CARD(new Function<String, String>() {
        @Override
        public String apply(String o) {
            return patterns.computeIfAbsent("(\\d{4})\\d{12}(\\w+)", Pattern::compile).matcher(o)
                    .replaceAll("$1************$2");
        }
    }),

    BANK_CARD(new Function<String, String>() {
        @Override
        public String apply(String o) {
            return patterns.computeIfAbsent("(\\d{4})\\d{10}(\\w+)", Pattern::compile).matcher(o)
                    .replaceAll("$1**********$2");
        }
    }),

    EMAIL(new Function<String, String>() {
        @Override
        public String apply(String o) {
            return patterns.computeIfAbsent("(?<=\\S)\\S*(?=@.*)", Pattern::compile).matcher(o)
                    .replaceAll("*");
        }
    }),

    PHONE(new Function<String, String>() {
        @Override
        public String apply(String o) {
            return patterns.computeIfAbsent("(\\d{3})\\d{4}(\\d+)", Pattern::compile).matcher(o)
                    .replaceAll("$1****$2");
        }
    }),

    ADDRESS(new Function<String, String>() {
        @Override
        public String apply(String o) {
            return patterns.computeIfAbsent("(?<=\\S{6})\\S", Pattern::compile).matcher(o)
                    .replaceAll("*");
        }
    });

    private static final Map<String, Pattern> patterns = new HashMap<>(SensitiveStrategy.values().length);
    private final Function<String, String> function;

    SensitiveStrategy(Function<String, String> function) {
        this.function = function;
    }

    public String apply(String value) {
        if (value == null || "".equals(value)) {
            return value;
        }
        return function.apply(value);
    }
}
