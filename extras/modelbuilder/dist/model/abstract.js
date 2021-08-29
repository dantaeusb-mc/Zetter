"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AbstractModel = void 0;
const interfaces_1 = require("../interfaces");
class AbstractModel {
    constructor(name, model, textureId = "#frame") {
        this.parts = [];
        this.name = name;
        this.model = model;
        this.textureId = textureId;
        this.build();
    }
    hasEdge(side) {
        return this.model.edges[side];
    }
    toJSON() {
        return {
            elements: this.parts.map(part => part.toJSON())
        };
    }
    hasConnection(side) {
        if (side == interfaces_1.Side.BACK) {
            return !this.model.edgesHaveBack;
        }
        else if (side == interfaces_1.Side.FRONT) {
            return true;
        }
        return !this.model.edges[side];
    }
}
exports.AbstractModel = AbstractModel;
//# sourceMappingURL=abstract.js.map