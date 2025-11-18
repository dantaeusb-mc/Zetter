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
    public static final ModelLayerLocation WALL_EASEL_BODY_LAYER = new ModelLayerLocation(new ResourceLocation(Zetter.MOD_ID, "wall_easel"), "body_layer");

    private static final String BACK_HORIZONTAL_SUPPORT = "back_horizontal_support";
    private static final String BACK_VERTICAL_SUPPORT = "back_vertical_support";
    private static final String TOP_PLANK = "top";
    private static final String BOTTOM_PLANK = "bottom";

    private final ModelPart backHorizontal;
    private final ModelPart backVertical;
    private final ModelPart top;
    private final ModelPart bottom;

    public WallEaselModel(ModelPart root) {
        this.backHorizontal = root.getChild(BACK_HORIZONTAL_SUPPORT);
        this.backVertical = root.getChild(BACK_VERTICAL_SUPPORT);
        this.top = root.getChild(TOP_PLANK);
        this.bottom = root.getChild(BOTTOM_PLANK);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild(
            BACK_HORIZONTAL_SUPPORT,
            CubeListBuilder.create()
                .texOffs(0, 6).
                addBox(-36.0F, -5.0F, -7.0F, 72.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 13)
                .addBox(-36.0F, -80.0F, -7.0F, 72.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 80.0F, 0.0F)
        );

        partdefinition.addOrReplaceChild(
            BACK_VERTICAL_SUPPORT,
            CubeListBuilder.create()
                .texOffs(0, 36)
                .addBox(27.0F, -80.0F, -5.0F, 4.0F, 80.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(12, 36)
                .addBox(-30.0F, -80.0F, -5.0F, 4.0F, 80.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(24, 36)
                .addBox(-3.0F, -80.0F, -5.0F, 4.0F, 80.0F, 2.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 80.0F, 0.0F)
        );

        partdefinition.addOrReplaceChild(
            TOP_PLANK,
            CubeListBuilder.create()
                .texOffs(0, 20)
                .addBox(-36.0F, -80.0F, -3.0F, 72.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 32)
                .addBox(-36.0F, -76.0F, -3.0F, 72.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 80.0F, 0.0F)
        );

        partdefinition.addOrReplaceChild(
            BOTTOM_PLANK,
            CubeListBuilder.create()
                .texOffs(0, 26)
                .addBox(-36.0F, -4.0F, -3.0F, 72.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0)
                .addBox(-36.0F, -5.0F, -3.0F, 72.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 80.0F, 0.0F)
        );

        return LayerDefinition.create(meshdefinition, 256, 256);
    }

    public Iterable<ModelPart> parts() {
        return ImmutableList.of(this.backHorizontal, this.backVertical, this.top, this.bottom);
    }

    @Override
    public void setupAnim(WallEaselEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

}
