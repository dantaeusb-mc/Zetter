"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.FramelessModel = void 0;
const interfaces_1 = require("../interfaces");
const abstract_1 = require("./abstract");
const plane_1 = require("./plane");
class FramelessModel extends abstract_1.AbstractModel {
    build() {
        const box = new plane_1.Plane({ x: 0, y: 0, z: 0 }, { x: 16, y: 16, z: 1 }, this.textureId);
        for (let faceSide of Object.keys(interfaces_1.Side).map(key => interfaces_1.Side[key])) {
            if (this.hasConnection(faceSide)) {
                box.removeFace(faceSide);
            }
        }
        box.removeFace(interfaces_1.Side.FRONT);
        this.parts.push(box);
    }
    toJSON() {
        return {
            elements: this.parts.map(part => part.toJSON()),
            textures: {
                particle: `block/iron_block`,
                frame: `zetter:entity/frame/iron/${this.name}`
            }
        };
    }
}
exports.FramelessModel = FramelessModel;
//# sourceMappingURL=frameless.js.map