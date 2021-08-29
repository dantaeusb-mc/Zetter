"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Plane = void 0;
const interfaces_1 = require("../interfaces");
const box_1 = require("./box");
class Plane extends box_1.Box {
    calculateUV(direction) {
        let fromX = 0;
        let fromY = 0;
        let toX = 0;
        let toY = 0;
        switch (direction) {
            // Y axis
            case interfaces_1.Direction.UP:
            case interfaces_1.Direction.DOWN:
                fromX = this.from.x;
                toX = this.to.x;
                fromY = this.from.z;
                toY = this.to.z;
                break;
            // Z axis
            case interfaces_1.Direction.NORTH:
            case interfaces_1.Direction.SOUTH:
                fromX = this.from.x;
                toX = this.to.x;
                fromY = this.from.y;
                toY = this.to.y;
                break;
            // X axis
            case interfaces_1.Direction.EAST:
            case interfaces_1.Direction.WEST:
                fromX = this.from.z;
                toX = this.to.z;
                fromY = this.from.y;
                toY = this.to.y;
                break;
        }
        if (direction == interfaces_1.Direction.WEST) {
            const width = toX - fromX;
            fromX = 16 - width;
            toX = 16;
        }
        if (direction == interfaces_1.Direction.DOWN) {
            const height = toY - fromY;
            fromY = 16 - height;
            toY = 16;
        }
        // Flip X as it's flipped on backside by default
        [toX, fromX] = [fromX, toX];
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
            __comment: "Plane"
        };
    }
}
exports.Plane = Plane;
//# sourceMappingURL=plane.js.map