package de.crazydev22.translations;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
public class Format {
    private final String pattern;
    private final List<Argument> arguments;
    private final int maxArgument;

    public Format(String pattern) {
        StringBuilder builder = new StringBuilder(pattern.length());
        var args = new ArrayList<Argument>();
        int currentArg = 0;

        boolean inTag = false;
        int valueStart = -1;
        char escape = '\'';

        int start = -1;
        int offset = 0;
        StringBuilder number = null;

        int index = 0;
        for (char c : pattern.toCharArray()) {
            int i = index++;

            switch (c) {
                case '{' -> {
                    start = i;
                    number = new StringBuilder();
                }
                case '}' -> {
                    if (start == -1)
                        break;

                    int target = number.isEmpty() ? currentArg++ : Integer.parseInt(number.toString());
                    args.add(new Argument(start + offset, i + offset, target));
                    start = -1;
                    number = null;
                }
                default -> {
                    if (start == -1)
                        break;

                    if (Character.isDigit(c)) number.append(c);
                    else {
                        start = -1;
                        number = null;
                    }
                }
            }

            if (!inTag && c == '<') {
                inTag = true;
                builder.append(c);
                continue;
            }

            if (!inTag) {
                builder.append(c);
                continue;
            }

            switch (c) {
                case '>' -> {
                    inTag = false;
                    if (valueStart != -1 && pattern.charAt(i-1) != escape) {
                        builder.append(escape);
                        offset++;
                    }
                    valueStart = -1;
                }
                case ':' -> {
                    if (valueStart != -1 && pattern.charAt(i-1) != escape) {
                        builder.append(escape);
                        offset++;
                    }
                    valueStart = i;
                }
                default -> {
                    if (valueStart == -1 || valueStart + 1 != i)
                        break;

                    if (c == '"' || c == '\'') escape = c;
                    else {
                        escape = '\'';
                        builder.append(escape);
                        offset++;
                    }
                }
            }

            builder.append(c);
        }
        this.pattern = builder.toString();
        this.arguments = Collections.unmodifiableList(args);
        this.maxArgument = args.stream()
                .mapToInt(Argument::target)
                .max()
                .orElse(-1);
    }

    public String format(String... args) {
        String result = pattern;
        int offset = 0;
        for (Argument arg : arguments) {
            if (arg.target >= args.length)
                continue;
            String before = result.substring(0, arg.start + offset);
            String after = result.substring(arg.end + offset);

            String value = args[arg.target];
            result = before + value + after;
            offset += value.length() - arg.offset;
        }

        return result;
    }

    private record Argument(int start, int end, int target, int offset) {
        private Argument(int start, int end, int target) {
            this(start, end + 1, target, end - start + 1);
        }
    }
}
