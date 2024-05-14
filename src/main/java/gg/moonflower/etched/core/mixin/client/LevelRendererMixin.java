package gg.moonflower.etched.core.mixin.client;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.sound.StopListeningSound;
import gg.moonflower.etched.client.GuiHook;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Unique
    private BlockPos etched$pos;

    @Shadow
    private ClientLevel level;

    @Shadow
    protected abstract void notifyNearbyEntities(Level level, BlockPos blockPos, boolean bl);

    @Inject(method = "playStreamingMusic(Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/RecordItem;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setNowPlaying(Lnet/minecraft/network/chat/Component;)V", shift = At.Shift.BEFORE))
    public void preNowPlaying(SoundEvent arg, BlockPos arg2, RecordItem musicDiscItem, CallbackInfo ci) {
        if (!this.level.getBlockState(this.etched$pos.above()).isAir() || !PlayableRecord.canShowMessage(this.etched$pos.getX() + 0.5, this.etched$pos.getY() + 0.5, this.etched$pos.getZ() + 0.5)) {
            GuiHook.setHidePlayingText(true);
        }
    }

    @Inject(method = "playStreamingMusic(Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/RecordItem;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setNowPlaying(Lnet/minecraft/network/chat/Component;)V", shift = At.Shift.AFTER))
    public void postNowPlaying(SoundEvent arg, BlockPos arg2, RecordItem musicDiscItem, CallbackInfo ci) {
        GuiHook.setHidePlayingText(false);
    }

    @Inject(method = "playStreamingMusic(Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/RecordItem;)V", at = @At("HEAD"), remap = false)
    public void playRecord(SoundEvent soundEvent, BlockPos pos, RecordItem musicDiscItem, CallbackInfo ci) {
        this.etched$pos = pos;
    }

    @ModifyVariable(method = "playStreamingMusic(Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/RecordItem;)V", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.BEFORE), index = 4, remap = false)
    public SoundInstance modifySoundInstance(SoundInstance soundInstance) {
        return StopListeningSound.create(soundInstance, () -> this.notifyNearbyEntities(this.level, this.etched$pos, false));
    }
}
