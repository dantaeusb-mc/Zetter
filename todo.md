## Reminder list:

* Do not use AWT. It is not supported on all platforms;
* Nbt tags are UpperCamelCase; when creating new tags follow that rule;
* Getters-setters, are, in fact, rarely needed; think about avoiding them when creating.

## To-do list:

#### Pre-release tasks:

* \[HIGH\] Check that I do `this.minecraft.getTextureManager().release`
* \[HIGH\] * Not saving current palette selection and pixel buffer in Easel Entity;
* \[HIGH\] ? Central piece of iron frame actually has no model (except back which it lacks for some reason);
* \[MED\] If painting has some problems, just remove it instead of crashing or throwing errors (i.e. requesting non-existent canvas);

#### Release tasks:

* \[HIGH\] When dropping painting, if cannot be loaded, just drop frame (gallery workaround);
* \[HIGH\] Configuration file for texture size;
* \[MED\] Reduce saving and sending format to write RGB data instead of RGBA;
* \[MED\] Make sure that no one can edit canvas unless they're standing in front of the easel;
* \[MED\] Make sure bucket tool has decent performance;
* \[MED\] Add data format validation;
* \[MED\] Use specific light levels for every partial canvas (or not, worth trying at least);
* \[MED\] It is said that capability (getCapability) should be cached by modders, we may need to do that here: Helper#getWorldCanvasTracker()
  
#### Would-be-nice-to-do:

* \[HIGH\] I don't like how different classes of canvas data created, would be nice to invent something better;
* \[HIGH\] ? Check that nothing breaks if player tries to draw to not loaded canvas;
* \[LOW\] Looks like if color in a palette somehow getting wrong value, it's unfixable with new color due to alpha channel: maybe we can set alpha to 255 explicitly when picking a color in order to remove potential problem;
* \[LOW\] Trying to unload non-existent canvases sometimes;
* \[LOW\] Remove network getters/setters: they're useless, and actually looks like a bad pattern (`public final`);
* \[LOW\] TIL LinkedLists are bad actually.

#### Planned features:

* Moderation table;
* Pencil color jitter;
* Show canvas in hands like map;
* Texture dispatcher which will prevent not only client request canvases too quick but server to sync canvases too frequent;
* Think about creating own atlas map with loaded paintings.
* Kubelka-Munk model for color blending? See https://stackoverflow.com/questions/1351442/is-there-an-algorithm-for-color-mixing-that-works-like-mixing-real-colors
* Floyd–Steinberg dithering filter https://www.wikiwand.com/en/Floyd%E2%80%93Steinberg_dithering