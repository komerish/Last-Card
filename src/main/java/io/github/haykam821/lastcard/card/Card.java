package io.github.haykam821.lastcard.card;

import eu.pb4.mapcanvas.api.core.CanvasColor;
import eu.pb4.mapcanvas.api.core.DrawableCanvas;
import eu.pb4.mapcanvas.api.utils.ViewUtils;
import io.github.haykam821.lastcard.game.PlayerEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemStack.TooltipSection;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

public abstract class Card {
	private final CardColor color;

	public Card(CardColor color) {
		this.color = color;
	}

	public final ItemStack createStack(PlayerEntry player) {
		ItemStackBuilder builder = ItemStackBuilder.of(this.color.getItem());
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
			.append(this.color.getName())
			.append(" ")
			.append(this.getName())
			.formatted(this.color.getFormatting());
	}

	public final boolean canPlay(PlayerEntry player) {
		if (!player.hasTurn()) return false;

		Card previousCard = player.getPhase().getDeck().getPreviousCard();
		return previousCard == null || this.isMatching(previousCard);
	}

	public boolean isMatching(Card card) {
		return this.color == card.color;
	}

	public void play(PlayerEntry player) {
		player.getPhase().sendMessageWithException(this.getCardPlayedMessage(player), player, this.getCardPlayedYouMessage());
		player.getPhase().updateBar();
	}

	public BossBar.Color getBossBarColor() {
		return this.color.getBossBarColor();
	}

	public final DrawableCanvas render() {
		DrawableCanvas canvas = this.color.getTemplate().copy();
		CanvasColor textColor = this.color.getCanvasTextColor();

		this.renderOverlay(canvas, textColor);
		this.renderOverlay(ViewUtils.flipY(ViewUtils.flipX(canvas)), textColor);
		
		return canvas;
	}

	public abstract void renderOverlay(DrawableCanvas canvas, CanvasColor textColor);

	private Text getCardPlayedMessage(PlayerEntry player) {
		return new TranslatableText("text.lastcard.card_played", player.getName(), this.getFullName()).formatted(Formatting.GOLD);
	}

	private Text getCardPlayedYouMessage() {
		return new TranslatableText("text.lastcard.card_played.you", this.getFullName()).formatted(Formatting.GOLD);
	}
}
