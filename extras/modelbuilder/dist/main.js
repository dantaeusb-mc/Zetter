"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    Object.defineProperty(o, k2, { enumerable: true, get: function() { return m[k]; } });
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
const models_1 = require("./models");
const variations_1 = require("./variations");
const fs = __importStar(require("fs"));
const items_1 = require("./items");
const blockstates_1 = require("./blockstates");
const recipes_1 = require("./recipes");
const frame_1 = require("./model/frame");
const frameless_1 = require("./model/frameless");
const thickframe_1 = require("./model/thickframe");
items_1.buildItems();
blockstates_1.buildBlockStates();
recipes_1.buildRecipes();
console.log(`==========================`);
console.log(`Processing models`);
console.log(`==========================`);
// Build parent frames
(function () {
    fs.mkdirSync(`result/models/block/`);
    fs.mkdirSync(`result/models/block/parent/`);
    for (let modelName in models_1.models) {
        const currentModel = models_1.models[modelName];
        console.log(`Processing parent ${modelName}`);
        const newFrameModel = new frame_1.FrameModel(modelName, currentModel.edges);
        fs.writeFileSync(`result/models/block/parent/${modelName}.json`, JSON.stringify(newFrameModel));
    }
})();
// Build iron frame
(function () {
    fs.mkdirSync(`result/models/block/iron/`);
    for (let modelName in models_1.models) {
        const currentModel = models_1.models[modelName];
        console.log(`Processing frameless ${modelName}`);
        const newFrameModel = new frameless_1.FramelessModel(modelName, currentModel.edges);
        fs.writeFileSync(`result/models/block/iron/${modelName}.json`, JSON.stringify(newFrameModel));
    }
})();
// Build golden frame
(function () {
    fs.mkdirSync(`result/models/block/gold/`);
    for (let modelName in models_1.models) {
        const currentModel = models_1.models[modelName];
        console.log(`Processing golden ${modelName}`);
        const newFrameModel = new thickframe_1.ThickFrameModel(modelName, currentModel.edges);
        fs.writeFileSync(`result/models/block/gold/${modelName}.json`, JSON.stringify(newFrameModel));
    }
})();
// Build wood frames
(function () {
    for (let material in variations_1.materialVariations) {
        if (!variations_1.materialVariations[material].isWood) {
            continue;
        }
        console.log(`Processing ${material} model variation`);
        fs.mkdirSync(`result/models/block/${material}/`);
        for (let modelName in models_1.models) {
            const newFrameModel = {
                parent: `zetter:block/frame/parent/${modelName}`,
                textures: {
                    particle: `block/${material}_planks`,
                    frame: `zetter:entity/frame/${material}/${modelName}`
                }
            };
            fs.writeFileSync(`result/models/block/${material}/${modelName}.json`, JSON.stringify(newFrameModel));
        }
    }
})();
console.log(`==========================`);
console.log(`Done`);
console.log(`==========================`);
//# sourceMappingURL=main.js.map