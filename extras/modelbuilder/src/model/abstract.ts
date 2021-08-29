import {Box} from "./box";
import {MinecraftModel, Side} from "../interfaces";
import {ModelDefinition} from "../models";

export abstract class AbstractModel {
    protected name: string;
    protected model: ModelDefinition;
    protected textureId: string;

    protected parts: Box[] = [];

    constructor(name: string, model: ModelDefinition, textureId: string = "#frame") {
        this.name = name;
        this.model = model;
        this.textureId = textureId;

        this.build();
    }

    public hasEdge(side: Side): boolean {
        return this.model.edges[side];
    }

    protected abstract build(): void;

    public toJSON(): MinecraftModel {
        return {
            elements: this.parts.map(part => part.toJSON())
        };
    }

    public hasConnection(side: Side): boolean {
        if (side == Side.BACK) {
            return !this.model.edgesHaveBack;
        } else if (side == Side.FRONT) {
            return true;
        }

        return !this.model.edges[side];
    }
}