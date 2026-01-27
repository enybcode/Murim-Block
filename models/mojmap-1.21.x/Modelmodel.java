// Made with Blockbench 5.0.7
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class Modelmodel<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "model"), "main");
	private final ModelPart corps;
	private final ModelPart brasgauche;
	private final ModelPart brasdroit;
	private final ModelPart jambegauche;
	private final ModelPart jambedroit;

	public Modelmodel(ModelPart root) {
		this.corps = root.getChild("corps");
		this.brasgauche = root.getChild("brasgauche");
		this.brasdroit = this.brasgauche.getChild("brasdroit");
		this.jambegauche = root.getChild("jambegauche");
		this.jambedroit = root.getChild("jambedroit");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition corps = partdefinition.addOrReplaceChild("corps", CubeListBuilder.create().texOffs(0, 0).addBox(
				-4.0F, -5.0F, -2.0F, 5.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition brasgauche = partdefinition.addOrReplaceChild("brasgauche",
				CubeListBuilder.create().texOffs(0, 15)
						.addBox(-1.0F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(8, 13)
						.addBox(-1.0F, -1.0F, 1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(0, 13)
						.addBox(-1.0F, -1.0F, 2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(12, 11)
						.addBox(-1.0F, 0.0F, 2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(4, 13)
						.addBox(-1.0F, 1.0F, 2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(12, 13)
						.addBox(-1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(4, 15)
						.addBox(-1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(12, 9)
						.addBox(-1.0F, 0.0F, 3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(12, 7)
						.addBox(-1.0F, 0.0F, 4.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offset(2.0F, 20.0F, -5.0F));

		PartDefinition brasdroit = brasgauche.addOrReplaceChild("brasdroit",
				CubeListBuilder.create().texOffs(4, 11)
						.addBox(-1.0F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(8, 9)
						.addBox(-1.0F, -1.0F, 1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(0, 9)
						.addBox(-1.0F, -1.0F, 2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(8, 7)
						.addBox(-1.0F, 0.0F, 2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(4, 9)
						.addBox(-1.0F, 1.0F, 2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(0, 11)
						.addBox(-1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(8, 11)
						.addBox(-1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(0, 7)
						.addBox(-1.0F, 0.0F, 3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(4, 7)
						.addBox(-1.0F, 0.0F, 4.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-6.0F, 0.0F, 0.0F));

		PartDefinition jambegauche = partdefinition.addOrReplaceChild("jambegauche",
				CubeListBuilder.create().texOffs(16, 7)
						.addBox(0.0F, -2.0F, -3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(16, 9)
						.addBox(1.0F, -1.0F, -3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(8, 15)
						.addBox(0.0F, -2.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(12, 15)
						.addBox(1.0F, -1.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-1.0F, 24.0F, 1.0F));

		PartDefinition jambedroit = partdefinition.addOrReplaceChild("jambedroit",
				CubeListBuilder.create().texOffs(16, 11)
						.addBox(-1.0F, -2.0F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(16, 13)
						.addBox(-2.0F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(16, 15)
						.addBox(-1.0F, -2.0F, 2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(0, 17)
						.addBox(-2.0F, -1.0F, 2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-2.0F, 24.0F, -2.0F));

		return LayerDefinition.create(meshdefinition, 32, 32);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
		corps.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		brasgauche.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		jambegauche.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		jambedroit.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {
		this.brasgauche.xRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
		this.brasdroit.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * limbSwingAmount;
		this.jambedroit.xRot = Mth.cos(limbSwing * 1.0F) * 1.0F * limbSwingAmount;
		this.jambegauche.xRot = Mth.cos(limbSwing * 1.0F) * 1.0F * limbSwingAmount;
	}
}