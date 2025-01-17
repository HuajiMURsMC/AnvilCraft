package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.inventory.SliderMenu;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class CreativeDynamoBlockEntity extends BlockEntity implements IPowerProducer, IPowerConsumer, MenuProvider {
    private PowerGrid grid = null;
    @Setter
    private int power = 16;

    public static @NotNull CreativeDynamoBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new CreativeDynamoBlockEntity(type, pos, blockState);
    }

    public CreativeDynamoBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.CREATIVE_DYNAMO.get(), pos, blockState);
    }

    private CreativeDynamoBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("power", power);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.power = tag.getInt("power");
    }

    @Override
    public int getOutputPower() {
        return this.power > 0 ? this.power : 0;
    }

    @Override
    public int getInputPower() {
        return this.power < 0 ? -this.power : 0;
    }

    @Override
    public @NotNull PowerComponentType getComponentType() {
        return this.power > 0 ? PowerComponentType.PRODUCER : PowerComponentType.CONSUMER;
    }

    @Override
    public @NotNull BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public void setGrid(PowerGrid grid) {
        this.grid = grid;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return ModBlocks.CREATIVE_DYNAMO.get().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
        return new SliderMenu(i, -8192, 8192, this::setPower);
    }

    @Override
    public Level getCurrentLevel() {
        return super.getLevel();
    }
}
