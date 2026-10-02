package net.wchbdotnet.arcaneward;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.SpritePixels;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.PlayerChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.kit.KitType;
import net.runelite.api.widgets.ItemQuantityMode;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import static net.runelite.api.Constants.CLIENT_DEFAULT_ZOOM;

@PluginDescriptor(
	name = "Arcane Ward",
	description = "Changes the local appearance of Elidinis' ward (f) and (or)",
	tags = {"elidinis", "ward", "arcane", "shield", "cosmetic"}
)
public final class ArcaneWardPlugin extends Plugin
{
	static final int ELIDINIS_WARD_F = ItemID.ELIDINIS_WARD_FORTIFIED;
	static final int ELIDINIS_WARD_OR = ItemID.ELIDINIS_WARD_ORNAMENT;
	static final int ARCANE_SPIRIT_SHIELD = ItemID.ARCANE;

	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private ArcaneWardConfig config;
	@Inject private ConfigManager configManager;
	@Inject private OverlayManager overlayManager;
	@Inject private InventoryIconOverlay inventoryIconOverlay;

	private ItemAppearanceSnapshot fortifiedWardSnapshot;
	private ItemAppearanceSnapshot ornamentWardSnapshot;
	private ItemAppearanceSnapshot arcaneSnapshot;
	private boolean localShieldSubstituted;
	private boolean changingComposition;
	private int appliedVisualEquipmentId = -1;
	private BufferedImage originalFortifiedWardIcon;
	private BufferedImage originalOrnamentWardIcon;
	private ArcaneFortifiedModels fortifiedModels;

	@Override
	protected void startUp()
	{
		fortifiedModels = new ArcaneFortifiedModels(client);
		overlayManager.add(inventoryIconOverlay);
		clientThread.invokeLater(this::refresh);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(inventoryIconOverlay);
		clientThread.invokeLater(this::restoreEverything);
	}

	@Provides
	ArcaneWardConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ArcaneWardConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (ArcaneWardConfig.GROUP.equals(event.getGroup()))
		{
			clientThread.invokeLater(this::refresh);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(this::refresh);
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			fortifiedWardSnapshot = null;
			ornamentWardSnapshot = null;
			arcaneSnapshot = null;
			localShieldSubstituted = false;
			appliedVisualEquipmentId = -1;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.WORN)
		{
			clientThread.invokeLater(this::applyEquippedAppearance);
		}
	}

	@Subscribe
	public void onPlayerChanged(PlayerChanged event)
	{
		if (!changingComposition && event.getPlayer() == client.getLocalPlayer())
		{
			clientThread.invokeLater(this::applyEquippedAppearance);
		}
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		ShieldAppearance appearance = config.shieldAppearance();
		if (appearance.isRecoloredArcane() && localShieldSubstituted)
		{
			if (!isSupportedWard(getEquippedShieldId()))
			{
				clearSubstitutionFromActualEquipment();
				return;
			}
			Player player = client.getLocalPlayer();
			if (player != null && player.getPlayerComposition() != null)
			{
				int shieldIndex = KitType.SHIELD.getIndex();
				int[] equipment = player.getPlayerComposition().getEquipmentIds();
				if (equipment[shieldIndex] == appliedVisualEquipmentId)
				{
					fortifiedModels.paintPlayerModel(player.getModel(), appearance);
				}
			}
		}
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		if (equipment == null)
		{
			return;
		}

		Item shield = equipment.getItem(KitType.SHIELD.getIndex());
		if (shield == null || !isSupportedWard(shield.getId()))
		{
			return;
		}

		MenuEntry[] entries = event.getMenuEntries();
		for (int index = entries.length - 1; index >= 0; --index)
		{
			MenuEntry sourceEntry = entries[index];
			Widget widget = sourceEntry.getWidget();
			if (widget == null
				|| WidgetUtil.componentToInterface(widget.getId()) != InterfaceID.WORNITEMS
				|| !"Examine".equals(sourceEntry.getOption()))
			{
				continue;
			}

			Widget itemWidget = widget.getChild(1);
			if (itemWidget == null || !isSupportedWard(itemWidget.getItemId()))
			{
				continue;
			}

			ShieldAppearance next = config.shieldAppearance().next();
			client.getMenu().createMenuEntry(index)
				.setOption("Use " + next + " appearance")
				.setTarget(sourceEntry.getTarget())
				.setType(MenuAction.RUNELITE)
				.onClick(entry -> configManager.setConfiguration(
					ArcaneWardConfig.GROUP,
					"appearance",
					next.name()));
			return;
		}
	}

	private void refresh()
	{
		restoreEverything();
		fortifiedModels.load();
		captureOriginalIcons();
		if (config.shieldAppearance().usesArcaneModel())
		{
			applyInventoryAndGroundAppearance(ELIDINIS_WARD_F);
			applyInventoryAndGroundAppearance(ELIDINIS_WARD_OR);
		}
		createReplacementIcon();
		applyEquippedAppearance();
	}

	private void captureOriginalIcons()
	{
		client.getItemModelCache().reset();
		client.getItemSpriteCache().reset();
		SpritePixels fortifiedSprite = client.createItemSprite(ELIDINIS_WARD_F, 1, 1,
			SpritePixels.DEFAULT_SHADOW_COLOR, ItemQuantityMode.NEVER, false, CLIENT_DEFAULT_ZOOM);
		SpritePixels ornamentSprite = client.createItemSprite(ELIDINIS_WARD_OR, 1, 1,
			SpritePixels.DEFAULT_SHADOW_COLOR, ItemQuantityMode.NEVER, false, CLIENT_DEFAULT_ZOOM);
		originalFortifiedWardIcon = fortifiedSprite == null ? null : fortifiedSprite.toBufferedImage();
		originalOrnamentWardIcon = ornamentSprite == null ? null : ornamentSprite.toBufferedImage();
		inventoryIconOverlay.setNativeImages(originalFortifiedWardIcon, originalOrnamentWardIcon);
	}

	private void createReplacementIcon()
	{
		if (!config.shieldAppearance().usesArcaneModel())
		{
			inventoryIconOverlay.setReplacementImage(null);
			return;
		}
		client.getItemModelCache().reset();
		client.getItemSpriteCache().reset();
		SpritePixels fortifiedSprite = client.createItemSprite(ELIDINIS_WARD_F, 1, 1,
			SpritePixels.DEFAULT_SHADOW_COLOR, ItemQuantityMode.NEVER, false, CLIENT_DEFAULT_ZOOM);
		SpritePixels ornamentSprite = client.createItemSprite(ELIDINIS_WARD_OR, 1, 1,
			SpritePixels.DEFAULT_SHADOW_COLOR, ItemQuantityMode.NEVER, false, CLIENT_DEFAULT_ZOOM);
		inventoryIconOverlay.setReplacementImages(
			fortifiedSprite == null ? null : fortifiedSprite.toBufferedImage(),
			ornamentSprite == null ? null : ornamentSprite.toBufferedImage());
	}

	private void applyInventoryAndGroundAppearance(int wardId)
	{
		ShieldAppearance appearance = config.shieldAppearance();
		ItemComposition ward = client.getItemDefinition(wardId);
		ItemComposition arcane = client.getItemDefinition(ARCANE_SPIRIT_SHIELD);
		if (wardId == ELIDINIS_WARD_F && fortifiedWardSnapshot == null)
		{
			fortifiedWardSnapshot = new ItemAppearanceSnapshot(ward);
		}
		else if (wardId == ELIDINIS_WARD_OR && ornamentWardSnapshot == null)
		{
			ornamentWardSnapshot = new ItemAppearanceSnapshot(ward);
		}

		ward.setInventoryModel(arcane.getInventoryModel());
		if (appearance.isRecoloredArcane())
		{
			ward.setColorToReplace(fortifiedModels.inventoryColorsToReplace());
			ward.setColorToReplaceWith(fortifiedModels.inventoryReplacementColors(appearance));
		}
		else
		{
			ward.setColorToReplace(clone(arcane.getColorToReplace()));
			ward.setColorToReplaceWith(clone(arcane.getColorToReplaceWith()));
		}
		ward.setTextureToReplace(clone(arcane.getTextureToReplace()));
		ward.setTextureToReplaceWith(clone(arcane.getTextureToReplaceWith()));
		ward.setXan2d(arcane.getXan2d());
		ward.setYan2d(arcane.getYan2d());
		ward.setZan2d(arcane.getZan2d());
		client.getItemModelCache().reset();
		client.getItemSpriteCache().reset();
	}

	private void applyEquippedAppearance()
	{
		ShieldAppearance appearance = config.shieldAppearance();
		if (!appearance.usesArcaneModel())
		{
			clearSubstitutionFromActualEquipment();
			return;
		}

		int equippedShieldId = getEquippedShieldId();
		if (!isSupportedWard(equippedShieldId))
		{
			clearSubstitutionFromActualEquipment();
			return;
		}

		Player player = client.getLocalPlayer();
		if (player == null || player.getPlayerComposition() == null)
		{
			return;
		}

		PlayerComposition composition = player.getPlayerComposition();
		int shieldIndex = KitType.SHIELD.getIndex();
		int[] equipment = composition.getEquipmentIds();
		int current = equipment[shieldIndex];
		if (localShieldSubstituted && current == appliedVisualEquipmentId)
		{
			return;
		}
		localShieldSubstituted = false;
		appliedVisualEquipmentId = -1;
		if (appearance.isRecoloredArcane())
		{
			prepareArcaneEquipmentDefinition();
		}
		int replacement = ARCANE_SPIRIT_SHIELD + PlayerComposition.ITEM_OFFSET;
		changingComposition = true;
		try
		{
			equipment[shieldIndex] = replacement;
			composition.removeColorTextureOverride(KitType.SHIELD);
			composition.setHash();
			localShieldSubstituted = true;
			appliedVisualEquipmentId = replacement;
		}
		finally
		{
			changingComposition = false;
		}
	}

	private void restoreEverything()
	{
		inventoryIconOverlay.setReplacementImage(null);
		restoreEquippedAppearance();
		if (fortifiedWardSnapshot != null)
		{
			fortifiedWardSnapshot.restore();
			fortifiedWardSnapshot = null;
		}
		if (ornamentWardSnapshot != null)
		{
			ornamentWardSnapshot.restore();
			ornamentWardSnapshot = null;
		}
		if (arcaneSnapshot != null)
		{
			arcaneSnapshot.restore();
			arcaneSnapshot = null;
		}
		client.getItemModelCache().reset();
		client.getItemSpriteCache().reset();
	}

	private void prepareArcaneEquipmentDefinition()
	{
		ItemComposition arcane = client.getItemDefinition(ARCANE_SPIRIT_SHIELD);
		if (arcaneSnapshot == null)
		{
			arcaneSnapshot = new ItemAppearanceSnapshot(arcane);
		}
		arcane.setColorToReplace(ArcaneFortifiedPalette.equippedFind());
		arcane.setColorToReplaceWith(ArcaneFortifiedPalette.equippedOriginal());
		client.getItemModelCache().reset();
	}

	private void restoreEquippedAppearance()
	{
		clearSubstitutionFromActualEquipment();
	}

	private void clearSubstitutionFromActualEquipment()
	{
		if (!localShieldSubstituted)
		{
			return;
		}

		Player player = client.getLocalPlayer();
		if (player != null && player.getPlayerComposition() != null)
		{
			PlayerComposition composition = player.getPlayerComposition();
			int shieldIndex = KitType.SHIELD.getIndex();
			int[] equipment = composition.getEquipmentIds();
			if (equipment[shieldIndex] == appliedVisualEquipmentId)
			{
				int actualShieldId = getEquippedShieldId();
				int actualEquipmentId = equipmentIdForShield(actualShieldId);
				changingComposition = true;
				try
				{
					equipment[shieldIndex] = actualEquipmentId;
					composition.removeColorTextureOverride(KitType.SHIELD);
					composition.setHash();
				}
				finally
				{
					changingComposition = false;
				}
			}
		}
		localShieldSubstituted = false;
		appliedVisualEquipmentId = -1;
	}

	private int getEquippedShieldId()
	{
		ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		if (equipment == null)
		{
			return -1;
		}
		Item shield = equipment.getItem(KitType.SHIELD.getIndex());
		return shield == null ? -1 : shield.getId();
	}

	static boolean isSupportedWard(int itemId)
	{
		return itemId == ELIDINIS_WARD_F || itemId == ELIDINIS_WARD_OR;
	}

	static int equipmentIdForShield(int itemId)
	{
		return itemId < 0 ? 0 : itemId + PlayerComposition.ITEM_OFFSET;
	}

	private static short[] clone(short[] value)
	{
		return value == null ? null : value.clone();
	}
}
