package me.dantaeusb.zetter.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.CanvasItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Objects;

public class CanvasItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static ItemRenderer renderer = null;
    private static BakedModel bakedModel = null;

    public CanvasItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet)
    {
        super(dispatcher, modelSet);
    }

    @Override
    public void renderByItem(ItemStack itemStack, ItemTransforms.TransformType transformType, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay)
    {
        if (renderer == null) { renderer = Minecraft.getInstance().getItemRenderer(); }

        poseStack.pushPose();

        final boolean rightMain = Minecraft.getInstance().player.getMainArm() == HumanoidArm.RIGHT;
        final boolean mainHand = Minecraft.getInstance().player.getMainHandItem() == itemStack;
        final boolean offHand = Minecraft.getInstance().player.getOffhandItem() == itemStack;
        final boolean inHand = mainHand || offHand;

        // Scale and prepare
        /*poseStack.scale(scaleFactor, scaleFactor, scaleFactor);
        poseStack.mulPose(Vector3f.XP.rotationDegrees(180.0F));
        poseStack.translate(0, -16.0D, 0);*/

        //renderer.render(itemStack, transformType, offHand == rightMain, poseStack, buffer, combinedLight, combinedOverlay, bakedModel);

        ClientLevel hahaLevelInWithoutLevelRenderer = Minecraft.getInstance().level;
        String canvasCode = CanvasItem.getCanvasCode(itemStack);

        if (inHand) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            poseStack.scale(0.38F, 0.38F, 0.38F);
            poseStack.translate(-0.5D, -0.5D, 0.0D);
            poseStack.scale(0.0078125F, 0.0078125F, 0.0078125F);
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            poseStack.scale(0.38F, 0.38F, 0.38F);
        }

        if (hahaLevelInWithoutLevelRenderer != null && !itemStack.isEmpty() && canvasCode != null && CanvasItem.getCanvasData(itemStack, Minecraft.getInstance().level) != null) {
            CanvasRenderer.getInstance().renderCanvas(poseStack, buffer, canvasCode, CanvasItem.getCanvasData(itemStack, Minecraft.getInstance().level), combinedLight);
        }

        poseStack.popPose();
    }

    public static void handleModel(Map<ResourceLocation, BakedModel> modelRegistry)
    {
        ResourceLocation location = new ModelResourceLocation(Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(ZetterItems.CANVAS.get())), "inventory");
        bakedModel = modelRegistry.get(location);
    }
}