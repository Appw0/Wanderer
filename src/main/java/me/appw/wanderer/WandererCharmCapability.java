package me.appw.wanderer;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import static net.minecraftforge.common.util.Constants.NBT.*;

import net.minecraftforge.common.world.ForgeWorldType;
import top.theillusivec4.curios.api.type.capability.ICurio;

import javax.annotation.Nonnull;

public class WandererCharmCapability implements ICurio {
    ItemStack stack;

    public WandererCharmCapability(ItemStack stack) {
        this.stack = stack;
        CompoundNBT stackNBT = stack.getOrCreateTag();
        if (!stackNBT.contains("StoredSpawns", TAG_LIST)) {
            stackNBT.put("StoredSpawns", new ListNBT());
        }
    }

    @Override
    @Nonnull
    public DropRule getDropRule(LivingEntity livingEntity) {
        return DropRule.ALWAYS_KEEP;
    }

    @Override
    public void curioTick(String identifier, int index, LivingEntity livingEntity) {
        if (!livingEntity.level.isClientSide && livingEntity.tickCount % 1200 == 0) {
            if (livingEntity instanceof ServerPlayerEntity) {
                ServerPlayerEntity player = (ServerPlayerEntity) livingEntity;
                CompoundNBT stackNBT = stack.getOrCreateTag();
                ListNBT spawns = stackNBT.getList("StoredSpawns", 9);
                Wanderer.LOGGER.debug(spawns);
                ListNBT pos = spawns.getList(spawns.size() - 1);
                BlockPos spawnPos = player.blockPosition();
                double dist = spawnPos.distSqr(pos.getDouble(0), pos.getDouble(1), pos.getDouble(2), true);
                if (dist > Math.pow(300, 2)) {
                    ListNBT newPos = new ListNBT();
                    while (!player.level.getBlockState(spawnPos.below()).isCollisionShapeFullBlock(player.level, spawnPos.below())
                    && !player.level.isWaterAt(spawnPos.below())) {
                        if (spawnPos.getY() == 1) {
                            spawnPos = player.blockPosition();
                        } else {
                            spawnPos = spawnPos.below();
                        }
                    }
                    newPos.add(DoubleNBT.valueOf(spawnPos.getX()));
                    newPos.add(DoubleNBT.valueOf(spawnPos.getY()));
                    newPos.add(DoubleNBT.valueOf(spawnPos.getZ()));
                    newPos.add(StringNBT.valueOf(player.level.dimension().getRegistryName().toString()));
                    spawns.add(newPos);
                    stackNBT.putBoolean("RecentDeath", false);
                    if (spawns.size() > 8) {
                        spawns.remove(0);
                    }
                }
            }
        }
    }
}
