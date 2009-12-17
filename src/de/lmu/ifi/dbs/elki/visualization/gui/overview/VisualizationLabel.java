package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;

public class VisualizationLabel extends VisualizationInfo {
  String label;
  
  public VisualizationLabel(String label, double width, double height) {
    super(width, height);
    this.label = label;
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
    // Should not be called, since we've overridden isVisible and thumbnailEnabled above.
    throw new UnsupportedOperationException("Labels don't have a detail view.");
  }

  @Override
  public Element makeElement(SVGPlot plot) {
    double fontsize = .1;
    Element text = plot.svgText(width/2, height/2 + .35*fontsize, this.label);
    SVGUtil.setAtt(text, SVGConstants.SVG_STYLE_ATTRIBUTE, "fill:black; font-size:"+fontsize+"; font-family: 'Times New Roman', sans-serif");
    SVGUtil.setAtt(text, SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE, SVGConstants.SVG_MIDDLE_VALUE);
    return text;
  }

  @Override
  public boolean hasDetails() {
    return false;
  }  
}
