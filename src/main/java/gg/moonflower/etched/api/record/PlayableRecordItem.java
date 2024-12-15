package gg.moonflower.etched.api.record;

import gg.moonflower.etched.api.sound.download.SoundSourceManager;
import gg.moonflower.etched.core.Etched;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.net.Proxy;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class PlayableRecordItem extends Item implements PlayableRecord {

    private static final Component ALBUM = Component.translatable("item." + Etched.MOD_ID + ".etched_music_disc.album").withStyle(ChatFormatting.DARK_GRAY);

    public PlayableRecordItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.JUKEBOX) || state.getValue(JukeboxBlock.HAS_RECORD)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        if (this.getMusic(stack).isEmpty()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            Player player = context.getPlayer();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof JukeboxBlockEntity jukeboxblockentity) {
                jukeboxblockentity.setTheItem(stack.copy());
                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
            }

            stack.shrink(1);
            if (player != null) {
                player.awardStat(Stats.PLAY_RECORD);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

//    @Override
//    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag tooltipFlag) {
//        this.getAlbum(stack).ifPresent(track -> {
//            boolean album = this.getTrackCount(stack) > 1;
//            list.add(track.getDisplayName().copy().withStyle(ChatFormatting.GRAY));
//            SoundSourceManager.getBrandText(track.url())
//                    .map(component -> Component.literal("  ").append(component.copy()))
//                    .map(component -> album ? component.append(" ").append(ALBUM) : component)
//                    .ifPresentOrElse(list::add, () -> {
//                        if (album) {
//                            list.add(ALBUM);
//                        }
//                    });
//        });
//    }
    // FIXME

    @Override
    public CompletableFuture<AlbumCover> getAlbumCover(ItemStack stack, Proxy proxy, ResourceManager resourceManager) {
        return this.getAlbum(stack).map(data -> SoundSourceManager.resolveAlbumCover(data.url(), null, proxy, resourceManager)).orElseGet(() -> CompletableFuture.completedFuture(AlbumCover.EMPTY));
    }
}
