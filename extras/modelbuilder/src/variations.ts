export const materialVariations: { [key: string]: MaterialVariation } = {
    "acacia": {canHavePlate: true, isWood: true},
    "birch": {canHavePlate: true, isWood: true},
    "dark_oak": {canHavePlate: true, isWood: true},
    "jungle": {canHavePlate: true, isWood: true},
    "oak": {canHavePlate: true, isWood: true},
    "spruce": {canHavePlate: true, isWood: true},
    "crimson": {canHavePlate: true, isWood: true},
    "warped": {canHavePlate: true, isWood: true},
    "mangrove": {canHavePlate: true, isWood: true},
    "iron": {canHavePlate: false, isWood: false},
    "gold": {canHavePlate: true, isWood: false}
};

export interface MaterialVariation {
    canHavePlate: boolean,
    isWood: boolean
}

export enum plateVariations {
    BASIC = "basic",
    PLATED = "plated"
}

export enum paintingVariations {
    EMPTY = "empty",
    PAINTING = "painting"
}