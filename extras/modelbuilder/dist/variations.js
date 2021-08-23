"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.paintingVariations = exports.plateVariations = exports.materialVariations = void 0;
exports.materialVariations = {
    "acacia": { canHavePlate: true, isWood: true },
    "birch": { canHavePlate: true, isWood: true },
    "dark_oak": { canHavePlate: true, isWood: true },
    "jungle": { canHavePlate: true, isWood: true },
    "oak": { canHavePlate: true, isWood: true },
    "spruce": { canHavePlate: true, isWood: true },
    "crimson": { canHavePlate: true, isWood: true },
    "warped": { canHavePlate: true, isWood: true },
    "iron": { canHavePlate: false, isWood: false },
    "gold": { canHavePlate: true, isWood: false }
};
var plateVariations;
(function (plateVariations) {
    plateVariations["BASIC"] = "basic";
    plateVariations["PLATED"] = "plated";
})(plateVariations = exports.plateVariations || (exports.plateVariations = {}));
var paintingVariations;
(function (paintingVariations) {
    paintingVariations["EMPTY"] = "empty";
    paintingVariations["PAINTING"] = "painting";
})(paintingVariations = exports.paintingVariations || (exports.paintingVariations = {}));
//# sourceMappingURL=variations.js.map