export enum Side {
    TOP = "top",
    BOTTOM = "bottom",
    LEFT = "left",
    RIGHT = "right",
    FRONT = "front",
    BACK = "back"
}

export interface Sides {
    [Side.TOP]: boolean,
    [Side.BOTTOM]: boolean,
    [Side.LEFT]: boolean,
    [Side.RIGHT]: boolean,
    [Side.FRONT]: boolean,
    [Side.BACK]: boolean,
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

export interface MinecraftChildModel {
    parent: string,
    textures?: {
        particle: string,
        frame: string
    }
}

interface MinecraftElementsModel {
    elements: MinecraftModelElement[],
    textures?: {
        particle: string,
        [key: string]: string
    }
}

export type MinecraftModel = (MinecraftElementsModel | MinecraftChildModel);

export interface MinecraftTexture {
    code: string;
    location: string;
}