package com.dantaeusb.zetter.client.renderer.entity.model;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.entity.model.QuadrupedModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SmallFrameModel<T extends Entity> extends EntityModel<T> {
    protected ModelRenderer topEdgeModel;
    protected ModelRenderer rightEdgeModel;
    protected ModelRenderer bottomEdgeModel;
    protected ModelRenderer leftEdgeModel;
    protected ModelRenderer backModel;

    public SmallFrameModel(int width, int height) {
        super();

        // Changing rotation point actually affects box position
        this.topEdgeModel = new ModelRenderer(this, 0, 0);
        this.topEdgeModel.setTextureSize(128, 128);
        this.topEdgeModel.addBox(-(width / 2.0F) - 1.0F, (height / 2.0F) - 1.0F, -1.0F, width + 2.0F, 2.0F, 2.0F);
        //this.topEdgeModel.setRotationPoint(0.0F, (height / 2.0F), 1.0F);

        this.rightEdgeModel = new ModelRenderer(this, 0, 6);
        this.rightEdgeModel.setTextureSize(128, 128);
        this.rightEdgeModel.addBox(-(width / 2.0F) - 1.0F, -(height / 2.0F) + 1.0F, -1.0F, 2.0F, height - 2.0F, 2.0F);
        //this.rightEdgeModel.setRotationPoint(0.0F, (height / 2.0F), 1.0F);

        this.bottomEdgeModel = new ModelRenderer(this, 0, 0);
        this.bottomEdgeModel.setTextureSize(128, 128);
        this.bottomEdgeModel.addBox(-(width / 2.0F) - 1.0F, -(height / 2.0F) - 1.0F, -1.0F, width + 2.0F, 2.0F, 2.0F);
        //this.bottomEdgeModel.setRotationPoint(0.0F, -(height / 2.0F), 1.0F);

        this.leftEdgeModel = new ModelRenderer(this, 0, 6);
        this.leftEdgeModel.setTextureSize(128, 128);
        this.leftEdgeModel.addBox((width / 2.0F) - 1.0F, -(height / 2.0F) + 1.0F, -1.0F, 2.0F, height - 2.0F, 2.0F);
        //this.leftEdgeModel.setRotationPoint((width / 2.0F), 0.0F, 1.0F);

        this.backModel = new ModelRenderer(this, 6, 6);
        this.backModel.setTextureSize(128, 128);
        this.backModel.addBox(-(width / 2.0F) + 1.0F, -(height / 2.0F) + 1.0F, 1.0F, width - 2.0F, height - 2.0F, 0.0F);
    }

    public void render(MatrixStack matrixStackIn, IVertexBuilder bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
        this.getParts().forEach((modelRenderer) -> {
            modelRenderer.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
        });
    }

    protected Iterable<ModelRenderer> getParts() {
        return ImmutableList.of(this.topEdgeModel, this.rightEdgeModel, this.bottomEdgeModel, this.leftEdgeModel, this.backModel);
    }

    /**
     * Sets this entity's model rotation angles
     */
    public void setRotationAngles(T entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    }
}