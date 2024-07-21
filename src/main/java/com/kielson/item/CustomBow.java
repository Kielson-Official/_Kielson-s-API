package com.kielson.item;

import com.kielson.util.CustomRangedWeapon;
import com.kielson.util.RangedConfig;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;

import java.util.HashSet;
import java.util.function.Supplier;

public class CustomBow extends BowItem {
    // Instances are kept a list of, so model predicates can be automatically registered
    public final static HashSet<CustomBow> instances = new HashSet<>();
    public CustomBow(Settings settings, Supplier<Ingredient> repairIngredientSupplier) {
        super(settings);
        this.repairIngredientSupplier = repairIngredientSupplier;
        instances.add(this);
    }

    private final Supplier<Ingredient> repairIngredientSupplier;

    @Deprecated
    public void config(RangedConfig config) {
        ((CustomRangedWeapon) this).configure(config);
    }
    public void configure(RangedConfig config) {
        ((CustomRangedWeapon) this).setRangedWeaponConfig(config);
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return this.repairIngredientSupplier.get().test(ingredient) || super.canRepair(stack, ingredient);
    }
}
