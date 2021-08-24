import {MinecraftModel, Side, Sides} from "../interfaces";
import {Box} from "./box";
import {AbstractModel} from "./abstract";
import {Plane} from "./plane";

export class FramelessModel extends AbstractModel {
    protected build(): void {
        const box: Box = new Plane({x: 0, y: 0, z: 0}, {x: 16, y: 16, z: 1}, this.textureId);

        for (let faceSide of Object.keys(Side).map(key => Side[key])) {
            if(this.hasConnection(faceSide)) {
                box.removeFace(faceSide);
            }
        }

        box.removeFace(Side.FRONT);

        this.parts.push(box);
    }

    public toJSON(): MinecraftModel {
        return {
            elements: this.parts.map(part => part.toJSON()),
            textures: {
                particle: `block/iron_block`,
                frame: `zetter:entity/frame/iron/${this.name}`
            }
        };
    }
}