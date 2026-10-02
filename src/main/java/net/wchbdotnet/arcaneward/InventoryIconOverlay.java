package net.wchbdotnet.arcaneward;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.BufferProvider;
import net.runelite.api.Client;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

final class InventoryIconOverlay extends WidgetItemOverlay
{
	private final ArcaneWardConfig config;
	private final Client client;
	private BufferedImage fortifiedReplacementImage;
	private BufferedImage ornamentReplacementImage;
	private BufferedImage originalFortifiedWardImage;
	private BufferedImage originalOrnamentWardImage;

	@Inject
	InventoryIconOverlay(ArcaneWardConfig config, Client client)
	{
		this.config = config;
		this.client = client;
		showOnInventory();
		showOnEquipment();
		showOnBank();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem itemWidget)
	{
		if (!ArcaneWardPlugin.isSupportedWard(itemId)
			|| !config.shieldAppearance().usesArcaneModel()
			|| replacementImage(itemId) == null)
		{
			return;
		}

		BufferedImage replacementImage = replacementImage(itemId);
		BufferedImage originalWardImage = originalImage(itemId);
		Rectangle bounds = itemWidget.getCanvasBounds();
		eraseUncoveredNativePixels(graphics, bounds, originalWardImage, replacementImage);
		graphics.drawImage(replacementImage, bounds.x, bounds.y, null);
	}

	private void eraseUncoveredNativePixels(Graphics2D graphics, Rectangle bounds,
		BufferedImage original, BufferedImage replacement)
	{
		if (original == null || original.getWidth() != replacement.getWidth()
			|| original.getHeight() != replacement.getHeight())
		{
			return;
		}
		BufferProvider buffer = client.getBufferProvider();
		if (buffer == null || buffer.getPixels() == null)
		{
			return;
		}
		int[] canvasPixels = buffer.getPixels();
		int canvasWidth = buffer.getWidth();
		int canvasHeight = buffer.getHeight();
		for (int y = 0; y < original.getHeight(); ++y)
		{
			int cleanX = findCleanBackgroundX(original, replacement, y);
			int sampleCanvasX = bounds.x + cleanX;
			int canvasY = bounds.y + y;
			if (sampleCanvasX < 0 || sampleCanvasX >= canvasWidth
				|| canvasY < 0 || canvasY >= canvasHeight)
			{
				continue;
			}
			graphics.setColor(new Color(canvasPixels[canvasY * canvasWidth + sampleCanvasX] & 0xffffff));
			for (int x = 0; x < original.getWidth(); ++x)
			{
				if ((original.getRGB(x, y) >>> 24) != 0
					&& (replacement.getRGB(x, y) >>> 24) == 0)
				{
					graphics.fillRect(bounds.x + x, canvasY, 1, 1);
				}
			}
		}
	}

	private static int findCleanBackgroundX(BufferedImage original, BufferedImage replacement, int y)
	{
		for (int x = 0; x < original.getWidth(); ++x)
		{
			if ((original.getRGB(x, y) >>> 24) == 0
				&& (replacement.getRGB(x, y) >>> 24) == 0)
			{
				return x;
			}
		}
		return 0;
	}

	void setReplacementImage(BufferedImage replacementImage)
	{
		fortifiedReplacementImage = replacementImage;
		ornamentReplacementImage = replacementImage;
	}

	void setReplacementImages(BufferedImage fortifiedImage, BufferedImage ornamentImage)
	{
		fortifiedReplacementImage = fortifiedImage;
		ornamentReplacementImage = ornamentImage;
	}

	void setNativeImages(BufferedImage fortifiedImage, BufferedImage ornamentImage)
	{
		originalFortifiedWardImage = fortifiedImage;
		originalOrnamentWardImage = ornamentImage;
	}

	private BufferedImage replacementImage(int itemId)
	{
		return itemId == ArcaneWardPlugin.ELIDINIS_WARD_OR
			? ornamentReplacementImage : fortifiedReplacementImage;
	}

	private BufferedImage originalImage(int itemId)
	{
		return itemId == ArcaneWardPlugin.ELIDINIS_WARD_OR
			? originalOrnamentWardImage : originalFortifiedWardImage;
	}
}
