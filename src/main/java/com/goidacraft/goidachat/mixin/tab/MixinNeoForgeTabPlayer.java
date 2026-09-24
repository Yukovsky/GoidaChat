package com.goidacraft.goidachat.mixin.tab;

import com.goidacraft.goidachat.util.VanishCompat;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * В TAB v5.5.0 для NeoForge метод {@code NeoForgeTabPlayer.isVanished0()} всегда возвращает {@code false},
 * из-за чего в плейсхолдерах вроде {@code %online%}, {@code %staffonline%} и в таб-листе игроки в vanish
 * (мод Vanishmod) продолжают считаться и отображаться онлайн.
 *
 * Этот миксин безопасно подставляет реальный vanish-статус игрока из {@link VanishCompat}.
 */
@Pseudo
@Mixin(targets = "me.neznamy.tab.platforms.neoforge.NeoForgeTabPlayer", remap = false)
public abstract class MixinNeoForgeTabPlayer {

    @Shadow(remap = false)
    public abstract ServerPlayer getPlayer();

    @Inject(method = "isVanished0", at = @At("HEAD"), cancellable = true, remap = false)
    private void goidachat$isVanished0(CallbackInfoReturnable<Boolean> cir) {
        try {
            ServerPlayer player = this.getPlayer();
            if (player != null && VanishCompat.isVanished(player)) {
                cir.setReturnValue(true);
            }
        } catch (Throwable ignored) {
        }
    }
}
