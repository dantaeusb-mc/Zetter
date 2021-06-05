import {Side, Sides} from './interfaces';

interface Model {
    edges: Sides,
    connections: Sides
}

// @todo: I think I'm starting to see a pattern... and we can reduce this definition twice
export const models = {
    "1x1": {
        edges: {[Side.TOP]: true, [Side.BOTTOM]: true, [Side.LEFT]: true, [Side.RIGHT]: true},
        connections: {[Side.TOP]: false, [Side.BOTTOM]: false, [Side.LEFT]: false, [Side.RIGHT]: false}
    },
    // 1xN, N e [2..3]
    "top_u": {
        edges: {[Side.TOP]: true, [Side.BOTTOM]: false, [Side.LEFT]: true, [Side.RIGHT]: true},
        connections: {[Side.TOP]: false, [Side.BOTTOM]: true, [Side.LEFT]: false, [Side.RIGHT]: false}
    },
    "center_vertical": {
        edges: {[Side.TOP]: true, [Side.BOTTOM]: false, [Side.LEFT]: true, [Side.RIGHT]: true},
        connections: {[Side.TOP]: true, [Side.BOTTOM]: true, [Side.LEFT]: false, [Side.RIGHT]: false}
    },
    "bottom_u": {
        edges: {[Side.TOP]: false, [Side.BOTTOM]: true, [Side.LEFT]: true, [Side.RIGHT]: true},
        connections: {[Side.TOP]: true, [Side.BOTTOM]: false, [Side.LEFT]: false, [Side.RIGHT]: false}
    },
    // Nx1, N e [2..3]
    "left_u": {
        edges: {[Side.TOP]: true, [Side.BOTTOM]: true, [Side.LEFT]: true, [Side.RIGHT]: false},
        connections: {[Side.TOP]: false, [Side.BOTTOM]: false, [Side.LEFT]: false, [Side.RIGHT]: true}
    },
    "center_horizontal": {
        edges: {[Side.TOP]: true, [Side.BOTTOM]: true, [Side.LEFT]: false, [Side.RIGHT]: false},
        connections: {[Side.TOP]: false, [Side.BOTTOM]: false, [Side.LEFT]: true, [Side.RIGHT]: true}
    },
    "right_u": {
        edges: {[Side.TOP]: true, [Side.BOTTOM]: true, [Side.LEFT]: false, [Side.RIGHT]: true},
        connections: {[Side.TOP]: false, [Side.BOTTOM]: false, [Side.LEFT]: true, [Side.RIGHT]: false}
    },
    // NxM, N e [2..4], M e [2..3]
    "top_left": {
        edges: {[Side.TOP]: true, [Side.BOTTOM]: false, [Side.LEFT]: true, [Side.RIGHT]: false},
        connections: {[Side.TOP]: false, [Side.BOTTOM]: true, [Side.LEFT]: false, [Side.RIGHT]: true}
    },
    "top": {
        edges: {[Side.TOP]: true, [Side.BOTTOM]: false, [Side.LEFT]: false, [Side.RIGHT]: false},
        connections: {[Side.TOP]: false, [Side.BOTTOM]: true, [Side.LEFT]: true, [Side.RIGHT]: true}
    },
    "top_right": {
        edges: {[Side.TOP]: true, [Side.BOTTOM]: false, [Side.LEFT]: false, [Side.RIGHT]: true},
        connections: {[Side.TOP]: false, [Side.BOTTOM]: true, [Side.LEFT]: true, [Side.RIGHT]: false}
    },
    "left": {
        edges: {[Side.TOP]: false, [Side.BOTTOM]: false, [Side.LEFT]: true, [Side.RIGHT]: false},
        connections: {[Side.TOP]: true, [Side.BOTTOM]: true, [Side.LEFT]: false, [Side.RIGHT]: true}
    },
    "center": {
        edges: {[Side.TOP]: false, [Side.BOTTOM]: false, [Side.LEFT]: false, [Side.RIGHT]: false},
        connections: {[Side.TOP]: true, [Side.BOTTOM]: true, [Side.LEFT]: true, [Side.RIGHT]: true}
    },
    "right": {
        edges: {[Side.TOP]: false, [Side.BOTTOM]: false, [Side.LEFT]: false, [Side.RIGHT]: true},
        connections: {[Side.TOP]: true, [Side.BOTTOM]: true, [Side.LEFT]: true, [Side.RIGHT]: false}
    },
    "bottom_left": {
        edges: {[Side.TOP]: false, [Side.BOTTOM]: true, [Side.LEFT]: true, [Side.RIGHT]: false},
        connections: {[Side.TOP]: true, [Side.BOTTOM]: false, [Side.LEFT]: false, [Side.RIGHT]: true}
    },
    "bottom": {
        edges: {[Side.TOP]: false, [Side.BOTTOM]: true, [Side.LEFT]: false, [Side.RIGHT]: false},
        connections: {[Side.TOP]: true, [Side.BOTTOM]: false, [Side.LEFT]: true, [Side.RIGHT]: true}
    },
    "bottom_right": {
        edges: {[Side.TOP]: false, [Side.BOTTOM]: true, [Side.LEFT]: false, [Side.RIGHT]: true},
        connections: {[Side.TOP]: true, [Side.BOTTOM]: false, [Side.LEFT]: true, [Side.RIGHT]: false}
    },
}