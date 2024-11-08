import de.crazydev22.translations.Format;
import de.crazydev22.translations.TranslationRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class FormattingTest {

    @Test
    void test() {
        var mini = MiniMessage.miniMessage();
        var registry = new TranslationRegistry(mini, Locale.ROOT, r -> {
            r.register("test", Locale.ROOT, new Format("<green>Test {1} <lang:test2:{2}> {0}"));
            r.register("test2", Locale.ROOT, new Format("\n<yellow>Hi {0}"));
            return true;
        });

        assertTrue(registry.contains("test"));
        var msg = Component.text()
                .append(Component.translatable("test",
                        Component.text(1),
                        Component.text(2),
                        Component.text(3).decorate(TextDecoration.UNDERLINED)))
                .color(NamedTextColor.RED)
                .build();

        var colored = registry.render(msg, Locale.ROOT);
        var serialized = mini.serialize(colored);
        assertEquals("<red><green>Test 2 \n<yellow>Hi <underlined>3</underlined></yellow> 1", serialized);
    }
}
