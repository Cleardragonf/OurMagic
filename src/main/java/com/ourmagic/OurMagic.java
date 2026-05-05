package com.ourmagic;

import com.mojang.logging.LogUtils;
import com.ourmagic.command.OurMagicCommands;
import com.ourmagic.network.ModNetwork;
import com.ourmagic.recipe.ModRecipeSerializers;
import com.ourmagic.registry.ModBlocks;
import com.ourmagic.registry.ModCreativeTabs;
import com.ourmagic.registry.ModItems;
import com.ourmagic.registry.ModMenus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(OurMagic.MOD_ID)
public class OurMagic {
    public static final String MOD_ID = "ourmagic";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OurMagic() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModCreativeTabs.register(modBus);
        ModMenus.register(modBus);
        ModRecipeSerializers.register(modBus);
        ModNetwork.register();

        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        OurMagicCommands.register(event.getDispatcher());
    }
}
