"use strict";
var _a, _b, _c, _d, _e, _f, _g, _h, _j, _k, _l, _m, _o, _p, _q, _r;
exports.__esModule = true;
exports.models = void 0;
var interfaces_1 = require("./interfaces");
exports.models = {
    "1x1": {
        edges: (_a = {}, _a[interfaces_1.Side.TOP] = true, _a[interfaces_1.Side.BOTTOM] = true, _a[interfaces_1.Side.LEFT] = true, _a[interfaces_1.Side.RIGHT] = true, _a)
    },
    "top_u": {
        edges: (_b = {}, _b[interfaces_1.Side.TOP] = true, _b[interfaces_1.Side.BOTTOM] = false, _b[interfaces_1.Side.LEFT] = true, _b[interfaces_1.Side.RIGHT] = true, _b)
    },
    "center_vertical": {
        edges: (_c = {}, _c[interfaces_1.Side.TOP] = true, _c[interfaces_1.Side.BOTTOM] = false, _c[interfaces_1.Side.LEFT] = true, _c[interfaces_1.Side.RIGHT] = true, _c)
    },
    "bottom_u": {
        edges: (_d = {}, _d[interfaces_1.Side.TOP] = false, _d[interfaces_1.Side.BOTTOM] = true, _d[interfaces_1.Side.LEFT] = true, _d[interfaces_1.Side.RIGHT] = true, _d)
    },
    "left_u": {
        edges: (_e = {}, _e[interfaces_1.Side.TOP] = true, _e[interfaces_1.Side.BOTTOM] = true, _e[interfaces_1.Side.LEFT] = true, _e[interfaces_1.Side.RIGHT] = false, _e)
    },
    "center_horizontal": {
        edges: (_f = {}, _f[interfaces_1.Side.TOP] = true, _f[interfaces_1.Side.BOTTOM] = true, _f[interfaces_1.Side.LEFT] = false, _f[interfaces_1.Side.RIGHT] = false, _f)
    },
    "right_u": {
        edges: (_g = {}, _g[interfaces_1.Side.TOP] = true, _g[interfaces_1.Side.BOTTOM] = true, _g[interfaces_1.Side.LEFT] = false, _g[interfaces_1.Side.RIGHT] = true, _g)
    },
    "top_left": {
        edges: (_h = {}, _h[interfaces_1.Side.TOP] = true, _h[interfaces_1.Side.BOTTOM] = false, _h[interfaces_1.Side.LEFT] = true, _h[interfaces_1.Side.RIGHT] = false, _h)
    },
    "top": {
        edges: (_j = {}, _j[interfaces_1.Side.TOP] = true, _j[interfaces_1.Side.BOTTOM] = false, _j[interfaces_1.Side.LEFT] = false, _j[interfaces_1.Side.RIGHT] = false, _j)
    },
    "top_right": {
        edges: (_k = {}, _k[interfaces_1.Side.TOP] = true, _k[interfaces_1.Side.BOTTOM] = false, _k[interfaces_1.Side.LEFT] = false, _k[interfaces_1.Side.RIGHT] = true, _k)
    },
    "left": {
        edges: (_l = {}, _l[interfaces_1.Side.TOP] = false, _l[interfaces_1.Side.BOTTOM] = false, _l[interfaces_1.Side.LEFT] = true, _l[interfaces_1.Side.RIGHT] = false, _l)
    },
    "center": {
        edges: (_m = {}, _m[interfaces_1.Side.TOP] = false, _m[interfaces_1.Side.BOTTOM] = false, _m[interfaces_1.Side.LEFT] = false, _m[interfaces_1.Side.RIGHT] = false, _m)
    },
    "right": {
        edges: (_o = {}, _o[interfaces_1.Side.TOP] = false, _o[interfaces_1.Side.BOTTOM] = false, _o[interfaces_1.Side.LEFT] = false, _o[interfaces_1.Side.RIGHT] = true, _o)
    },
    "bottom_left": {
        edges: (_p = {}, _p[interfaces_1.Side.TOP] = false, _p[interfaces_1.Side.BOTTOM] = true, _p[interfaces_1.Side.LEFT] = true, _p[interfaces_1.Side.RIGHT] = false, _p)
    },
    "bottom": {
        edges: (_q = {}, _q[interfaces_1.Side.TOP] = false, _q[interfaces_1.Side.BOTTOM] = true, _q[interfaces_1.Side.LEFT] = false, _q[interfaces_1.Side.RIGHT] = false, _q)
    },
    "bottom_right": {
        edges: (_r = {}, _r[interfaces_1.Side.TOP] = false, _r[interfaces_1.Side.BOTTOM] = true, _r[interfaces_1.Side.LEFT] = false, _r[interfaces_1.Side.RIGHT] = true, _r)
    }
};
