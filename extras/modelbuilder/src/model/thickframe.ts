import {MinecraftModel, Side} from "../interfaces";
import {Box} from "./box";
import {AbstractModel} from "./abstract";
import {Edge} from "./edge";
import {Plane} from "./plane";

export class ThickFrameModel extends AbstractModel {
    protected build(): void {
        for (let edgeSide of Object.keys(this.edges)) {
            if (!this.edges[edgeSide]) {
                continue;
            }

            const edge: Edge = ThickFrameModel.getEdge(edgeSide, this.textureId);
            edge.removeFace(Side.BACK);

            if (edgeSide === Side.LEFT || edgeSide === Side.RIGHT) {
                edge.removeFace(Side.TOP);
                edge.removeFace(Side.BOTTOM);

                if (this.edges[Side.TOP]) {
                    edge.shrink(Side.TOP, 1);
                }

                if (this.edges[Side.BOTTOM]) {
                    edge.shrink(Side.BOTTOM, 1);
                }
            }

            if (edgeSide === Side.TOP || edgeSide === Side.BOTTOM) {
                edge.removeFace(Side.LEFT);
                edge.removeFace(Side.RIGHT);

                if (this.edges[Side.LEFT]) {
                    edge.shrink(Side.LEFT, 1);
                }

                if (this.edges[Side.RIGHT]) {
                    edge.shrink(Side.RIGHT, 1);
                }
            }

            // For fucks sake, there's still no way to iterate over enum in TS
            for (let faceSide of Object.keys(Side).map(key => Side[key])) {
                if (this.edgeConnected(Side[edgeSide], faceSide)) {
                    edge.removeFace(faceSide);
                }
            }

            this.parts.push(edge);
        }

        if (this.edges[Side.TOP] && this.edges[Side.RIGHT]) {
            const corner: Box = ThickFrameModel.getCorner(Side.TOP, this.textureId);
            this.parts.push(corner);
        }

        if (this.edges[Side.TOP] && this.edges[Side.LEFT]) {
            const corner: Box = ThickFrameModel.getCorner(Side.RIGHT, this.textureId);
            this.parts.push(corner);
        }

        if (this.edges[Side.BOTTOM] && this.edges[Side.LEFT]) {
            const corner: Box = ThickFrameModel.getCorner(Side.BOTTOM, this.textureId);
            this.parts.push(corner);
        }

        if (this.edges[Side.BOTTOM] && this.edges[Side.RIGHT]) {
            const corner: Box = ThickFrameModel.getCorner(Side.LEFT, this.textureId);
            this.parts.push(corner);
        }

        // Only back face
        const back = new Plane({x: 0, y: 0, z: 1}, {x: 16, y: 16, z: 1}, this.textureId);
        back.removeFace(Side.TOP);
        back.removeFace(Side.LEFT);
        back.removeFace(Side.RIGHT);
        back.removeFace(Side.BOTTOM);
        back.removeFace(Side.FRONT);

        this.parts.push(back);
    }

    public toJSON(): MinecraftModel {
        return {
            elements: this.parts.map(part => part.toJSON()),
            textures: {
                particle: `block/gold_block`,
                frame: `zetter:entity/frame/gold/${this.name}`,
                corner: `zetter:entity/frame/gold/corner`
            }
        };
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

    public static getEdge(side: string, textureId: string): Edge {
        switch (side) {
            case Side.TOP:
                return new Edge({x: 0, y: 15, z: 0}, {x: 16, y: 17, z: 1}, textureId, Side.TOP);
            case Side.BOTTOM:
                return new Edge({x: 0, y: -1, z: 0}, {x: 16, y: 1, z: 1}, textureId, Side.BOTTOM);
            case Side.LEFT:
                return new Edge({x: 15, y: 0, z: 0}, {x: 17, y: 16, z: 1}, textureId, Side.LEFT);
            case Side.RIGHT:
                return new Edge({x: -1, y: 0, z: 0}, {x: 1, y: 16, z: 1}, textureId, Side.RIGHT);
        }
    }

    public static getCorner(side: string, textureId: string): Box {
        switch (side) {
            // top left
            case Side.TOP:
                return new Box({x: -2, y: 15, z: -1}, {x: 1, y: 18, z: 1}, "#corner");
            // top right
            case Side.RIGHT:
                return new Box({x: 15, y: 15, z: -1}, {x: 18, y: 18, z: 1}, "#corner");
            // bottom right
            case Side.BOTTOM:
                return new Box({x: 15, y: -2, z: -1}, {x: 18, y: 1, z: 1}, "#corner");
            // bottom left
            case Side.LEFT:
                return new Box({x: -2, y: -2, z: -1}, {x: 1, y: 1, z: 1}, "#corner");
        }
    }
}