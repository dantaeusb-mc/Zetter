"use strict";
var __assign = (this && this.__assign) || function () {
    __assign = Object.assign || function(t) {
        for (var s, i = 1, n = arguments.length; i < n; i++) {
            s = arguments[i];
            for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p))
                t[p] = s[p];
        }
        return t;
    };
    return __assign.apply(this, arguments);
};
exports.__esModule = true;
var interfaces_1 = require("./interfaces");
var models_1 = require("./models");
var variations_1 = require("./variations");
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
    };
    Box.prototype.removeFace = function (side) {
        this.faces[side] = false;
    };
    Box.prototype.getFaces = function () {
        var builtFaces = {
            north: {
                uv: this.calculateUV(interfaces_1.Direction.NORTH),
                texture: this.textureId
            }
        };
        for (var side in this.faces) {
            if (this.faces[side]) {
                var direction = Box.getDirectionFromSide(side);
                builtFaces[direction] = this.buildFace(direction);
            }
        }
        return builtFaces;
    };
    Box.prototype.buildFace = function (direction) {
        return {
            uv: this.calculateUV(direction),
            texture: this.textureId
        };
    };
    Box.prototype.calculateUV = function (direction) {
        var fromX = 0;
        var fromY = 0;
        var width = 0;
        var height = 0;
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
            case interfaces_1.Direction.UP:
            case interfaces_1.Direction.DOWN:
                width = this.to.x - this.from.x;
                height = this.to.z - this.from.z;
                break;
            case interfaces_1.Direction.NORTH:
            case interfaces_1.Direction.SOUTH:
                width = this.to.x - this.from.x;
                height = this.to.y - this.from.y;
                break;
            case interfaces_1.Direction.EAST:
            case interfaces_1.Direction.WEST:
                width = this.to.z - this.from.z;
                height = this.to.y - this.from.y;
                break;
        }
        var toX = fromX + width;
        var toY = fromY + height;
        if (toX > 16 || toY > 16) {
            console.warn("One of the UV maps are calculated incorrectly: " + fromX + ", " + fromY + ", " + toX + ", " + toY);
            console.debug("Direction: " + direction + ", from: " + JSON.stringify(this.from) + ", to: " + JSON.stringify(this.to));
        }
        return [fromX, fromY, toX, toY];
    };
    Box.prototype.toJSON = function () {
        return {
            from: [this.from.x, this.from.y, this.from.z],
            to: [this.to.x, this.to.y, this.to.z],
            faces: this.getFaces(),
            comment: this.edge
        };
    };
    Box.getDirectionFromSide = function (side) {
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
    };
    return Box;
}());
var FrameModel = (function () {
    function FrameModel(edges) {
        var _this = this;
        this.parts = [];
        this.textureId = "#frame";
        this.edges = edges;
        this.connections = __assign({}, edges);
        Object.keys(this.edges).map(function (key) { return _this.connections[key] = !edges[key]; });
        this.buildBoxes();
    }
    FrameModel.prototype.buildBoxes = function () {
        for (var _i = 0, _a = Object.keys(this.edges); _i < _a.length; _i++) {
            var edgeSide = _a[_i];
            if (!this.edges[edgeSide]) {
                continue;
            }
            var box = FrameModel.getBox(edgeSide, this.textureId);
            if (edgeSide === interfaces_1.Side.LEFT || edgeSide === interfaces_1.Side.RIGHT) {
                if (this.hasEdge(interfaces_1.Side.TOP)) {
                    box.shrink(interfaces_1.Side.TOP, 1);
                    box.removeFace(interfaces_1.Side.TOP);
                    console.log("Shrinking " + edgeSide + " on top");
                }
                if (this.hasEdge(interfaces_1.Side.BOTTOM)) {
                    box.shrink(interfaces_1.Side.BOTTOM, 1);
                    box.removeFace(interfaces_1.Side.BOTTOM);
                    console.log("Shrinking " + edgeSide + " on bottom");
                }
            }
            for (var _b = 0, _c = Object.keys(interfaces_1.Side).map(function (key) { return interfaces_1.Side[key]; }); _b < _c.length; _b++) {
                var faceSide = _c[_b];
                if (this.edgeConnected(interfaces_1.Side[edgeSide], faceSide)) {
                    box.removeFace(faceSide);
                }
            }
            this.parts.push(box);
        }
    };
    FrameModel.prototype.toJSON = function () {
        return {
            elements: this.parts
        };
    };
    FrameModel.prototype.hasEdge = function (side) {
        return this.edges[side];
    };
    FrameModel.prototype.edgeConnected = function (edgeSide, faceSide) {
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
    FrameModel.prototype.hasConnection = function (side) {
        return this.connections[side];
    };
    FrameModel.getBox = function (side, textureId) {
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
    };
    return FrameModel;
}());
(function () {
    if (!fs.existsSync("result/model/")) {
        fs.mkdirSync("result/model/");
    }
    if (!fs.existsSync("result/model/parent/")) {
        fs.mkdirSync("result/model/parent/");
    }
    for (var modelName in models_1.models) {
        var currentModel = models_1.models[modelName];
        console.log("Processing " + modelName);
        var newFrameModel = new FrameModel(currentModel.edges);
        fs.writeFileSync("result/model/parent/" + modelName + ".json", JSON.stringify(newFrameModel));
    }
})();
(function () {
    for (var _i = 0, materials_1 = variations_1.materials; _i < materials_1.length; _i++) {
        var material = materials_1[_i];
        console.log("Processing " + material + " model variation");
        for (var modelName in models_1.models) {
            var newFrameModel = {
                parent: "zetter:block/frame/parent/" + modelName,
                textures: {
                    particle: "block/" + material + "_planks",
                    frame: "zetter:paintings/entity/frame/" + material + "/" + modelName
                }
            };
            if (!fs.existsSync("result/model/" + material + "/")) {
                fs.mkdirSync("result/model/" + material + "/");
            }
            fs.writeFileSync("result/model/" + material + "/" + modelName + ".json", JSON.stringify(newFrameModel));
        }
    }
})();
(function () {
    if (!fs.existsSync("result/blockstates/")) {
        fs.mkdirSync("result/blockstates/");
    }
    for (var _i = 0, materials_2 = variations_1.materials; _i < materials_2.length; _i++) {
        var material = materials_2[_i];
        console.log("Processing " + material + " state variation");
        for (var modelName in models_1.models) {
            var newFrameBlockState = {
                "variants": {
                    "": { "model": "zetter:block/frame/" + material + "/" + modelName }
                }
            };
            if (!fs.existsSync("result/blockstates/" + material + "/")) {
                fs.mkdirSync("result/blockstates/" + material + "/");
            }
            fs.writeFileSync("result/blockstates/" + material + "/" + modelName + ".json", JSON.stringify(newFrameBlockState));
        }
    }
})();
