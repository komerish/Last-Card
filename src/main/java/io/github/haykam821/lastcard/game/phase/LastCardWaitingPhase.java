package io.github.haykam821.lastcard.game.phase;

import java.util.Random;

import io.github.haykam821.lastcard.game.LastCardConfig;
import io.github.haykam821.lastcard.game.map.LastCardMap;
import io.github.haykam821.lastcard.game.map.LastCardMapBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameResult;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.item.ItemPickupEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;
import xyz.nucleoid.stimuli.event.world.FluidFlowEvent;

public class LastCardWaitingPhase implements GamePlayerEvents.Offer, GameActivityEvents.Tick, PlayerDamageEvent, PlayerDeathEvent, GameActivityEvents.RequestStart, FluidFlowEvent, ItemPickupEvent {
	private final GameSpace gameSpace;
	private final ServerWorld world;

	private final LastCardConfig config;
	private final LastCardMap map;

	public LastCardWaitingPhase(GameSpace gameSpace, ServerWorld world, LastCardConfig config, LastCardMap map) {
		this.gameSpace = gameSpace;
		this.world = world;

		this.config = config;
		this.map = map;
	}

	public static GameOpenProcedure open(GameOpenContext<LastCardConfig> context) {
		LastCardConfig config = context.config();

		MinecraftServer server = context.server();
		Random random = server.getOverworld().getRandom();

		LastCardMapBuilder mapBuilder = new LastCardMapBuilder(config);
		LastCardMap map = mapBuilder.create(server);

		RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
			.setTimeOfDay(config.getTimeOfDay().get(random))
			.setGenerator(map.createGenerator(server));

		return context.openWithWorld(worldConfig, (activity, world) -> {
			LastCardWaitingPhase phase = new LastCardWaitingPhase(activity.getGameSpace(), world, config, map);
			GameWaitingLobby.addTo(activity, config.getPlayerConfig());

			LastCardActivePhase.setRules(activity);
			activity.deny(GameRuleType.MODIFY_INVENTORY);

			// Listeners
			activity.listen(GamePlayerEvents.OFFER, phase);
			activity.listen(GameActivityEvents.TICK, phase);
			activity.listen(PlayerDamageEvent.EVENT, phase);
			activity.listen(PlayerDeathEvent.EVENT, phase);
			activity.listen(GameActivityEvents.REQUEST_START, phase);
			activity.listen(FluidFlowEvent.EVENT, phase);
			activity.listen(ItemPickupEvent.EVENT, phase);
		});
	}

	@Override
	public ActionResult onFluidFlow(ServerWorld world, BlockPos fluidPos, BlockState fluidBlock, Direction flowDirection, BlockPos flowTo, BlockState flowToBlock) {
		return ActionResult.FAIL;
	}

	@Override
	public ActionResult onPickupItem(ServerPlayerEntity player, ItemEntity entity, ItemStack stack) {
		return ActionResult.FAIL;
	}

	@Override
	public PlayerOfferResult onOfferPlayer(PlayerOffer offer) {
		return this.map.getWaitingSpawn().acceptOffer(offer, this.world, GameMode.ADVENTURE);
	}

	@Override
	public void onTick() {
		for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
			if (!this.map.contains(player)) {
				this.spawn(player);
			}
		}
	}

	@Override
	public ActionResult onDamage(ServerPlayerEntity player, DamageSource source, float amount) {
		return ActionResult.FAIL;
	}

	@Override
	public ActionResult onDeath(ServerPlayerEntity player, DamageSource source) {
		this.spawn(player);
		return ActionResult.FAIL;
	}

	@Override
	public GameResult onRequestStart() {
		LastCardActivePhase.open(this.gameSpace, this.world, this.config, this.map);
		return GameResult.ok();
	}

	private void spawn(ServerPlayerEntity player) {
		this.map.getWaitingSpawn().teleport(player);
	}
}