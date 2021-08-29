"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Box = void 0;
const interfaces_1 = require("../interfaces");
class Box {
    constructor(from, to, textureId) {
        this.from = from;
        this.to = to;
        this.textureId = textureId;
        this.faces = { top: true, bottom: true, left: true, right: true, front: true, back: true };
    }
    removeFace(side) {
        this.faces[side] = false;
    }
    getFaces() {
        let builtFaces = {};
        for (let side in this.faces) {
            if (this.faces[side]) {
                const direction = Box.getDirectionFromSide(side);
                builtFaces[direction] = this.buildFace(direction);
            }
        }
        return builtFaces;
    }
    buildFace(direction) {
        return {
            uv: this.calculateUV(direction),
            texture: this.textureId
        };
    }
    shrink(side, amount) {
        switch (side) {
            case interfaces_1.Side.TOP:
                this.to.y = this.to.y - amount;
                return;
            case interfaces_1.Side.BOTTOM:
                this.from.y = this.from.y + amount;
                return;
            case interfaces_1.Side.LEFT:
                this.to.x = this.to.x - amount;
                return;
            case interfaces_1.Side.RIGHT:
                this.from.x = this.from.x + amount;
                return;
        }
    }
    /**
     * Basic UV wrap
     * +-+-+-+-+
     * |#|T|B|#|
     * |L|F|B|R|
     * +-+-+-+-+
     * T = top, B = bottom, L = left, etc.
     * +-+-+-+-+
     * |#|U|D|#|
     * |E|N|S|W|
     * +-+-+-+-+
     * U = up, D = down, E = east, etc.
     *
     * @param direction
     * @protected
     */
    calculateUV(direction) {
        let fromX = 0;
        let fromY = 0;
        const length = this.to.x - this.from.x;
        const height = this.to.y - this.from.y;
        const width = this.to.z - this.from.z;
        if ([interfaces_1.Direction.UP, interfaces_1.Direction.DOWN].includes(direction)) {
            fromX += width;
            if (direction == interfaces_1.Direction.DOWN) {
                fromX += length;
            }
        }
        if ([interfaces_1.Direction.EAST, interfaces_1.Direction.NORTH, interfaces_1.Direction.SOUTH, interfaces_1.Direction.WEST].includes(direction)) {
            fromY += width;
            if ([interfaces_1.Direction.NORTH, interfaces_1.Direction.SOUTH, interfaces_1.Direction.WEST].includes(direction)) {
                fromX += width;
            }
            if ([interfaces_1.Direction.SOUTH, interfaces_1.Direction.WEST].includes(direction)) {
                fromX += length;
            }
            if (direction == interfaces_1.Direction.WEST) {
                fromX += length;
            }
        }
        let toX = 0;
        let toY = 0;
        switch (direction) {
            // Y axis
            case interfaces_1.Direction.UP:
            case interfaces_1.Direction.DOWN:
                toX = fromX + length;
                toY = fromY + width;
                break;
            // Z axis
            case interfaces_1.Direction.NORTH:
            case interfaces_1.Direction.SOUTH:
                toX = fromX + length;
                toY = fromY + height;
                break;
            // X axis
            case interfaces_1.Direction.EAST:
            case interfaces_1.Direction.WEST:
                toX = fromX + width;
                toY = fromY + height;
                break;
        }
        if (toX > 16 || toY > 16) {
            console.warn(`One of the UV maps are calculated incorrectly: ${fromX}, ${fromY}, ${toX}, ${toY}`);
            console.debug(`Direction: ${direction}, from: ${JSON.stringify(this.from)}, to: ${JSON.stringify(this.to)}`);
        }
        return [fromX, fromY, toX, toY];
    }
    toJSON() {
        return {
            from: [this.from.x, this.from.y, this.from.z],
            to: [this.to.x, this.to.y, this.to.z],
            faces: this.getFaces(),
            __comment: "Box"
        };
    }
    static getDirectionFromSide(side) {
        switch (side) {
            case interfaces_1.Side.TOP:
                return interfaces_1.Direction.UP;
            case interfaces_1.Side.BOTTOM:
                return interfaces_1.Direction.DOWN;
            case interfaces_1.Side.LEFT:
                return interfaces_1.Direction.EAST;
            case interfaces_1.Side.RIGHT:
                return interfaces_1.Direction.WEST;
            case interfaces_1.Side.FRONT:
                return interfaces_1.Direction.NORTH;
            case interfaces_1.Side.BACK:
                return interfaces_1.Direction.SOUTH;
        }
    }
}
exports.Box = Box;
//# sourceMappingURL=box.js.map