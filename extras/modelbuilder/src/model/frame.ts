import {Side} from "../interfaces";
import {Box} from "./box";
import {AbstractModel} from "./abstract";
import {Edge} from "./edge";
import {Plane} from "./plane";

export class FrameModel extends AbstractModel {
    protected build(): void {
        for (let edgeSide of Object.keys(this.edges)) {
            if (!this.edges[edgeSide]) {
                continue;
            }

            const edge: Edge = FrameModel.getEdge(edgeSide, this.textureId);
            edge.removeFace(Side.BACK);

            if (edgeSide === Side.LEFT || edgeSide === Side.RIGHT) {
                // Glue with top edge
                if (this.hasEdge(Side.TOP)) {
                    edge.shrink(Side.TOP, 1);
                    edge.removeFace(Side.TOP);
                }

                // Glue with bottom edge
                if (this.hasEdge(Side.BOTTOM)) {
                    edge.shrink(Side.BOTTOM, 1);
                    edge.removeFace(Side.BOTTOM);
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

        // Only back face
        const back = new Plane({x: 0, y: 0, z: 1}, {x: 16, y: 16, z: 1}, this.textureId);
        back.removeFace(Side.TOP);
        back.removeFace(Side.LEFT);
        back.removeFace(Side.RIGHT);
        back.removeFace(Side.BOTTOM);
        back.removeFace(Side.FRONT);

        this.parts.push(back);
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
                return new Edge({x: 0, y: 15, z: 0}, {x: 16, y: 16, z: 1}, textureId, Side.TOP);
            case Side.BOTTOM:
                return new Edge({x: 0, y: 0, z: 0}, {x: 16, y: 1, z: 1}, textureId, Side.BOTTOM);
            case Side.LEFT:
                return new Edge({x: 15, y: 0, z: 0}, {x: 16, y: 16, z: 1}, textureId, Side.LEFT);
            case Side.RIGHT:
                return new Edge({x: 0, y: 0, z: 0}, {x: 1, y: 16, z: 1}, textureId, Side.RIGHT);
        }
    }
}