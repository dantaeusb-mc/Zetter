import {
    Direction,
    MinecraftModelElement,
    MinecraftModelFace,
    MinecraftModelFaces,
    Side,
    Sides,
    Vector3i
} from './interfaces';
import {models} from "./models";
import {materials} from "./variations";
import * as fs from 'fs';

class Box {
    from: Vector3i;
    to: Vector3i;
    textureId: string;
    faces: Sides;
    edge: Side;

    constructor(from: Vector3i, to: Vector3i, textureId: string, edge: Side) {
        this.from = from;
        this.to = to;
        this.textureId = textureId;
        this.faces = {top: true, bottom: true, left: true, right: true};
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
                this.to.x = this.to.x + amount;
                return;
        }
    }

    public removeFace(side: Side) {
        this.faces[side] = false;
    }

    public getFaces(): MinecraftModelFaces {
        let builtFaces: MinecraftModelFaces = {
            north: {
                uv: this.calculateUV(Direction.NORTH),
                texture: this.textureId
            }
        };

        for (let side in this.faces) {
            if (this.faces[side]) {
                const direction: Direction = Box.getDirectionFromSide(side);
                builtFaces[direction] = this.buildFace(direction);
            }
        }

        return builtFaces;
    }

    private buildFace(direction: Direction): MinecraftModelFace {
        return {
            uv:  this.calculateUV(direction),
            texture: this.textureId
        }
    }

    private calculateUV(direction: Direction): number[] {
        let fromX: number = 0;
        let fromY: number = 0;
        let width: number = 0;
        let height: number = 0;

        if (direction == Direction.NORTH) {
            switch (this.edge) {
                case Side.TOP:
                    fromY = 1;
                    break;
                case Side.BOTTOM:
                    fromY = 14;
                    break;
                case Side.LEFT:
                    fromX = 1;
                    break;
                case Side.RIGHT:
                    fromX = 14;
                    break;
            }
        } else {
            switch (this.edge) {
                case Side.TOP:
                    fromY = 0;
                    break;
                case Side.BOTTOM:
                    fromY = 15;
                    break;
                case Side.LEFT:
                    fromX = 0;
                    break;
                case Side.RIGHT:
                    fromX = 15;
                    break;
            }
        }


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
            comment: this.edge
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
        }
    }
}

class FrameModel {
    edges: Sides;
    connections: Sides;
    parts: Box[] = [];
    textureId: string = "#frame";

    constructor(edges: {[key in Side]: boolean}) {
        this.edges = edges;
        this.connections = { ...edges };

        Object.keys(this.edges).map((key) => this.connections[key] = !edges[key]);

        this.buildBoxes();
    }

    private buildBoxes(): void {
        for (let edgeSide of Object.keys(this.edges)) {
            if (!this.edges[edgeSide]) {
                continue;
            }

            const box: Box = FrameModel.getBox(edgeSide, this.textureId);

            if (edgeSide === Side.LEFT || edgeSide === Side.RIGHT) {
                // Glue with top edge
                if (this.hasEdge(Side.TOP)) {
                    box.shrink(Side.TOP, 1);
                    box.removeFace(Side.TOP);

                    console.log(`Shrinking ${edgeSide} on top`);
                }

                // Glue with bottom edge
                if (this.hasEdge(Side.BOTTOM)) {
                    box.shrink(Side.BOTTOM, 1);
                    box.removeFace(Side.BOTTOM);

                    console.log(`Shrinking ${edgeSide} on bottom`);
                }
            }

            // For fucks sake, there's still no way to iterate over enum in TS
            for (let faceSide of Object.keys(Side).map(key => Side[key])) {
                if(this.edgeConnected(Side[edgeSide], faceSide)) {
                    box.removeFace(faceSide);
                }
            }

            this.parts.push(box);
        }

        // @todo: add back face
    }

    public toJSON() {
        return {
            elements: this.parts
        };
    }

    public hasEdge(side: Side): boolean {
        return this.edges[side];
    }

    public edgeConnected(edgeSide: Side, faceSide: Side): boolean {
        switch (edgeSide) {
            case Side.TOP:
            case Side.BOTTOM:
                // Example: our right side connected for top edge if we have connection on right
                return (faceSide == Side.RIGHT && this.hasConnection(Side.RIGHT))
                    || (faceSide == Side.LEFT && this.hasConnection(Side.LEFT));
            case Side.LEFT:
            case Side.RIGHT:
                return (faceSide == Side.TOP && this.hasConnection(Side.TOP))
                    || (faceSide == Side.BOTTOM && this.hasConnection(Side.BOTTOM));
        }
    }

    public hasConnection(side: Side): boolean {
        return this.connections[side];
    }

    public static getBox(side: string, textureId: string): Box {
        switch (side) {
            case Side.TOP:
                return new Box({x: 0,y: 15, z: 0}, {x: 16,y: 16, z: 1}, textureId, Side.TOP);
            case Side.BOTTOM:
                return new Box({x: 0,y: 0, z: 0}, {x: 16,y: 1, z: 1}, textureId, Side.BOTTOM);
            case Side.LEFT:
                return new Box({x: 15,y: 0, z: 0}, {x: 16,y: 16, z: 1}, textureId, Side.LEFT);
            case Side.RIGHT:
                return new Box({x: 0,y: 0, z: 0}, {x: 1,y: 16, z: 1}, textureId, Side.RIGHT);
        }
    }
}

interface TexturedFrameModel {
    parent: string,
    textures: {
        particle: string,
        frame: string
    }
}

// Build parent frames
(function () {
    if (!fs.existsSync(`result/model/`)) {
        fs.mkdirSync(`result/model/`);
    }

    if (!fs.existsSync(`result/model/parent/`)) {
        fs.mkdirSync(`result/model/parent/`);
    }

    for (let modelName in models) {
        const currentModel = models[modelName];

        console.log(`Processing ${modelName}`);

        const newFrameModel = new FrameModel(currentModel.edges);

        fs.writeFileSync(`result/model/parent/${modelName}.json`, JSON.stringify(newFrameModel));
    }
})();

// Build styled frames
(function () {
    for (let material of materials) {
        console.log(`Processing ${material} model variation`);

        for (let modelName in models) {
            const newFrameModel: TexturedFrameModel = {
                parent: `zetter:block/frame/parent/${modelName}`,
                textures: {
                    particle: `block/${material}_planks`,
                    frame: `zetter:entity/frame/${material}/${modelName}`
                }
            };

            if (!fs.existsSync(`result/model/${material}/`)) {
                fs.mkdirSync(`result/model/${material}/`);
            }

            fs.writeFileSync(`result/model/${material}/${modelName}.json`, JSON.stringify(newFrameModel));
        }
    }
})();

// Build blockstates
(function () {
    if (!fs.existsSync(`result/blockstates/`)) {
        fs.mkdirSync(`result/blockstates/`);
    }

    for (let material of materials) {
        console.log(`Processing ${material} state variation`);

        for (let modelName in models) {
            const newFrameBlockState = {
                "variants": {
                    "": { "model": `zetter:block/frame/${material}/${modelName}` }
                }
            };

            if (!fs.existsSync(`result/blockstates/${material}/`)) {
                fs.mkdirSync(`result/blockstates/${material}/`);
            }

            fs.writeFileSync(`result/blockstates/${material}/${modelName}.json`, JSON.stringify(newFrameBlockState));
        }
    }
})();