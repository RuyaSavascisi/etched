package gg.moonflower.etched.api.record;

import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.common.component.EtchedMusicComponent;
import gg.moonflower.etched.core.registry.EtchedComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.net.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Denotes an item as having the capability of being played as a record item.
 *
 * @author Ocelot
 * @since 2.0.0
 */
public interface PlayableRecord {

    /**
     * Checks to see if the specified stack can be played.
     *
     * @param stack The stack to check
     * @return Whether that stack can play
     */
    static boolean isPlayableRecord(ItemStack stack) {
        return stack.has(DataComponents.JUKEBOX_PLAYABLE);
    }

    /**
     * Checks to see if the local player is close enough to receive the record text.
     *
     * @param x The x position of the entity
     * @param y The y position of the entity
     * @param z The z position of the entity
     * @return Whether the player is within distance
     */
    @OnlyIn(Dist.CLIENT)
    static boolean canShowMessage(double x, double y, double z) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null || player.distanceToSqr(x, y, z) <= 4096.0;
    }

    /**
     * Sends a packet to the client notifying them to begin playing an entity record.
     *
     * @param entity  The entity playing the record
     * @param record  The record to play
     * @param restart Whether to restart the track from the beginning or start a new playback
     */
    static void playEntityRecord(Entity entity, ItemStack record, boolean restart) {
//        EtchedMessages.PLAY.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), new ClientboundPlayEntityMusicPacket(record, entity, restart));
    }

    /**
     * Sends a packet to the client notifying them to stop playing an entity record.
     *
     * @param entity The entity to stop playing records
     */
    static void stopEntityRecord(Entity entity) {
//        EtchedMessages.PLAY.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), new ClientboundPlayEntityMusicPacket(entity));
    }

    /**
     * Retrieves the music for the specified stack.
     *
     * @param stack The stack to check
     * @return The tracks on that record
     */
    static Optional<List<TrackData>> getStackMusic(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        EtchedMusicComponent music = stack.getComponents().get(EtchedComponents.MUSIC.get());
        return music != null ? Optional.of(music.tracks()) : Optional.empty();
    }

    /**
     * Retrieves the album music for the specified stack.
     *
     * @param stack The stack to check
     * @return The album track on that record
     */
    static Optional<TrackData> getStackAlbum(ItemStack stack) {
        return Optional.empty(); // TODO
    }

    /**
     * Retrieves the number of tracks on the specified stack.
     *
     * @param stack The stack to check
     * @return The number of tracks on the record
     */
    static int getStackTrackCount(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof PlayableRecord record)) {
            return 0;
        }
        return record.getTrackCount(stack);
    }

    /**
     * Checks to see if this item can be played.
     *
     * @param stack The stack to check
     * @return Whether it can play
     */
    default boolean canPlay(ItemStack stack) {
        return this.getMusic(stack).isPresent();
    }

    /**
     * Creates the sound for an entity.
     *
     * @param stack               The stack to play
     * @param entity              The entity to play the sound for
     * @param track               The track to play on the disc
     * @param attenuationDistance The attenuation distance of the sound
     * @return The sound to play or nothing to error
     */
    @OnlyIn(Dist.CLIENT)
    default Optional<? extends SoundInstance> createEntitySound(ItemStack stack, Entity entity, int track, int attenuationDistance) {
        return track < 0 ? Optional.empty() : this.getMusic(stack).filter(tracks -> track < tracks.length).map(tracks -> SoundTracker.getEtchedRecord(tracks[track].url(), tracks[track].getDisplayName(), entity, attenuationDistance, false));
    }

    /**
     * Creates the sound for an entity with the default attenuation distance.
     *
     * @param stack  The stack to play
     * @param entity The entity to play the sound for
     * @param track  The track to play on the disc
     * @return The sound to play or nothing to error
     */
    @OnlyIn(Dist.CLIENT)
    default Optional<? extends SoundInstance> createEntitySound(ItemStack stack, Entity entity, int track) {
        return this.createEntitySound(stack, entity, track, 16);
    }

    /**
     * Retrieves the album cover for this item.
     *
     * @param stack The stack to get art for
     * @return A future for a potential cover
     */
    @OnlyIn(Dist.CLIENT)
    CompletableFuture<AlbumCover> getAlbumCover(ItemStack stack, Proxy proxy, ResourceManager resourceManager);

    /**
     * Retrieves the music URL from the specified stack.
     *
     * @param stack The stack to get NBT from
     * @return The optional URL for that item
     */
    Optional<TrackData[]> getMusic(ItemStack stack);

    /**
     * Retrieves the album data from the specified stack.
     *
     * @param stack The stack to get the album for
     * @return The album data or the first track if not an album
     */
    Optional<TrackData> getAlbum(ItemStack stack);

    /**
     * Retrieves the number of tracks in the specified stack.
     *
     * @param stack The stack to get tracks for
     * @return The number of tracks
     */
    int getTrackCount(ItemStack stack);
}
