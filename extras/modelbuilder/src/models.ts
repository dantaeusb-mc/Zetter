import {Side} from './interfaces';

export const models = {
    "1x1": {
        edges: {[Side.TOP]: true, [Side.BOTTOM]: true, [Side.LEFT]: true, [Side.RIGHT]: true}
    },
    // 1xN, N e [2..3]
    "top_u": {
        edges: {[Side.TOP]: true, [Side.BOTTOM]: false, [Side.LEFT]: true, [Side.RIGHT]: true}
    },
    "center_vertical": {
        edges: {[Side.TOP]: true, [Side.BOTTOM]: false, [Side.LEFT]: true, [Side.RIGHT]: true}
    },
    "bottom_u": {
        edges: {[Side.TOP]: false, [Side.BOTTOM]: true, [Side.LEFT]: true, [Side.RIGHT]: true}
    },
    // Nx1, N e [2..3]
    "left_u": {
        edges: {[Side.TOP]: true, [Side.BOTTOM]: true, [Side.LEFT]: true, [Side.RIGHT]: false}
    },
    "center_horizontal": {
        edges: {[Side.TOP]: true, [Side.BOTTOM]: true, [Side.LEFT]: false, [Side.RIGHT]: false}
    },
    "right_u": {
        edges: {[Side.TOP]: true, [Side.BOTTOM]: true, [Side.LEFT]: false, [Side.RIGHT]: true}
    },
    // NxM, N e [2..4], M e [2..3]
    "top_left": {
        edges: {[Side.TOP]: true, [Side.BOTTOM]: false, [Side.LEFT]: true, [Side.RIGHT]: false}
    },
    "top": {
        edges: {[Side.TOP]: true, [Side.BOTTOM]: false, [Side.LEFT]: false, [Side.RIGHT]: false}
    },
    "top_right": {
        edges: {[Side.TOP]: true, [Side.BOTTOM]: false, [Side.LEFT]: false, [Side.RIGHT]: true}
    },
    "left": {
        edges: {[Side.TOP]: false, [Side.BOTTOM]: false, [Side.LEFT]: true, [Side.RIGHT]: false}
    },
    "center": {
        edges: {[Side.TOP]: false, [Side.BOTTOM]: false, [Side.LEFT]: false, [Side.RIGHT]: false}
    },
    "right": {
        edges: {[Side.TOP]: false, [Side.BOTTOM]: false, [Side.LEFT]: false, [Side.RIGHT]: true}
    },
    "bottom_left": {
        edges: {[Side.TOP]: false, [Side.BOTTOM]: true, [Side.LEFT]: true, [Side.RIGHT]: false}
    },
    "bottom": {
        edges: {[Side.TOP]: false, [Side.BOTTOM]: true, [Side.LEFT]: false, [Side.RIGHT]: false}
    },
    "bottom_right": {
        edges: {[Side.TOP]: false, [Side.BOTTOM]: true, [Side.LEFT]: false, [Side.RIGHT]: true}
    },
}