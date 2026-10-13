package com.murimblock;

import com.murimblock.combat.CombatAttachments;
import com.murimblock.combat.CombatEvents;
import com.murimblock.combat.knockdown.KnockdownAttachments;
import com.murimblock.combat.knockdown.KnockdownEvents;
import com.murimblock.command.MurimblockCommands;
import com.murimblock.cultivation.CultivationAttachments;
import com.murimblock.cultivation.CultivationEvents;
import com.murimblock.integration.epicfight.ArmoredCrabEpicFight;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightContentRegistries;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimSkills;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.MartialService;
import com.murimblock.martial.SectLibrary;
import com.murimblock.item.SectTokenItem;
import com.murimblock.mob.MurimEntities;
import com.murimblock.network.MurimblockNetworking;
import com.murimblock.qi.QiAttachments;
import com.murimblock.qi.QiEvents;
import com.murimblock.qi.charge.QiChargeEvents;
import com.murimblock.world.MurimWorldgen;
import com.murimblock.world.block.MurimBlocks;
import com.murimblock.world.structure.SanctuaryPlacement;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Entry point for Murimblock.
 */
@Mod(Murimblock.MOD_ID)
public final class Murimblock {
    public static final String MOD_ID = "murimblock";

    public Murimblock(IEventBus modEventBus) {
        MurimEntities.register(modEventBus);
        // Blocks first: they add their block items to MurimItems before its registry is attached.
        MurimBlocks.register(modEventBus);
        MurimItems.register(modEventBus);
        MurimWorldgen.register(modEventBus);
        ArmoredCrabEpicFight.register(modEventBus);
        EpicFightContentRegistries.register(modEventBus);
        MurimSkills.register(modEventBus);
        CombatAttachments.register(modEventBus);
        CultivationAttachments.register(modEventBus);
        QiAttachments.register(modEventBus);
        KnockdownAttachments.register(modEventBus);
        MartialService.register(modEventBus);
        MurimAnimations.registerMotions();
        modEventBus.addListener(MurimblockNetworking::onRegisterPayloadHandlers);
        EpicFightBridge.registerHooks();
        NeoForge.EVENT_BUS.addListener(EpicFightBridge::onPlayerTick);

        NeoForge.EVENT_BUS.addListener(CombatEvents::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(CombatEvents::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(CombatEvents::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(CultivationEvents::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(QiEvents::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(QiEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(QiEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, QiEvents::onPlayerDeath);
        NeoForge.EVENT_BUS.addListener(QiEvents::onServerStarted);
        NeoForge.EVENT_BUS.addListener(QiEvents::onServerStopped);
        NeoForge.EVENT_BUS.addListener(QiEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(QiChargeEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(QiChargeEvents::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(QiChargeEvents::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(QiChargeEvents::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(MurimblockCommands::onRegisterCommands);
        // The Cloud Cliff Sanctuary: built once near the spawn; every player gets the sect token that finds it.
        NeoForge.EVENT_BUS.addListener(SanctuaryPlacement::onServerStarted);
        NeoForge.EVENT_BUS.addListener(SanctuaryPlacement::onServerTick);
        NeoForge.EVENT_BUS.addListener(SanctuaryPlacement::onServerStopping);
        NeoForge.EVENT_BUS.addListener(SectTokenItem::onPlayerLoggedIn);
        // Knock-down in PvP: K.O. instead of death, carry (V) or execute (B).
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, KnockdownEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, KnockdownEvents::onDeath);
        NeoForge.EVENT_BUS.addListener(KnockdownEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(KnockdownEvents::onDamageTaken);
        NeoForge.EVENT_BUS.addListener(KnockdownEvents::onAttack);
        NeoForge.EVENT_BUS.addListener(KnockdownEvents::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(KnockdownEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(KnockdownEvents::onInteractEntity);
        NeoForge.EVENT_BUS.addListener(KnockdownEvents::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(KnockdownEvents::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(KnockdownEvents::onMount);
        NeoForge.EVENT_BUS.addListener(KnockdownEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(KnockdownEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(KnockdownEvents::onLoggedOut);
        // Martial arts: the sect library hands out the manual; cooldowns are forgotten on logout.
        NeoForge.EVENT_BUS.addListener(SectLibrary::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) MartialService.forget(player);
        });
    }
}
