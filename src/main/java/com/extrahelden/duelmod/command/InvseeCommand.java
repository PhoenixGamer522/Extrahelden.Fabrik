package com.extrahelden.duelmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.item.ItemStack;

/**
 * /invsee <player> - öffnet das Inventar des Zielspielers und erlaubt Bearbeitung (live).
 */
public class InvseeCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("invsee")
                .requires(src -> src.hasPermission(2)) // nur OPs
                .then(Commands.argument("target", StringArgumentType.word())
                        .executes(ctx -> {
                            String targetName = StringArgumentType.getString(ctx, "target");
                            ServerPlayer executor = ctx.getSource().getPlayerOrException();
                            ServerPlayer target = executor.server.getPlayerList().getPlayerByName(targetName);

                            if (target == null) {
                                executor.sendSystemMessage(Component.literal("§cSpieler nicht gefunden!"));
                                return 0;
                            }

                            openInventory(executor, target);
                            return 1;
                        })));
    }

    private static void openInventory(ServerPlayer executor, ServerPlayer target) {
        // MenuProvider mit einer anonymen Implementierung: erzeugt unser InvseeMenu
        executor.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Inventar von " + target.getName().getString());
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
                // Achtung: createMenu wird clientseitig UND serverseitig benutzt; wir übergeben hier das target als ServerPlayer
                return new InvseeMenu(id, playerInventory, target);
            }
        });
    }

    // -------------------------
    // Das Container-Menu
    // -------------------------
    private static class InvseeMenu extends AbstractContainerMenu {
        private final Inventory targetInv;
        private final int targetSize;
        private final ServerPlayer targetPlayer;

        protected InvseeMenu(int id, Inventory playerInv, ServerPlayer target) {
            // MenuType.GENERIC_9x4 passt für 36 Slots (4 Reihen x 9) – wir benutzen genau 36 Slots für das Spieler-Haupt-Inventar
            super(MenuType.GENERIC_9x4, id);

            this.targetPlayer = target;
            this.targetInv = target.getInventory();
            this.targetSize = 36; // wir zeigen 4 Reihen (36 Slots) vom Ziel-Inventar (Hauptinventar inkl. Hotbar)

            // Ziel-Inventar-Slots (oben): 4 Reihen x 9
            int slotIndex = 0;
            for (int row = 0; row < 4; ++row) {
                for (int col = 0; col < 9; ++col) {
                    this.addSlot(new Slot(this.targetInv, slotIndex++, 8 + col * 18, 18 + row * 18));
                }
            }

            // Ausführender Spieler - main inventory (3x9)
            for (int row = 0; row < 3; ++row) {
                for (int col = 0; col < 9; ++col) {
                    this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 86 + row * 18));
                }
            }
            // Ausführender Spieler - hotbar
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col, 8 + col * 18, 144));
            }
        }

        @Override
        public boolean stillValid(Player player) {
            // Menü bleibt offen, auch wenn der Zielspieler weit weg ist.
            return true;
        }

        /**
         * Wichtig: Implementiert quickMoveStack (Shift-Click).
         * Index-Bereiche:
         * 0 .. targetSize-1               = Ziel-Inventar
         * targetSize .. slots.size()-1    = Ausführender Spieler-Inventar
         */
        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            ItemStack result = ItemStack.EMPTY;
            Slot slot = this.slots.get(index);
            if (slot != null && slot.hasItem()) {
                ItemStack slotStack = slot.getItem();
                result = slotStack.copy();

                int totalSlots = this.slots.size();
                int playerStart = this.targetSize;
                int playerEnd = totalSlots;

                if (index < this.targetSize) {
                    // Aus Ziel -> in Spieler-Inventar verschieben
                    if (!this.moveItemStackTo(slotStack, playerStart, playerEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // Aus Spieler -> in Ziel-Inventar verschieben
                    if (!this.moveItemStackTo(slotStack, 0, this.targetSize, false)) {
                        return ItemStack.EMPTY;
                    }
                }

                if (slotStack.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }

                slot.onTake(player, slotStack);
            }
            return result;
        }
    }
}
