package de.crazydev22.translations;

import net.kyori.adventure.text.*;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.renderer.AbstractComponentRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

@SuppressWarnings("all")
public abstract class TranslatableComponentRenderer<C> extends AbstractComponentRenderer<C> {
    private static final Set<Style.Merge> MERGES = Style.Merge.merges(Style.Merge.COLOR, Style.Merge.DECORATIONS, Style.Merge.INSERTION, Style.Merge.FONT);

    @Override
    protected @NotNull Component renderBlockNbt(@NotNull BlockNBTComponent component, @NotNull C context) {
        BlockNBTComponent.Builder builder = nbt(context, Component.blockNBT(), component)
                .pos(component.pos());
        return this.mergeStyleAndOptionallyDeepRender(component, builder, context);
    }

    @Override
    protected @NotNull Component renderEntityNbt(@NotNull EntityNBTComponent component, @NotNull C context) {
        EntityNBTComponent.Builder builder = nbt(context, Component.entityNBT(), component)
                .selector(component.selector());
        return mergeStyleAndOptionallyDeepRender(component, builder, context);
    }

    @Override
    protected @NotNull Component renderStorageNbt(@NotNull StorageNBTComponent component, @NotNull C context) {
        StorageNBTComponent.Builder builder = nbt(context, Component.storageNBT(), component)
                .storage(component.storage());
        return mergeStyleAndOptionallyDeepRender(component, builder, context);
    }

    protected <O extends NBTComponent<O, B>, B extends NBTComponentBuilder<O, B>> B nbt(@NotNull C context, B builder, O oldComponent) {
        builder.nbtPath(oldComponent.nbtPath())
                .interpret(oldComponent.interpret());
        Component separator = oldComponent.separator();
        if (separator != null) {
            builder.separator(render(separator, context));
        }
        return builder;
    }

    @Override
    protected @NotNull Component renderKeybind(@NotNull KeybindComponent component, @NotNull C context) {
        KeybindComponent.Builder builder = Component.keybind().keybind(component.keybind());
        return mergeStyleAndOptionallyDeepRender(component, builder, context);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected @NotNull Component renderScore(@NotNull ScoreComponent component, @NotNull C context) {
        ScoreComponent.Builder builder = Component.score()
                .name(component.name())
                .objective(component.objective())
                .value(component.value());
        return mergeStyleAndOptionallyDeepRender(component, builder, context);
    }

    @Override
    protected @NotNull Component renderSelector(@NotNull SelectorComponent component, @NotNull C context) {
        SelectorComponent.Builder builder = Component.selector().pattern(component.pattern());
        return mergeStyleAndOptionallyDeepRender(component, builder, context);
    }

    @Override
    protected @NotNull Component renderText(@NotNull TextComponent component, @NotNull C context) {
        TextComponent.Builder builder = Component.text().content(component.content());
        return mergeStyleAndOptionallyDeepRender(component, builder, context);
    }

    protected <O extends BuildableComponent<O, B>, B extends ComponentBuilder<O, B>> O mergeStyleAndOptionallyDeepRender(Component component, B builder, C context) {
        this.mergeStyle(component, builder, context);
        return optionallyRenderChildrenAppendAndBuild(component.children(), builder, context);
    }

    protected <O extends BuildableComponent<O, B>, B extends ComponentBuilder<O, B>> O optionallyRenderChildrenAppendAndBuild(List<Component> children, B builder, C context) {
        if (!children.isEmpty()) {
            children.forEach(child -> builder.append(this.render(child, context)));
        }
        return builder.build();
    }

    protected <B extends ComponentBuilder<?, ?>> void mergeStyle(Component component, B builder, C context) {
        builder.mergeStyle(component, MERGES);
        builder.clickEvent(component.clickEvent());
        @Nullable HoverEvent<?> hoverEvent = component.hoverEvent();
        if (hoverEvent != null) {
            builder.hoverEvent(hoverEvent.withRenderedValue(this, context));
        }
    }
}
