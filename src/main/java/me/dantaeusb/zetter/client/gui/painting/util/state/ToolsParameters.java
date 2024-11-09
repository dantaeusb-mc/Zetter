package me.dantaeusb.zetter.client.gui.painting.util.state;

import me.dantaeusb.zetter.painting.Tool;
import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.painting.parameters.BrushParameters;
import me.dantaeusb.zetter.painting.parameters.BucketParameters;
import me.dantaeusb.zetter.painting.parameters.PencilParameters;

import javax.annotation.Nullable;

public class ToolsParameters {
  private final PencilParameters pencilParameters;
  private final BrushParameters brushParameters;
  private final BucketParameters bucketParameters;

  public ToolsParameters(PencilParameters pencilParameters, BrushParameters brushParameters, BucketParameters bucketParameters) {
    this.pencilParameters = pencilParameters;
    this.brushParameters = brushParameters;
    this.bucketParameters = bucketParameters;
  }

  public ToolsParameters() {
    this(new PencilParameters(), new BrushParameters(), new BucketParameters());
  }

  public @Nullable AbstractToolParameters getToolParameters(Tool tool) {
    return switch (tool) {
      case PENCIL -> pencilParameters;
      case BRUSH -> brushParameters;
      case BUCKET -> bucketParameters;
      default -> null;
    };
  }

  public PencilParameters getPencilParameters() {
    return pencilParameters;
  }

  public BrushParameters getBrushParameters() {
    return brushParameters;
  }

  public BucketParameters getBucketParameters() {
    return bucketParameters;
  }
}
