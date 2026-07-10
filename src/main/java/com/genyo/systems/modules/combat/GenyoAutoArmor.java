package com.genyo.systems.modules.combat;

import com.genyo.Genyo;
import com.genyo.events.network.PlayerTickEvent;
import com.genyo.managers.Managers;
import com.genyo.systems.modules.GenyoModule;
import com.genyo.systems.settings.FloatSetting;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;

import java.util.PriorityQueue;
import java.util.Queue;

public class GenyoAutoArmor extends GenyoModule {

    public GenyoAutoArmor() {
        super(Genyo.COMBAT, "genyo-auto-armor", "Automatically replaces armor pieces.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Priority> priorityConfig = sgGeneral.add(new EnumSetting.Builder<Priority>()
        .name("Priority")
        .description("Armor enchantment priority")
        .defaultValue(Priority.BLAST_PROTECTION)
        .build()
    );

    private final Setting<Double> minDurabilityConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("Min Durability")
        .description("Durability percent to replace armor")
        .min(0.0d)
        .defaultValue(0.0d)
        .max(20.0d)
        .build()
    );

    private final Setting<Boolean> elytraPriorityConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Elytra Priority")
        .description("Prioritizes existing elytras in the chestplate armor slot")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> blastLeggingsConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Leggings - Blast Priority")
        .description("Prioritizes Blast Protection leggings")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> noBindingConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("No Binding")
        .description("Avoids armor with the Curse of Binding enchantment")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> inventoryConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Allow Inventory")
        .description("Allows armor to be swapped while in the inventory menu")
        .defaultValue(false)
        .build()
    );

    //
    private final Queue<ArmorSlot> helmet = new PriorityQueue<>();
    private final Queue<ArmorSlot> chestplate = new PriorityQueue<>();
    private final Queue<ArmorSlot> leggings = new PriorityQueue<>();
    private final Queue<ArmorSlot> boots = new PriorityQueue<>();

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    };

    @EventHandler
    public void onTick(PlayerTickEvent event)
    {
        if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen && inventoryConfig.get()))
        {
            return;
        }
        //
        helmet.clear();
        chestplate.clear();
        leggings.clear();
        boots.clear();
        for (int j = 0; j < 36; j++)
        {
            ItemStack stack = mc.player.getInventory().getStack(j);
            if (stack.isEmpty() || !isArmor(stack))
            {
                continue;
            }
            if (noBindingConfig.get() && hasEnchantment(stack, Enchantments.BINDING_CURSE))
            {
                continue;
            }
            int index = stack.get(DataComponentTypes.EQUIPPABLE).slot().getEntitySlotId();
            float dura = (stack.getMaxDamage() - stack.getDamage()) / (float) stack.getMaxDamage();
            if (dura < minDurabilityConfig.get())
            {
                continue;
            }
            ArmorSlot data = new ArmorSlot(index, j, stack);
            switch (index)
            {
                case 0 -> helmet.add(data);
                case 1 -> chestplate.add(data);
                case 2 -> leggings.add(data);
                case 3 -> boots.add(data);
            }
        }
        for (int i = 0; i < 4; i++)
        {
            ItemStack armorStack = mc.player.getEquippedStack(ARMOR_SLOTS[i]);
            if (elytraPriorityConfig.get() && armorStack.getItem() == Items.ELYTRA)
            {
                continue;
            }
            float armorDura = (armorStack.getMaxDamage() - armorStack.getDamage()) / (float) armorStack.getMaxDamage();
            if (!armorStack.isEmpty() || armorDura >= minDurabilityConfig.get())
            {
                continue;
            }
            switch (i)
            {
                case 0 ->
                {
                    if (!helmet.isEmpty())
                    {
                        ArmorSlot helmetSlot = helmet.poll();
                        swapArmor(helmetSlot.getType(), EquipmentSlot.HEAD, helmetSlot.getSlot());
                    }
                }
                case 1 ->
                {
                    if (!chestplate.isEmpty())
                    {
                        ArmorSlot chestSlot = chestplate.poll();
                        swapArmor(chestSlot.getType(), EquipmentSlot.CHEST, chestSlot.getSlot());
                    }
                }
                case 2 ->
                {
                    if (!leggings.isEmpty())
                    {
                        ArmorSlot leggingsSlot = leggings.poll();
                        swapArmor(leggingsSlot.getType(), EquipmentSlot.LEGS, leggingsSlot.getSlot());
                    }
                }
                case 3 ->
                {
                    if (!boots.isEmpty())
                    {
                        ArmorSlot bootsSlot = boots.poll();
                        swapArmor(bootsSlot.getType(), EquipmentSlot.FEET, bootsSlot.getSlot());
                    }
                }
            }
        }
    }

    public void swapArmor(int armorSlot, EquipmentSlot equipmentSlot, int slot)
    {
        ItemStack stack = mc.player.getEquippedStack(equipmentSlot);
        //
        armorSlot = 8 - armorSlot;
        Managers.INVENTORY.pickupSlot(slot < 9 ? slot + 36 : slot);
        boolean rt = !stack.isEmpty();
        Managers.INVENTORY.pickupSlot(armorSlot);
        if (rt)
        {
            Managers.INVENTORY.pickupSlot(slot < 9 ? slot + 36 : slot);
        }
    }

    public enum Priority
    {
        BLAST_PROTECTION(Enchantments.BLAST_PROTECTION),
        PROTECTION(Enchantments.PROTECTION),
        PROJECTILE_PROTECTION(Enchantments.PROJECTILE_PROTECTION);

        //
        private final RegistryKey<Enchantment> enchant;

        Priority(RegistryKey<Enchantment> enchant)
        {
            this.enchant = enchant;
        }

        public RegistryKey<Enchantment> getEnchantment()
        {
            return enchant;
        }
    }

    public boolean hasEnchantment(ItemStack armorStack, RegistryKey<Enchantment> enchantment)
    {
        if (armorStack.getComponents().contains(DataComponentTypes.ENCHANTMENTS))
        {
            for (RegistryEntry<Enchantment> entry : armorStack.getComponents()
                .get(DataComponentTypes.ENCHANTMENTS).getEnchantments())
            {
                if (entry.getKey().isPresent() && entry.getKey().get().equals(enchantment))
                {
                    return true;
                }
            }
        }
        return false;
    }

    //
    public class ArmorSlot implements Comparable<ArmorSlot>
    {
        //
        private final int armorType;
        private final int slot;
        private final ItemStack armorStack;

        public ArmorSlot(int armorType, int slot, ItemStack armorStack)
        {
            this.armorType = armorType;
            this.slot = slot;
            this.armorStack = armorStack;
        }

        @Override
        public int compareTo(ArmorSlot other)
        {
            if (armorType != other.armorType)
            {
                return 0;
            }
            final ItemStack otherStack = other.getArmorStack();

            int armorDura = armorStack.getMaxDamage() - armorStack.getDamage();
            int otherDura = other.armorStack.getMaxDamage() - other.armorStack.getDamage();
            int durabilityDiff = armorDura - otherDura;

            if (durabilityDiff != 0)
            {
                return durabilityDiff;
            }
            RegistryKey<Enchantment> enchantment = priorityConfig.get().getEnchantment();
            if (blastLeggingsConfig.get() && armorType == 2
                && hasEnchantment(armorStack, Enchantments.BLAST_PROTECTION))
            {
                return -1;
            }
            if (hasEnchantment(armorStack, enchantment))
            {
                return hasEnchantment(otherStack, enchantment) ? 0 : -1;
            }
            else
            {
                return hasEnchantment(otherStack, enchantment) ? 1 : 0;
            }
        }

        public ItemStack getArmorStack()
        {
            return armorStack;
        }

        public int getType()
        {
            return armorType;
        }

        public int getSlot()
        {
            return slot;
        }
    }

    private boolean isArmor(ItemStack itemStack) {
        return itemStack.isIn(ItemTags.FOOT_ARMOR) || itemStack.isIn(ItemTags.LEG_ARMOR) || itemStack.isIn(ItemTags.CHEST_ARMOR) || itemStack.isIn(ItemTags.HEAD_ARMOR);
    }

}
