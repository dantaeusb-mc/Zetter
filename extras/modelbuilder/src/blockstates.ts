import fs from "fs";
import {materialVariations} from "./variations";
import rimraf from "rimraf";
import {ModelKey} from "./models";

export const buildBlockStates = function () {
    console.log(`==========================`);
    console.log(`Processing block states`);
    console.log(`==========================`);

    rimraf.sync(`result/blockstates/`);
    fs.mkdirSync(`result/blockstates/`);

    for (let material in materialVariations) {
        console.log(`Processing ${material} state`);

        for (let modelName of Object.keys(ModelKey).map(key => ModelKey[key])) {
            const newFrameBlockState = {
                "variants": {
                    "": { "model": `zetter:block/frame/${material}/${modelName}` }
                }
            };

            if (!fs.existsSync(`result/blockstates/${material}/`)) {
                fs.mkdirSync(`result/blockstates/${material}/`);
            }

            fs.writeFileSync(`result/blockstates/${material}/${modelName}.json`, JSON.stringify(newFrameBlockState));
        }
    }
};