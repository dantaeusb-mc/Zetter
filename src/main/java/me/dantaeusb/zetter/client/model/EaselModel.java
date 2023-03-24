package me.dantaeusb.zetter.client.model;

import com.google.common.collect.ImmutableList;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public class EaselModel<T extends EaselEntity> extends ListModel<T> {
    public static final ModelLayerLocation EASEL_BODY_LAYER = new ModelLayerLocation(new ResourceLocation(Zetter.MOD_ID, "easel"), "body_layer");

    private static final String RACK = "rack";
    private static final String TOP_PLANK = "top";
    private static final String BACK_LEG = "back";
    private static final String FRONT_LEFT_LEG = "front_left";
    private static final String FRONT_RIGHT_LEG = "front_right";

    private final ModelPart rack;
    private final ModelPart topPlank;
    private final ModelPart backLeg;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;

    public EaselModel(ModelPart part) {
        this.rack = part.getChild(RACK);
        this.topPlank = part.getChild(TOP_PLANK);
        this.backLeg = part.getChild(BACK_LEG);
        this.frontLeftLeg = part.getChild(FRONT_LEFT_LEG);
        this.frontRightLeg = part.getChild(FRONT_RIGHT_LEG);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild(
            RACK,
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(1.0F, 11.5F, 3.5F, 14.0F, 1.0F, 4.0F),
            PartPose.offset(-8.0F, 0.0F, -9.0F)
        );

        partdefinition.addOrReplaceChild(
            TOP_PLANK,
            CubeListBuilder.create()
                .texOffs(0, 5)
                .addBox(1.0F, 26.0F, 5.0F, 14.0F, 2.0F, 1.0F),
            PartPose.offsetAndRotation(-8.0F, 0.0F, -9.0F, 0.1745F, 0.0F, 0.0F)
        );

        partdefinition.addOrReplaceChild(
            FRONT_LEFT_LEG,
            CubeListBuilder.create()
                .texOffs(0, 8)
                .addBox(12.0F, 1.0F, 7.0F, 2.0F, 30.0F, 1.0F),
            PartPose.offsetAndRotation(-8.0F, 0.0F, -12.0F, 0.1745F, 0.0F, 0.0F)
        );

        partdefinition.addOrReplaceChild(
            FRONT_RIGHT_LEG,
            CubeListBuilder.create()
                .texOffs(12, 8)
                .addBox(2.0F, 1.0F, 7.0F, 2.0F, 30.0F, 1.0F),
            PartPose.offsetAndRotation(-8.0F, 0.0F, -12.0F, 0.1745F, 0.0F, 0.0F)
        );

        partdefinition.addOrReplaceChild(
            BACK_LEG,
            CubeListBuilder.create()
                .texOffs(6, 8)
                .addBox(0.0F, 0.0F, 0.0F, 2.0F, 30.0F, 1.0F),
            PartPose.offsetAndRotation(-1.0F, 0.0F, 7.0F, -0.2182F, 0.0F, 0.0F)
        );

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public Iterable<ModelPart> parts() {
        return ImmutableList.of(this.rack, this.topPlank, this.backLeg, this.frontLeftLeg, this.frontRightLeg);
    }

    @Override
    public void setupAnim(EaselEntity entity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
    }
}
