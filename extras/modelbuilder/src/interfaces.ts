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
    up?: MinecraftModelFace;
    down?: MinecraftModelFace;
    north?: MinecraftModelFace;
    south?: MinecraftModelFace;
    east?: MinecraftModelFace;
    west?: MinecraftModelFace;
}

export interface MinecraftModelElement {
    from: number[];
    to: number[];
    faces: MinecraftModelFaces;
    comment?: string;
}

export interface MinecraftTexture {
    code: string;
    location: string;
}