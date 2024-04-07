package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.api.depository.ItemDepositorySlot;
import dev.dubhe.anvilcraft.block.entity.AutoCrafterBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.inventory.component.ReadOnlySlot;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

@Getter
public class AutoCrafterMenu extends AbstractContainerMenu {
    public final AutoCrafterBlockEntity blockEntity;
    private final Slot resultSlot;
    private final Level level;

    public AutoCrafterMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(menuType, containerId, inventory, inventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public AutoCrafterMenu(MenuType<?> menuType, int containerId, Inventory inventory, BlockEntity blockEntity) {
        super(menuType, containerId);
        checkContainerSize(inventory, 9);

        this.blockEntity = (AutoCrafterBlockEntity) blockEntity;
        this.level = inventory.player.level();

        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                addSlot(new ItemDepositorySlot(this.blockEntity.getDepository(), i * 3 + j, 26 + j * 18, 18 + i * 18));
            }
        }

        addSlot(resultSlot = new ReadOnlySlot(new SimpleContainer(1), 0, 8 + 7 * 18, 18 + 2 * 18));

    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    // CREDIT GOES TO: diesieben07 | https://github.com/diesieben07/SevenCommons
    // must assign a slot number to each of the slots used by the GUI.
    // For this container, we can see both the tile inventory's slots as well as the player inventory slots and the hotbar.
    // Each time we add a Slot to the container, it automatically increases the slotIndex, which means
    //  0 - 8 = hotbar slots (which will map to the InventoryPlayer slot numbers 0 - 8)
    //  9 - 35 = player inventory slots (which map to the InventoryPlayer slot numbers 9 - 35)
    //  36 - 44 = TileInventory slots, which map to our TileEntity slot numbers 0 - 8)
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    // THIS YOU HAVE TO DEFINE!
    private static final int TE_INVENTORY_SLOT_COUNT = 9;  // must be the number of slots you have!

    @Override
    public ItemStack quickMoveStack(Player playerIn, int pIndex) {
        Slot sourceSlot = slots.get(pIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;  //EMPTY_ITEM
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Check if the slot clicked is one of the vanilla container slots
        if (pIndex < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the tile inventory
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX
                    + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;  // EMPTY_ITEM
            }
        } else if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // This is a TE slot so merge the stack into the players inventory
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            System.out.println("Invalid slotIndex:" + pIndex);
            return ItemStack.EMPTY;
        }
        // If stack size == 0 (the entire stack was moved) set slot contents to null
        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.AUTO_CRAFTER.get());
    }

    //    public static AutoCrafterMenu clientOf(MenuType<AutoCrafterMenu> type, int containerId, Inventory inventory) {
//        return new Client(type, containerId, inventory);
//    }
//
//    public static AutoCrafterMenu serverOf(int containerId, @NotNull Inventory inventory, AutoCrafterBlockEntity blockEntity) {
//        return new Server(containerId, inventory, blockEntity);
//    }
//
//    private final Inventory inventory;
//    private final Slot resultSlot;
//
//    public AutoCrafterMenu(int containerId, @NotNull Inventory inventory, @NotNull CraftingContainer machine) {
//        this(ModMenuTypes.AUTO_CRAFTER.get(), containerId, inventory, machine);
//    }
//
//    public AutoCrafterMenu(MenuType<AutoCrafterMenu> type, int containerId, @NotNull Inventory inventory, @NotNull CraftingContainer machine) {
//        super(type, containerId, machine);
//        this.inventory = inventory;
//        this.machine.startOpen(this.inventory.player);
//        int i, j;
//        for (i = 0; i < 3; ++i) {
//            for (j = 0; j < 3; ++j) {
//                this.addSlot(new FilterSlot(this.machine, j + i * 3, 26 + j * 18, 18 + i * 18, this));
//            }
//        }
//        this.addSlot(resultSlot = new ReadOnlySlot(new SimpleContainer(1), 0, 8 + 7 * 18, 18 + 2 * 18));
//        for (i = 0; i < 3; ++i) {
//            for (j = 0; j < 9; ++j) {
//                this.addSlot(new Slot(this.inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
//            }
//        }
//        for (i = 0; i < 9; ++i) {
//            this.addSlot(new Slot(this.inventory, i, 8 + i * 18, 142));
//        }
//        this.updateResult();
//    }
//
//    public @Nullable IFilterBlockEntity getEntity() {
//        return this.machine instanceof IFilterBlockEntity entity ? entity : null;
//    }
//
//    @Override
//    public void removed(@NotNull Player player) {
//        super.removed(player);
//        this.machine.stopOpen(player);
//    }
//
//    @Override
//    public void slotsChanged(@NotNull Container container) {
//        super.slotsChanged(container);
//        if (container == getMachine()) {
//            this.updateResult();
//        }
//    }
//
//    @Override
//    public CraftingContainer getMachine() {
//        return (CraftingContainer) super.getMachine();
//    }
//
//    public void updateResult() {
//        if (!(this.getMachine() instanceof AutoCrafterBlockEntity entity)) return;
//        Level level = getInventory().player.level();
//        AutoCrafterContainer container = new AutoCrafterContainer(NonNullList.of(ItemStack.EMPTY, getMachine().getItems().stream().map(ItemStack::copy).toArray(ItemStack[]::new)));
//        for (int i = 0; i < container.items.size(); i++) {
//            if (!container.getItem(i).isEmpty()) continue;
//            container.setItem(i, entity.getFilter().get(i));
//        }
//        if (container.isEmpty()) {
//            getResultSlot().set(ItemStack.EMPTY);
//            return;
//        }
//        CraftingRecipe recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, container, level).orElse(null);
//        getResultSlot().set(recipe == null ? ItemStack.EMPTY : recipe.assemble(container, level.registryAccess()));
//    }
//
//    private static class Server extends AutoCrafterMenu implements IFilterMenuSever {
//        public Server(int containerId, @NotNull Inventory inventory, AutoCrafterBlockEntity blockEntity) {
//            super(containerId, inventory, blockEntity);
//        }
//        @Override
//        public AutoCrafterBlockEntity getBlockEntity() {
//            return super.getBlockEntity();
//        }
//        @Override
//        public AbstractContainerMenu getMenu() {
//            return this;
//        }
//        @Override
//        public ServerPlayer getPlayer() {
//            return (ServerPlayer) getInventory().player;
//        }
//        @Override
//        public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player){
//            IFilterMenuSever.super.clicked(slotId, button, clickType, player);
//            super.clicked(slotId, button, clickType, player);
//        }
//        @Override
//        public void setRecord(boolean record){
//            super.setRecord(record);
//            IFilterMenuSever.super.setRecord(record);
//        }
//    }
//
//    private static class Client extends AutoCrafterMenu {
//        public Client(MenuType<AutoCrafterMenu> type, int containerId, Inventory inventory) {
//            super(type, containerId, inventory, new AutoCrafterContainer(NonNullList.withSize(9, ItemStack.EMPTY)));
//        }
//    }
//
//    public AutoCrafterBlockEntity getBlockEntity() {
//        return (AutoCrafterBlockEntity) getMachine();
//    }
}
