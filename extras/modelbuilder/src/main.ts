import { Side, Sides, Vector3i, MinecraftModelElement, MinecraftModelFaces, MinecraftModelFace } from './interfaces';
import { models } from "./models"
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

    }

    public removeFace(side: Side) {
        this.faces[side] = false;
    }

    public getFaces(): MinecraftModelFaces {
        let builtFaces: MinecraftModelFaces = {
            north: {
                uv: [0, 0, 0, 0],
                texture: this.textureId
            }
        };

        for (let side in this.faces) {
            if (this.faces[side]) {
                builtFaces[Box.getFaceKeyFromSide(side)] = this.buildFace();
            }
        }

        return builtFaces;
    }

    private buildFace(): MinecraftModelFace {
        return {
            // @todo: incorrect, could be 1x1
            uv: [0, 0, this.getWidth(), this.getHeight()],
            texture: this.textureId
        }
    }

    public getWidth(): number {
        return this.to.x - this.from.x;
    }

    public getHeight(): number {
        return this.to.y - this.from.y;
    }

    public toJSON(): MinecraftModelElement {
        return {
            from: [this.from.x, this.from.y, this.from.z],
            to: [this.to.x, this.to.y, this.to.z],
            faces: this.getFaces(),
            comment: this.edge
        };
    }

    public static getFaceKeyFromSide(side: string): string {
        switch (side) {
            case Side.TOP:
                return "up";
            case Side.BOTTOM:
                return "down";
            case Side.LEFT:
                return "east";
            case Side.RIGHT:
                return "west";
        }
    }
}

class FramePart {
    edges: Sides;
    connections: Sides;
    parts: Box[] = [];
    textureId: string;

    constructor(edges: {[key in Side]: boolean}, connections: {[key in Side]: boolean}) {
        this.edges = edges;
        this.connections = connections;

        this.buildBoxes();
    }

    private buildBoxes(): void {
        for (let edgeSide in this.edges) {
            if (!this.edges[edgeSide]) {
                continue;
            }

            const box: Box = FramePart.getBox(edgeSide, this.textureId);

            if (Side[edgeSide] === Side.LEFT || Side[edgeSide] === Side.RIGHT) {
                // Glue with top edge
                if (this.hasEdge(Side.TOP)) {
                    box.shrink(Side.TOP, 1);
                    box.removeFace(Side.TOP);
                }

                // Glue with bottom edge
                if (this.hasEdge(Side.BOTTOM)) {
                    box.shrink(Side.BOTTOM, 1);
                    box.removeFace(Side.BOTTOM);
                }
            }

            // For fuck's sake, there's still no way to iterate over enum in TS
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
            textures: {
                particle: "block/oak_planks",
                frame: "block/oak_planks",
            },
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
                return new Box({x: 15,y: 0, z: 0}, {x: 16,y: 15, z: 1}, textureId, Side.LEFT);
            case Side.RIGHT:
                return new Box({x: 0,y: 1, z: 0}, {x: 1,y: 16, z: 1}, textureId, Side.RIGHT);
        }
    }
}

class FrameModelsBuilder {
    particleTexture: string;
    frameTexture: string;
    titleTexture?: string;

    constructor(particleTexture: string, frameTexture: string) {
        let builtModels = [];

        for (let modelName in models) {
            const currentModel = models[modelName];

            console.log(modelName);

            console.log(JSON.stringify(currentModel));

            const newFramePart = new FramePart(currentModel.edges, currentModel.connections);

            builtModels[modelName] = newFramePart;

            fs.writeFileSync("result/" + modelName + ".json", JSON.stringify(newFramePart));
        }

        console.log(JSON.stringify(builtModels));
    }
}

new FrameModelsBuilder("block/dark_oak_planks", "zetter:paintings/entity/frame/dark_oak/bottom_right")