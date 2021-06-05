"use strict";
var _a, _b, _c, _d, _e, _f, _g, _h, _j, _k, _l, _m, _o, _p, _q, _r, _s, _t, _u, _v, _w, _x, _y, _z, _0, _1, _2, _3, _4, _5, _6, _7;
exports.__esModule = true;
exports.models = void 0;
var interfaces_1 = require("./interfaces");
exports.models = {
    "1x1": {
        edges: (_a = {}, _a[interfaces_1.Side.TOP] = true, _a[interfaces_1.Side.BOTTOM] = true, _a[interfaces_1.Side.LEFT] = true, _a[interfaces_1.Side.RIGHT] = true, _a),
        connections: (_b = {}, _b[interfaces_1.Side.TOP] = false, _b[interfaces_1.Side.BOTTOM] = false, _b[interfaces_1.Side.LEFT] = false, _b[interfaces_1.Side.RIGHT] = false, _b)
    },
    "top_u": {
        edges: (_c = {}, _c[interfaces_1.Side.TOP] = true, _c[interfaces_1.Side.BOTTOM] = false, _c[interfaces_1.Side.LEFT] = true, _c[interfaces_1.Side.RIGHT] = true, _c),
        connections: (_d = {}, _d[interfaces_1.Side.TOP] = false, _d[interfaces_1.Side.BOTTOM] = true, _d[interfaces_1.Side.LEFT] = false, _d[interfaces_1.Side.RIGHT] = false, _d)
    },
    "center_vertical": {
        edges: (_e = {}, _e[interfaces_1.Side.TOP] = true, _e[interfaces_1.Side.BOTTOM] = false, _e[interfaces_1.Side.LEFT] = true, _e[interfaces_1.Side.RIGHT] = true, _e),
        connections: (_f = {}, _f[interfaces_1.Side.TOP] = true, _f[interfaces_1.Side.BOTTOM] = true, _f[interfaces_1.Side.LEFT] = false, _f[interfaces_1.Side.RIGHT] = false, _f)
    },
    "bottom_u": {
        edges: (_g = {}, _g[interfaces_1.Side.TOP] = false, _g[interfaces_1.Side.BOTTOM] = true, _g[interfaces_1.Side.LEFT] = true, _g[interfaces_1.Side.RIGHT] = true, _g),
        connections: (_h = {}, _h[interfaces_1.Side.TOP] = true, _h[interfaces_1.Side.BOTTOM] = false, _h[interfaces_1.Side.LEFT] = false, _h[interfaces_1.Side.RIGHT] = false, _h)
    },
    "left_u": {
        edges: (_j = {}, _j[interfaces_1.Side.TOP] = true, _j[interfaces_1.Side.BOTTOM] = true, _j[interfaces_1.Side.LEFT] = true, _j[interfaces_1.Side.RIGHT] = false, _j),
        connections: (_k = {}, _k[interfaces_1.Side.TOP] = false, _k[interfaces_1.Side.BOTTOM] = false, _k[interfaces_1.Side.LEFT] = false, _k[interfaces_1.Side.RIGHT] = true, _k)
    },
    "center_horizontal": {
        edges: (_l = {}, _l[interfaces_1.Side.TOP] = true, _l[interfaces_1.Side.BOTTOM] = true, _l[interfaces_1.Side.LEFT] = false, _l[interfaces_1.Side.RIGHT] = false, _l),
        connections: (_m = {}, _m[interfaces_1.Side.TOP] = false, _m[interfaces_1.Side.BOTTOM] = false, _m[interfaces_1.Side.LEFT] = true, _m[interfaces_1.Side.RIGHT] = true, _m)
    },
    "right_u": {
        edges: (_o = {}, _o[interfaces_1.Side.TOP] = true, _o[interfaces_1.Side.BOTTOM] = true, _o[interfaces_1.Side.LEFT] = false, _o[interfaces_1.Side.RIGHT] = true, _o),
        connections: (_p = {}, _p[interfaces_1.Side.TOP] = false, _p[interfaces_1.Side.BOTTOM] = false, _p[interfaces_1.Side.LEFT] = true, _p[interfaces_1.Side.RIGHT] = false, _p)
    },
    "top_left": {
        edges: (_q = {}, _q[interfaces_1.Side.TOP] = true, _q[interfaces_1.Side.BOTTOM] = false, _q[interfaces_1.Side.LEFT] = true, _q[interfaces_1.Side.RIGHT] = false, _q),
        connections: (_r = {}, _r[interfaces_1.Side.TOP] = false, _r[interfaces_1.Side.BOTTOM] = true, _r[interfaces_1.Side.LEFT] = false, _r[interfaces_1.Side.RIGHT] = true, _r)
    },
    "top": {
        edges: (_s = {}, _s[interfaces_1.Side.TOP] = true, _s[interfaces_1.Side.BOTTOM] = false, _s[interfaces_1.Side.LEFT] = false, _s[interfaces_1.Side.RIGHT] = false, _s),
        connections: (_t = {}, _t[interfaces_1.Side.TOP] = false, _t[interfaces_1.Side.BOTTOM] = true, _t[interfaces_1.Side.LEFT] = true, _t[interfaces_1.Side.RIGHT] = true, _t)
    },
    "top_right": {
        edges: (_u = {}, _u[interfaces_1.Side.TOP] = true, _u[interfaces_1.Side.BOTTOM] = false, _u[interfaces_1.Side.LEFT] = false, _u[interfaces_1.Side.RIGHT] = true, _u),
        connections: (_v = {}, _v[interfaces_1.Side.TOP] = false, _v[interfaces_1.Side.BOTTOM] = true, _v[interfaces_1.Side.LEFT] = true, _v[interfaces_1.Side.RIGHT] = false, _v)
    },
    "left": {
        edges: (_w = {}, _w[interfaces_1.Side.TOP] = false, _w[interfaces_1.Side.BOTTOM] = false, _w[interfaces_1.Side.LEFT] = true, _w[interfaces_1.Side.RIGHT] = false, _w),
        connections: (_x = {}, _x[interfaces_1.Side.TOP] = true, _x[interfaces_1.Side.BOTTOM] = true, _x[interfaces_1.Side.LEFT] = false, _x[interfaces_1.Side.RIGHT] = true, _x)
    },
    "center": {
        edges: (_y = {}, _y[interfaces_1.Side.TOP] = false, _y[interfaces_1.Side.BOTTOM] = false, _y[interfaces_1.Side.LEFT] = false, _y[interfaces_1.Side.RIGHT] = false, _y),
        connections: (_z = {}, _z[interfaces_1.Side.TOP] = true, _z[interfaces_1.Side.BOTTOM] = true, _z[interfaces_1.Side.LEFT] = true, _z[interfaces_1.Side.RIGHT] = true, _z)
    },
    "right": {
        edges: (_0 = {}, _0[interfaces_1.Side.TOP] = false, _0[interfaces_1.Side.BOTTOM] = false, _0[interfaces_1.Side.LEFT] = false, _0[interfaces_1.Side.RIGHT] = true, _0),
        connections: (_1 = {}, _1[interfaces_1.Side.TOP] = true, _1[interfaces_1.Side.BOTTOM] = true, _1[interfaces_1.Side.LEFT] = true, _1[interfaces_1.Side.RIGHT] = false, _1)
    },
    "bottom_left": {
        edges: (_2 = {}, _2[interfaces_1.Side.TOP] = false, _2[interfaces_1.Side.BOTTOM] = true, _2[interfaces_1.Side.LEFT] = true, _2[interfaces_1.Side.RIGHT] = false, _2),
        connections: (_3 = {}, _3[interfaces_1.Side.TOP] = true, _3[interfaces_1.Side.BOTTOM] = false, _3[interfaces_1.Side.LEFT] = false, _3[interfaces_1.Side.RIGHT] = true, _3)
    },
    "bottom": {
        edges: (_4 = {}, _4[interfaces_1.Side.TOP] = false, _4[interfaces_1.Side.BOTTOM] = true, _4[interfaces_1.Side.LEFT] = false, _4[interfaces_1.Side.RIGHT] = false, _4),
        connections: (_5 = {}, _5[interfaces_1.Side.TOP] = true, _5[interfaces_1.Side.BOTTOM] = false, _5[interfaces_1.Side.LEFT] = true, _5[interfaces_1.Side.RIGHT] = true, _5)
    },
    "bottom_right": {
        edges: (_6 = {}, _6[interfaces_1.Side.TOP] = false, _6[interfaces_1.Side.BOTTOM] = true, _6[interfaces_1.Side.LEFT] = false, _6[interfaces_1.Side.RIGHT] = true, _6),
        connections: (_7 = {}, _7[interfaces_1.Side.TOP] = true, _7[interfaces_1.Side.BOTTOM] = false, _7[interfaces_1.Side.LEFT] = true, _7[interfaces_1.Side.RIGHT] = false, _7)
    }
};
