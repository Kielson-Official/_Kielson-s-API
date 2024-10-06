package com.kielson.item;

import com.kielson.KielsonsEntityAttributes;
import com.kielson.util.BowInterface;
import com.kielson.util.RangedWeaponHelper;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

import static com.kielson.KielsonsAPI.MOD_ID;

public class CustomBow extends RangedWeaponItem implements BowInterface {
    private final double projectileVelocity;
    private final double pullTime;

    public final static HashSet<CustomBow> instances = new HashSet<>();

    public CustomBow(double rangedDamage, double pullTime, double projectileVelocity, net.minecraft.item.Item.Settings settings) {
        super(settings.attributeModifiers(AttributeModifiersComponent.builder()
                .add(KielsonsEntityAttributes.RANGED_DAMAGE, new EntityAttributeModifier(Identifier.of(MOD_ID, "custom_bow"), rangedDamage, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.HAND)
                .add(KielsonsEntityAttributes.PULL_TIME, new EntityAttributeModifier(Identifier.of(MOD_ID, "custom_bow"), pullTime, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.HAND)
                .build()));

        instances.add(this);
        this.projectileVelocity = projectileVelocity;
        this.pullTime = pullTime;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity playerEntity)) {
            return;
        }
        ItemStack itemStack = playerEntity.getProjectileType(stack);
        if (itemStack.isEmpty()) {
            return;
        }
        int useTicks = getMaxUseTime(stack, user) - remainingUseTicks;
        float f = getPullProgress(useTicks, user, stack);
        if ((double)f < 0.1) {
            return;
        }
        List<ItemStack> list = load(stack, itemStack, playerEntity);
        if (world instanceof ServerWorld serverWorld) {
            if (!list.isEmpty()) {
                float speed = (float) (getPullProgress(useTicks, user, itemStack) * projectileVelocity);
                shootAll(serverWorld, playerEntity, playerEntity.getActiveHand(), stack, list, speed, 1.0f, f == 1.0f, null);
            }
        }
        world.playSound(null, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f / (world.getRandom().nextFloat() * 0.4f + 1.2f) + f * 0.5f);
        playerEntity.incrementStat(Stats.USED.getOrCreateStat(this));
    }

    @Override
    public Predicate<ItemStack> getProjectiles() {
        return BOW_PROJECTILES;
    }

    @Override
    public int getRange() {
        return 15;
    }

    @Override
    protected void shoot(LivingEntity shooter, ProjectileEntity projectile, int index, float speed, float divergence, float yaw, @Nullable LivingEntity target) {
        projectile.setVelocity(shooter, shooter.getPitch(), shooter.getYaw() + yaw, 0.0f, speed, divergence);
        if (projectile instanceof PersistentProjectileEntity persistentProjectile) {
            double damage = shooter.getAttributeValue(KielsonsEntityAttributes.RANGED_DAMAGE) / projectileVelocity;
            ItemStack handStack = shooter.getStackInHand(shooter.getActiveHand());
            if (handStack.getItem() instanceof BowInterface && RangedWeaponHelper.checkEnchantmentLevel(handStack, Enchantments.POWER).isPresent()){
                damage += (int) ((damage * 0.25) * (RangedWeaponHelper.checkEnchantmentLevel(handStack, Enchantments.POWER).get() + 1));
            }
            persistentProjectile.setDamage(damage);
        }
    }

    public static float getPullProgress(int useTicks, LivingEntity user, ItemStack itemStack) {
        float pullTime = (float) user.getAttributeValue(KielsonsEntityAttributes.PULL_TIME);
        if (itemStack.getItem() instanceof BowInterface && RangedWeaponHelper.checkEnchantmentLevel(itemStack, Enchantments.QUICK_CHARGE).isPresent()){
            pullTime -= 0.25f * RangedWeaponHelper.checkEnchantmentLevel(itemStack, Enchantments.QUICK_CHARGE).get();
        }
        pullTime *= 20.0f;
        float f = (float)useTicks / pullTime;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }
        return f;
    }


    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return (int) (72000 * pullTime);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        boolean bl = !user.getProjectileType(itemStack).isEmpty();
        if (!user.isInCreativeMode() && !bl) {
            return TypedActionResult.fail(itemStack);
        } else {
            user.setCurrentHand(hand);
            return TypedActionResult.consume(itemStack);
        }
    }
}
