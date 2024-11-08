/*
Copyright (C) 2024 Julian Krings

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, version 3.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package de.crazydev22.translations;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import net.kyori.examination.Examinable;
import net.kyori.examination.ExaminableProperty;
import net.kyori.examination.string.StringExaminer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

@EqualsAndHashCode
public class KyoriTranslationRegistry implements Examinable {
    private static final Pattern SINGLE_QUOTE_PATTERN = Pattern.compile("'");

    private final Map<String, Translation> translations = new ConcurrentHashMap<>();
    private Locale defaultLocale = Locale.US;

    public boolean contains(@NotNull String key) {
        return translations.containsKey(key);
    }

    public boolean contains(@NotNull String key, @NotNull Locale locale) {
        return translate(key, locale) != null;
    }

    public @Nullable Format translate(@NotNull String key, @NotNull Locale locale) {
        Translation translation = translations.get(key);
        if (translation == null) return null;
        return translation.translate(locale);
    }

    public void defaultLocale(@NotNull Locale locale) {
        this.defaultLocale = locale;
    }

    @NotNull
    public Locale defaultLocale() {
        return this.defaultLocale;
    }

    public void register(@NotNull String key, @NotNull Locale locale, @NotNull Format format) {
        translations.computeIfAbsent(key, Translation::new).register(locale, format);
    }

    public void registerAll(@NotNull Locale locale, @NotNull Map<String, Format> formats) {
        registerAll(locale, formats.keySet(), formats::get);
    }

    public void registerAll(@NotNull Locale locale, @NotNull Set<String> keys, Function<String, Format> function) {
        IllegalArgumentException firstError = null;
        int errorCount = 0;
        for (final String key : keys) {
            try {
                register(key, locale, function.apply(key));
            } catch (final IllegalArgumentException e) {
                if (firstError == null) {
                    firstError = e;
                }
                errorCount++;
            }
        }
        if (firstError != null) {
            if (errorCount == 1) {
                throw firstError;
            } else if (errorCount > 1) {
                throw new IllegalArgumentException(String.format("Invalid key (and %d more)", errorCount - 1), firstError);
            }
        }
    }

    public void registerAll(@NotNull Locale locale, @NotNull Path path, boolean escapeSingleQuotes) {
        try (final BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            registerAll(locale, new PropertyResourceBundle(reader), escapeSingleQuotes);
        } catch (IOException ignored) {}
    }

    public void registerAll(@NotNull Locale locale, @NotNull ResourceBundle bundle, boolean escapeSingleQuotes) {
        registerAll(locale, bundle.keySet(), key -> {
            String format = bundle.getString(key);
            return new Format(escapeSingleQuotes ? SINGLE_QUOTE_PATTERN.matcher(format).replaceAll("''") : format);
        });
    }

    public void unregister(@NotNull String key) {
        translations.remove(key);
    }

    public void unregister(@NotNull String key, @NotNull Locale locale) {
        Translation translation = translations.get(key);
        if (translation == null) return;
        translation.formats.remove(locale);
    }

    @Override
    public @NotNull Stream<? extends ExaminableProperty> examinableProperties() {
        return Stream.of(ExaminableProperty.of("translations", this.translations));
    }

    @Override
    public String toString() {
        return examine(StringExaminer.simpleEscaping());
    }

    @EqualsAndHashCode
    @RequiredArgsConstructor
    private class Translation implements Examinable {
        private final String key;
        private final Map<Locale, Format> formats = new ConcurrentHashMap<>();

        void register(@NotNull Locale locale, @NotNull Format format) {
            if (this.formats.putIfAbsent(requireNonNull(locale, "locale"), requireNonNull(format, "message format")) != null) {
                throw new IllegalArgumentException(String.format("Translation already exists: %s for %s", this.key, locale));
            }
        }

        @Nullable Format translate(final @NotNull Locale locale) {
            Format format = this.formats.get(requireNonNull(locale, "locale"));
            if (format == null) format = this.formats.get(new Locale(locale.getLanguage())); // try without country
            if (format == null) format = this.formats.get(defaultLocale); // try default locale
            if (format == null) format = this.formats.get(new Locale(defaultLocale.getLanguage())); // try default locale without country
            return format;
        }

        @Override
        public @NotNull Stream<? extends ExaminableProperty> examinableProperties() {
            return Stream.of(
                    ExaminableProperty.of("key", this.key),
                    ExaminableProperty.of("formats", this.formats)
            );
        }

        @Override
        public String toString() {
            return examine(StringExaminer.simpleEscaping());
        }
    }
}
