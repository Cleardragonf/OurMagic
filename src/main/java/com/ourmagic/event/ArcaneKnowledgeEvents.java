package com.ourmagic.event;

import com.ourmagic.OurMagic;
import com.ourmagic.magic.ArcaneKnowledgeBook;
import com.ourmagic.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public final class ArcaneKnowledgeEvents {
    private static final float GENERAL_DROP_CHANCE = 0.05F;
    private static final float MAGIC_USER_DROP_CHANCE = 0.50F;

    private ArcaneKnowledgeEvents() {
    }

    @SubscribeEvent
    public static void livingDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide || event.getSource().getEntity() == null) {
            return;
        }

        boolean magicUser = isMagicUser(event.getEntity().getType());
        float chance = magicUser ? MAGIC_USER_DROP_CHANCE : GENERAL_DROP_CHANCE;
        if (event.getEntity().getRandom().nextFloat() > chance) {
            return;
        }

        ItemStack book = ArcaneKnowledgeBook.create(event.getEntity().getRandom(), magicUser);
        event.getDrops().add(new ItemEntity(event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), book));
    }

    private static boolean isMagicUser(EntityType<?> type) {
        return type == EntityType.WITCH
                || type == EntityType.EVOKER
                || type == EntityType.ILLUSIONER
                || type == EntityType.VEX
                || type == ModEntities.WARLOCK.get();
    }
}
