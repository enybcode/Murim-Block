package net.mcreator.murimblock.client.gui;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

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

import net.mcreator.murimblock.world.inventory.TenchniqueguiMenu;
import net.mcreator.murimblock.procedures.DisplayswordfirstoffProcedure;
import net.mcreator.murimblock.procedures.DisplaySwordFirstProcedure;
import net.mcreator.murimblock.network.TenchniqueguiButtonMessage;
import net.mcreator.murimblock.init.MurimBlockModScreens;

public class TenchniqueguiScreen extends AbstractContainerScreen<TenchniqueguiMenu> implements MurimBlockModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;
	private ImageButton imagebutton_datian;
	private ImageButton imagebutton_skill;
	private ImageButton imagebutton_icone_tecnique_de_lepee_tome_1;
	private ImageButton imagebutton_close;

	public TenchniqueguiScreen(TenchniqueguiMenu container, Inventory inventory, Component text) {
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

	private static final ResourceLocation texture = ResourceLocation.parse("murim_block:textures/screens/tenchniquegui.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.parse("murim_block:textures/screens/gui.png"), this.leftPos + -49, this.topPos + -12, 0, 0, 270, 179, 270, 179);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.parse("murim_block:textures/screens/fondgui-tecnique.png"), this.leftPos + -48, this.topPos + -11, 0, 0, 270, 179, 270, 179);
		if (DisplayswordfirstoffProcedure.execute(entity)) {
			guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.parse("murim_block:textures/screens/competence_bloquer.png"), this.leftPos + -25, this.topPos + 27, 0, 0, 28, 28, 28, 28);
		}
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.parse("murim_block:textures/screens/competence_bloquer.png"), this.leftPos + 44, this.topPos + 27, 0, 0, 28, 28, 28, 28);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.parse("murim_block:textures/screens/competence_bloquer.png"), this.leftPos + -25, this.topPos + 67, 0, 0, 28, 28, 28, 28);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.parse("murim_block:textures/screens/competence_bloquer.png"), this.leftPos + 44, this.topPos + 67, 0, 0, 28, 28, 28, 28);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.parse("murim_block:textures/screens/competence_bloquer.png"), this.leftPos + -25, this.topPos + 109, 0, 0, 28, 28, 28, 28);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.parse("murim_block:textures/screens/competence_bloquer.png"), this.leftPos + 44, this.topPos + 109, 0, 0, 28, 28, 28, 28);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.parse("murim_block:textures/screens/competence_bloquer.png"), this.leftPos + 103, this.topPos + 27, 0, 0, 28, 28, 28, 28);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.parse("murim_block:textures/screens/competence_bloquer.png"), this.leftPos + 172, this.topPos + 27, 0, 0, 28, 28, 28, 28);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.parse("murim_block:textures/screens/competence_bloquer.png"), this.leftPos + 103, this.topPos + 67, 0, 0, 28, 28, 28, 28);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.parse("murim_block:textures/screens/competence_bloquer.png"), this.leftPos + 172, this.topPos + 67, 0, 0, 28, 28, 28, 28);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.parse("murim_block:textures/screens/competence_bloquer.png"), this.leftPos + 103, this.topPos + 109, 0, 0, 28, 28, 28, 28);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.parse("murim_block:textures/screens/competence_bloquer.png"), this.leftPos + 172, this.topPos + 109, 0, 0, 28, 28, 28, 28);
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
	}

	@Override
	public void init() {
		super.init();
		imagebutton_datian = new ImageButton(this.leftPos + -93, this.topPos + 16, 60, 15, new WidgetSprites(ResourceLocation.parse("murim_block:textures/screens/datian.png"), ResourceLocation.parse("murim_block:textures/screens/datian.png")), e -> {
			int x = TenchniqueguiScreen.this.x;
			int y = TenchniqueguiScreen.this.y;
			if (true) {
				ClientPacketDistributor.sendToServer(new TenchniqueguiButtonMessage(0, x, y, z));
				TenchniqueguiButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		}) {
			@Override
			public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
				guiGraphics.blit(RenderPipelines.GUI_TEXTURED, sprites.get(isActive(), isHoveredOrFocused()), getX(), getY(), 0, 0, width, height, width, height);
			}
		};
		this.addRenderableWidget(imagebutton_datian);
		imagebutton_skill = new ImageButton(this.leftPos + -93, this.topPos + 43, 60, 15, new WidgetSprites(ResourceLocation.parse("murim_block:textures/screens/skill.png"), ResourceLocation.parse("murim_block:textures/screens/skill.png")), e -> {
			int x = TenchniqueguiScreen.this.x;
			int y = TenchniqueguiScreen.this.y;
			if (true) {
				ClientPacketDistributor.sendToServer(new TenchniqueguiButtonMessage(1, x, y, z));
				TenchniqueguiButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		}) {
			@Override
			public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
				guiGraphics.blit(RenderPipelines.GUI_TEXTURED, sprites.get(isActive(), isHoveredOrFocused()), getX(), getY(), 0, 0, width, height, width, height);
			}
		};
		this.addRenderableWidget(imagebutton_skill);
		imagebutton_icone_tecnique_de_lepee_tome_1 = new ImageButton(this.leftPos + -25, this.topPos + 27, 28, 28,
				new WidgetSprites(ResourceLocation.parse("murim_block:textures/screens/icone_tecnique_de_lepee_tome_1.png"), ResourceLocation.parse("murim_block:textures/screens/icone_tecnique_de_lepee_tome_1.png")), e -> {
				}) {
			@Override
			public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
				int x = TenchniqueguiScreen.this.x;
				int y = TenchniqueguiScreen.this.y;
				if (DisplaySwordFirstProcedure.execute(entity))
					guiGraphics.blit(RenderPipelines.GUI_TEXTURED, sprites.get(isActive(), isHoveredOrFocused()), getX(), getY(), 0, 0, width, height, width, height);
			}
		};
		this.addRenderableWidget(imagebutton_icone_tecnique_de_lepee_tome_1);
		imagebutton_close = new ImageButton(this.leftPos + -48, this.topPos + -11, 10, 10, new WidgetSprites(ResourceLocation.parse("murim_block:textures/screens/close.png"), ResourceLocation.parse("murim_block:textures/screens/close.png")), e -> {
			int x = TenchniqueguiScreen.this.x;
			int y = TenchniqueguiScreen.this.y;
			if (true) {
				ClientPacketDistributor.sendToServer(new TenchniqueguiButtonMessage(3, x, y, z));
				TenchniqueguiButtonMessage.handleButtonAction(entity, 3, x, y, z);
			}
		}) {
			@Override
			public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
				guiGraphics.blit(RenderPipelines.GUI_TEXTURED, sprites.get(isActive(), isHoveredOrFocused()), getX(), getY(), 0, 0, width, height, width, height);
			}
		};
		this.addRenderableWidget(imagebutton_close);
	}
}