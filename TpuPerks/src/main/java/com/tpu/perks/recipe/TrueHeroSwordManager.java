package com.tpu.perks.recipe;

import com.tpu.perks.TpuPerks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.advancement.Advancement;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Registra en el servidor, una única vez al arrancar el plugin:
 *  1. El ítem resultado: una espada de netherite con Filo 10, Unbreaking 3,
 *     Mending, Barrido (Sweeping Edge) 2 y Botín (Looting) 5, todos por
 *     encima del máximo vanilla legal (se aplican como "unsafe enchantments"
 *     a propósito, ya que son el premio de un perk especial).
 *  2. La receta con forma exacta pedida (grid 3x3, ver comentario de layout).
 *  3. Un advancement invisible/oculto que sirve como "logro" THE TRUE HERO:
 *     se le otorga al jugador al comprar el perk, lo que dispara el toast
 *     morado nativo de Minecraft y el broadcast global automático de logros.
 *
 * IMPORTANTE sobre "receta descubierta": Bukkit no permite ocultar una receta
 * shaped del libro de recetas de TODOS los jugadores por defecto sin más - una
 * vez registrada en el servidor, cualquier jugador que reúna los ingredientes
 * podría craftearla si ya la conoce. Por eso esta receta se registra con
 * discovery deshabilitado por defecto de forma efectiva: solo se le enseña
 * (player.discoverRecipe) al jugador que compra el perk, así que el resto del
 * servidor nunca la ve aparecer en su libro de recetas ni sabe que existe.
 */
public final class TrueHeroSwordManager {

    private final TpuPerks plugin;
    private final NamespacedKey recipeKey;
    private final NamespacedKey advancementKey;

    public TrueHeroSwordManager(TpuPerks plugin) {
        this.plugin = plugin;
        this.recipeKey = new NamespacedKey(plugin, "true_hero_sword");
        this.advancementKey = new NamespacedKey(plugin, "true_hero_advancement");
    }

    /** Debe llamarse una vez en onEnable(), después de que el servidor esté listo para recetas. */
    public void registerAll() {
        registerRecipe();
        registerAdvancement();
    }

    // ---------------------------------------------------------------------
    // Receta
    // ---------------------------------------------------------------------

    private void registerRecipe() {
        // Evita re-registrar si el plugin se recarga en caliente (plugman, /reload, etc.).
        if (Bukkit.getRecipe(recipeKey) != null) {
            Bukkit.removeRecipe(recipeKey);
        }

        ItemStack result = buildSwordItem();

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);

        // Layout pedido explícitamente (numeración 1-9 de izquierda a derecha, arriba a abajo):
        //   1: nada          2: bloque netherite   3: nada
        //   4: estrella      5: bloque netherite   6: estrella
        //   7: nada          8: bara de blaze      9: nada
        //
        // Bukkit shape usa filas de 3 caracteres cada una; un espacio " " = slot vacío.
        recipe.shape(
                " N ",
                "SNS",
                " B "
        );
        recipe.setIngredient('N', new RecipeChoice.MaterialChoice(Material.NETHERITE_BLOCK));
        recipe.setIngredient('S', new RecipeChoice.MaterialChoice(Material.NETHER_STAR));
        recipe.setIngredient('B', new RecipeChoice.MaterialChoice(Material.BLAZE_ROD));

        Bukkit.addRecipe(recipe);
    }

    private ItemStack buildSwordItem() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();

        meta.displayName(Component.text("Filo del Héroe Verdadero", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Forjada por manos que la leyenda recordará.",
                                NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, true)
        ));

        // Encantamientos por encima del máximo vanilla legal: se aplican como "unsafe"
        // a propósito, ya que este es el premio de un perk especial y no debe estar
        // sujeto a los límites normales de mesa de encantar/yunque.
        meta.addEnchant(Enchantment.SHARPNESS, 10, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addEnchant(Enchantment.SWEEPING_EDGE, 2, true);
        meta.addEnchant(Enchantment.LOOTING, 5, true);

        sword.setItemMeta(meta);
        return sword;
    }

    /** Enseña la receta SOLO a este jugador, sin afectar el libro de recetas de nadie más. */
    public void discoverRecipeFor(Player player) {
        player.discoverRecipe(recipeKey);
    }

    // ---------------------------------------------------------------------
    // Advancement / logro "THE TRUE HERO"
    // ---------------------------------------------------------------------

    private void registerAdvancement() {
        if (Bukkit.getAdvancement(advancementKey) != null) {
            return; // Los advancements cargados vía loadAdvancement no se pueden "recargar" limpiamente.
        }

        // JSON de advancement estándar de Minecraft. "hidden": true evita que aparezca
        // en el árbol de logros hasta que se otorga; "announce_to_chat": true es lo que
        // dispara el broadcast global automático de Minecraft (el mismo sistema que
        // anuncia "Steve ha conseguido el logro [X]" a todo el servidor).
        String json = """
                {
                  "display": {
                    "icon": { "id": "minecraft:netherite_sword" },
                    "title": { "text": "THE TRUE HERO", "color": "light_purple", "bold": true },
                    "description": { "text": "Desbloqueaste el legado del héroe verdadero." },
                    "frame": "challenge",
                    "show_toast": true,
                    "announce_to_chat": true,
                    "hidden": true
                  },
                  "criteria": {
                    "unlocked_by_perk": {
                      "trigger": "minecraft:impossible",
                      "conditions": {}
                    }
                  }
                }
                """;

        Advancement advancement = Bukkit.getUnsafe().loadAdvancement(advancementKey, json);
        if (advancement == null) {
            plugin.getLogger().warning("No se pudo registrar el advancement THE TRUE HERO.");
        }
    }

    /**
     * Otorga el advancement al jugador (dispara el toast morado nativo + el broadcast
     * global automático de Minecraft) y reproduce un sonido épico adicional para
     * reforzar el momento, tal como se pidió.
     */
    public void grantAdvancement(Player player) {
        Advancement advancement = Bukkit.getAdvancement(advancementKey);
        if (advancement == null) {
            return;
        }

        var progress = player.getAdvancementProgress(advancement);
        if (!progress.isDone()) {
            for (String criterion : progress.getRemainingCriteria()) {
                progress.awardCriteria(criterion);
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.6f);
    }
}
