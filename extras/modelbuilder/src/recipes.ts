import fs from "fs";
import {materialVariations, paintingVariations, plateVariations} from "./variations";
import rimraf from "rimraf";

export interface MinecraftRecipe {
    type: string,
    pattern: string[],
    key: {
        [key: string]: { item?: string, tag?: string }
    },
    result: {
        item: string
    }
}


export const buildRecipes = function () {
    console.log(`==========================`);
    console.log(`Processing recipes`);

    rimraf.sync(`result/recipes/`);

    fs.mkdirSync(`result/recipes/`);

    for (let material of materialVariations) {
        for (let platedVariant of Object.values(plateVariations)) {
            console.log(`Added ${material} ${platedVariant} recipe variations`);

            const recipe: MinecraftRecipe = {
                type: "minecraft:crafting_shaped",
                pattern: [
                    "PNP",
                    "pLp",
                    "PpP"
                ],
                key: {
                    "P": {
                        item: `minecraft:${material}_planks`
                    },
                    "N": {
                        item: platedVariant == plateVariations.PLATED ? "minecraft:gold_nugget" : "minecraft:iron_nugget"
                    },
                    "p": {
                        item: "minecraft:stick"
                    },
                    "L": {
                        item: "minecraft:leather"
                    }
                },
                result: {
                    item: `zetter:${material}_${platedVariant}_frame`
                }
            }

            fs.writeFileSync(`result/recipes/${material}_${platedVariant}_frame.json`, JSON.stringify(recipe));
        }
    }
};