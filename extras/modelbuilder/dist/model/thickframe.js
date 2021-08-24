"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ThickFrameModel = void 0;
const interfaces_1 = require("../interfaces");
const box_1 = require("./box");
const abstract_1 = require("./abstract");
const edge_1 = require("./edge");
const plane_1 = require("./plane");
class ThickFrameModel extends abstract_1.AbstractModel {
    build() {
        for (let edgeSide of Object.keys(this.edges)) {
            if (!this.edges[edgeSide]) {
                continue;
            }
            const edge = ThickFrameModel.getEdge(edgeSide, this.textureId);
            edge.removeFace(interfaces_1.Side.BACK);
            if (edgeSide === interfaces_1.Side.LEFT || edgeSide === interfaces_1.Side.RIGHT) {
                edge.removeFace(interfaces_1.Side.TOP);
                edge.removeFace(interfaces_1.Side.BOTTOM);
                if (this.edges[interfaces_1.Side.TOP]) {
                    edge.shrink(interfaces_1.Side.TOP, 1);
                }
                if (this.edges[interfaces_1.Side.BOTTOM]) {
                    edge.shrink(interfaces_1.Side.BOTTOM, 1);
                }
            }
            if (edgeSide === interfaces_1.Side.TOP || edgeSide === interfaces_1.Side.BOTTOM) {
                edge.removeFace(interfaces_1.Side.LEFT);
                edge.removeFace(interfaces_1.Side.RIGHT);
                if (this.edges[interfaces_1.Side.LEFT]) {
                    edge.shrink(interfaces_1.Side.LEFT, 1);
                }
                if (this.edges[interfaces_1.Side.RIGHT]) {
                    edge.shrink(interfaces_1.Side.RIGHT, 1);
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
        if (this.edges[interfaces_1.Side.TOP] && this.edges[interfaces_1.Side.RIGHT]) {
            const corner = ThickFrameModel.getCorner(interfaces_1.Side.TOP, this.textureId);
            this.parts.push(corner);
        }
        if (this.edges[interfaces_1.Side.TOP] && this.edges[interfaces_1.Side.LEFT]) {
            const corner = ThickFrameModel.getCorner(interfaces_1.Side.RIGHT, this.textureId);
            this.parts.push(corner);
        }
        if (this.edges[interfaces_1.Side.BOTTOM] && this.edges[interfaces_1.Side.LEFT]) {
            const corner = ThickFrameModel.getCorner(interfaces_1.Side.BOTTOM, this.textureId);
            this.parts.push(corner);
        }
        if (this.edges[interfaces_1.Side.BOTTOM] && this.edges[interfaces_1.Side.RIGHT]) {
            const corner = ThickFrameModel.getCorner(interfaces_1.Side.LEFT, this.textureId);
            this.parts.push(corner);
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
    toJSON() {
        return {
            elements: this.parts.map(part => part.toJSON()),
            textures: {
                particle: `block/gold_block`,
                frame: `zetter:entity/frame/gold/${this.name}`,
                corner: `zetter:entity/frame/gold/corner`
            }
        };
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
                return new edge_1.Edge({ x: 0, y: 15, z: 0 }, { x: 16, y: 17, z: 1 }, textureId, interfaces_1.Side.TOP);
            case interfaces_1.Side.BOTTOM:
                return new edge_1.Edge({ x: 0, y: -1, z: 0 }, { x: 16, y: 1, z: 1 }, textureId, interfaces_1.Side.BOTTOM);
            case interfaces_1.Side.LEFT:
                return new edge_1.Edge({ x: 15, y: 0, z: 0 }, { x: 17, y: 16, z: 1 }, textureId, interfaces_1.Side.LEFT);
            case interfaces_1.Side.RIGHT:
                return new edge_1.Edge({ x: -1, y: 0, z: 0 }, { x: 1, y: 16, z: 1 }, textureId, interfaces_1.Side.RIGHT);
        }
    }
    static getCorner(side, textureId) {
        switch (side) {
            // top left
            case interfaces_1.Side.TOP:
                return new box_1.Box({ x: -2, y: 15, z: -1 }, { x: 1, y: 18, z: 1 }, "#corner");
            // top right
            case interfaces_1.Side.RIGHT:
                return new box_1.Box({ x: 15, y: 15, z: -1 }, { x: 18, y: 18, z: 1 }, "#corner");
            // bottom right
            case interfaces_1.Side.BOTTOM:
                return new box_1.Box({ x: 15, y: -2, z: -1 }, { x: 18, y: 1, z: 1 }, "#corner");
            // bottom left
            case interfaces_1.Side.LEFT:
                return new box_1.Box({ x: -2, y: -2, z: -1 }, { x: 1, y: 1, z: 1 }, "#corner");
        }
    }
}
exports.ThickFrameModel = ThickFrameModel;
//# sourceMappingURL=thickframe.js.map