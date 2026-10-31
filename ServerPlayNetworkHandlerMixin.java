package com.example.battlecats.mixin;

import com.example.battlecats.BattleManager;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts the "swap with offhand" key (default F) while a lane battle is running.
 *   F         -> fire Cat Cannon
 *   Shift + F -> upgrade wallet
 */
@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onPlayerAction", at = @At("HEAD"), cancellable = true)
    private void battlecats$onPlayerAction(PlayerActionC2SPacket packet, CallbackInfo ci) {
        if (packet.getAction() != PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND) return;
        // The packet is first seen on the network thread; only act once it is on the server thread.
        if (this.player.getServer() == null || !this.player.getServer().isOnThread()) return;
        if (BattleManager.handleSwapKey(this.player)) {
            ci.cancel();
        }
    }
}
