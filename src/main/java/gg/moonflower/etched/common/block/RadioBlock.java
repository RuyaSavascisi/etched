package gg.moonflower.etched.common.block;

import com.mojang.serialization.MapCodec;
import gg.moonflower.etched.common.blockentity.RadioBlockEntity;
import gg.moonflower.etched.common.menu.RadioMenu;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.mixin.client.render.LevelRendererAccessor;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public class RadioBlock extends BaseEntityBlock {

    public static final MapCodec<RadioBlock> CODEC = simpleCodec(RadioBlock::new);

    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty PORTAL = BooleanProperty.create("portal");
    private static final VoxelShape X_SHAPE = Block.box(5.0D, 0.0D, 2.0D, 11.0D, 8.0D, 14.0D);
    private static final VoxelShape Z_SHAPE = Block.box(2.0D, 0.0D, 5.0D, 14.0D, 8.0D, 11.0D);
    private static final VoxelShape ROTATED_SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 8.0D, 13.0D);
    private static final Component CONTAINER_TITLE = Component.translatable("container." + Etched.MOD_ID + ".radio");

    public RadioBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ROTATION, 0).setValue(POWERED, false).setValue(PORTAL, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(Items.CAKE) && !state.getValue(PORTAL)) {
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            level.setBlock(pos, state.setValue(PORTAL, true), 3);
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!level.isClientSide()) {
            MenuProvider menuProvider = state.getMenuProvider(level, pos);
            if (menuProvider != null) {
                String url = level.getBlockEntity(pos) instanceof RadioBlockEntity be ? be.getUrl() : null;
                player.openMenu(menuProvider, buf -> buf.writeUtf(url != null ? url : ""));
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(ROTATION, Mth.floor((double) ((180.0F + context.getRotation()) * 16.0F / 360.0F) + 0.5) & 15)
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos blockPos2, boolean bl) {
        if (!level.isClientSide()) {
            if (state.getValue(POWERED) != level.hasNeighborSignal(pos)) {
                level.setBlock(pos, state.cycle(POWERED), 2);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof RadioBlockEntity radio) {
                if (radio.isPlaying()) {
                    level.levelEvent(1011, pos, 0);
                }
                Clearable.tryClear(blockEntity);
            }

            super.onRemove(state, level, pos, newState, moving);
        }
    }

    @Override
    public MenuProvider getMenuProvider(BlockState blockState, Level level, BlockPos blockPos) {
        return new SimpleMenuProvider((menuId, playerInventory, player) -> new RadioMenu(menuId, ContainerLevelAccess.create(level, blockPos)), CONTAINER_TITLE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext collisionContext) {
        int rotation = state.getValue(ROTATION);
        if (rotation % 8 == 0) {
            return Z_SHAPE;
        }
        if (rotation % 8 == 4) {
            return X_SHAPE;
        }
        return ROTATED_SHAPE;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState blockState) {
        return true;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), 16));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), 16));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION, POWERED, PORTAL);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(state.getValue(PORTAL) ? EtchedBlocks.PORTAL_RADIO_ITEM.get() : EtchedBlocks.RADIO.get());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!level.getBlockState(pos.above()).isAir()) {
            return;
        }

        if (!(level.getBlockEntity(pos) instanceof RadioBlockEntity radio)) {
            return;
        }

        if (radio.getUrl() == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Map<BlockPos, SoundInstance> sounds = ((LevelRendererAccessor) minecraft.levelRenderer).getPlayingJukeboxSongs();
        if (sounds.containsKey(pos) && minecraft.getSoundManager().isActive(sounds.get(pos))) {
            level.addParticle(ParticleTypes.NOTE, pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D, random.nextInt(25) / 24D, 0, 0);
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
        if (!level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, EtchedBlocks.RADIO_BE.get(), RadioBlockEntity::tickClient);
    }
}