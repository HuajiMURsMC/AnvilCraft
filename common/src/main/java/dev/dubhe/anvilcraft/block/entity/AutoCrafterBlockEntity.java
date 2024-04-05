package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.AutoCrafterBlock;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.inventory.AutoCrafterMenu;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.function.Predicate;

@Getter
@SuppressWarnings("NullableProblems")
public class AutoCrafterBlockEntity extends BaseMachineBlockEntity implements CraftingContainer, IFilterBlockEntity {
    @Setter
    private boolean record = false;
    @Getter
    private final NonNullList<Boolean> disabled = this.getNewDisabled();
    @Getter
    private final NonNullList<@Unmodifiable ItemStack> filter = this.getNewFilter();
    private static int levelHash = 1;
    private static NonNullList<ItemStack> remainingItemsCache;
    private final Deque<AutoCrafterCache> cache = new ArrayDeque<>();

    public AutoCrafterBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.AUTO_CRAFTER.get(), pos, blockState);
    }

    public AutoCrafterBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState, 9);
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("block.anvilcraft.auto_crafter");
    }

    public static void tick(Level level, BlockPos pos, BlockEntity e) {
        if (!(e instanceof AutoCrafterBlockEntity entity)) return;
        BlockState state = level.getBlockState(pos);
        if (state.getValue(AutoCrafterBlock.LIT)) AutoCrafterBlockEntity.craft(level, entity);
    }

    private boolean canCraft() {
        if (!this.isRecord()) return true;
        for (int i = 0; i < this.items.size(); i++) {
            if (this.getItem(i).isEmpty() && !(this.getDisabled().get(i) || isRecord() && getFilter().get(i).isEmpty()))
                return false;
        }
        return true;
    }

    private static void craft(@NotNull Level level, @NotNull AutoCrafterBlockEntity entity) {
        ItemStack itemStack;
        if (entity.isEmpty()) return;
        if (!entity.canCraft()) return;
        Optional<AutoCrafterCache> cacheOptional = entity.cache.stream().filter(recipe -> recipe.test(entity)).findFirst();
        Optional<CraftingRecipe> optional;
        if (cacheOptional.isPresent()) {
            optional = cacheOptional.get().getRecipe();
        } else {
            optional = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, entity, level);
            AutoCrafterCache cache = new AutoCrafterCache(entity, optional);
            entity.cache.push(cache);
            while (entity.cache.size() >= 10) {
                entity.cache.pop();
            }
        }
        if (optional.isEmpty()) return;
        if (level.hashCode() != levelHash) {
            levelHash = level.hashCode();
            remainingItemsCache = level.getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, entity, level);
        }
        itemStack = optional.get().assemble(entity, level.registryAccess());
        if (!itemStack.isItemEnabled(level.enabledFeatures())) return;
        Container result = new SimpleContainer(1);
        result.setItem(0, itemStack);
        if (!entity.insertOrDropItem(entity.getDirection(), level, entity.getBlockPos(), result, 0, false, false, true, false)) {
            return;
        }
        for (int i = 0; i < 9; i++) {
            ItemStack stack = entity.getItem(i);
            stack.shrink(1);
            entity.setItem(i, stack);
        }
        Container container1 = new SimpleContainer(remainingItemsCache.size());
        for (int i = 0; i < remainingItemsCache.size(); i++) {
            container1.setItem(i, remainingItemsCache.get(i));
        }
        for (int i = 0; i < remainingItemsCache.size(); i++) {
            entity.insertOrDropItem(entity.getDirection(), level, entity.getBlockPos(), container1, i, true, true, true, false);
        }
        level.updateNeighborsAt(entity.getBlockPos(), ModBlocks.AUTO_CRAFTER.get());
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return AutoCrafterMenu.serverOf(containerId, inventory, this);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.loadTag(tag);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        this.saveTag(tag);
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack insertingStack) {
        if (this.getDisabled().get(index)) return false;
        ItemStack storedStack = this.items.get(index);
        ItemStack filterStack = this.getFilter().get(index);
        if (isRecord() && filterStack.isEmpty()) return insertingStack.isEmpty();
        int count = storedStack.getCount();
        if (count >= storedStack.getMaxStackSize()) {
            return false;
        }
        if (storedStack.isEmpty()) {
            return filterStack.isEmpty() || ItemStack.isSameItemSameTags(insertingStack, filterStack);
        }
        return !this.smallerStackExist(count, storedStack, index);
    }

    private boolean smallerStackExist(int count, ItemStack itemStack, int index) {
        for (int index2 = index + 1; index2 < 9; ++index2) {
            ItemStack itemStack1;
            if (this.getDisabled().get(index2) ||
                    (isRecord() && getFilter().get(index2).isEmpty()) ||
                    (!(itemStack1 = this.getItem(index2)).isEmpty() && (itemStack1.getCount() >= count) ||
                            !ItemStack.isSameItemSameTags(itemStack1, itemStack) ||
                            (itemStack1).isEmpty()))
                continue;
            return true;
        }
        return false;
    }

    @Override
    public Direction getDirection() {
        if (this.level == null) return Direction.UP;
        BlockState state = this.level.getBlockState(this.getBlockPos());
        if (state.is(ModBlocks.AUTO_CRAFTER.get())) return state.getValue(AutoCrafterBlock.FACING);
        return Direction.UP;
    }

    @Override
    public void setDirection(Direction direction) {
        BlockPos pos = this.getBlockPos();
        Level level = this.getLevel();
        if (null == level) return;
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.AUTO_CRAFTER.get())) return;
        level.setBlockAndUpdate(pos, state.setValue(AutoCrafterBlock.FACING, direction));
    }

    @Override
    public int getWidth() {
        return 3;
    }

    @Override
    public int getHeight() {
        return 3;
    }

    @Override
    public void fillStackedContents(StackedContents contents) {
        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack itemStack = this.getItem(i);
            contents.accountSimpleStack(itemStack);
        }
    }

    public int getRedstoneSignal() {
        int i = 0;
        for (int j = 0; j < this.getContainerSize(); ++j) {
            ItemStack itemStack = this.getItem(j);
            if (itemStack.isEmpty() && !this.getDisabled().get(j)) continue;
            ++i;
        }
        return i;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
        return AutoCrafterMenu.serverOf(i, inventory, this);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static class AutoCrafterCache implements Predicate<Container> {
        private final Container container;
        @Getter
        private final Optional<CraftingRecipe> recipe;

        public AutoCrafterCache(@NotNull Container container, Optional<CraftingRecipe> recipe) {
            this.container = new SimpleContainer(container.getContainerSize());
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack item = container.getItem(i).copy();
                item.setCount(1);
                this.container.setItem(i, item);
            }
            this.recipe = recipe;
        }

        @Override
        public boolean test(@NotNull Container container) {
            if (container.getContainerSize() != this.container.getContainerSize()) return false;
            for (int i = 0; i < this.container.getContainerSize(); i++) {
                if (!ItemStack.isSameItemSameTags(container.getItem(i), this.container.getItem(i))) return false;
            }
            return true;
        }
    }
}