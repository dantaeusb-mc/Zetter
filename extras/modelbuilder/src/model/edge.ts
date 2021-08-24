import {
    Direction,
    MinecraftModelElement,
    Side,
    Sides,
    Vector3i
} from "../interfaces";
import {Box} from "./box";

export class Edge extends Box {
    protected edge: Side;

    constructor(from: Vector3i, to: Vector3i, textureId: string, edge: Side) {
        super(from, to, textureId);

        this.edge = edge;
    }

    public shrink(side: Side, amount: number): void {
        switch (side) {
            case Side.TOP:
                this.to.y = this.to.y - amount;
                return;
            case Side.BOTTOM:
                this.from.y = this.from.y + amount;
                return;
            case Side.LEFT:
                this.to.x = this.to.x - amount;
                return;
            case Side.RIGHT:
                this.from.x = this.from.x + amount;
                return;
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

        if (direction == Direction.NORTH) {
            switch (this.edge) {
                case Side.TOP:
                    fromY = 1;
                    break;
                case Side.BOTTOM:
                    fromY = 16 - height - 1;
                    break;
                case Side.LEFT:
                    fromX = 1;
                    break;
                case Side.RIGHT:
                    fromX = 16 - width - 1;
                    break;
            }
        } else {
            switch (this.edge) {
                case Side.TOP:
                    fromY = 0;
                    break;
                case Side.BOTTOM:
                    fromY = 16 - height;
                    break;
                case Side.LEFT:
                    fromX = 0;
                    break;
                case Side.RIGHT:
                    fromX = 16 - width;
                    break;
            }
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
            __comment: this.edge
        };
    }
}