import {getModel, ModelKey} from "./models";
import {materialVariations} from "./variations";
import rimraf from "rimraf";
import * as fs from 'fs';
import {buildItems} from "./items";
import {buildBlockStates} from "./blockstates";
import {buildRecipes} from "./recipes";
import {FrameModel} from "./model/frame";
import {FramelessModel} from "./model/frameless";
import {MinecraftModel, Side} from "./interfaces";
import {ThickFrameModel} from "./model/thickframe";

buildItems();
buildBlockStates();
buildRecipes();

console.log(`==========================`);
console.log(`Processing models`);
console.log(`==========================`);

// Build parent frames
(function () {
    fs.mkdirSync(`result/models/block/`);
    fs.mkdirSync(`result/models/block/parent/`);

    // TS enums are sooooo gooooood
    for (let modelName of Object.keys(ModelKey).map(key => ModelKey[key])) {
        const currentModel = getModel(modelName);

        console.log(`Processing parent ${modelName}`);

        const newFrameModel = new FrameModel(modelName, currentModel);

        fs.writeFileSync(`result/models/block/parent/${modelName}.json`, JSON.stringify(newFrameModel));
    }
})();

// Build iron frame
(function () {
    fs.mkdirSync(`result/models/block/iron/`);

    for (let modelName of Object.keys(ModelKey).map(key => ModelKey[key])) {
        const currentModel = getModel(modelName, true);

        console.log(`Processing frameless ${modelName}`);

        const newFrameModel = new FramelessModel(modelName, currentModel);

        fs.writeFileSync(`result/models/block/iron/${modelName}.json`, JSON.stringify(newFrameModel));
    }
})();

// Build golden frame
(function () {
    fs.mkdirSync(`result/models/block/gold/`);

    for (let modelName of Object.keys(ModelKey).map(key => ModelKey[key])) {
        const currentModel = getModel(modelName, true);

        console.log(`Processing golden ${modelName}`);

        const newFrameModel = new ThickFrameModel(modelName, currentModel);

        fs.writeFileSync(`result/models/block/gold/${modelName}.json`, JSON.stringify(newFrameModel));
    }
})();

// Build wood frames
(function () {
    for (let material in materialVariations) {
        if (!materialVariations[material].isWood) {
            continue;
        }

        console.log(`Processing ${material} model variation`);
        fs.mkdirSync(`result/models/block/${material}/`);

        for (let modelName of Object.keys(ModelKey).map(key => ModelKey[key])) {
            const newFrameModel: MinecraftModel = {
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