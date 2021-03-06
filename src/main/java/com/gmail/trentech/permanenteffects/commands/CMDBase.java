package com.gmail.trentech.permanenteffects.commands;

import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.data.manipulator.mutable.PotionEffectData;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

import com.gmail.trentech.permanenteffects.Main;
import com.gmail.trentech.pjc.core.ConfigManager;
import com.gmail.trentech.pjc.help.Help;

import ninja.leaping.configurate.ConfigurationNode;

public class CMDBase implements CommandExecutor {

	private final String command;
	private final PotionEffectType potionType;
	private final ConfigurationNode node;

	private ConfigManager configManager = ConfigManager.get(Main.getPlugin());
	private ConfigurationNode config = configManager.getConfig();

	public CMDBase(String command, String node, PotionEffectType potionType) {
		this.command = command;
		this.potionType = potionType;

		if (config.getNode(node).isVirtual()) {
			config.getNode(node, "enable").setValue(true);
			config.getNode(node, "message", "on").setValue("&e" + potionType.getName().replace("EFFECT.", "").toUpperCase() + " &2effect enabled");
			config.getNode(node, "message", "off").setValue("&e" + potionType.getName().replace("EFFECT.", "").toUpperCase() + " &2effect disabled");
			configManager.save();
		}

		this.node = config.getNode(node, "message");

		Text text = Text.builder().color(TextColors.GREEN).onHover(TextActions.showText(Text.of("Set ", node.replace("_", " "), " effect"))).onClick(TextActions.runCommand("/permanenteffects:effects " + command)).append(Text.of(" /effects " + command)).build();

		CMDEffects.map.put(command + ":" + node, text);
	}

	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		Help help = Help.get("effects " + command).get();
		
		if (args.hasAny("help")) {			
			help.execute(src);
			return CommandResult.empty();
		}
		
		if (!args.hasAny("on|off")) {
			throw new CommandException(help.getUsageText());
			
		}
		String value = args.<String> getOne("on|off").get();

		if (!value.equalsIgnoreCase("on") && !value.equalsIgnoreCase("off")) {
			throw new CommandException(help.getUsageText());
		}

		Player player;
		if (args.hasAny("player")) {
			if (!src.hasPermission("permanenteffects.cmd.effects." + command + ".others")) {
				throw new CommandException(Text.of(TextColors.RED, "You do not have permission to set effects on other players"));
			}
			String playerName = args.<String> getOne("player").get();

			Optional<Player> optionalPlayer = Sponge.getServer().getPlayer(playerName);

			if (!optionalPlayer.isPresent()) {
				throw new CommandException(Text.of(TextColors.RED, playerName, " not found"));
			}
			player = optionalPlayer.get();
		} else {
			if (!(src instanceof Player)) {
				throw new CommandException(Text.of(TextColors.RED, "Must specify player from console"));
			}
			player = (Player) src;
		}

		if (value.equalsIgnoreCase("on")) {
			PotionEffect effect = Sponge.getRegistry().createBuilder(PotionEffect.Builder.class).potionType(potionType).duration(1000000).amplifier(1).particles(false).build();

			PotionEffectData effects = player.getOrCreate(PotionEffectData.class).get();
			effects.addElement(effect);

			player.offer(effects);

			player.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(node.getNode("on").getString()));

			if (!src.equals(player)) {
				player.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(node.getNode("on").getString()));
			}
		} else if (value.equalsIgnoreCase("off")) {
			PotionEffectData effects = player.getOrCreate(PotionEffectData.class).get();

			for (PotionEffect effect : effects.asList()) {
				if (effect.getType().equals(potionType)) {
					effects.remove(effect);
					player.offer(effects);
					player.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(node.getNode("off").getString()));

					if (!src.equals(player)) {
						player.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(node.getNode("off").getString()));
					}
					break;
				}
			}
		}

		return CommandResult.success();
	}
}