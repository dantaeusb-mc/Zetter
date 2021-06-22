"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.models = void 0;
const interfaces_1 = require("./interfaces");
exports.models = {
    "1x1": {
        edges: { [interfaces_1.Side.TOP]: true, [interfaces_1.Side.BOTTOM]: true, [interfaces_1.Side.LEFT]: true, [interfaces_1.Side.RIGHT]: true }
    },
    // 1xN, N e [2..3]
    "top_u": {
        edges: { [interfaces_1.Side.TOP]: true, [interfaces_1.Side.BOTTOM]: false, [interfaces_1.Side.LEFT]: true, [interfaces_1.Side.RIGHT]: true }
    },
    "center_vertical": {
        edges: { [interfaces_1.Side.TOP]: true, [interfaces_1.Side.BOTTOM]: false, [interfaces_1.Side.LEFT]: true, [interfaces_1.Side.RIGHT]: true }
    },
    "bottom_u": {
        edges: { [interfaces_1.Side.TOP]: false, [interfaces_1.Side.BOTTOM]: true, [interfaces_1.Side.LEFT]: true, [interfaces_1.Side.RIGHT]: true }
    },
    // Nx1, N e [2..3]
    "left_u": {
        edges: { [interfaces_1.Side.TOP]: true, [interfaces_1.Side.BOTTOM]: true, [interfaces_1.Side.LEFT]: true, [interfaces_1.Side.RIGHT]: false }
    },
    "center_horizontal": {
        edges: { [interfaces_1.Side.TOP]: true, [interfaces_1.Side.BOTTOM]: true, [interfaces_1.Side.LEFT]: false, [interfaces_1.Side.RIGHT]: false }
    },
    "right_u": {
        edges: { [interfaces_1.Side.TOP]: true, [interfaces_1.Side.BOTTOM]: true, [interfaces_1.Side.LEFT]: false, [interfaces_1.Side.RIGHT]: true }
    },
    // NxM, N e [2..4], M e [2..3]
    "top_left": {
        edges: { [interfaces_1.Side.TOP]: true, [interfaces_1.Side.BOTTOM]: false, [interfaces_1.Side.LEFT]: true, [interfaces_1.Side.RIGHT]: false }
    },
    "top": {
        edges: { [interfaces_1.Side.TOP]: true, [interfaces_1.Side.BOTTOM]: false, [interfaces_1.Side.LEFT]: false, [interfaces_1.Side.RIGHT]: false }
    },
    "top_right": {
        edges: { [interfaces_1.Side.TOP]: true, [interfaces_1.Side.BOTTOM]: false, [interfaces_1.Side.LEFT]: false, [interfaces_1.Side.RIGHT]: true }
    },
    "left": {
        edges: { [interfaces_1.Side.TOP]: false, [interfaces_1.Side.BOTTOM]: false, [interfaces_1.Side.LEFT]: true, [interfaces_1.Side.RIGHT]: false }
    },
    "center": {
        edges: { [interfaces_1.Side.TOP]: false, [interfaces_1.Side.BOTTOM]: false, [interfaces_1.Side.LEFT]: false, [interfaces_1.Side.RIGHT]: false }
    },
    "right": {
        edges: { [interfaces_1.Side.TOP]: false, [interfaces_1.Side.BOTTOM]: false, [interfaces_1.Side.LEFT]: false, [interfaces_1.Side.RIGHT]: true }
    },
    "bottom_left": {
        edges: { [interfaces_1.Side.TOP]: false, [interfaces_1.Side.BOTTOM]: true, [interfaces_1.Side.LEFT]: true, [interfaces_1.Side.RIGHT]: false }
    },
    "bottom": {
        edges: { [interfaces_1.Side.TOP]: false, [interfaces_1.Side.BOTTOM]: true, [interfaces_1.Side.LEFT]: false, [interfaces_1.Side.RIGHT]: false }
    },
    "bottom_right": {
        edges: { [interfaces_1.Side.TOP]: false, [interfaces_1.Side.BOTTOM]: true, [interfaces_1.Side.LEFT]: false, [interfaces_1.Side.RIGHT]: true }
    },
};
//# sourceMappingURL=models.js.map