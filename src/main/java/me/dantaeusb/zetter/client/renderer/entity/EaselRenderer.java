package me.dantaeusb.zetter.client.renderer.entity;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.model.EaselModel;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class EaselRenderer extends EntityWithCanvasRenderer<EaselEntity> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(Zetter.MOD_ID, "textures/entity/easel.png");

    public EaselRenderer(EntityRendererProvider.Context context) {
        super(context, new EaselModel<>(context.bakeLayer(EaselModel.EASEL_BODY_LAYER)), TEXTURE);
    }
}
