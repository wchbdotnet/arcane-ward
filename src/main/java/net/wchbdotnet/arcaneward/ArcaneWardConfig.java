package net.wchbdotnet.arcaneward;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(ArcaneWardConfig.GROUP)
public interface ArcaneWardConfig extends Config
{
	String GROUP = "arcaneWard";

	@ConfigItem(
		keyName = "appearance",
		name = "Appearance",
		description = "Choose how Elidinis' ward (f) and (or) appear on your client"
	)
	default ShieldAppearance shieldAppearance()
	{
		return ShieldAppearance.ARCANE_F;
	}
}
