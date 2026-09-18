package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.capability.canvastracker.CanvasClientTracker;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.capability.paintingregistry.PaintingRegistry;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.bus.api.IEventBus;

import java.util.function.Supplier;

public class ZetterCapabilities
{
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Zetter.MOD_ID);

    public static final Supplier<AttachmentType<CanvasTracker>> CANVAS_TRACKER = ATTACHMENT_TYPES.register("canvas_tracker", () -> AttachmentType.serializable(
            holder -> {
                if (holder instanceof Level world) {
                    CanvasTracker tracker = world.isClientSide() ? new CanvasClientTracker() : new CanvasServerTracker();
                    tracker.setLevel(world);
                    return tracker;
                }
                throw new IllegalArgumentException("CanvasTracker can only be attached to a Level");
            }
    ).build());

    public static final Supplier<AttachmentType<PaintingRegistry>> PAINTING_REGISTRY = ATTACHMENT_TYPES.register("painting_registry", () -> AttachmentType.serializable(
            holder -> {
                if (holder instanceof Level world) {
                    if (!world.isClientSide()) {
                        return new PaintingRegistry(world);
                    }
                    // For client level, return a dummy registry, as the build method requires returning a non-null object for initialization
                    return new PaintingRegistry(world);
                }
                throw new IllegalArgumentException("PaintingRegistry can only be attached to a Level");
            }
    ).build());

    public static void init(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }
}