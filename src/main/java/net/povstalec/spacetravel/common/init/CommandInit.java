package net.povstalec.spacetravel.common.init;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.povstalec.spacetravel.SpaceTravel;
import net.povstalec.spacetravel.common.capabilities.SpaceshipCapability;
import net.povstalec.spacetravel.common.capabilities.SpaceshipCapabilityProvider;
import net.povstalec.spacetravel.common.config.SpaceRegionCommonConfig;
import net.povstalec.spacetravel.common.util.DimensionUtil;

public class CommandInit
{
	public static void registerCommands(RegisterCommandsEvent event)
	{
		register(event.getDispatcher());
	}
	
	private static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		//Dev commands
		dispatcher.register(Commands.literal(SpaceTravel.MODID)
				.then(Commands.literal("dimension")
					.then(Commands.literal("create").executes(CommandInit::createRandomDimension)
							.then(Commands.argument("name", StringArgumentType.word()).executes(CommandInit::createNamedDimension))))
				.requires(commandSourceStack -> commandSourceStack.hasPermission(2)));
		
		dispatcher.register(Commands.literal(SpaceTravel.MODID)
				.then(Commands.literal("spaceship")
					.then(Commands.literal("speed")
							.then(Commands.argument("x_axis", IntegerArgumentType.integer())
									.then(Commands.argument("y_axis", IntegerArgumentType.integer())
											.then(Commands.argument("z_axis", IntegerArgumentType.integer()).executes(CommandInit::setSpaceshipSpeed))))
							.requires(commandSourceStack -> commandSourceStack.hasPermission(2)))
					.then(Commands.literal("rotate")
							.then(Commands.argument("x_axis", DoubleArgumentType.doubleArg())
									.then(Commands.argument("y_axis", DoubleArgumentType.doubleArg())
											.then(Commands.argument("z_axis", DoubleArgumentType.doubleArg()).executes(CommandInit::rotateSpaceship)
													.requires(commandSourceStack -> commandSourceStack.hasPermission(2))))))));
		
		dispatcher.register(Commands.literal(SpaceTravel.MODID)
				.then(Commands.literal("spaceship")
						.then(Commands.literal("pos")
								.then(Commands.literal("get").executes(CommandInit::getSpaceshipPos)))));
		
		// Client commands
		dispatcher.register(Commands.literal(SpaceTravel.MODID)
				.then(Commands.literal("render")
					.then(Commands.literal("reload").executes(CommandInit::reloadRenderer))));
	}
	
	private static int createRandomDimension(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		MinecraftServer server = context.getSource().getServer();
		
		DimensionUtil.createSpaceship(server);
		context.getSource().sendSuccess(() -> Component.literal("Created a new Dimension"), false); //TODO Translation
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int createNamedDimension(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		MinecraftServer server = context.getSource().getServer();
		String name = StringArgumentType.getString(context, "name");
		
		if(ResourceLocation.isValidPath(name)) // TODO Check for existing dimensions
		{
			DimensionUtil.createSpaceship(server, name);
			context.getSource().sendSuccess(() -> Component.literal("Created a new Dimension"), false); //TODO Translation
		}
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int getSpaceshipPos(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		ServerLevel level = context.getSource().getLevel();
		
		if(level != null)
		{
			LazyOptional<SpaceshipCapability> spaceshipCapability = level.getCapability(SpaceshipCapabilityProvider.SPACESHIP);
			
			spaceshipCapability.ifPresent(cap ->
			{
				if(cap != null)
					context.getSource().sendSuccess(() -> Component.literal(cap.spaceship.getSpaceCoords().toString()), false); //TODO Translation
			});
		}
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int reloadRenderer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		ServerPlayer player = context.getSource().getPlayer();
		ServerLevel level = context.getSource().getLevel();

		if(player != null && level != null)
		{
			SpaceTravel.updatePlayerRenderer(level, player);
			
			context.getSource().sendSuccess(() -> Component.literal("Reloaded renderer"), false); //TODO Translation
		}
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int setSpaceshipSpeed(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		ServerLevel level = context.getSource().getLevel();
		int xAxis = IntegerArgumentType.getInteger(context, "x_axis");
		int yAxis = IntegerArgumentType.getInteger(context, "y_axis");
		int zAxis = IntegerArgumentType.getInteger(context, "z_axis");
		
		if(level != null)
		{
			LazyOptional<SpaceshipCapability> spaceshipCapability = level.getCapability(SpaceshipCapabilityProvider.SPACESHIP);
			
			spaceshipCapability.ifPresent(cap -> 
			{
				if(cap != null)
					cap.spaceship.setSpeed(xAxis, yAxis, zAxis);
			});
		}
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int rotateSpaceship(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		ServerLevel level = context.getSource().getLevel();
		double xAxis = DoubleArgumentType.getDouble(context, "x_axis");
		double yAxis = DoubleArgumentType.getDouble(context, "y_axis");
		double zAxis = DoubleArgumentType.getDouble(context, "z_axis");
		
		if(level != null)
		{
			LazyOptional<SpaceshipCapability> spaceshipCapability = level.getCapability(SpaceshipCapabilityProvider.SPACESHIP);
			
			spaceshipCapability.ifPresent(cap -> 
			{
				if(cap != null)
					cap.spaceship.rotate(xAxis, yAxis, zAxis);
			});
		}
		
		return Command.SINGLE_SUCCESS;
	}
}
