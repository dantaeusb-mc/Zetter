import {Box} from "./box";
import {MinecraftModel, Side, Sides} from "../interfaces";

export abstract class AbstractModel {
    protected name: string;
    protected edges: Sides;
    protected textureId: string;

    protected parts: Box[] = [];

    constructor(name: string, edges: {[key in Side]: boolean}, textureId: string = "#frame") {
        this.name = name;
        this.edges = edges;
        this.textureId = textureId;

        this.build();
    }

    public hasEdge(side: Side): boolean {
        return this.edges[side];
    }

    protected abstract build(): void;

    public toJSON(): MinecraftModel {
        return {
            elements: this.parts.map(part => part.toJSON())
        };
    }

    public hasConnection(side: Side): boolean {
        return !this.edges[side];
    }
}