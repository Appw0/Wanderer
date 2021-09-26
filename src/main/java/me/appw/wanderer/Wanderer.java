package me.appw.wanderer;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.jmx.Server;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotTypeMessage;

import java.util.Optional;
import java.util.stream.Collectors;

@Mod("wanderer")
public class Wanderer {

    public static final String MODID = "wanderer";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<Item> WANDERER_CHARM = ITEMS.register("wanderer_charm", WandererCharmItem::new);

    public static final Logger LOGGER = LogManager.getLogger();

    public Wanderer() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
        MinecraftForge.EVENT_BUS.register(this);
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    private void setup(final FMLCommonSetupEvent event) {
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {
        InterModComms.sendTo("curios", SlotTypeMessage.REGISTER_TYPE, () -> new SlotTypeMessage.Builder("curio").size(1).build());
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {
            if (!event.getPlayer().level.isClientSide) {
                ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
                Optional<ImmutableTriple<String, Integer, ItemStack>> curioOpt = CuriosApi.getCuriosHelper().findEquippedCurio(WANDERER_CHARM.get(), player);
                if (curioOpt.isPresent()) {
                    ImmutableTriple<String, Integer, ItemStack> curio = curioOpt.get();
                    CompoundNBT nbt = curio.right.getOrCreateTag();
                    nbt.putBoolean("RecentDeath", true);

                    ListNBT spawns = nbt.getList("StoredSpawns", 9);
                    LOGGER.debug(spawns);
                    if (spawns.size() > 0) {
                        ListNBT NBTpos = spawns.getList(spawns.size() - 1);
                        BlockPos pos = new BlockPos(NBTpos.getDouble(0), NBTpos.getDouble(1), NBTpos.getDouble(2));
                        LOGGER.debug(pos);
                        player.teleportTo(pos.getX(), pos.getY(), pos.getZ());
                        curio.right.hurtAndBreak(1, player, p -> CuriosApi.getCuriosHelper().onBrokenCurio(curio.left, curio.middle, p));
                    }
                }
            }
        }
        @SubscribeEvent
        public static void playerDeath(LivingDeathEvent event) {
            if (!event.getEntity().level.isClientSide) {
                if (event.getEntity() instanceof ServerPlayerEntity) {
                    ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();
                    Optional<ImmutableTriple<String, Integer, ItemStack>> curio = CuriosApi.getCuriosHelper().findEquippedCurio(WANDERER_CHARM.get(), player);
                    if (curio.isPresent()) {
                        CompoundNBT nbt = curio.get().right.getOrCreateTag();
                        ListNBT spawns = nbt.getList("StoredSpawns", 9);
                        if (nbt.getBoolean("RecentDeath")) {
                            spawns.remove(spawns.size() - 1);
                        }

//                        if (spawns.size() > 0) {
//                            ListNBT NBTpos = spawns.getList(spawns.size() - 1);
//                            BlockPos pos = new BlockPos(NBTpos.getDouble(0), NBTpos.getDouble(1), NBTpos.getDouble(2));
//                            player.setRespawnPosition(player.level.dimension(), pos, player.getRandom().nextFloat(), true, false);
//                        }
                    }
                }
            }
        }
    }
}
