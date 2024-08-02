package net.povstalec.spacetravel.common.space.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;
import net.povstalec.spacetravel.SpaceTravel;
import net.povstalec.spacetravel.common.util.AxisRotation;
import net.povstalec.spacetravel.common.util.SpaceCoords;
import net.povstalec.spacetravel.common.util.TextureLayer;

public class SpaceObject implements INBTSerializable<CompoundTag>
{
	public static final float DEFAULT_DISTANCE = 100.0F;
	
	public static final String PARENT_NAME = "parent_name";
	
	public static final String SPACE_OBJECT_CHILDREN = "space_object_children";
	//TODO Other children types
	
	public static final String COORDS = "coords";
	public static final String AXIS_ROTATION = "axis_rotation";
	
	public static final String TEXTURE_LAYERS = "texture_layers";

	public static final String NAME = "name";
	
	public static final ResourceLocation SPACE_OBJECT_LOCATION = new ResourceLocation(SpaceTravel.MODID, "space_object");
	public static final ResourceKey<Registry<SpaceObject>> REGISTRY_KEY = ResourceKey.createRegistryKey(SPACE_OBJECT_LOCATION);
	public static final Codec<ResourceKey<SpaceObject>> RESOURCE_KEY_CODEC = ResourceKey.codec(REGISTRY_KEY);
	
	@Nullable
	protected String parentName;
	@Nullable
	protected SpaceObject parent;
	
	protected ArrayList<SpaceObject> children = new ArrayList<SpaceObject>();
	
	protected SpaceCoords coords; // Absolute coordinates of the center (not necessarily the object itself, since it can be orbiting some other object for example)
	protected AxisRotation axisRotation;
	
	private ArrayList<TextureLayer> textureLayers;
	
	public String name;
	
	public SpaceObject(){}
	
	public SpaceObject(Optional<String> parentName, SpaceCoords coords, AxisRotation axisRotation, List<TextureLayer> textureLayers)
	{
		if(parentName.isPresent())
				this.parentName = parentName.get();
		
		this.coords = coords;
		this.axisRotation = axisRotation;
		
		this.textureLayers = new ArrayList<TextureLayer>(textureLayers);
	}
	
	public SpaceCoords getSpaceCoords()
	{
		return this.coords;
	}
	
	public Vector3f getPosition(long ticks)
	{
		return new Vector3f();
	}
	
	public AxisRotation getAxisRotation()
	{
		return axisRotation;
	}
	
	public ArrayList<TextureLayer> getTextureLayers()
	{
		return textureLayers;
	}
	
	public Optional<String> getParentName()
	{
		if(parentName != null)
			return Optional.of(parentName);
		
		return Optional.empty();
	}
	
	public float sizeMultiplier(float distance)
	{
		return 1 / distance;
	}
	
	public void addChild(SpaceObject child)
	{
		if(child.parent != null)
		{
			SpaceTravel.LOGGER.error(this.toString() + " already has a parent");
			return;
		}
		
		this.children.add(child);
		child.parent = this;
		child.coords = child.coords.add(this.coords);
		
		child.addCoordsToChildren(this.coords);
	}
	
	private void addCoordsToChildren(SpaceCoords coords)
	{
		for(SpaceObject childOfChild : this.children)
		{
			childOfChild.coords = childOfChild.coords.add(coords);
			childOfChild.addCoordsToChildren(coords);
		}
	}
	
	@Override
	public String toString()
	{
		if(name != null)
			return name.toString();
		
		return this.getClass().toString();
	}
	
	//============================================================================================
	//*************************************Saving and Loading*************************************
	//============================================================================================
	
	/*@Override
	public CompoundTag serializeNBT()
	{
		CompoundTag tag = new CompoundTag();
		tag.putLong(LY, ly);
		tag.putDouble(KM, km);
		
		return tag;
	}
	
	@Override
	public void deserializeNBT(CompoundTag tag)
	{
		ly = tag.getLong(LY);
		km = tag.getDouble(KM);
	}*/
	
	public void writeToBuffer(FriendlyByteBuf buffer)
	{
		buffer.writeOptional(Optional.ofNullable(parentName), (buf, key) -> buf.writeUtf(key));
		coords.writeToBuffer(buffer);
		axisRotation.writeToBuffer(buffer);
		//TODO Write texture layers
	}
	
	public static SpaceObject readFromBuffer(FriendlyByteBuf buffer)
	{
		return new SpaceObject(buffer.readOptional((buf) -> buf.readUtf()), SpaceCoords.readFromBuffer(buffer), AxisRotation.readFromBuffer(buffer), new ArrayList<TextureLayer>()); // TODO Read texture layers
	}

	@Override
	public CompoundTag serializeNBT()
	{
		CompoundTag tag = new CompoundTag();
		
		if(parentName != null)
			tag.putString(PARENT_NAME, parentName);
		
		/*CompoundTag spaceObjectChildren = new CompoundTag();
		//CompoundTag starChildren = new CompoundTag();
		
		for(SpaceObject spaceObject : children)
		{
			//TODO Use instanceof to serialize it under the proper name, so it can be easily deserialized to its correct type
			spaceObjectChildren.put(SPACE_OBJECT_CHILDREN, spaceObject.serializeNBT());
		}*/
		
		tag.put(COORDS, coords.serializeNBT());
		tag.put(AXIS_ROTATION, axisRotation.serializeNBT());
		
		return tag;
	}

	@Override
	public void deserializeNBT(CompoundTag tag)
	{
		if(tag.contains(PARENT_NAME))
			this.parentName = tag.getString(PARENT_NAME);
	
	SpaceCoords coords = new SpaceCoords();
	coords.deserializeNBT(tag.getCompound(COORDS));
	this.coords = coords;
	
	AxisRotation axisRotation = new AxisRotation();
	axisRotation.deserializeNBT(tag.getCompound(AXIS_ROTATION));
	this.axisRotation = axisRotation;
	
	this.textureLayers = new ArrayList<TextureLayer>();
	}
}