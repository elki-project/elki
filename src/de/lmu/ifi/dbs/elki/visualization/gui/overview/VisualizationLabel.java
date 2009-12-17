package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;

public class VisualizationLabel extends VisualizationInfo {
  String label;
  double width;
  double height;
  
  public VisualizationLabel(String label, double width, double height) {
    super();
    this.label = label;
    this.width = width;
    this.height = height;
  }

  @Override
  public boolean isVisible() {
    return true;
  }

  @Override
  public boolean thumbnailEnabled() {
    return false;
  }

  @Override
  public Element build(@SuppressWarnings("unused") SVGPlot plot) {
    throw new UnsupportedOperationException("Labels don't have a detail view.");
  }

  @Override
  protected Visualizer getVisualization() {
    // Should not be called, since we've overriden isVisible and thumbnailEnabled above.
    throw new UnsupportedOperationException("Labels don't have a detail view.");
  }

  @Override
  public Element makeElement(SVGPlot plot) {
    Element text = plot.svgText(width/2, height/2 + .05, this.label);
    SVGUtil.setAtt(text, SVGConstants.SVG_STYLE_ATTRIBUTE, "fill:black; font-size: .1");
    SVGUtil.setAtt(text, SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE, SVGConstants.SVG_MIDDLE_VALUE);
    return text;
  }

  @Override
  public boolean hasDetails() {
    return false;
  }  
}
