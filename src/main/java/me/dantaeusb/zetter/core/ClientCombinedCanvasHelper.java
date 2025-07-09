package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * Helper to handle events and combination of the combined canvas when stitching.
 */
public class ClientCombinedCanvasHelper {
    private static final ClientCombinedCanvasHelper INSTANCE = new ClientCombinedCanvasHelper();

    private ArrayList<String> canvasCodesForCombination = new ArrayList<>();

    private ClientCombinedCanvasHelper() {
    }

    public static ClientCombinedCanvasHelper getInstance() {
        return INSTANCE;
    }

    public @Nullable CanvasData getOrRequestCombinedCanvas(Container craftingInventory, Level world) {
        if () {
        }

        CanvasRenderer.getInstance().queueCanvasTextureUpdate();
    }

    public void handleCanvasRegistration(String canvasCode, CanvasData canvasData) {
        if (!this.isCanvasExpectedForCombination(canvasCode)) {
            return;
        }


    }

    public boolean isCanvasExpectedForCombination(String canvasCode) {
        return this.canvasCodesForCombination.contains(canvasCode);
    }

    public void cleanup() {
        this.canvasCodesForCombination.clear();
    }
}
