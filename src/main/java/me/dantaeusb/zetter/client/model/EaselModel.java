package me.dantaeusb.zetter.client.model;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.Model;
import net.minecraft.client.renderer.model.ModelRenderer;

public class EaselModel extends Model {
    private final ModelRenderer rack;
    private final ModelRenderer topPlank;
    private final ModelRenderer backLeg;
    private final ModelRenderer frontLeftLeg;
    private final ModelRenderer frontRightLeg;

    public EaselModel() {
        super(RenderType::entityCutoutNoCull);

        this.texWidth = 64;
        this.texHeight = 64;

        this.rack = new ModelRenderer(this, 0, 0);
        this.rack
            .addBox(1.0F, 11.5F, 3.5F, 14.0F, 1.0F, 4.0F)
            .texOffs(0, 0);

        this.rack.setPos(-8.0F, 0.0F, -9.0F);

        this.topPlank = new ModelRenderer(this, 0, 0);
        this.topPlank
            .addBox(1.0F, 26.0F, 5.0F, 14.0F, 2.0F, 1.0F)
            .texOffs(0, 5);

        this.topPlank.setPos(-8.0F, 0.0F, -9.0F);
        setRotationAngle(this.topPlank, 0.1745F, 0.0F, 0.0F);

        this.backLeg = new ModelRenderer(this, 0, 0);
        this.backLeg
            .addBox(0.0F, 0.0F, 0.0F, 2.0F, 30.0F, 1.0F)
            .texOffs(6, 8);

        this.backLeg.setPos(-1.0F, 0.0F, 7.0F);
        setRotationAngle(this.backLeg, -0.2182F, 0.0F, 0.0F);

        this.frontLeftLeg = new ModelRenderer(this, 0, 0);
        this.frontLeftLeg
            .addBox(12.0F, 1.0F, 7.0F, 2.0F, 30.0F, 1.0F)
            .texOffs(0, 8);

        this.frontLeftLeg.setPos(-8.0F, 0.0F, -12.0F);
        setRotationAngle(this.frontLeftLeg, 0.1745F, 0.0F, 0.0F);

        this.frontRightLeg = new ModelRenderer(this, 0, 0);
        this.frontRightLeg
            .addBox(2.0F, 1.0F, 7.0F, 2.0F, 30.0F, 1.0F)
            .texOffs(12, 8);

        this.frontRightLeg.setPos(-8.0F, 0.0F, -12.0F);
        setRotationAngle(this.frontRightLeg, 0.1745F, 0.0F, 0.0F);
    }

    public static void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.xRot = x;
        modelRenderer.yRot = y;
        modelRenderer.zRot = z;
    }

    @Override
    public void renderToBuffer(MatrixStack poseStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.rack.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.topPlank.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.backLeg.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.frontLeftLeg.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.frontRightLeg.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
