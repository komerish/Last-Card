package io.github.haykam821.lastcard.game.player;

import java.util.ArrayList;
import java.util.List;

import io.github.haykam821.lastcard.Main;
import io.github.haykam821.lastcard.card.Card;
import io.github.haykam821.lastcard.card.display.CardDisplay;
import io.github.haykam821.lastcard.card.display.PrivateCardDisplay;
import io.github.haykam821.lastcard.card.display.PublicCardDisplay;
import io.github.haykam821.lastcard.game.map.Chair;
import io.github.haykam821.lastcard.game.phase.LastCardActivePhase;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import xyz.nucleoid.map_templates.TemplateRegion;

public class PlayerEntry {
	private static final int INITIAL_HAND_COUNT = 7;

	private final LastCardActivePhase phase;
	private final ServerPlayerEntity player;
	private final List<Card> cards = new ArrayList<>(INITIAL_HAND_COUNT);

	private final Chair chair;
	private final CardDisplay privateDisplay;
	private final CardDisplay publicDisplay;

	public PlayerEntry(LastCardActivePhase phase, ServerPlayerEntity player, TemplateRegion chair, TemplateRegion privateDisplay, TemplateRegion publicDisplay) {
		this.phase = phase;
		this.player = player;
		
		for (int index = 0; index < INITIAL_HAND_COUNT; index++) {
			this.cards.add(this.phase.getDeck().draw());
		}

		this.chair = new Chair(chair);
		this.privateDisplay = new PrivateCardDisplay(this, privateDisplay);
		this.publicDisplay = new PublicCardDisplay(this, publicDisplay);
	}

	public boolean hasTurn() {
		return this.phase.getTurnManager().hasTurn(this);
	}

	public Chair getChair() {
		return this.chair;
	}

	public void spawn() {
		this.player.getInventory().setStack(0, new ItemStack(Main.CARD_HAND));
		this.player.currentScreenHandler.sendContentUpdates();

		this.chair.teleport(this.player);
	}

	public Text getWinMessage() {
		return new TranslatableText("text.lastcard.win", this.getName()).formatted(Formatting.GOLD);
	}

	public Text getNextTurnMessage() {
		return new TranslatableText("text.lastcard.turn.next", this.getName()).formatted(Formatting.GOLD);
	}

	public Text getName() {
		return this.player.getDisplayName();
	}

	public LastCardActivePhase getPhase() {
		return this.phase;
	}

	public ServerPlayerEntity getPlayer() {
		return this.player;
	}

	public Iterable<Card> getCards() {
		return this.cards;
	}

	public int getCardCount() {
		return this.cards.size();
	}

	public void playCard(int index) {
		Card card = this.cards.get(index);
		this.playCard(card);
	}

	private void playCard(Card card) {
		if (card.canPlay(this)) {
			this.discardCard(card);
			card.play(this);

			if (this.cards.isEmpty()) {
				this.phase.endWithWinner(this);
			} else {
				this.phase.getTurnManager().cycleTurn();
			}
		}
	}

	private void discardCard(Card card) {
		this.phase.getDeck().discard(card);
		this.cards.remove(card);
	}

	public void drawForTurn() {
		Card card = this.phase.getDeck().draw();
		this.cards.add(card);

		this.phase.sendMessageWithException(this.getCardDrewMessage(), this, this.getCardDrewYouMessage(card));
		this.phase.getTurnManager().cycleTurn();
	}

	private Text getCardDrewMessage() {
		return new TranslatableText("text.lastcard.card_drew", this.getName()).formatted(Formatting.GOLD);
	}

	private Text getCardDrewYouMessage(Card card) {
		return new TranslatableText("text.lastcard.card_drew.you", card.getFullName()).formatted(Formatting.GOLD);
	}

	public void openCardHand() {
		CardHandGui.build(this).open();
	}

	// Displays
	private CardDisplay getDisplayViewableBy(ServerPlayerEntity viewer) {
		return this.getPlayer() == viewer ? this.privateDisplay : this.publicDisplay;
	}

	public void addDisplay(ServerPlayerEntity viewer) {
		this.getDisplayViewableBy(viewer).add(viewer);
	}

	public void removeDisplay(ServerPlayerEntity viewer) {
		this.getDisplayViewableBy(viewer).remove(viewer);
	}

	public void destroyDisplays() {
		this.privateDisplay.destroy();
		this.publicDisplay.destroy();
	}

	public void renderDisplays() {
		this.privateDisplay.render();
		this.publicDisplay.render();
	}

	@Override
	public String toString() {
		return "PlayerEntry{player=" + this.player + ", chair=" + this.chair + "}";
	}
}
