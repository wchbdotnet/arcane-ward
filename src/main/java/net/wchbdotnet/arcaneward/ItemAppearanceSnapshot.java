package net.wchbdotnet.arcaneward;

import net.runelite.api.ItemComposition;

final class ItemAppearanceSnapshot
{
	private final ItemComposition item;
	private final int inventoryModel;
	private final short[] colorToReplace;
	private final short[] colorToReplaceWith;
	private final short[] textureToReplace;
	private final short[] textureToReplaceWith;
	private final int xan2d;
	private final int yan2d;
	private final int zan2d;

	ItemAppearanceSnapshot(ItemComposition item)
	{
		this.item = item;
		inventoryModel = item.getInventoryModel();
		colorToReplace = clone(item.getColorToReplace());
		colorToReplaceWith = clone(item.getColorToReplaceWith());
		textureToReplace = clone(item.getTextureToReplace());
		textureToReplaceWith = clone(item.getTextureToReplaceWith());
		xan2d = item.getXan2d();
		yan2d = item.getYan2d();
		zan2d = item.getZan2d();
	}

	void restore()
	{
		item.setInventoryModel(inventoryModel);
		item.setColorToReplace(clone(colorToReplace));
		item.setColorToReplaceWith(clone(colorToReplaceWith));
		item.setTextureToReplace(clone(textureToReplace));
		item.setTextureToReplaceWith(clone(textureToReplaceWith));
		item.setXan2d(xan2d);
		item.setYan2d(yan2d);
		item.setZan2d(zan2d);
	}

	private static short[] clone(short[] value)
	{
		return value == null ? null : value.clone();
	}
}
