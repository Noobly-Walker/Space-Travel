package net.povstalec.spacetravel;

import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.DataPackRegistryEvent;
import net.povstalec.spacetravel.common.config.SpaceTravelConfig;
import net.povstalec.spacetravel.common.init.SpaceObjectRegistry;
import net.povstalec.spacetravel.common.space.STSpaceRegion;
import net.povstalec.spacetravel.common.space.generation.StarFieldTemplate;
import net.povstalec.stellarview.api.client.events.StellarViewReloadEvent;
import net.povstalec.stellarview.api.common.SpaceRegion;
import net.povstalec.stellarview.api.common.space_objects.resourcepack.*;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.povstalec.spacetravel.client.render.level.SpaceTravelDimensionSpecialEffects;
import net.povstalec.spacetravel.common.capabilities.SpaceshipCapability;
import net.povstalec.spacetravel.common.capabilities.SpaceshipCapabilityProvider;
import net.povstalec.spacetravel.common.data.Multiverse;
import net.povstalec.spacetravel.common.init.CommandInit;
import net.povstalec.spacetravel.common.init.PacketHandlerInit;
import net.povstalec.spacetravel.common.init.WorldGenInit;
import net.povstalec.spacetravel.common.packets.ClientBoundRenderCenterUpdatePacket;
import net.povstalec.spacetravel.common.packets.ClientBoundSpaceRegionClearPacket;
import net.povstalec.spacetravel.common.packets.ClientBoundSpaceRegionLoadPacket;
import net.povstalec.spacetravel.common.space.Spaceship;
import net.povstalec.spacetravel.common.space.Universe;

@Mod(SpaceTravel.MODID)
public class SpaceTravel
{
	public static final String MODID = "spacetravel";
	
	public static final Logger LOGGER = LogUtils.getLogger();
	
	public SpaceTravel()
	{
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		
		modEventBus.addListener((DataPackRegistryEvent.NewRegistry event) ->
		{
			event.dataPackRegistry(Universe.REGISTRY_KEY, Universe.CODEC, Universe.CODEC);
			
			event.dataPackRegistry(StarFieldTemplate.REGISTRY_KEY, StarFieldTemplate.CODEC, StarFieldTemplate.CODEC);
			
			event.dataPackRegistry(SpaceObjectRegistry.PLANET_REGISTRY_KEY, Planet.CODEC, Planet.CODEC);
			event.dataPackRegistry(SpaceObjectRegistry.MOON_REGISTRY_KEY, Moon.CODEC, Moon.CODEC);
			event.dataPackRegistry(SpaceObjectRegistry.STAR_REGISTRY_KEY, Star.CODEC, Star.CODEC);
			event.dataPackRegistry(SpaceObjectRegistry.BLACK_HOLE_REGISTRY_KEY, BlackHole.CODEC, BlackHole.CODEC);
			event.dataPackRegistry(SpaceObjectRegistry.NEBULA_REGISTRY_KEY, Nebula.CODEC, Nebula.CODEC);
			event.dataPackRegistry(SpaceObjectRegistry.STAR_FIELD_REGISTRY_KEY, StarField.CODEC, StarField.CODEC);
		});
		
		modEventBus.addListener(this::commonSetup);
		
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SpaceTravelConfig.CLIENT_CONFIG, MODID + "-client.toml");
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SpaceTravelConfig.COMMON_CONFIG, MODID + "-common.toml");
		
        WorldGenInit.register(modEventBus);
		
		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.addListener(CommandInit::registerCommands);
	}
	
	private void commonSetup(final FMLCommonSetupEvent event)
	{
		event.enqueueWork(() -> 
    	{
            PacketHandlerInit.register();
			
			SpaceObjectRegistry.register(new ResourceLocation(SpaceTravel.MODID, "planet"), Planet.class, Planet::new);
			SpaceObjectRegistry.register(new ResourceLocation(SpaceTravel.MODID, "moon"), Moon.class, Moon::new);
			SpaceObjectRegistry.register(new ResourceLocation(SpaceTravel.MODID, "star"), Star.class, Star::new);
			SpaceObjectRegistry.register(new ResourceLocation(SpaceTravel.MODID, "black_hole"), BlackHole.class, BlackHole::new);
			SpaceObjectRegistry.register(new ResourceLocation(SpaceTravel.MODID, "nebula"), Nebula.class, Nebula::new);
			SpaceObjectRegistry.register(new ResourceLocation(SpaceTravel.MODID, "star_field"), StarField.class, StarField::new);
    	});
	}
	
	// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
	@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static class ClientModEvents
	{
		@SubscribeEvent
		public static void registerDimensionalEffects(RegisterDimensionSpecialEffectsEvent event)
		{
			SpaceTravelDimensionSpecialEffects.register(event);
		}
	}
	
	@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
	public static class ClientForgeEvents
	{
		@SubscribeEvent
		public static void stellarViewReload(StellarViewReloadEvent event)
		{
			event.setCanceled(true);
		}
	}
	
	public static void updatePlayerRenderer(Level level, ServerPlayer player)
	{
		LazyOptional<SpaceshipCapability> spaceshipCapability = level.getCapability(SpaceshipCapabilityProvider.SPACESHIP);
		
		spaceshipCapability.ifPresent(cap -> 
		{
			if(cap != null)
			{
				Optional<Universe> universe = Multiverse.get(level).getUniverse(Multiverse.PRIME_UNIVERSE); //TODO There can be other universes
				
				if(universe.isPresent())
				{
					PacketHandlerInit.sendToPlayer(player, new ClientBoundRenderCenterUpdatePacket(new Spaceship())); //TODO Get coords from somewhere
					PacketHandlerInit.sendToPlayer(player, new ClientBoundSpaceRegionClearPacket());
					for(Map.Entry<SpaceRegion.RegionPos, STSpaceRegion> spaceRegionEntry : universe.get().getRegionsAt(new SpaceRegion.RegionPos(cap.spaceship.getSpaceCoords()), STSpaceRegion.SPACE_REGION_LOAD_DISTANCE, true).entrySet())
					{
						PacketHandlerInit.sendToPlayer(player, new ClientBoundSpaceRegionLoadPacket(spaceRegionEntry.getValue()));
					}
				}
			}
		});
	}
}
