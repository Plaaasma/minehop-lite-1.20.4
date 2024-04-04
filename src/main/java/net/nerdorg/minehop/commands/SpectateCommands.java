package net.nerdorg.minehop.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import net.nerdorg.minehop.entity.custom.ReplayEntity;
import net.nerdorg.minehop.networking.PacketHandler;
import net.nerdorg.minehop.util.Logger;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class SpectateCommands {
    public static HashMap<String, List<String>> spectatorList = new HashMap<>();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
            LiteralArgumentBuilder.<ServerCommandSource>literal("spec")
                .then(RequiredArgumentBuilder.<ServerCommandSource, String>argument("entity", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        // Iterate over all entities and add your custom entities to the suggestions
                        Iterable<Entity> entities = context.getSource().getWorld().iterateEntities();
                        for (Entity entity : entities) {
                            if (entity instanceof ReplayEntity) {
                                builder.suggest(entity.getNameForScoreboard(), new LiteralMessage(entity.getName().getString()));
                            }
                            else if (entity instanceof PlayerEntity) {
                                builder.suggest(entity.getNameForScoreboard(), new LiteralMessage(entity.getName().getString()));
                            }
                        }
                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        handleSpectateReplay(context);
                        return Command.SINGLE_SUCCESS;
                    })
                )
            ));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
            LiteralArgumentBuilder.<ServerCommandSource>literal("spectate")
                .then(RequiredArgumentBuilder.<ServerCommandSource, String>argument("entity", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        // Iterate over all entities and add your custom entities to the suggestions
                        Iterable<Entity> entities = context.getSource().getWorld().iterateEntities();
                        for (Entity entity : entities) {
                            if (entity instanceof ReplayEntity) {
                                builder.suggest(entity.getNameForScoreboard(), new LiteralMessage(entity.getName().getString()));
                            }
                            else if (entity instanceof PlayerEntity) {
                                builder.suggest(entity.getNameForScoreboard(), new LiteralMessage(entity.getName().getString()));
                            }
                        }
                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        handleSpectateReplay(context);
                        return Command.SINGLE_SUCCESS;
                    })
                )
        ));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
            LiteralArgumentBuilder.<ServerCommandSource>literal("unspec")
                .executes(context -> {
                    handleUnSpectate(context);
                    return Command.SINGLE_SUCCESS;
                })

        ));
    }

    private static void handleUnSpectate(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity serverPlayerEntity = context.getSource().getPlayer();

        if (!serverPlayerEntity.isSpectator()) {
            Logger.logFailure(serverPlayerEntity, "You are not spectating.");
        }
        else {
            Logger.logSuccess(serverPlayerEntity, "No longer spectating.");
            serverPlayerEntity.changeGameMode(GameMode.ADVENTURE);
            serverPlayerEntity.setCameraEntity(serverPlayerEntity);
            SpawnCommands.handleSpawn(context);
        }
    }

    private static void handleSpectateReplay(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity serverPlayerEntity = context.getSource().getPlayer();
        String nameString = new String(context.getArgument("entity", String.class).getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        Entity entity = context.getSource().getServer().getPlayerManager().getPlayer(nameString);
        if (entity == null) {
            Iterable<Entity> entities = context.getSource().getWorld().iterateEntities();
            for (Entity iterEntity : entities) {
                if (iterEntity instanceof ReplayEntity) {
                    if (iterEntity.getNameForScoreboard().equals(nameString)) {
                        entity = iterEntity;
                        break;
                    }
                }
            }
            if (entity == null) {
                Logger.logFailure(serverPlayerEntity, "Entity not found.");
                return;
            }
        }

        if (entity instanceof ReplayEntity replayEntity) {
            Logger.logSuccess(serverPlayerEntity, "Now spectating " + replayEntity.getNameForScoreboard() + ". Use /unspec to stop spectating.");
            serverPlayerEntity.changeGameMode(GameMode.SPECTATOR);
            serverPlayerEntity.teleport(replayEntity.getX(), replayEntity.getY(), replayEntity.getZ());
            serverPlayerEntity.setCameraEntity(replayEntity);
        }
        else if (entity instanceof PlayerEntity playerEntity) {
            if (playerEntity == serverPlayerEntity) {
                Logger.logFailure(serverPlayerEntity, "You cannot spectate yourself.");
            }
            else if (playerEntity.isCreative() || playerEntity.isSpectator()) {
                Logger.logFailure(serverPlayerEntity, "You cannot spectate another spectator.");
            }
            else {
                Logger.logSuccess(serverPlayerEntity, "Now spectating " + playerEntity.getNameForScoreboard() + ". Use /unspec to stop spectating.");
                serverPlayerEntity.changeGameMode(GameMode.SPECTATOR);
                serverPlayerEntity.teleport(playerEntity.getX(), playerEntity.getY(), playerEntity.getZ());
                serverPlayerEntity.setCameraEntity(playerEntity);
            }
        }
        else {
            Logger.logFailure(serverPlayerEntity, "You cannot spectate this entity.");
        }
    }
}