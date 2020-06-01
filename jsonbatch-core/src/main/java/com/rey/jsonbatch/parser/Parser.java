package com.rey.jsonbatch.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    private static final String PREFIX_JSON_PATH = "$";
    private static final String PREFIX_FUNC = "__";

    private static final String CHAR_ESCAPE = "\\";
    private static final String CHAR_QUOTE = "\"";
    private static final String CHAR_COMMA = ",";
    private static final String CHAR_CLOSE_BRACKET = ")";

    private static final Pattern PATTERN_FUNC = Pattern.compile("^__(\\w*)\\((.*$)");

    public List<TokenValue> parse(String str) {
        List<TokenValue> result = new ArrayList<>();
        str = str.trim();
        if(str.startsWith(PREFIX_JSON_PATH))
            result.add(TokenValue.of(Token.JSON_PATH, str));
        else if(str.startsWith(PREFIX_FUNC))
            parseFunction(result, str);
        else
            result.add(TokenValue.of(Token.RAW, str));
        return result;
    }

    private String parseFunction(List<TokenValue> values, String str) {
        Matcher matcher = PATTERN_FUNC.matcher(str);
        if(!matcher.matches()) {
            throw new IllegalArgumentException("Invalid format");
        }

        values.add(TokenValue.of(Token.FUNC, matcher.group(1)));
        str = parseArguments(values, matcher.group(2).trim());
        return str.trim();
    }

    private String parseArguments(List<TokenValue> values, String str) {
        boolean hasClose = false;
        while(!str.isEmpty() && !hasClose) {
            if(str.startsWith(CHAR_QUOTE))
                str = parseStringArgument(values, str.substring(1));
            else if(str.startsWith(PREFIX_FUNC))
                str = parseFunction(values, str);
            else
                str = parseRawArgument(values, str);

            if(str.startsWith(CHAR_COMMA))
                str = str.substring(1).trim();
            if(str.startsWith(CHAR_CLOSE_BRACKET)) {
                values.add(TokenValue.of(Token.END_FUNC));
                str = str.substring(1).trim();
                hasClose = true;
            }
        }
        if(!hasClose) {
            throw new IllegalArgumentException(("Expect ')' character but not found"));
        }
        return str.trim();
    }

    private String parseStringArgument(List<TokenValue> values, String str) {
        int i;
        boolean isEscaped = false;
        StringBuilder builder = new StringBuilder();
        for(i = 0; i < str.length(); i++) {
            if(str.charAt(i) == CHAR_ESCAPE.charAt(0)) {
                if(isEscaped)
                    builder.append(str.charAt(i));
                isEscaped = !isEscaped;
            }
            else if(str.charAt(i) == CHAR_QUOTE.charAt(0)) {
                if(isEscaped) {
                    builder.append((str.charAt(i)));
                    isEscaped = false;
                }
                else
                    break;
            }
            else {
                builder.append(str.charAt(i));
                isEscaped = false;
            }
        }
        if(i < str.length()) {
            String value = builder.toString();
            Token token = str.startsWith(PREFIX_JSON_PATH) ? Token.JSON_PATH : Token.RAW;
            values.add(TokenValue.of(token, token == Token.JSON_PATH ? value.trim() : value));
            return str.substring(i + 1).trim();
        }
        throw new IllegalArgumentException(("Expect '\"' character but not found"));
    }

    private String parseRawArgument(List<TokenValue> values, String str) {
        int i;
        for(i = 0; i < str.length(); i++) {
            if(str.charAt(i) == CHAR_COMMA.charAt(0) || str.charAt(i) == CHAR_CLOSE_BRACKET.charAt(0)) {
                break;
            }
        }
        if(i < str.length()) {
            String value = str.substring(0, i).trim();
            values.add(TokenValue.of(Token.RAW, value));
            return str.substring(i).trim();
        }
        throw new IllegalArgumentException(("Expect ',' or ')' character but not found"));
    }

}
