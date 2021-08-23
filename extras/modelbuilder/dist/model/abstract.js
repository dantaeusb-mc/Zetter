"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AbstractModel = void 0;
class AbstractModel {
    constructor(name, edges, textureId = "#frame") {
        this.parts = [];
        this.name = name;
        this.edges = edges;
        this.textureId = textureId;
        this.build();
    }
    hasEdge(side) {
        return this.edges[side];
    }
    toJSON() {
        return {
            elements: this.parts.map(part => part.toJSON())
        };
    }
    hasConnection(side) {
        return !this.edges[side];
    }
}
exports.AbstractModel = AbstractModel;
//# sourceMappingURL=abstract.js.map