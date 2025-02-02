package io.github.haykam821.lastcard.card;

import java.util.Objects;

import eu.pb4.mapcanvas.api.core.CanvasColor;
import eu.pb4.mapcanvas.api.core.DrawableCanvas;
import eu.pb4.mapcanvas.api.utils.ViewUtils;
import io.github.haykam821.lastcard.card.color.CardColor;
import io.github.haykam821.lastcard.card.color.ColorSelector;
import io.github.haykam821.lastcard.game.player.AbstractPlayerEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemStack.TooltipSection;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

public abstract class Card {
	private final ColorSelector selector;

	public Card(ColorSelector selector) {
		this.selector = Objects.requireNonNull(selector);
	}

	public final ItemStack createStack(AbstractPlayerEntry player) {
		ItemStackBuilder builder = ItemStackBuilder.of(this.selector.getItem());
		builder.setName(this.getFullName());

		if (this.canPlay(player)) {
			builder.addEnchantment(Enchantments.POWER, 1);
		}

		ItemStack stack = builder.build();
		stack.getNbt().putInt("HideFlags", TooltipSection.ENCHANTMENTS.getFlag());

		return stack;
	}

	public abstract Text getName();

	public final Text getFullName() {
		return new LiteralText("")
			.append(this.selector.getName())
			.append(" ")
			.append(this.getName())
			.formatted(this.selector.getFormatting());
	}

	public final boolean canPlay(AbstractPlayerEntry player) {
		if (!player.hasTurn()) return false;

		CardDeck deck = player.getPhase().getDeck();

		Card previousCard = deck.getPreviousCard();
		return previousCard == null || this.isMatching(previousCard, deck.getPreviousColor());
	}

	public boolean isMatching(Card card, CardColor color) {
		return this.selector.isMatching(color);
	}

	public void play(AbstractPlayerEntry player) {
		player.getPhase().sendMessageWithException(this.getCardPlayedMessage(player), player, this.getCardPlayedYouMessage());
		player.getPhase().updateBar();
	}

	public ColorSelector getSelector() {
		return this.selector;
	}

	public final DrawableCanvas render() {
		DrawableCanvas canvas = this.selector.getTemplate().copy();
		CanvasColor textColor = this.selector.getCanvasTextColor();

		this.renderOverlay(canvas, textColor);
		this.renderOverlay(ViewUtils.flipY(ViewUtils.flipX(canvas)), textColor);
		
		return canvas;
	}

	public abstract void renderOverlay(DrawableCanvas canvas, CanvasColor textColor);

	private Text getCardPlayedMessage(AbstractPlayerEntry player) {
		return new TranslatableText("text.lastcard.card_played", player.getName(), this.getFullName()).formatted(Formatting.GOLD);
	}

	private Text getCardPlayedYouMessage() {
		return new TranslatableText("text.lastcard.card_played.you", this.getFullName()).formatted(Formatting.GOLD);
	}
}
