import {
    Direction,
    MinecraftModelElement,
    MinecraftModelFace,
    MinecraftModelFaces,
    Side,
    Sides,
    Vector3i
} from "../interfaces";

export class Box {
    protected from: Vector3i;
    protected to: Vector3i;
    protected textureId: string;
    protected faces: Sides;

    constructor(from: Vector3i, to: Vector3i, textureId: string) {
        this.from = from;
        this.to = to;
        this.textureId = textureId;
        this.faces = {top: true, bottom: true, left: true, right: true, front: true, back: true};
    }

    public removeFace(side: Side) {
        this.faces[side] = false;
    }

    public getFaces(): MinecraftModelFaces {
        let builtFaces: MinecraftModelFaces = {};

        for (let side in this.faces) {
            if (this.faces[side]) {
                const direction: Direction = Box.getDirectionFromSide(side);
                builtFaces[direction] = this.buildFace(direction);
            }
        }

        return builtFaces;
    }

    protected buildFace(direction: Direction): MinecraftModelFace {
        return {
            uv:  this.calculateUV(direction),
            texture: this.textureId
        }
    }

    protected calculateUV(direction: Direction): number[] {
        let fromX: number = 0;
        let fromY: number = 0;
        let width: number = 0;
        let height: number = 0;

        switch (direction) {
            // Y axis
            case Direction.UP:
            case Direction.DOWN:
                width = this.to.x - this.from.x;
                height = this.to.z - this.from.z;
                break;
            // Z axis
            case Direction.NORTH:
            case Direction.SOUTH:
                width = this.to.x - this.from.x;
                height = this.to.y - this.from.y;
                break;
            // X axis
            case Direction.EAST:
            case Direction.WEST:
                width = this.to.z - this.from.z;
                height = this.to.y - this.from.y;
                break;
        }

        if (direction == Direction.DOWN) {
            fromY = 16 - height;
        }

        if (direction === Direction.WEST) {
            fromX = 16 - width;
        }

        const toX = fromX + width;
        const toY = fromY + height;

        if (toX > 16 || toY > 16) {
            console.warn(`One of the UV maps are calculated incorrectly: ${fromX}, ${fromY}, ${toX}, ${toY}`);
            console.debug(`Direction: ${direction}, from: ${JSON.stringify(this.from)}, to: ${JSON.stringify(this.to)}`);
        }

        return [fromX, fromY, toX, toY];
    }

    public toJSON(): MinecraftModelElement {
        return {
            from: [this.from.x, this.from.y, this.from.z],
            to: [this.to.x, this.to.y, this.to.z],
            faces: this.getFaces(),
            __comment: "Box"
        };
    }

    public static getDirectionFromSide(side: string): Direction {
        switch (side) {
            case Side.TOP:
                return Direction.UP;
            case Side.BOTTOM:
                return Direction.DOWN;
            case Side.LEFT:
                return Direction.EAST;
            case Side.RIGHT:
                return Direction.WEST;
            case Side.FRONT:
                return Direction.NORTH;
            case Side.BACK:
                return Direction.SOUTH;
        }
    }
}