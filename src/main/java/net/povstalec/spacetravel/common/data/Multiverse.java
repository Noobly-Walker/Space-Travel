package net.povstalec.spacetravel.common.data;

import java.util.*;

import javax.annotation.Nonnull;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.povstalec.spacetravel.SpaceTravel;
import net.povstalec.spacetravel.common.space.Universe;
import net.povstalec.spacetravel.common.space.objects.Star;
import net.povstalec.spacetravel.common.space.objects.StarField;

public class Multiverse extends SavedData
{
	private static final String FILE_NAME = SpaceTravel.MODID + "-multiverse";

	private static final String UNIVERSES = "universes";

	private HashMap<String, Universe> universes = new HashMap<String, Universe>(); // TODO Maybe change it from String to something better?
	
	private MinecraftServer server;
	
	/*public final void updateData(MinecraftServer server)
	{
		eraseData(server); //TODO Does this really need any erasing?
		
		registerSpaceObjectFromDataPacks(server);
	}

	public void eraseData(MinecraftServer server)
	{
		this.spaceObjects.clear();
		this.setDirty();
	}*/
	
	public void setupUniverse()
	{
		if(universes.containsKey("main"))
			SpaceTravel.LOGGER.error("Already contains main");
		else
		{
			universes.put("main", new Universe());
			SpaceTravel.LOGGER.info("Created new main");
		}
		
		registerStarFields(server);
		registerStars(server);
		
		this.setDirty();
	}
	
	public Optional<Universe> getUniverse(String name)
	{
		if(!universes.containsKey(name))
			return Optional.empty();
		
		return Optional.of(universes.get(name));
	}

	//============================================================================================
	//*********************************Registering Space Objects**********************************
	//============================================================================================
	
	public void registerStarFields(MinecraftServer server)
	{
		final RegistryAccess registries = server.registryAccess();
		final Registry<StarField> starFieldRegistry = registries.registryOrThrow(StarField.REGISTRY_KEY);
		
		Set<Map.Entry<ResourceKey<StarField>, StarField>> starFieldSet = starFieldRegistry.entrySet();
		starFieldSet.forEach((starFieldEntry) ->
		{
			StarField starField = starFieldEntry.getValue();
			starField.setResourceLocation(starFieldEntry.getKey().location());
			
			System.out.println("Loading Star Field " + starField.toString()); //TODO Remove
			
			Optional<Universe> universe = getUniverse("main");
			if(universe.isPresent())
				universe.get().addToRegion(starField, true); //TODO Maybe let datapacks decide?
		});
		SpaceTravel.LOGGER.info("Star Fields registered");
	}
	
	public void registerStars(MinecraftServer server)
	{
		final RegistryAccess registries = server.registryAccess();
		final Registry<Star> starRegistry = registries.registryOrThrow(Star.REGISTRY_KEY);
		
		Set<Map.Entry<ResourceKey<Star>, Star>> starSet = starRegistry.entrySet();
		starSet.forEach((starEntry) ->
		{
			Star star = starEntry.getValue();
			star.setResourceLocation(starEntry.getKey().location());
			
			System.out.println("Loading Star " + star.toString()); //TODO Remove
			
			Optional<Universe> universe = getUniverse("main");
			if(universe.isPresent())
				universe.get().addToRegion(star, true); //TODO Maybe let datapacks decide?
		});
		SpaceTravel.LOGGER.info("Star Fields registered");
	}

	//============================================================================================
	//*************************************Saving and Loading*************************************
	//============================================================================================

	private CompoundTag serialize()
	{
		CompoundTag tag = new CompoundTag();
		
		tag.put(UNIVERSES, serializeUniverses());
		
		return tag;
	}

	private CompoundTag serializeUniverses()
	{
		CompoundTag tag = new CompoundTag();
		
		for(Map.Entry<String, Universe> universeEntry : universes.entrySet())
		{
			tag.put(universeEntry.getKey().toString(), universeEntry.getValue().serializeNBT());
		}
		
		return tag;
	}
	
	private void deserialize(CompoundTag tag)
	{
		deserializeUniverses(tag.getCompound(UNIVERSES));
	}

	private void deserializeUniverses(CompoundTag tag)
	{
		SpaceTravel.LOGGER.info("Deserializing Universes");
		
		for(String name : tag.getAllKeys())
		{
			Universe universe = new Universe();
			universe.deserializeNBT(tag.getCompound(name));
			
			universes.put(name, universe);
			
			SpaceTravel.LOGGER.info("Deserialized " + name);
		}

	}
	
	//============================================================================================
	//********************************************Data********************************************
	//============================================================================================

	public Multiverse(MinecraftServer server)
	{
		this.server = server;
	}
	
	public static Multiverse create(MinecraftServer server)
	{
		return new Multiverse(server);
	}
	
	public static Multiverse load(MinecraftServer server, CompoundTag tag)
	{
		Multiverse data = create(server);

		data.server = server;
		data.deserialize(tag);
		
		return data;
	}
	
	public CompoundTag save(CompoundTag tag)
	{
		tag = serialize();
		
		return tag;
	}
	
	@Nonnull
	public static Multiverse get(Level level)
	{
		if(level.isClientSide())
			throw new RuntimeException("Don't access this client-side!");
		
		return Multiverse.get(level.getServer());
	}
	
	@Nonnull
	public static Multiverse get(MinecraftServer server)
	{
		DimensionDataStorage storage = server.overworld().getDataStorage();
		
		return storage.computeIfAbsent((tag) -> load(server, tag), () -> create(server), FILE_NAME);
	}
}
