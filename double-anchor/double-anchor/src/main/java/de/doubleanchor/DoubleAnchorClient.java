package de.doubleanchor;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Double Anchor (Client-Mod, nur fuer eigene Server / Singleplayer gedacht).
 *
 * Rechtsklick mit dem Anker (Slot 4) auf einen leeren Respawn-Anker:
 *   Tick 1: Slot 5 (Glowstone) -> genau 1x Glowstone einfuellen
 *   Tick 2: zurueck auf Slot 4 (Anker) -> Anker zuenden
 * Es wird KEIN zusaetzlicher Anker platziert.
 */
public class DoubleAnchorClient implements ClientModInitializer {

    // Hotbar-Slots 1-9 (so wie du sie im Spiel zaehlst)
    private static final int ANCHOR_SLOT = 4;
    private static final int GLOWSTONE_SLOT = 5;

    private static boolean running = false;
    private static int step = 0;
    private static BlockHitResult target = null;

    @Override
    public void onInitializeClient() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!level.isClientSide() || running || hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            // Im Nether funktioniert der Anker normal (setzt Spawn), da nichts abfangen
            if (level.dimensionType().respawnAnchorWorks()) {
                return InteractionResult.PASS;
            }
            if (!player.getMainHandItem().is(Items.RESPAWN_ANCHOR)) {
                return InteractionResult.PASS;
            }
            BlockState state = level.getBlockState(hit.getBlockPos());
            if (!state.is(Blocks.RESPAWN_ANCHOR)) {
                return InteractionResult.PASS;
            }
            // Geladener Anker: normales Verhalten (Explosion), nichts umbauen
            if (state.getValue(RespawnAnchorBlock.CHARGE) > 0) {
                return InteractionResult.PASS;
            }

            target = hit;
            step = 1;
            running = true;
            // Normales Platzieren eines weiteren Ankers verhindern
            return InteractionResult.FAIL;
        });

        ClientTickEvents.END_CLIENT_TICK.register(DoubleAnchorClient::tick);
    }

    private static void tick(Minecraft mc) {
        if (!running) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gameMode == null || target == null) {
            reset();
            return;
        }

        if (step == 1) {
            player.getInventory().setSelectedSlot(GLOWSTONE_SLOT - 1);
            mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, target);
            player.swing(InteractionHand.MAIN_HAND);
            step = 2;
        } else if (step == 2) {
            player.getInventory().setSelectedSlot(ANCHOR_SLOT - 1);
            mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, target);
            player.swing(InteractionHand.MAIN_HAND);
            reset();
        }
    }

    private static void reset() {
        running = false;
        step = 0;
        target = null;
    }
}
