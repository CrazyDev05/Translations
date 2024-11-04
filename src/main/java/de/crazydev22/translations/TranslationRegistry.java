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

import lombok.extern.java.Log;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import net.kyori.adventure.translation.Translator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.text.AttributedCharacterIterator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.logging.Level;

@Log
public class TranslationRegistry extends TranslatableComponentRenderer<Locale> {
    private final AtomicReference<KyoriTranslationRegistry> ref = new AtomicReference<>();
    private final Predicate<KyoriTranslationRegistry> loader;
    private final MiniMessage miniMessage;
    private transient Locale defaultLocale;

    public TranslationRegistry(MiniMessage miniMessage, Locale defaultLocale, Predicate<KyoriTranslationRegistry> loader) {
        this.miniMessage = miniMessage;
        this.defaultLocale = defaultLocale;
        this.loader = loader;
        reload();
    }

    public TranslationRegistry(File folder, MiniMessage miniMessage, Locale defaultLocale) {
        this(miniMessage, defaultLocale, fileLoader(folder));
    }

    public void defaultLocale(@NotNull Locale locale) {
        this.defaultLocale = locale;
        ref.get().defaultLocale(locale);
    }

    public boolean contains(@NotNull String key) {
        return ref.get().contains(key);
    }

    public boolean contains(@NotNull String key, @NotNull Locale locale) {
        return ref.get().translate(key, locale) != null;
    }

    @Override
    protected @Nullable MessageFormat translate(final @NotNull String key, final @NotNull Locale context) {
        return ref.get().translate(key, context);
    }

    @Override
    protected @NotNull Component renderTranslatable(final @NotNull TranslatableComponent component, final @NotNull Locale context) {
        var format = translate(component.key(), context);
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

        List<TranslationArgument> args = component.arguments();

        TextComponent.Builder builder = Component.text();
        this.mergeStyle(component, builder, context);

        // no arguments makes this render very simple
        if (args.isEmpty()) {
            builder.append(render(miniMessage.deserialize(format.format(null, new StringBuffer(), null).toString()), context));
            return optionallyRenderChildrenAppendAndBuild(component.children(), builder, context);
        }

        Object[] nulls = new Object[args.size()];
        StringBuffer sb = format.format(nulls, new StringBuffer(), null);
        AttributedCharacterIterator it = format.formatToCharacterIterator(nulls);

        while (it.getIndex() < it.getEndIndex()) {
            int end = it.getRunLimit();
            var index = (Integer) it.getAttribute(MessageFormat.Field.ARGUMENT);
            if (index != null) {
                TranslationArgument arg = args.get(index);
                if (arg.value() instanceof Component) {
                    builder.append(render(arg.asComponent(), context));
                } else {
                    builder.append(arg.asComponent()); // todo: number rendering?
                }
            } else {
                builder.append(render(miniMessage.deserialize(sb.substring(it.getIndex(), end)), context));
            }
            it.setIndex(end);
        }

        return optionallyRenderChildrenAppendAndBuild(component.children(), builder, context);
    }

    public void reload() {
        ref.updateAndGet(old -> {
            var reg = new KyoriTranslationRegistry(Key.key("crazydev22", "translations"));
            reg.defaultLocale(defaultLocale);
            return loader.test(reg) ? reg : old;
        });
    }

    public static Predicate<KyoriTranslationRegistry> fileLoader(File folder) {
        return registry -> {
            var files = folder.listFiles(file -> file.getName().endsWith(".properties"));
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
                    var locale = Translator.parseLocale(tag);
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
