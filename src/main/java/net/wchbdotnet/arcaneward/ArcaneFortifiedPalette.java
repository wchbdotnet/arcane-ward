package net.wchbdotnet.arcaneward;

final class ArcaneFortifiedPalette
{
	// Face colours present in the vanilla Arcane equipment models (11050/35756).
	private static final short PALE_SIGIL = 127;
	private static final short RECESSED_BRONZE = 7056;
	private static final short BRONZE_PANEL = 7089;
	private static final short BRONZE_FIELD = 7101;

	// The inventory/ground model (11310) has one additional recessed shade.
	private static final short RECESSED_BRONZE_MID = 7079;

	private ArcaneFortifiedPalette()
	{
	}

	static short[] equippedFind()
	{
		return new short[]{PALE_SIGIL, RECESSED_BRONZE, BRONZE_PANEL, BRONZE_FIELD};
	}

	static short[] equippedOriginal()
	{
		return new short[]{PALE_SIGIL, RECESSED_BRONZE, BRONZE_PANEL, BRONZE_FIELD};
	}

	static short[] inventoryFind()
	{
		return new short[]{PALE_SIGIL, RECESSED_BRONZE, RECESSED_BRONZE_MID, 7101};
	}

}
