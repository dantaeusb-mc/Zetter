"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Edge = void 0;
const interfaces_1 = require("../interfaces");
const box_1 = require("./box");
class Edge extends box_1.Box {
    constructor(from, to, textureId, edge) {
        super(from, to, textureId);
        this.edge = edge;
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
        if (direction == interfaces_1.Direction.NORTH) {
            switch (this.edge) {
                case interfaces_1.Side.TOP:
                    fromY = 1;
                    break;
                case interfaces_1.Side.BOTTOM:
                    fromY = 16 - height - 1;
                    break;
                case interfaces_1.Side.LEFT:
                    fromX = 1;
                    break;
                case interfaces_1.Side.RIGHT:
                    fromX = 16 - width - 1;
                    break;
            }
        }
        else {
            switch (this.edge) {
                case interfaces_1.Side.TOP:
                    fromY = 0;
                    break;
                case interfaces_1.Side.BOTTOM:
                    fromY = 16 - height;
                    break;
                case interfaces_1.Side.LEFT:
                    fromX = 0;
                    break;
                case interfaces_1.Side.RIGHT:
                    fromX = 16 - width;
                    break;
            }
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
            __comment: this.edge
        };
    }
}
exports.Edge = Edge;
//# sourceMappingURL=edge.js.map