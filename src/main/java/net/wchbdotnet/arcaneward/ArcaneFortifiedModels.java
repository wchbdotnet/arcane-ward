package net.wchbdotnet.arcaneward;

import java.util.Arrays;
import java.util.IdentityHashMap;
import net.runelite.api.Client;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;
import net.runelite.api.ModelData;

final class ArcaneFortifiedModels
{
	private static final int INVENTORY_MODEL = 11310;
	private static final int MALE_MODEL = 11050;
	private static final int FEMALE_MODEL = 35756;
	private static final short PALE_SIGIL = 127;
	private static final short FORTIFIED_GOLD = JagexColor.packHSL(7, 7, 86);
	private static final short CRIMSON_DARK = JagexColor.packHSL(0, 7, 24);
	private static final short CRIMSON_MID = JagexColor.packHSL(0, 7, 34);
	private static final short CRIMSON_BASE = JagexColor.packHSL(0, 7, 44);
	private static final short CRIMSON_BRIGHT = JagexColor.packHSL(1, 7, 52);
	private static final short TURQUOISE_DARK = JagexColor.packHSL(31, 6, 25);
	private static final short TURQUOISE_MID = JagexColor.packHSL(31, 7, 38);
	private static final short TURQUOISE_BASE = JagexColor.packHSL(31, 7, 49);
	private static final short TURQUOISE_BRIGHT = JagexColor.packHSL(30, 7, 60);

	private final Client client;
	private short[] maleOriginal;
	private short[] maleOr;
	private short[] maleF;
	private short[] femaleOriginal;
	private short[] femaleOr;
	private short[] femaleF;
	private short[] inventoryOriginal;
	private short[] inventoryOr;
	private short[] inventoryF;
	private LitColors maleLitOriginal;
	private LitColors maleLitOr;
	private LitColors maleLitF;
	private LitColors femaleLitOriginal;
	private LitColors femaleLitOr;
	private LitColors femaleLitF;
	private int[] equippedFaces1;
	private int[] equippedFaces2;
	private int[] equippedFaces3;
	private int equippedVertexCount;
	private final IdentityHashMap<Model, Integer> equippedTopologyStarts = new IdentityHashMap<>();

	ArcaneFortifiedModels(Client client)
	{
		this.client = client;
	}

	void load()
	{
		equippedTopologyStarts.clear();

		ModelData inventorySource = client.loadModelData(INVENTORY_MODEL);
		inventoryOriginal = inventorySource.getFaceColors().clone();
		inventoryOr = paintedColors(inventoryOriginal, ShieldAppearance.ARCANE_OR);
		inventoryF = paintedColors(inventoryOriginal, ShieldAppearance.ARCANE_F);

		ModelData maleSource = client.loadModelData(MALE_MODEL);
		equippedFaces1 = maleSource.getFaceIndices1().clone();
		equippedFaces2 = maleSource.getFaceIndices2().clone();
		equippedFaces3 = maleSource.getFaceIndices3().clone();
		equippedVertexCount = maleSource.getVerticesCount();
		maleOriginal = maleSource.getFaceColors().clone();
		maleOr = paintedColors(maleOriginal, ShieldAppearance.ARCANE_OR);
		maleF = paintedColors(maleOriginal, ShieldAppearance.ARCANE_F);
		maleLitOriginal = new LitColors(maleSource.light(64, 850, -30, -50, -30));
		maleLitOr = new LitColors(coloredClone(maleSource, maleOr).light(64, 850, -30, -50, -30));
		maleLitF = new LitColors(coloredClone(maleSource, maleF).light(64, 850, -30, -50, -30));

		ModelData femaleSource = client.loadModelData(FEMALE_MODEL);
		femaleOriginal = femaleSource.getFaceColors().clone();
		femaleOr = paintedColors(femaleOriginal, ShieldAppearance.ARCANE_OR);
		femaleF = paintedColors(femaleOriginal, ShieldAppearance.ARCANE_F);
		femaleLitOriginal = new LitColors(femaleSource.light(64, 850, -30, -50, -30));
		femaleLitOr = new LitColors(coloredClone(femaleSource, femaleOr).light(64, 850, -30, -50, -30));
		femaleLitF = new LitColors(coloredClone(femaleSource, femaleF).light(64, 850, -30, -50, -30));
	}

	short[] inventoryColorsToReplace()
	{
		return inventoryOriginal.clone();
	}

	short[] inventoryReplacementColors(ShieldAppearance appearance)
	{
		return (appearance == ShieldAppearance.ARCANE_F ? inventoryF : inventoryOr).clone();
	}

	boolean paintPlayerModel(Model model, ShieldAppearance appearance)
	{
		if (model == null || !appearance.isRecoloredArcane())
		{
			return false;
		}
		short[] maleTarget = appearance == ShieldAppearance.ARCANE_F ? maleF : maleOr;
		short[] femaleTarget = appearance == ShieldAppearance.ARCANE_F ? femaleF : femaleOr;
		LitColors maleLitTarget = appearance == ShieldAppearance.ARCANE_F ? maleLitF : maleLitOr;
		LitColors femaleLitTarget = appearance == ShieldAppearance.ARCANE_F ? femaleLitF : femaleLitOr;
		Integer cachedStart = equippedTopologyStarts.get(model);
		int topologyStart = cachedStart == null
			? findTopology(model, equippedFaces1, equippedFaces2, equippedFaces3, equippedVertexCount)
			: cachedStart;
		if (topologyStart >= 0)
		{
			short[] target = model.getFaceCount() >= maleTarget.length ? maleTarget : femaleTarget;
			LitColors litTarget = target == maleTarget ? maleLitTarget : femaleLitTarget;
			applyRawAt(model, target, topologyStart);
			applyLitAt(model, litTarget, topologyStart);
			equippedTopologyStarts.put(model, topologyStart);
			return true;
		}
		short[] unlit = model.getUnlitFaceColors();
		if (unlit != null)
		{
			int start = replaceAnySequenceAt(unlit, maleTarget, maleOriginal, maleOr, maleF);
			if (start >= 0)
			{
				applyLitAt(model, maleLitTarget, start);
				return true;
			}
			start = replaceAnySequenceAt(unlit, femaleTarget, femaleOriginal, femaleOr, femaleF);
			if (start >= 0)
			{
				applyLitAt(model, femaleLitTarget, start);
				return true;
			}
		}
		return replaceAnyLitSequence(model, maleLitTarget, maleLitOriginal, maleLitOr, maleLitF)
			|| replaceAnyLitSequence(model, femaleLitTarget, femaleLitOriginal, femaleLitOr, femaleLitF);
	}

	private static short[] paintedColors(short[] source, ShieldAppearance appearance)
	{
		short[] colors = source.clone();
		for (int face = 0; face < colors.length; ++face)
		{
			colors[face] = colors[face] == PALE_SIGIL
				? FORTIFIED_GOLD
				: bodyColor(colors[face], appearance);
		}
		return colors;
	}

	private static ModelData coloredClone(ModelData source, short[] colors)
	{
		ModelData result = source.cloneColors();
		System.arraycopy(colors, 0, result.getFaceColors(), 0, colors.length);
		return result;
	}

	private static short bodyColor(short original, ShieldAppearance appearance)
	{
		boolean turquoise = appearance == ShieldAppearance.ARCANE_F;
		switch (original & 0xffff)
		{
			case 5594:
			case 7056:
				return turquoise ? TURQUOISE_DARK : CRIMSON_DARK;
			case 7079:
			case 7089:
				return turquoise ? TURQUOISE_MID : CRIMSON_MID;
			case 7104:
				return turquoise ? TURQUOISE_BRIGHT : CRIMSON_BRIGHT;
			default:
				return turquoise ? TURQUOISE_BASE : CRIMSON_BASE;
		}
	}

	private static int replaceAnySequenceAt(short[] target, short[] replacement, short[]... candidates)
	{
		for (short[] candidate : candidates)
		{
			int start = replaceSequenceAt(target, candidate, replacement);
			if (start >= 0)
			{
				return start;
			}
		}
		return -1;
	}

	private static int replaceSequenceAt(short[] target, short[] find, short[] replacement)
	{
		if (find == null || target.length < find.length)
		{
			return -1;
		}
		outer:
		for (int start = 0; start <= target.length - find.length; ++start)
		{
			for (int offset = 0; offset < find.length; ++offset)
			{
				if (target[start + offset] != find[offset])
				{
					continue outer;
				}
			}
			System.arraycopy(replacement, 0, target, start, replacement.length);
			return start;
		}
		return -1;
	}

	private static void applyLitAt(Model target, LitColors replacement, int start)
	{
		int[] first = target.getFaceColors1();
		int[] second = target.getFaceColors2();
		int[] third = target.getFaceColors3();
		if (first == null || start + replacement.first.length > first.length)
		{
			return;
		}
		System.arraycopy(replacement.first, 0, first, start, replacement.first.length);
		System.arraycopy(replacement.second, 0, second, start, replacement.second.length);
		System.arraycopy(replacement.third, 0, third, start, replacement.third.length);
	}

	private static void applyRawAt(Model target, short[] replacement, int start)
	{
		short[] colors = target.getUnlitFaceColors();
		if (colors != null && start + replacement.length <= colors.length)
		{
			System.arraycopy(replacement, 0, colors, start, replacement.length);
		}
	}

	private static int findTopology(Model target, int[] source1, int[] source2,
		int[] source3, int sourceVertexCount)
	{
		int[] target1 = target.getFaceIndices1();
		int[] target2 = target.getFaceIndices2();
		int[] target3 = target.getFaceIndices3();
		if (target1 == null || target1.length < source1.length)
		{
			return -1;
		}
		int[] vertexMap = new int[sourceVertexCount];
		for (int start = 0; start <= target1.length - source1.length; ++start)
		{
			Arrays.fill(vertexMap, -1);
			boolean matches = true;
			for (int face = 0; face < source1.length; ++face)
			{
				if (!mapsVertex(vertexMap, source1[face], target1[start + face])
					|| !mapsVertex(vertexMap, source2[face], target2[start + face])
					|| !mapsVertex(vertexMap, source3[face], target3[start + face]))
				{
					matches = false;
					break;
				}
			}
			if (matches)
			{
				return start;
			}
		}
		return -1;
	}

	private static boolean mapsVertex(int[] vertexMap, int source, int target)
	{
		if (vertexMap[source] == -1)
		{
			vertexMap[source] = target;
			return true;
		}
		return vertexMap[source] == target;
	}

	private static boolean replaceAnyLitSequence(Model target, LitColors replacement, LitColors... candidates)
	{
		for (LitColors candidate : candidates)
		{
			if (replaceLitSequence(target, candidate, replacement))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean replaceLitSequence(Model target, LitColors find, LitColors replacement)
	{
		int[] target1 = target.getFaceColors1();
		int[] target2 = target.getFaceColors2();
		int[] target3 = target.getFaceColors3();
		if (target1 == null || target1.length < find.first.length)
		{
			return false;
		}
		outer:
		for (int start = 0; start <= target1.length - find.first.length; ++start)
		{
			for (int offset = 0; offset < find.first.length; ++offset)
			{
				if (target1[start + offset] != find.first[offset]
					|| target2[start + offset] != find.second[offset]
					|| target3[start + offset] != find.third[offset])
				{
					continue outer;
				}
			}
			System.arraycopy(replacement.first, 0, target1, start, replacement.first.length);
			System.arraycopy(replacement.second, 0, target2, start, replacement.second.length);
			System.arraycopy(replacement.third, 0, target3, start, replacement.third.length);
			return true;
		}
		return false;
	}

	private static final class LitColors
	{
		private final int[] first;
		private final int[] second;
		private final int[] third;

		private LitColors(Model model)
		{
			first = model.getFaceColors1().clone();
			second = model.getFaceColors2().clone();
			third = model.getFaceColors3().clone();
		}
	}
}
