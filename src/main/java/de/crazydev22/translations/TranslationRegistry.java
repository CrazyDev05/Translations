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

import lombok.NonNull;
import lombok.extern.java.Log;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.translation.Translator;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.logging.Level;

@Log
public class TranslationRegistry extends TranslatableComponentRenderer<Locale> {
    private final AtomicReference<KyoriTranslationRegistry> ref = new AtomicReference<>();
    private final Predicate<KyoriTranslationRegistry> loader;
    private final MiniMessage miniMessage;

    public TranslationRegistry(@NonNull MiniMessage miniMessage, @NonNull Locale defaultLocale, @NonNull Predicate<@NotNull KyoriTranslationRegistry> loader) {
        this.miniMessage = miniMessage;
        this.loader = loader;
        var reg = new KyoriTranslationRegistry();
        reg.defaultLocale(defaultLocale);
        ref.set(reg);
        reload();
    }

    public TranslationRegistry(@NonNull File folder, @NonNull MiniMessage miniMessage, @NonNull Locale defaultLocale) {
        this(miniMessage, defaultLocale, fileLoader(folder));
    }

    public void defaultLocale(@NonNull Locale locale) {
        ref.get().defaultLocale(locale);
    }

    @NotNull
    public Locale defaultLocale() {
        return ref.get().defaultLocale();
    }

    public boolean contains(@NonNull String key) {
        return ref.get().contains(key);
    }

    public boolean contains(@NonNull String key, @NonNull Locale locale) {
        return ref.get().translate(key, locale) != null;
    }

    @Override
    protected @NotNull Component renderTranslatable(@NotNull TranslatableComponent component, @NotNull Locale context) {
        Format format = ref.get().translate(component.key(), context);
        if (format == null) {
            TranslatableComponent.Builder builder = Component.translatable()
                    .key(component.key()).fallback(component.fallback());
            if (!component.arguments().isEmpty()) {
                List<TranslationArgument> args = new ArrayList<>(component.arguments());
                for (int i = 0, size = args.size(); i < size; i++) {
                    TranslationArgument arg = args.get(i);
                    if (arg.value() instanceof Component) {
                        args.set(i, TranslationArgument.component(render(((Component) arg.value()), context)));
                    }
                }
                builder.arguments(args);
            }
            return mergeStyleAndOptionallyDeepRender(component, builder, context);
        }

        TextComponent.Builder builder = Component.text();
        this.mergeStyle(component, builder, context);
        String[] args = component.arguments()
                .stream()
                .map(TranslationArgumentLike::asComponent)
                .map(Component::compact)
                .map(miniMessage::serialize)
                .toArray(String[]::new);

        Component translated = miniMessage.deserialize(format.format(args));
        builder.append(render(translated, context));

        return optionallyRenderChildrenAppendAndBuild(component.children(), builder, context);
    }

    public void reload() {
        ref.updateAndGet(old -> {
            KyoriTranslationRegistry reg = new KyoriTranslationRegistry();
            reg.defaultLocale(old.defaultLocale());
            return loader.test(reg) ? reg : old;
        });
    }

    @NotNull
    public static Predicate<@NotNull KyoriTranslationRegistry> fileLoader(@NonNull File folder) {
        return registry -> {
            File[] files = folder.listFiles(file -> file.getName().endsWith(".properties"));
            if (files == null) {
                log.warning("Failed to find translation files in " + folder);
                return false;
            }
            if (files.length == 0) {
                log.warning("No translation files found in " + folder);
                return false;
            }

            try {
                for (File file : files) {
                    String tag = file.getName();
                    tag = tag.substring(0, tag.length() - ".properties".length());
                    Locale locale = Translator.parseLocale(tag);
                    if (locale == null) {
                        log.warning("Failed to parse locale from " + tag);
                        continue;
                    }

                    registry.registerAll(locale, file.toPath(), true);
                }
                return true;
            } catch (Throwable e) {
                log.log(Level.SEVERE, "Failed to load translation files in " + folder, e);
                return false;
            }
        };
    }
}
