"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    Object.defineProperty(o, k2, { enumerable: true, get: function() { return m[k]; } });
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const interfaces_1 = require("./interfaces");
const models_1 = require("./models");
const variations_1 = require("./variations");
const rimraf_1 = __importDefault(require("rimraf"));
const fs = __importStar(require("fs"));
class Box {
    constructor(from, to, textureId, edge) {
        this.from = from;
        this.to = to;
        this.textureId = textureId;
        this.faces = { top: true, bottom: true, left: true, right: true };
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
                this.to.x = this.to.x + amount;
                return;
        }
    }
    removeFace(side) {
        this.faces[side] = false;
    }
    getFaces() {
        let builtFaces = {
            [interfaces_1.Direction.NORTH]: {
                uv: this.calculateUV(interfaces_1.Direction.NORTH),
                texture: this.textureId
            }
        };
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
        if (direction == interfaces_1.Direction.NORTH) {
            switch (this.edge) {
                case interfaces_1.Side.TOP:
                    fromY = 1;
                    break;
                case interfaces_1.Side.BOTTOM:
                    fromY = 14;
                    break;
                case interfaces_1.Side.LEFT:
                    fromX = 1;
                    break;
                case interfaces_1.Side.RIGHT:
                    fromX = 14;
                    break;
            }
        }
        else {
            switch (this.edge) {
                case interfaces_1.Side.TOP:
                    fromY = 0;
                    break;
                case interfaces_1.Side.BOTTOM:
                    fromY = 15;
                    break;
                case interfaces_1.Side.LEFT:
                    fromX = 0;
                    break;
                case interfaces_1.Side.RIGHT:
                    fromX = 15;
                    break;
            }
        }
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
        }
    }
}
class Plane {
    constructor(from, to, textureId) {
        this.from = from;
        this.to = to;
        this.textureId = textureId;
    }
    getFaces() {
        return {
            [interfaces_1.Direction.NORTH]: {
                uv: [0, 0, 16, 16],
                texture: this.textureId
            },
            // Don't know why it's so weird tbh
            [interfaces_1.Direction.SOUTH]: {
                uv: [16, 0, 0, 16],
                texture: this.textureId
            },
        };
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
class FrameModel {
    constructor(edges) {
        this.parts = [];
        this.textureId = "#frame";
        this.edges = edges;
        this.connections = Object.assign({}, edges);
        Object.keys(this.edges).map((key) => this.connections[key] = !edges[key]);
        this.buildBoxes();
        this.buildBackPlane();
    }
    buildBoxes() {
        for (let edgeSide of Object.keys(this.edges)) {
            if (!this.edges[edgeSide]) {
                continue;
            }
            const box = FrameModel.getBox(edgeSide, this.textureId);
            if (edgeSide === interfaces_1.Side.LEFT || edgeSide === interfaces_1.Side.RIGHT) {
                // Glue with top edge
                if (this.hasEdge(interfaces_1.Side.TOP)) {
                    box.shrink(interfaces_1.Side.TOP, 1);
                    box.removeFace(interfaces_1.Side.TOP);
                    console.log(`Shrinking ${edgeSide} on top`);
                }
                // Glue with bottom edge
                if (this.hasEdge(interfaces_1.Side.BOTTOM)) {
                    box.shrink(interfaces_1.Side.BOTTOM, 1);
                    box.removeFace(interfaces_1.Side.BOTTOM);
                    console.log(`Shrinking ${edgeSide} on bottom`);
                }
            }
            // For fucks sake, there's still no way to iterate over enum in TS
            for (let faceSide of Object.keys(interfaces_1.Side).map(key => interfaces_1.Side[key])) {
                if (this.edgeConnected(interfaces_1.Side[edgeSide], faceSide)) {
                    box.removeFace(faceSide);
                }
            }
            this.parts.push(box);
        }
        // @todo: add back face
    }
    buildBackPlane() {
        this.back = new Plane({ x: 0, y: 0, z: 0.75 }, { x: 16, y: 16, z: 1 }, this.textureId);
    }
    toJSON() {
        return {
            elements: [this.back, ...this.parts]
        };
    }
    hasEdge(side) {
        return this.edges[side];
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
    hasConnection(side) {
        return this.connections[side];
    }
    static getBox(side, textureId) {
        switch (side) {
            case interfaces_1.Side.TOP:
                return new Box({ x: 0, y: 15, z: 0 }, { x: 16, y: 16, z: 1 }, textureId, interfaces_1.Side.TOP);
            case interfaces_1.Side.BOTTOM:
                return new Box({ x: 0, y: 0, z: 0 }, { x: 16, y: 1, z: 1 }, textureId, interfaces_1.Side.BOTTOM);
            case interfaces_1.Side.LEFT:
                return new Box({ x: 15, y: 0, z: 0 }, { x: 16, y: 16, z: 1 }, textureId, interfaces_1.Side.LEFT);
            case interfaces_1.Side.RIGHT:
                return new Box({ x: 0, y: 0, z: 0 }, { x: 1, y: 16, z: 1 }, textureId, interfaces_1.Side.RIGHT);
        }
    }
    static getBackPlane(textureId) {
        return new Box({ x: 0, y: 15, z: 0 }, { x: 16, y: 16, z: 0 }, textureId, interfaces_1.Side.TOP);
    }
}
// Build item frames
(function () {
    rimraf_1.default.sync(`result/models/`);
    fs.mkdirSync(`result/models/`);
    fs.mkdirSync(`result/models/item/`);
    for (let material of variations_1.materials) {
        console.log(`Processing ${material} icon variations`);
        for (let modelType of ["", "_plated"]) {
            const itemModel = {
                parent: "item/generated",
                textures: {
                    layer0: "zetter:item/frame",
                    layer1: "zetter:item/custom_painting_variant_missing"
                },
                overrides: [
                    { predicate: { hasPainting: 0 }, model: "zetter:item/custom_painting_variant_missing" },
                    { predicate: { hasPainting: 1 }, model: "zetter:item/custom_painting_variant_framed" }
                ]
            };
            fs.writeFileSync(`result/models/item/${material}${modelType}_frame.json`, JSON.stringify(itemModel));
        }
    }
})();
// Build parent frames
(function () {
    fs.mkdirSync(`result/models/block/`);
    fs.mkdirSync(`result/models/block/parent/`);
    for (let modelName in models_1.models) {
        const currentModel = models_1.models[modelName];
        console.log(`Processing ${modelName}`);
        const newFrameModel = new FrameModel(currentModel.edges);
        fs.writeFileSync(`result/models/block/parent/${modelName}.json`, JSON.stringify(newFrameModel));
    }
})();
// Build styled frames
(function () {
    for (let material of variations_1.materials) {
        console.log(`Processing ${material} model variation`);
        for (let modelName in models_1.models) {
            const newFrameModel = {
                parent: `zetter:block/frame/parent/${modelName}`,
                textures: {
                    particle: `block/${material}_planks`,
                    frame: `zetter:entity/frame/${material}/${modelName}`
                }
            };
            if (!fs.existsSync(`result/models/block/${material}/`)) {
                fs.mkdirSync(`result/models/block/${material}/`);
            }
            fs.writeFileSync(`result/models/block/${material}/${modelName}.json`, JSON.stringify(newFrameModel));
        }
    }
})();
// Build blockstates
(function () {
    rimraf_1.default.sync(`result/blockstates/`);
    fs.mkdirSync(`result/blockstates/`);
    for (let material of variations_1.materials) {
        console.log(`Processing ${material} state variation`);
        for (let modelName in models_1.models) {
            const newFrameBlockState = {
                "variants": {
                    "": { "model": `zetter:block/frame/${material}/${modelName}` }
                }
            };
            if (!fs.existsSync(`result/blockstates/${material}/`)) {
                fs.mkdirSync(`result/blockstates/${material}/`);
            }
            fs.writeFileSync(`result/blockstates/${material}/${modelName}.json`, JSON.stringify(newFrameBlockState));
        }
    }
})();
//# sourceMappingURL=main.js.map