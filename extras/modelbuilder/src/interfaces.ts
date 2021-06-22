export enum Side {
    TOP = "top",
    BOTTOM = "bottom",
    LEFT = "left",
    RIGHT = "right"
}

export interface Sides {
    [Side.TOP]: boolean,
    [Side.BOTTOM]: boolean,
    [Side.LEFT]: boolean,
    [Side.RIGHT]: boolean,
}

export enum Direction {
    UP = "up",
    DOWN = "down",
    NORTH = "north",
    SOUTH = "south",
    EAST = "east",
    WEST = "west"
}

export interface Vector3i {
    x: number;
    y: number;
    z: number;
}

export interface MinecraftModelFace {
    uv: number[];
    texture: string;
}

export interface MinecraftModelFaces {
    [Direction.UP]?: MinecraftModelFace;
    [Direction.DOWN]?: MinecraftModelFace;
    [Direction.NORTH]?: MinecraftModelFace;
    [Direction.SOUTH]?: MinecraftModelFace;
    [Direction.EAST]?: MinecraftModelFace;
    [Direction.WEST]?: MinecraftModelFace;
}

export interface MinecraftModelElement {
    from: number[];
    to: number[];
    faces: MinecraftModelFaces;
    __comment?: string;
}

export interface MinecraftModelItemOverride {
    predicate: { [key: string]: number },
    model: string
}

export interface MinecraftModelItem {
    parent: string,
    textures: {
        [key: string]: string
    },
    overrides?: MinecraftModelItemOverride[],
    __comment?: string
}

export interface MinecraftTexture {
    code: string;
    location: string;
}