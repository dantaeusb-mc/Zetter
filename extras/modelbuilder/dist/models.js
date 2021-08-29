"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getModel = exports.ModelKey = void 0;
const interfaces_1 = require("./interfaces");
var ModelKey;
(function (ModelKey) {
    ModelKey["ONE_BY_ONE"] = "1x1";
    ModelKey["TOP_U"] = "top_u";
    ModelKey["CENTER_VERTICAL"] = "center_vertical";
    ModelKey["BOTTOM_U"] = "bottom_u";
    ModelKey["LEFT_U"] = "left_u";
    ModelKey["CENTER_HORIZONTAL"] = "center_horizontal";
    ModelKey["RIGHT_U"] = "right_u";
    ModelKey["TOP_LEFT"] = "top_left";
    ModelKey["TOP"] = "top";
    ModelKey["TOP_RIGHT"] = "top_right";
    ModelKey["LEFT"] = "left";
    ModelKey["CENTER"] = "center";
    ModelKey["RIGHT"] = "right";
    ModelKey["BOTTOM_LEFT"] = "bottom_left";
    ModelKey["BOTTOM"] = "bottom";
    ModelKey["BOTTOM_RIGHT"] = "bottom_right";
})(ModelKey = exports.ModelKey || (exports.ModelKey = {}));
const models = {
    [ModelKey.ONE_BY_ONE]: { [interfaces_1.Side.TOP]: true, [interfaces_1.Side.BOTTOM]: true, [interfaces_1.Side.LEFT]: true, [interfaces_1.Side.RIGHT]: true },
    // 1xN, N e [2..3]
    [ModelKey.TOP_U]: { [interfaces_1.Side.TOP]: true, [interfaces_1.Side.BOTTOM]: false, [interfaces_1.Side.LEFT]: true, [interfaces_1.Side.RIGHT]: true },
    [ModelKey.CENTER_VERTICAL]: { [interfaces_1.Side.TOP]: true, [interfaces_1.Side.BOTTOM]: false, [interfaces_1.Side.LEFT]: true, [interfaces_1.Side.RIGHT]: true },
    [ModelKey.BOTTOM_U]: { [interfaces_1.Side.TOP]: false, [interfaces_1.Side.BOTTOM]: true, [interfaces_1.Side.LEFT]: true, [interfaces_1.Side.RIGHT]: true },
    // Nx1, N e [2..3]
    [ModelKey.LEFT_U]: { [interfaces_1.Side.TOP]: true, [interfaces_1.Side.BOTTOM]: true, [interfaces_1.Side.LEFT]: true, [interfaces_1.Side.RIGHT]: false },
    [ModelKey.CENTER_HORIZONTAL]: { [interfaces_1.Side.TOP]: true, [interfaces_1.Side.BOTTOM]: true, [interfaces_1.Side.LEFT]: false, [interfaces_1.Side.RIGHT]: false },
    [ModelKey.RIGHT_U]: { [interfaces_1.Side.TOP]: true, [interfaces_1.Side.BOTTOM]: true, [interfaces_1.Side.LEFT]: false, [interfaces_1.Side.RIGHT]: true },
    // NxM, N e [2..4], M e [2..3]
    [ModelKey.TOP_LEFT]: { [interfaces_1.Side.TOP]: true, [interfaces_1.Side.BOTTOM]: false, [interfaces_1.Side.LEFT]: true, [interfaces_1.Side.RIGHT]: false },
    [ModelKey.TOP]: { [interfaces_1.Side.TOP]: true, [interfaces_1.Side.BOTTOM]: false, [interfaces_1.Side.LEFT]: false, [interfaces_1.Side.RIGHT]: false },
    [ModelKey.TOP_RIGHT]: { [interfaces_1.Side.TOP]: true, [interfaces_1.Side.BOTTOM]: false, [interfaces_1.Side.LEFT]: false, [interfaces_1.Side.RIGHT]: true },
    [ModelKey.LEFT]: { [interfaces_1.Side.TOP]: false, [interfaces_1.Side.BOTTOM]: false, [interfaces_1.Side.LEFT]: true, [interfaces_1.Side.RIGHT]: false },
    [ModelKey.CENTER]: { [interfaces_1.Side.TOP]: false, [interfaces_1.Side.BOTTOM]: false, [interfaces_1.Side.LEFT]: false, [interfaces_1.Side.RIGHT]: false },
    [ModelKey.RIGHT]: { [interfaces_1.Side.TOP]: false, [interfaces_1.Side.BOTTOM]: false, [interfaces_1.Side.LEFT]: false, [interfaces_1.Side.RIGHT]: true },
    [ModelKey.BOTTOM_LEFT]: { [interfaces_1.Side.TOP]: false, [interfaces_1.Side.BOTTOM]: true, [interfaces_1.Side.LEFT]: true, [interfaces_1.Side.RIGHT]: false },
    [ModelKey.BOTTOM]: { [interfaces_1.Side.TOP]: false, [interfaces_1.Side.BOTTOM]: true, [interfaces_1.Side.LEFT]: false, [interfaces_1.Side.RIGHT]: false },
    [ModelKey.BOTTOM_RIGHT]: { [interfaces_1.Side.TOP]: false, [interfaces_1.Side.BOTTOM]: true, [interfaces_1.Side.LEFT]: false, [interfaces_1.Side.RIGHT]: true }
};
const getModel = function (key, edgesHaveBack = false) {
    // we never need a front edge (part) of the frame
    return {
        edges: models[key],
        edgesHaveBack: edgesHaveBack
    };
};
exports.getModel = getModel;
//# sourceMappingURL=models.js.map