package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@SuppressWarnings("unused")
public interface IBucketPickupInjector extends BucketPickup {
    @Override
    default @NotNull ItemStack pickupBlock(LevelAccessor level, BlockPos pos, @NotNull BlockState state) {
        System.out.println(state);
        if (state.is(Blocks.LAVA_CAULDRON)) {
            level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
            return Items.LAVA_BUCKET.getDefaultInstance();
        }
        if (state.is(ModBlocks.LAVA_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == 3) {
            level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
            return Items.LAVA_BUCKET.getDefaultInstance();
        }
        if (state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == 3) {
            level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
            return Items.WATER_BUCKET.getDefaultInstance();
        }
        if (state.is(Blocks.POWDER_SNOW_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == 3) {
            level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
            return Items.POWDER_SNOW_BUCKET.getDefaultInstance();
        }
        return Items.BUCKET.getDefaultInstance();
    }

    @Override
    @SuppressWarnings("EqualsBetweenInconvertibleTypes")
    default @NotNull Optional<SoundEvent> getPickupSound() {
        if (this.equals(Blocks.WATER_CAULDRON)) return Optional.of(SoundEvents.BUCKET_FILL);
        if (this.equals(Blocks.LAVA_CAULDRON)) return Optional.of(SoundEvents.BUCKET_FILL_LAVA);
        if (this.equals(Blocks.POWDER_SNOW_CAULDRON)) return Optional.of(SoundEvents.BUCKET_FILL_POWDER_SNOW);
        return Optional.of(SoundEvents.EMPTY);
    }
}