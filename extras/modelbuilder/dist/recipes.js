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
    rimraf_1.default.sync(`result/recipes/`);
    fs_1.default.mkdirSync(`result/recipes/`);
    for (let material of variations_1.materialVariations) {
        for (let platedVariant of Object.values(variations_1.plateVariations)) {
            console.log(`Added ${material} ${platedVariant} recipe variations`);
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
};
exports.buildRecipes = buildRecipes;
//# sourceMappingURL=recipes.js.map