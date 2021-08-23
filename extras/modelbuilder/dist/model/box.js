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
    calculateUV(direction) {
        let fromX = 0;
        let fromY = 0;
        let width = 0;
        let height = 0;
        switch (direction) {
            // Y axis
            case interfaces_1.Direction.UP:
            case interfaces_1.Direction.DOWN:
                width = this.to.x - this.from.x;
                height = this.to.z - this.from.z;
                break;
            // Z axis
            case interfaces_1.Direction.NORTH:
            case interfaces_1.Direction.SOUTH:
                width = this.to.x - this.from.x;
                height = this.to.y - this.from.y;
                break;
            // X axis
            case interfaces_1.Direction.EAST:
            case interfaces_1.Direction.WEST:
                width = this.to.z - this.from.z;
                height = this.to.y - this.from.y;
                break;
        }
        if (direction == interfaces_1.Direction.DOWN) {
            fromY = 16 - height;
        }
        if (direction === interfaces_1.Direction.WEST) {
            fromX = 16 - width;
        }
        const toX = fromX + width;
        const toY = fromY + height;
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