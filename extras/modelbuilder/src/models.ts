import {Side} from './interfaces';

export enum ModelKey {
    ONE_BY_ONE = "1x1",
    TOP_U = "top_u",
    CENTER_VERTICAL = "center_vertical",
    BOTTOM_U = "bottom_u",
    LEFT_U = "left_u",
    CENTER_HORIZONTAL = "center_horizontal",
    RIGHT_U = "right_u",
    TOP_LEFT = "top_left",
    TOP = "top",
    TOP_RIGHT = "top_right",
    LEFT = "left",
    CENTER = "center",
    RIGHT = "right",
    BOTTOM_LEFT = "bottom_left",
    BOTTOM = "bottom",
    BOTTOM_RIGHT = "bottom_right"
}

export type EdgeDefinition = {
    [Side.TOP]: boolean, [Side.BOTTOM]: boolean, [Side.LEFT]: boolean, [Side.RIGHT]: boolean
} | {[key in Side]?: boolean};

export interface ModelDefinition {
    edges: EdgeDefinition,
    edgesHaveBack: boolean
}

const models: {[key in ModelKey] : EdgeDefinition} = {
    [ModelKey.ONE_BY_ONE]: {[Side.TOP]: true, [Side.BOTTOM]: true, [Side.LEFT]: true, [Side.RIGHT]: true},
    // 1xN, N e [2..3]
    [ModelKey.TOP_U]: {[Side.TOP]: true, [Side.BOTTOM]: false, [Side.LEFT]: true, [Side.RIGHT]: true},
    [ModelKey.CENTER_VERTICAL]: {[Side.TOP]: true, [Side.BOTTOM]: false, [Side.LEFT]: true, [Side.RIGHT]: true},
    [ModelKey.BOTTOM_U]: {[Side.TOP]: false, [Side.BOTTOM]: true, [Side.LEFT]: true, [Side.RIGHT]: true},
    // Nx1, N e [2..3]
    [ModelKey.LEFT_U]: {[Side.TOP]: true, [Side.BOTTOM]: true, [Side.LEFT]: true, [Side.RIGHT]: false},
    [ModelKey.CENTER_HORIZONTAL]: {[Side.TOP]: true, [Side.BOTTOM]: true, [Side.LEFT]: false, [Side.RIGHT]: false},
    [ModelKey.RIGHT_U]: {[Side.TOP]: true, [Side.BOTTOM]: true, [Side.LEFT]: false, [Side.RIGHT]: true},
    // NxM, N e [2..4], M e [2..3]
    [ModelKey.TOP_LEFT]: {[Side.TOP]: true, [Side.BOTTOM]: false, [Side.LEFT]: true, [Side.RIGHT]: false},
    [ModelKey.TOP]: {[Side.TOP]: true, [Side.BOTTOM]: false, [Side.LEFT]: false, [Side.RIGHT]: false},
    [ModelKey.TOP_RIGHT]: {[Side.TOP]: true, [Side.BOTTOM]: false, [Side.LEFT]: false, [Side.RIGHT]: true},
    [ModelKey.LEFT]: {[Side.TOP]: false, [Side.BOTTOM]: false, [Side.LEFT]: true, [Side.RIGHT]: false},
    [ModelKey.CENTER]: {[Side.TOP]: false, [Side.BOTTOM]: false, [Side.LEFT]: false, [Side.RIGHT]: false},
    [ModelKey.RIGHT]: {[Side.TOP]: false, [Side.BOTTOM]: false, [Side.LEFT]: false, [Side.RIGHT]: true},
    [ModelKey.BOTTOM_LEFT]: {[Side.TOP]: false, [Side.BOTTOM]: true, [Side.LEFT]: true, [Side.RIGHT]: false},
    [ModelKey.BOTTOM]: {[Side.TOP]: false, [Side.BOTTOM]: true, [Side.LEFT]: false, [Side.RIGHT]: false},
    [ModelKey.BOTTOM_RIGHT]: {[Side.TOP]: false, [Side.BOTTOM]: true, [Side.LEFT]: false, [Side.RIGHT]: true}
}

export const getModel = function (key: ModelKey, edgesHaveBack: boolean = false): ModelDefinition {
    // we never need a front edge (part) of the frame
    return {
        edges: models[key],
        edgesHaveBack: edgesHaveBack
    };
}