package me.dantaeusb.zetter.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.entity.item.StandBoardEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

/**
 * Left as Blockbench exports it, so that it can be exported again over the top.
 * That space is the living entity one — upside down, and facing along X rather
 * than Z — and {@link me.dantaeusb.zetter.client.renderer.entity.StandBoardRenderer}
 * turns it into the easel model space before rendering.
 *
 * The slate is the "board" part, and has its own place on the texture; the chalk is
 * drawn over it by the renderer, not by the model.
 */
public class StandBoardModel<T extends StandBoardEntity> extends EntityModel<T> {
    public static final ModelLayerLocation STAND_BOARD_BODY_LAYER = new ModelLayerLocation(new ResourceLocation(Zetter.MOD_ID, "standing_board"), "main");

    private final ModelPart front;
    private final ModelPart back;
    private final ModelPart main;

    public StandBoardModel(ModelPart root) {
        this.front = root.getChild("front");
        this.back = root.getChild("back");
        this.main = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition front = partdefinition.addOrReplaceChild("front", CubeListBuilder.create(), PartPose.offset(0.0F, 9.0F, 0.0F));

        front.addOrReplaceChild("stand_r1", CubeListBuilder.create().texOffs(26, 31).addBox(0.0F, 16.0F, -1.0F, 1.0F, 1.0F, 12.0F, new CubeDeformation(0.0F))
            .texOffs(24, 44).addBox(0.0F, 16.0F, 11.0F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(20, 43).addBox(0.0F, 16.0F, -2.0F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(16, 43).addBox(0.0F, 1.0F, 12.0F, 1.0F, 23.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(12, 43).addBox(0.0F, 1.0F, -3.0F, 1.0F, 23.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(28, 0).addBox(0.0F, 0.0F, -3.0F, 1.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -8.0F, -5.0F, 0.0F, 0.0F, -0.2618F));

        PartDefinition back = partdefinition.addOrReplaceChild("back", CubeListBuilder.create(), PartPose.offset(0.0F, 1.0F, 8.0F));

        back.addOrReplaceChild("stand_r2", CubeListBuilder.create().texOffs(6, 43).addBox(0.0F, 0.0F, -1.0F, 1.0F, 24.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -15.0F, 3.1416F, 0.0F, -2.8798F));

        back.addOrReplaceChild("stand_r3", CubeListBuilder.create().texOffs(0, 43).addBox(0.0F, 0.0F, -1.0F, 1.0F, 24.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, 3.1416F, 0.0F, -2.8798F));

        back.addOrReplaceChild("bottom_r1", CubeListBuilder.create().texOffs(0, 29).addBox(0.0F, 15.0F, -1.0F, 1.0F, 2.0F, 12.0F, new CubeDeformation(0.0F))
            .texOffs(28, 17).addBox(0.0F, 0.0F, -1.0F, 1.0F, 2.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -3.0F, 3.1416F, 0.0F, -2.8798F));

        PartDefinition main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(28, 44).addBox(-1.0F, -24.0F, -7.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(36, 44).addBox(-1.0F, -24.0F, 5.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        main.addOrReplaceChild("board_r1", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 1.0F, -6.0F, 0.0F, 15.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -23.0F, -1.0F, 0.0F, 0.0F, -0.2618F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.front.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.back.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
