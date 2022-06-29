package me.dantaeusb.zetter.menu.painting;

import net.minecraft.util.Tuple;

import java.util.Stack;
import java.util.UUID;

public class PaintingAction {
    public final UUID authorId;
    public final String toolCode;
    public final Long startTime;

    private boolean committed = false;
    private Stack<Tuple<Float, Float>> coordinates = new Stack<>();

    public PaintingAction(UUID authorId, String toolCode, Long startTime) {
        this.authorId = authorId;
        this.toolCode = toolCode;
        this.startTime = startTime;
    }

    /**
     * Add action coordinates (like path of the brush)
     * Returns true if can add, false if it should be committed
     * and new action stack prepared
     *
     * @param posX
     * @param posY
     * @return
     */
    public boolean addCoordinates(float posX, float posY) {
        // @todo: check that it's not the time to submit action
        this.coordinates.push(new Tuple<>(posX, posY));

        return true;
    }

    public void commit() {
        this.committed = true;
    }

    public boolean isCommitted() {
        return this.committed;
    }
}
