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
    console.log(`==========================`);

    rimraf.sync(`result/recipes/`);

    fs.mkdirSync(`result/recipes/`);

    // Wooden frames
    for (let material in materialVariations) {
        if (!materialVariations[material].isWood) {
            continue;
        }

        for (let platedVariant of Object.values(plateVariations)) {
            console.log(`Added ${material} ${platedVariant} frame recipe variation`);

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

    console.log(`Added iron frame recipe variations`);

    const ironFrameRecipe: MinecraftRecipe = {
        type: "minecraft:crafting_shaped",
        pattern: [
            "NPN",
            "PLP",
            "NPN"
        ],
        key: {
            "N": {
                item: `minecraft:iron_nugget`
            },
            "P": {
                item: "minecraft:glass_pane"
            },
            "L": {
                item: "minecraft:leather"
            }
        },
        result: {
            item: `zetter:iron_frame`
        }
    }

    fs.writeFileSync(`result/recipes/iron_frame.json`, JSON.stringify(ironFrameRecipe));

    console.log(`Added golden frame recipe variations`);

    const goldenFrameRecipe: MinecraftRecipe = {
        type: "minecraft:crafting_shaped",
        pattern: [
            "EGE",
            "GLG",
            "EGE"
        ],
        key: {
            "G": {
                item: `minecraft:gold_ingot`
            },
            "E": {
                item: `minecraft:emerald`
            },
            "L": {
                item: "minecraft:leather"
            }
        },
        result: {
            item: `zetter:gold_frame`
        }
    }

    fs.writeFileSync(`result/recipes/gold_frame.json`, JSON.stringify(goldenFrameRecipe));
};