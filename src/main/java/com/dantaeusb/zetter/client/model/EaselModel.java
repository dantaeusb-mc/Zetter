package com.dantaeusb.zetter.client.model;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.entity.item.EaselEntity;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class EaselModel<T extends EaselEntity> extends ListModel<T> {
    public static final ModelLayerLocation EASEL_BODY_LAYER = new ModelLayerLocation(new ResourceLocation(Zetter.MOD_ID, "easel"), "body_layer");

    private static final String RACK = "rack";
    private static final String CANVAS = "canvas";
    private static final String TOP_PLANK = "top";
    private static final String BACK_LEG = "base";
    private static final String FRONT_LEGS = "lid";

    private final ModelPart rack;
    private final ModelPart canvas;
    private final ModelPart topPlank;
    private final ModelPart backLeg;
    private final ModelPart frontLegs;

    public EaselModel(ModelPart part) {
        super(RenderType::entityCutoutNoCullZOffset);
        this.rack = part.getChild(RACK);
        this.canvas = part.getChild(CANVAS);
        this.topPlank = part.getChild(TOP_PLANK);
        this.backLeg = part.getChild(BACK_LEG);
        this.frontLegs = part.getChild(FRONT_LEGS);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild(
                RACK,
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(1.0F, 11.5F, 3.5F, 14.0F, 1.0F, 4.0F),
                PartPose.offset(-8.0F, 0.0F, -8.0F)
        );

        partdefinition.addOrReplaceChild(
                CANVAS,
                CubeListBuilder.create()
                        .texOffs(6, 5)
                        .addBox(0.0F, 12.0F, 3.0F, 16.0F, 18.0F, 1.0F),
                PartPose.offsetAndRotation(-8.0F, 0.0F, -8.0F, 0.1745F, 0.0F, 0.0F)
        );

        partdefinition.addOrReplaceChild(
                TOP_PLANK,
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(1.0F, 26.0F, 5.0F, 14.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(-8.0F, 0.0F, -8.0F,0.1745F, 0.0F, 0.0F)
        );

        partdefinition.addOrReplaceChild(
                BACK_LEG,
                CubeListBuilder.create()
                        .texOffs(0, 6)
                        .addBox(0.0F, 0.0F, 0.0F, 2.0F, 30.0F, 1.0F),
                PartPose.offsetAndRotation(-1.0F, 0.0F, 7.0F, -0.2182F, 0.0F, 0.0F)
        );

        partdefinition.addOrReplaceChild(
                FRONT_LEGS,
                CubeListBuilder.create()
                        .texOffs(0, 6)
                        .addBox(12.0F, 1.0F, 7.0F, 2.0F, 30.0F, 1.0F)
                        .addBox(2.0F, 1.0F, 7.0F, 2.0F, 30.0F, 1.0F),
                PartPose.offsetAndRotation(-8.0F, 0.0F, -11.0F, 0.1745F, 0.0F, 0.0F)
        );

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public Iterable<ModelPart> parts() {
        return ImmutableList.of(this.rack, this.canvas, this.topPlank, this.backLeg, this.frontLegs);
    }

    @Override
    public void setupAnim(EaselEntity entity, float p_102619_, float p_102620_, float p_102621_, float p_102622_, float p_102623_) {
    }
}
