"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.FrameModel = void 0;
const interfaces_1 = require("../interfaces");
const abstract_1 = require("./abstract");
const edge_1 = require("./edge");
const plane_1 = require("./plane");
class FrameModel extends abstract_1.AbstractModel {
    build() {
        for (let edgeSide of Object.keys(this.model.edges)) {
            if (!this.model.edges[edgeSide]) {
                continue;
            }
            const edge = FrameModel.getEdge(edgeSide, this.textureId);
            edge.removeFace(interfaces_1.Side.BACK);
            if (edgeSide === interfaces_1.Side.LEFT || edgeSide === interfaces_1.Side.RIGHT) {
                // Glue with top edge
                if (this.hasEdge(interfaces_1.Side.TOP)) {
                    edge.shrink(interfaces_1.Side.TOP, 1);
                    edge.removeFace(interfaces_1.Side.TOP);
                }
                // Glue with bottom edge
                if (this.hasEdge(interfaces_1.Side.BOTTOM)) {
                    edge.shrink(interfaces_1.Side.BOTTOM, 1);
                    edge.removeFace(interfaces_1.Side.BOTTOM);
                }
            }
            // For fucks sake, there's still no way to iterate over enum in TS
            for (let faceSide of Object.keys(interfaces_1.Side).map(key => interfaces_1.Side[key])) {
                if (this.edgeConnected(interfaces_1.Side[edgeSide], faceSide)) {
                    edge.removeFace(faceSide);
                }
            }
            this.parts.push(edge);
        }
        // Only back face
        const back = new plane_1.Plane({ x: 0, y: 0, z: 1 }, { x: 16, y: 16, z: 1 }, this.textureId);
        back.removeFace(interfaces_1.Side.TOP);
        back.removeFace(interfaces_1.Side.LEFT);
        back.removeFace(interfaces_1.Side.RIGHT);
        back.removeFace(interfaces_1.Side.BOTTOM);
        back.removeFace(interfaces_1.Side.FRONT);
        this.parts.push(back);
    }
    edgeConnected(edgeSide, faceSide) {
        switch (edgeSide) {
            case interfaces_1.Side.TOP:
            case interfaces_1.Side.BOTTOM:
                // Example: our right side connected for top edge if we have connection on right
                return (faceSide == interfaces_1.Side.RIGHT && this.hasConnection(interfaces_1.Side.RIGHT))
                    || (faceSide == interfaces_1.Side.LEFT && this.hasConnection(interfaces_1.Side.LEFT));
            case interfaces_1.Side.LEFT:
            case interfaces_1.Side.RIGHT:
                return (faceSide == interfaces_1.Side.TOP && this.hasConnection(interfaces_1.Side.TOP))
                    || (faceSide == interfaces_1.Side.BOTTOM && this.hasConnection(interfaces_1.Side.BOTTOM));
        }
    }
    static getEdge(side, textureId) {
        switch (side) {
            case interfaces_1.Side.TOP:
                return new edge_1.Edge({ x: 0, y: 15, z: 0 }, { x: 16, y: 16, z: 1 }, textureId, interfaces_1.Side.TOP);
            case interfaces_1.Side.BOTTOM:
                return new edge_1.Edge({ x: 0, y: 0, z: 0 }, { x: 16, y: 1, z: 1 }, textureId, interfaces_1.Side.BOTTOM);
            case interfaces_1.Side.LEFT:
                return new edge_1.Edge({ x: 15, y: 0, z: 0 }, { x: 16, y: 16, z: 1 }, textureId, interfaces_1.Side.LEFT);
            case interfaces_1.Side.RIGHT:
                return new edge_1.Edge({ x: 0, y: 0, z: 0 }, { x: 1, y: 16, z: 1 }, textureId, interfaces_1.Side.RIGHT);
        }
    }
}
exports.FrameModel = FrameModel;
//# sourceMappingURL=frame.js.map