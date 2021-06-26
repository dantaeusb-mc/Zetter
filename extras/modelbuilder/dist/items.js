"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.buildItems = void 0;
const fs_1 = __importDefault(require("fs"));
const variations_1 = require("./variations");
const rimraf_1 = __importDefault(require("rimraf"));
const buildItems = function () {
    console.log(`==========================`);
    console.log(`Processing icon variations`);
    rimraf_1.default.sync(`result/models/`);
    fs_1.default.mkdirSync(`result/models/`);
    fs_1.default.mkdirSync(`result/models/item/`);
    fs_1.default.mkdirSync(`result/models/item/frame/`);
    // Real icons
    for (let material of variations_1.materialVariations) {
        console.log(`Processing ${material} real icons`);
        for (let plateVariation of Object.values(variations_1.plateVariations)) {
            for (let paintingVariation of Object.values(variations_1.paintingVariations)) {
                let itemModel = {
                    parent: "item/generated",
                    textures: {
                        layer0: `zetter:item/frame/${material}_${plateVariation}`,
                    },
                };
                if (paintingVariation == variations_1.paintingVariations.PAINTING) {
                    itemModel.textures.layer1 = "zetter:item/frame/painting";
                }
                fs_1.default.writeFileSync(`result/models/item/frame/${material}_${plateVariation}_${paintingVariation}.json`, JSON.stringify(itemModel));
            }
        }
    }
    // Overrides
    for (let material of variations_1.materialVariations) {
        console.log(`Processing ${material} reference icons`);
        for (let plateVariation of Object.values(variations_1.plateVariations)) {
            let itemModel = {
                parent: "item/generated",
                textures: {
                    layer0: `zetter:item/frame/${material}_${plateVariation}`,
                },
                overrides: [
                    { predicate: { painting: 0 }, model: `zetter:item/frame/${material}_${plateVariation}_${variations_1.paintingVariations.EMPTY}` },
                    { predicate: { painting: 1 }, model: `zetter:item/frame/${material}_${plateVariation}_${variations_1.paintingVariations.PAINTING}` }
                ]
            };
            fs_1.default.writeFileSync(`result/models/item/${material}_${plateVariation}_frame.json`, JSON.stringify(itemModel));
        }
    }
};
exports.buildItems = buildItems;
//# sourceMappingURL=items.js.map