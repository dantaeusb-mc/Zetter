import fs from "fs";
import {materialVariations} from "./variations";
import {models} from "./models";
import rimraf from "rimraf";

export const buildBlockStates = function () {
    console.log(`==========================`);
    console.log(`Processing block states`);

    rimraf.sync(`result/blockstates/`);
    fs.mkdirSync(`result/blockstates/`);

    for (let material of materialVariations) {
        console.log(`Processing ${material} state`);

        for (let modelName in models) {
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