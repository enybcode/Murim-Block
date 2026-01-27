package net.mcreator.murimblock.client.renderer;

public class CrabRenderer extends MobRenderer<CrabEntity, LivingEntityRenderState, Modelmodel> {
	private CrabEntity entity = null;

	public CrabRenderer(EntityRendererProvider.Context context) {
		super(context, new Modelmodel(context.bakeLayer(Modelmodel.LAYER_LOCATION)), 0.5f);
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	@Override
	public void extractRenderState(CrabEntity entity, LivingEntityRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		this.entity = entity;
	}

	@Override
	public ResourceLocation getTextureLocation(LivingEntityRenderState state) {
		return ResourceLocation.parse("murim_block:textures/entities/crabe.png");
	}
}