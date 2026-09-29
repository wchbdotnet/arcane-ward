package net.wchbdotnet.arcaneward;

import static org.junit.Assert.assertArrayEquals;
import org.junit.Test;

public class ArcaneFortifiedPaletteTest
{
	@Test
	public void mapsOnlyExistingArcaneColourGroups()
	{
		assertArrayEquals(new short[]{127, 7056, 7089, 7101}, ArcaneFortifiedPalette.equippedFind());
		assertArrayEquals(new short[]{127, 7056, 7079, 7101}, ArcaneFortifiedPalette.inventoryFind());
	}
}
