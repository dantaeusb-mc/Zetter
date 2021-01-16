package com.dantaeusb.immersivemp.state.properties;


import net.minecraft.block.WallHeight;
import net.minecraft.state.*;
import net.minecraft.state.properties.*;
import net.minecraft.util.Direction;
import net.minecraft.world.gen.feature.jigsaw.JigsawOrientation;

import java.util.Collection;
import java.util.Optional;

public class LockBlockStateProperties {
    public static final BooleanProperty LOCKED = BooleanProperty.create("locked");
}
