package net.wchbdotnet.arcaneward;

public enum ShieldAppearance
{
	ELIDINIS("Original ward"),
	ARCANE_BASE("Arcane spirit shield"),
	ARCANE_OR("Arcane spirit shield (or)"),
	ARCANE_F("Arcane spirit shield (f)");

	private final String label;

	ShieldAppearance(String label)
	{
		this.label = label;
	}

	ShieldAppearance next()
	{
		ShieldAppearance[] appearances = values();
		return appearances[(ordinal() + 1) % appearances.length];
	}

	boolean usesArcaneModel()
	{
		return this != ELIDINIS;
	}

	boolean isRecoloredArcane()
	{
		return this == ARCANE_OR || this == ARCANE_F;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
