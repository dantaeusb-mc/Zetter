package me.dantaeusb.zetter.client.model;

import com.google.common.collect.ImmutableList;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.entity.item.WallEaselEntity;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

import net.minecraft.resources.ResourceLocation;

public class WallEaselModel<T extends WallEaselEntity> extends ListModel<T> {
    public static final ModelLayerLocation EASEL_BODY_LAYER = new ModelLayerLocation(new ResourceLocation(Zetter.MOD_ID, "wall_easel"), "body_layer");

    private static final String BACK = "back";
    private static final String TOP_PLANK = "top";
    private static final String LEFT_PLANK = "left";
    private static final String RIGHT_PLANK = "right";
    private static final String BOTTOM_PLANK = "bottom";

    private final ModelPart back;
    private final ModelPart top;
    private final ModelPart left;
    private final ModelPart right;
    private final ModelPart bottom;

    public WallEaselModel(ModelPart root) {
        this.back = root.getChild(BACK);
        this.top = root.getChild(TOP_PLANK);
        this.left = root.getChild(LEFT_PLANK);
        this.right = root.getChild(RIGHT_PLANK);
        this.bottom = root.getChild(BOTTOM_PLANK);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild(
            BACK,
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-40.0F, -80.0F, -8.0F, 80.0F, 80.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F)
            );

        partdefinition.addOrReplaceChild(
            TOP_PLANK,
            CubeListBuilder.create()
                .texOffs(0, 99)
                .addBox(36.0F, -80.0F, -7.0F, 4.0F, 80.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(12, 99).addBox(-40.0F, -80.0F, -7.0F, 4.0F, 80.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(0, 81).addBox(-36.0F, -5.0F, -7.0F, 72.0F, 5.0F, 7.0F, new CubeDeformation(0.0F))
            .texOffs(0, 93).addBox(-36.0F, -80.0F, -7.0F, 72.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 256, 256);
    }

    public Iterable<ModelPart> parts() {
        return ImmutableList.of(this.back, this.top, this.left, this.right, this.bottom);
    }

    @Override
    public void setupAnim(WallEaselEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

}
