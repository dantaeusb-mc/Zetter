package me.dantaeusb.zetter.client.renderer.entity;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.model.WallEaselModel;
import me.dantaeusb.zetter.entity.item.WallEaselEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class WallEaselRenderer extends EntityWithCanvasRenderer<WallEaselEntity> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(Zetter.MOD_ID, "textures/entity/wall_easel.png");

    public WallEaselRenderer(EntityRendererProvider.Context context) {
        super(context, new WallEaselModel<>(context.bakeLayer(WallEaselModel.WALL_EASEL_BODY_LAYER)), TEXTURE);
    }
}
