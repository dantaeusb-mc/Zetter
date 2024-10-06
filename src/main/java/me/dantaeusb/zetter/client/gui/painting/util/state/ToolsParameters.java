package me.dantaeusb.zetter.client.gui.painting.util.state;

import me.dantaeusb.zetter.painting.parameters.BrushParameters;
import me.dantaeusb.zetter.painting.parameters.BucketParameters;
import me.dantaeusb.zetter.painting.parameters.PencilParameters;

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
