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

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.util.TriState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;

public class KyoriTranslationRegistry implements TranslationRegistry {
    private final TranslationRegistry registry;

    public KyoriTranslationRegistry(Key name) {
        registry = TranslationRegistry.create(name);
    }

    @Override
    public boolean contains(@NotNull String key) {
        return registry.contains(key);
    }

    @Override
    public @NotNull TriState hasAnyTranslations() {
        return registry.hasAnyTranslations();
    }

    @Override
    public @NotNull Key name() {
        return registry.name();
    }

    @Override
    public @Nullable MessageFormat translate(@NotNull String key, @NotNull Locale locale) {
        return registry.translate(key, locale);
    }

    @Override
    public @Nullable Component translate(@NotNull TranslatableComponent component, @NotNull Locale locale) {
        return registry.translate(component, locale);
    }

    @Override
    public void defaultLocale(@NotNull Locale locale) {
        registry.defaultLocale(locale);
    }

    @Override
    public void register(@NotNull String key, @NotNull Locale locale, @NotNull MessageFormat format) {
        registry.register(key, locale, format);
    }

    @Override
    public void registerAll(@NotNull Locale locale, @NotNull Map<String, MessageFormat> formats) {
        registry.registerAll(locale, formats);
    }

    @Override
    public void registerAll(@NotNull Locale locale, @NotNull Set<String> keys, Function<String, MessageFormat> function) {
        registry.registerAll(locale, keys, function);
    }

    @Override
    public void registerAll(@NotNull Locale locale, @NotNull Path path, boolean escapeSingleQuotes) {
        registry.registerAll(locale, path, escapeSingleQuotes);
    }

    @Override
    public void registerAll(@NotNull Locale locale, @NotNull ResourceBundle bundle, boolean escapeSingleQuotes) {
        registry.registerAll(locale, bundle, escapeSingleQuotes);
    }

    @Override
    public void unregister(@NotNull String key) {
        registry.unregister(key);
    }
}
