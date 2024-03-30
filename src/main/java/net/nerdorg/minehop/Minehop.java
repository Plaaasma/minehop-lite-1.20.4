package net.nerdorg.minehop;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.nerdorg.minehop.block.ModBlocks;
import net.nerdorg.minehop.block.entity.ModBlockEntities;
import net.nerdorg.minehop.commands.BoostCommands;
import net.nerdorg.minehop.commands.MapUtilCommands;
import net.nerdorg.minehop.commands.SpawnCommands;
import net.nerdorg.minehop.commands.ZoneManagementCommands;
import net.nerdorg.minehop.config.MinehopConfig;
import net.nerdorg.minehop.config.ConfigWrapper;
import net.nerdorg.minehop.data.DataManager;
import net.nerdorg.minehop.entity.ModEntities;
import net.nerdorg.minehop.entity.custom.ResetEntity;
import net.nerdorg.minehop.item.ModItems;
import net.nerdorg.minehop.networking.JoinManager;
import net.nerdorg.minehop.timer.TimerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Minehop implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("minehop");
    public static final String MOD_ID = "minehop";

	public static boolean override_config = false;
	public static double o_sv_friction = 0;
	public static double o_sv_accelerate = 0;
	public static double o_sv_airaccelerate = 0;
	public static double o_sv_maxairspeed = 0;
	public static double o_speed_mul = 0;
	public static double o_sv_gravity = 0;

	public static List<DataManager.MapData> mapList = new ArrayList<>();
	public static List<DataManager.RecordData> personalRecordList = new ArrayList<>();
	public static List<DataManager.RecordData> recordList = new ArrayList<>();
	public static HashMap<String, Integer> timerManager = new HashMap<>();

	@Override
	public void onInitialize() {
		AutoConfig.register(MinehopConfig.class, JanksonConfigSerializer::new);
		ConfigWrapper.loadConfig();

		ConfigWrapper.register();
		DataManager.register();
		JoinManager.register();

		TimerManager.register();

		SpawnCommands.register();
		MapUtilCommands.register();
		ZoneManagementCommands.register();
		BoostCommands.register();

		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		ModBlockEntities.registerBlockEntities();

		FabricDefaultAttributeRegistry.register(ModEntities.RESET_ENTITY, ResetEntity.createResetEntityAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.START_ENTITY, ResetEntity.createResetEntityAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.END_ENTITY, ResetEntity.createResetEntityAttributes());
	}
}