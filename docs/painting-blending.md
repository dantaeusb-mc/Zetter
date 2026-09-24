# Blending and dithering

What happens to a colour between the tool carrying it and the pixel keeping it.
Lives in `painting/pipes`, driven from `AbstractTool#pixelChange`.

## Pipes

Every pixel a tool touches runs through the tool's pipes in order, each one taking
the colour so far and handing back the colour to store:

```
tool colour ──▶ DitheringPipe ──▶ BlendingPipe ──▶ canvas pixel
                (paint it, or       (how far to
                 keep the old)       move towards it)
```

`Pipe#shouldUsePipe` skips a pipe that has nothing to do, so an opaque stroke at
full intensity writes the colour untouched. It is handed the colour as well as the
tool, because a **translucent** colour has to be composited however hard the tool is
pressing — it never simply replaces what it lands on.

Both pipes read their setting from the action's parameters, never from the screen.
Parameters travel with the action and get replayed on the server and on every other
client, so a pipe that asked the UI anything would give a different answer there.
For the same reason **pipes have to be pure**: the same action replayed twice has to
land on the same pixels, or the painting drifts apart between players. See
[painting-history.md](painting-history.md).

## Dithering

Leaves some pixels under the cursor alone, so a limited palette can fake shades it
does not have. Which pixels is decided from the position **on the canvas**, not
under the cursor, so a pattern lines up across strokes instead of breaking at every
stroke boundary.

| option | paints where |
|---|---|
| `NO_DITHERING` | everywhere |
| `CHECKER` | `(x + y)` even |
| `CHECKER_INVERTED` | `(x + y)` odd |
| `SPARSE` | `x` and `y` both even — one pixel in four |

## Blending

Mixes the tool's colour into the colour already there. How much of the new colour
lands is the **intensity**; which route the mix takes between the two colours is the
**blending option**.

Intensity is the tool's intensity times a local factor — the brush passes its
falloff, so pixels near the edge of the brush blend less than the ones under the
middle. The result is always exact at both ends: intensity 0 keeps the old colour
untouched, intensity 1 gives the new colour untouched.

### Alpha

Canvases are not necessarily opaque. A blackboard's canvas starts empty and lets the
slate behind it through, so the pipe tracks **how much of a pixel is covered at all**
alongside what colour the covered part is.

Coverage follows **source-over**, the ordinary way of stacking one translucent thing
on another — what the stroke does not cover, the canvas shows through:

```
sourceAlpha = colour's own alpha × intensity
resultAlpha = sourceAlpha + canvasAlpha × (1 − sourceAlpha)
weight      = sourceAlpha ÷ resultAlpha
```

The colours themselves are then mixed by the blending option above, using `weight` —
the share of the result the stroke accounts for. So the coverage arithmetic is exact
while the colour still follows whichever mixing model the painter chose.

Two cases are worth holding on to:

- **On an opaque canvas `weight` is the intensity**, exactly, which is what the pipe
  mixed by before any of this existed. Every painting already in a world replays to
  the same bytes — `applyPipe` short-circuits the case outright, and the general path
  was measured to agree with it bit for bit across 590,490 colour pairs.
- **On an empty canvas `weight` is 1**, so a soft stroke puts down its own colour at
  a low alpha instead of mixing itself halfway into the nothing behind it and
  arriving dark. This is the whole reason the alpha bookkeeping is separate from the
  colour mixing.

> Source-over can only ever **add** coverage. Rubbing a pixel back out is
> destination-out, a different operation (`resultAlpha = canvasAlpha × (1 −
> sourceAlpha)`), so a tool that erases needs it rather than a transparent colour,
> which under source-over is simply a no-op. The sponge does its own, below, and
> runs no pipes at all.

## The sponge

Wiping a board down, in `painting/tools/Sponge.java`. It sweeps the same
[capsule](../src/main/java/me/dantaeusb/zetter/painting/tools/brush/Capsule.java) the
brush does, but instead of mixing a colour in it does two things to what is already
there — drags it along, then lifts some of it off:

```
face    = min(1, proximity × 2.5)
grip    = face × lane

dragged = the pixel one reach back along the stroke
colour ← lerpPremultiplied(colour, dragged, smear × grip)
alpha  ← alpha × (1 − erase × grip)
```

A brush tapers, because bristles do. A sponge is a **flat face** pressed against the
board, so it takes the same off everything under it and gives way only at the rim —
that is what `face` is. With a brush's taper instead, a sponge wipes so much less than
its own width that clearing a board turns into scrubbing.

### Smearing

The drag is what makes a sponge a sponge rather than an eraser. Each pass pulls what
lies **behind** a pixel, along the way the hand came, onto the pixel itself, so chalk
travels forward and fades out into a tail. `reach` is normally exactly as far as the
hand moved in that tick, capped, so a fast swipe carries chalk further than a careful
rub without ever picking a colour up in one place and setting it down in another.

Colours are mixed **premultiplied** — see `Color#lerpPremultiplied`. Most of a
blackboard is transparent, and on a canvas that starts empty transparent means
transparent *black*, so mixing it in the plain way would drag grey dirt in from the
bare slate around every mark.

> The sponge reads pixels it is also writing, so it reads them from a copy of the
> region taken before the sweep. Reading the canvas live would let each pixel pick up
> what the one before it had just been given, and every one of those would feed the
> next: the smear would run the whole width of the sweep, and which way it ran would
> depend on which way the loop happened to go. The copy is a local, not a field —
> tools are single instances shared by the client and the server thread.

### Wetness

The only dial, and everything comes off it on one cubic curve. A sponge does not get
steadily better as it wets; it is either damp enough to lift chalk or it is not, so
most of the range sits near the top (half wet is already 89% of soaked) and the whole
falloff is bunched at the dry end.

| | dry | soaked |
|---|---|---|
| `erase` | 0.025 | 0.2 |
| `smear` | 0.55 | 0.02 |

Read the curve one way for the erase and the other way for the smear, so what the
sponge stops lifting it starts pushing. Neither erase figure is anywhere near 1: a
pass is dozens of overlapping touches, so they compound, and soaked they take a
stroke down to a smear in one sweep and off entirely in two or three. A board cleaned
once still looks like a board somebody drew on.

`lane` is the last of it. The sponge is divided into lanes across the direction of
travel, each taking a different share of both the drag and the lift, and wetness
flattens them out — a soaked sponge meets the board evenly, a dry one only catches in
places. Because the lane comes from the pixel's **perpendicular distance to the stroke
axis**, the lanes run *along* the stroke and a pixel keeps the same one for as long as
the hand keeps going the same way. Wipe from a different angle and they fall
differently, so a board that has been cleaned many times carries a record of every
direction somebody cleaned it from. A dab has no direction, so no lanes and no drag —
it only lifts.

> Erasing is more sensitive to overlap than painting is. Painting the same colour
> twice leaves the same colour; erasing twice erases twice, so how hard the sponge
> bites depends on how slowly it was dragged. That is fair for a sponge, but it is
> why the erase constants read much lower than the effect they produce.

### Additive

A plain mix of the RGB channels where they stand, the way two lamps pointed at one
spot add up. Opposite colours meet in the middle as a grey.

### Subtractive

The way pigment behaves: the hue travels **around** the colour wheel rather than
across it, so working yellow into blue passes through green instead of washing out,
and the mix keeps the saturation the two colours started with.

Done in **OkLCh** — the polar form of the Oklab space that okHSL is built on:

- lightness and chroma are interpolated directly
- hue takes the shorter way round; hues exactly opposite each other are the same
  distance either way, and the tie always breaks the same direction, because
  replaying an action has to give the same colour every time
- a grey has no meaningful hue, so it borrows the hue of the other side instead of
  dragging the mix towards whatever fell out of the conversion

okHSL itself would be the obvious choice, but it fits every colour into the RGB
gamut, and that fitting loses accuracy on deep blues — enough that a stroke lands on
a visibly wrong colour. Oklab has no such step and round-trips exactly. `Color`
keeps both: okHSL for the picker, where gamut fitting is what makes a colour wheel
predictable, and Oklab underneath it for mixing.

### Adding an option

The buttons are read straight off the widgets texture in the order the enum lists
them, so a new option needs its artwork appended to that strip and a translation
key. Nothing else knows how many options there are.

> Keep the enum **names** stable once a world has been painted in. They are what
> goes over the wire and into the parameters, and an action naming an option that no
> longer exists silently falls back to the default.
