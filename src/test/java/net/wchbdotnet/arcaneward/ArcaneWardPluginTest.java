package net.wchbdotnet.arcaneward;

import net.runelite.api.PlayerComposition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ArcaneWardPluginTest
{
	@Test
	public void supportsBothWardVariants()
	{
		assertTrue(ArcaneWardPlugin.isSupportedWard(ArcaneWardPlugin.ELIDINIS_WARD_F));
		assertTrue(ArcaneWardPlugin.isSupportedWard(ArcaneWardPlugin.ELIDINIS_WARD_OR));
		assertFalse(ArcaneWardPlugin.isSupportedWard(ArcaneWardPlugin.ARCANE_SPIRIT_SHIELD));
	}

	@Test
	public void restoresCompositionFromActualEquipment()
	{
		assertEquals(0, ArcaneWardPlugin.equipmentIdForShield(-1));
		assertEquals(
			ArcaneWardPlugin.ELIDINIS_WARD_F + PlayerComposition.ITEM_OFFSET,
			ArcaneWardPlugin.equipmentIdForShield(ArcaneWardPlugin.ELIDINIS_WARD_F));
		assertEquals(
			ArcaneWardPlugin.ELIDINIS_WARD_OR + PlayerComposition.ITEM_OFFSET,
			ArcaneWardPlugin.equipmentIdForShield(ArcaneWardPlugin.ELIDINIS_WARD_OR));
	}
}
