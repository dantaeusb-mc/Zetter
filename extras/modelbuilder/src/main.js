"use strict";
exports.__esModule = true;
var interfaces_1 = require("./interfaces");
var models_1 = require("./models");
var fs = require("fs");
var Box = (function () {
    function Box(from, to, textureId, edge) {
        this.from = from;
        this.to = to;
        this.textureId = textureId;
        this.faces = { top: true, bottom: true, left: true, right: true };
        this.edge = edge;
    }
    Box.prototype.shrink = function (side, amount) {
    };
    Box.prototype.removeFace = function (side) {
        this.faces[side] = false;
    };
    Box.prototype.getFaces = function () {
        var builtFaces = {
            north: {
                uv: [0, 0, 0, 0],
                texture: this.textureId
            }
        };
        for (var side in this.faces) {
            if (this.faces[side]) {
                builtFaces[Box.getFaceKeyFromSide(side)] = this.buildFace();
            }
        }
        return builtFaces;
    };
    Box.prototype.buildFace = function () {
        return {
            uv: [0, 0, this.getWidth(), this.getHeight()],
            texture: this.textureId
        };
    };
    Box.prototype.getWidth = function () {
        return this.to.x - this.from.x;
    };
    Box.prototype.getHeight = function () {
        return this.to.y - this.from.y;
    };
    Box.prototype.toJSON = function () {
        return {
            from: [this.from.x, this.from.y, this.from.z],
            to: [this.to.x, this.to.y, this.to.z],
            faces: this.getFaces(),
            comment: this.edge
        };
    };
    Box.getFaceKeyFromSide = function (side) {
        switch (side) {
            case interfaces_1.Side.TOP:
                return "up";
            case interfaces_1.Side.BOTTOM:
                return "down";
            case interfaces_1.Side.LEFT:
                return "east";
            case interfaces_1.Side.RIGHT:
                return "west";
        }
    };
    return Box;
}());
var FramePart = (function () {
    function FramePart(edges, connections) {
        this.parts = [];
        this.edges = edges;
        this.connections = connections;
        this.buildBoxes();
    }
    FramePart.prototype.buildBoxes = function () {
        for (var edgeSide in this.edges) {
            if (!this.edges[edgeSide]) {
                continue;
            }
            var box = FramePart.getBox(edgeSide, this.textureId);
            if (interfaces_1.Side[edgeSide] === interfaces_1.Side.LEFT || interfaces_1.Side[edgeSide] === interfaces_1.Side.RIGHT) {
                if (this.hasEdge(interfaces_1.Side.TOP)) {
                    box.shrink(interfaces_1.Side.TOP, 1);
                    box.removeFace(interfaces_1.Side.TOP);
                }
                if (this.hasEdge(interfaces_1.Side.BOTTOM)) {
                    box.shrink(interfaces_1.Side.BOTTOM, 1);
                    box.removeFace(interfaces_1.Side.BOTTOM);
                }
            }
            for (var _i = 0, _a = Object.keys(interfaces_1.Side).map(function (key) { return interfaces_1.Side[key]; }); _i < _a.length; _i++) {
                var faceSide = _a[_i];
                if (this.edgeConnected(interfaces_1.Side[edgeSide], faceSide)) {
                    box.removeFace(faceSide);
                }
            }
            this.parts.push(box);
        }
    };
    FramePart.prototype.toJSON = function () {
        return {
            textures: {
                particle: "block/oak_planks",
                frame: "block/oak_planks"
            },
            elements: this.parts
        };
    };
    FramePart.prototype.hasEdge = function (side) {
        return this.edges[side];
    };
    FramePart.prototype.edgeConnected = function (edgeSide, faceSide) {
        switch (edgeSide) {
            case interfaces_1.Side.TOP:
            case interfaces_1.Side.BOTTOM:
                return (faceSide == interfaces_1.Side.RIGHT && this.hasConnection(interfaces_1.Side.RIGHT))
                    || (faceSide == interfaces_1.Side.LEFT && this.hasConnection(interfaces_1.Side.LEFT));
            case interfaces_1.Side.LEFT:
            case interfaces_1.Side.RIGHT:
                return (faceSide == interfaces_1.Side.TOP && this.hasConnection(interfaces_1.Side.TOP))
                    || (faceSide == interfaces_1.Side.BOTTOM && this.hasConnection(interfaces_1.Side.BOTTOM));
        }
    };
    FramePart.prototype.hasConnection = function (side) {
        return this.connections[side];
    };
    FramePart.getBox = function (side, textureId) {
        switch (side) {
            case interfaces_1.Side.TOP:
                return new Box({ x: 0, y: 15, z: 0 }, { x: 16, y: 16, z: 1 }, textureId, interfaces_1.Side.TOP);
            case interfaces_1.Side.BOTTOM:
                return new Box({ x: 0, y: 0, z: 0 }, { x: 16, y: 1, z: 1 }, textureId, interfaces_1.Side.BOTTOM);
            case interfaces_1.Side.LEFT:
                return new Box({ x: 15, y: 0, z: 0 }, { x: 16, y: 15, z: 1 }, textureId, interfaces_1.Side.LEFT);
            case interfaces_1.Side.RIGHT:
                return new Box({ x: 0, y: 1, z: 0 }, { x: 1, y: 16, z: 1 }, textureId, interfaces_1.Side.RIGHT);
        }
    };
    return FramePart;
}());
var FrameModelsBuilder = (function () {
    function FrameModelsBuilder(particleTexture, frameTexture) {
        var builtModels = [];
        for (var modelName in models_1.models) {
            var currentModel = models_1.models[modelName];
            console.log(modelName);
            console.log(JSON.stringify(currentModel));
            var newFramePart = new FramePart(currentModel.edges, currentModel.connections);
            builtModels[modelName] = newFramePart;
            fs.writeFileSync("result/" + modelName + ".json", JSON.stringify(newFramePart));
        }
        console.log(JSON.stringify(builtModels));
    }
    return FrameModelsBuilder;
}());
new FrameModelsBuilder("block/dark_oak_planks", "zetter:paintings/entity/frame/dark_oak/bottom_right");
