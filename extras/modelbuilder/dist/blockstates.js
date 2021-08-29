"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.buildBlockStates = void 0;
const fs_1 = __importDefault(require("fs"));
const variations_1 = require("./variations");
const rimraf_1 = __importDefault(require("rimraf"));
const models_1 = require("./models");
const buildBlockStates = function () {
    console.log(`==========================`);
    console.log(`Processing block states`);
    console.log(`==========================`);
    rimraf_1.default.sync(`result/blockstates/`);
    fs_1.default.mkdirSync(`result/blockstates/`);
    for (let material in variations_1.materialVariations) {
        console.log(`Processing ${material} state`);
        for (let modelName of Object.keys(models_1.ModelKey).map(key => models_1.ModelKey[key])) {
            const newFrameBlockState = {
                "variants": {
                    "": { "model": `zetter:block/frame/${material}/${modelName}` }
                }
            };
            if (!fs_1.default.existsSync(`result/blockstates/${material}/`)) {
                fs_1.default.mkdirSync(`result/blockstates/${material}/`);
            }
            fs_1.default.writeFileSync(`result/blockstates/${material}/${modelName}.json`, JSON.stringify(newFrameBlockState));
        }
    }
};
exports.buildBlockStates = buildBlockStates;
//# sourceMappingURL=blockstates.js.map