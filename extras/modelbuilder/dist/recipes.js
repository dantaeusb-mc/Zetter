"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.buildRecipes = void 0;
const fs_1 = __importDefault(require("fs"));
const variations_1 = require("./variations");
const rimraf_1 = __importDefault(require("rimraf"));
const buildRecipes = function () {
    console.log(`==========================`);
    console.log(`Processing recipes`);
    console.log(`==========================`);
    rimraf_1.default.sync(`result/recipes/`);
    fs_1.default.mkdirSync(`result/recipes/`);
    // Wooden frames
    for (let material in variations_1.materialVariations) {
        if (!variations_1.materialVariations[material].isWood) {
            continue;
        }
        for (let platedVariant of Object.values(variations_1.plateVariations)) {
            console.log(`Added ${material} ${platedVariant} frame recipe variation`);
            const recipe = {
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
                        item: platedVariant == variations_1.plateVariations.PLATED ? "minecraft:gold_nugget" : "minecraft:iron_nugget"
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
            };
            fs_1.default.writeFileSync(`result/recipes/${material}_${platedVariant}_frame.json`, JSON.stringify(recipe));
        }
    }
    console.log(`Added iron frame recipe variations`);
    const ironFrameRecipe = {
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
    };
    fs_1.default.writeFileSync(`result/recipes/iron_frame.json`, JSON.stringify(ironFrameRecipe));
    console.log(`Added golden frame recipe variations`);
    const goldenFrameRecipe = {
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
    };
    fs_1.default.writeFileSync(`result/recipes/gold_frame.json`, JSON.stringify(goldenFrameRecipe));
};
exports.buildRecipes = buildRecipes;
//# sourceMappingURL=recipes.js.map