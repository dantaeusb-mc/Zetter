import {
    Direction,
    MinecraftModelElement,
    MinecraftModelFace,
    MinecraftModelFaces,
    Side,
    Sides,
    Vector3i
} from "../interfaces";
import {Box} from "./box";

export class Plane extends Box {
    protected calculateUV(direction: Direction): number[] {
        let fromX: number = 0;
        let fromY: number = 0;

        let toX = 0;
        let toY = 0;

        switch (direction) {
            // Y axis
            case Direction.UP:
            case Direction.DOWN:
                fromX = this.from.x;
                toX = this.to.x;
                fromY = this.from.z;
                toY = this.to.z;
                break;
            // Z axis
            case Direction.NORTH:
            case Direction.SOUTH:
                fromX = this.from.x;
                toX = this.to.x;
                fromY = this.from.y;
                toY = this.to.y;
                break;
            // X axis
            case Direction.EAST:
            case Direction.WEST:
                fromX = this.from.z;
                toX = this.to.z;
                fromY = this.from.y;
                toY = this.to.y;
                break;
        }

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
            __comment: "Plane"
        };
    }
}