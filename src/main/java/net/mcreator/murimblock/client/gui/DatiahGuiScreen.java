package net.mcreator.murimblock.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.GuiGraphics;

import net.mcreator.murimblock.world.inventory.DatiahGuiMenu;
import net.mcreator.murimblock.procedures.QiBarProcedureProcedure;
import net.mcreator.murimblock.procedures.GetCultivationNameProcedure;
import net.mcreator.murimblock.init.MurimBlockModScreens;

public class DatiahGuiScreen extends AbstractContainerScreen<DatiahGuiMenu> implements MurimBlockModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;
	private ImageButton imagebutton_datian;
	private ImageButton imagebutton_skill;

	public DatiahGuiScreen(DatiahGuiMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 176;
		this.imageHeight = 166;
	}

	@Override
	public void updateMenuState(int elementType, String name, Object elementState) {
		menuStateUpdateActive = true;
		menuStateUpdateActive = false;
	}

	private static final ResourceLocation texture = ResourceLocation.parse("murim_block:textures/screens/datiah_gui.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.parse("murim_block:textures/screens/gui.png"), this.leftPos + -48, this.topPos + -11, 0, 0, 270, 179, 270, 179);
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		return super.keyPressed(key, b, c);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, QiBarProcedureProcedure.execute(entity), 96, 25, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.murim_block.datiah_gui.label_datian"), 96, 7, -12829636, false);
		guiGraphics.drawString(this.font, GetCultivationNameProcedure.execute(entity), 96, 43, -12829636, false);
	}

	@Override
	public void init() {
		super.init();
		imagebutton_datian = new ImageButton(this.leftPos + -93, this.topPos + 16, 60, 15, new WidgetSprites(ResourceLocation.parse("murim_block:textures/screens/datian.png"), ResourceLocation.parse("murim_block:textures/screens/datian.png")), e -> {
		}) {
			@Override
			public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
				guiGraphics.blit(RenderPipelines.GUI_TEXTURED, sprites.get(isActive(), isHoveredOrFocused()), getX(), getY(), 0, 0, width, height, width, height);
			}
		};
		this.addRenderableWidget(imagebutton_datian);
		imagebutton_skill = new ImageButton(this.leftPos + -93, this.topPos + 43, 60, 15, new WidgetSprites(ResourceLocation.parse("murim_block:textures/screens/skill.png"), ResourceLocation.parse("murim_block:textures/screens/skill.png")), e -> {
		}) {
			@Override
			public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
				guiGraphics.blit(RenderPipelines.GUI_TEXTURED, sprites.get(isActive(), isHoveredOrFocused()), getX(), getY(), 0, 0, width, height, width, height);
			}
		};
		this.addRenderableWidget(imagebutton_skill);
	}
}