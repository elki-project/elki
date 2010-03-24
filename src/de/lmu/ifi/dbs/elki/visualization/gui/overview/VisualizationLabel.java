package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.style.PropertiesBasedStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;

/**
 * Trivial "visualizer" that displays a label.
 * The visualizer is meant to be used for dimension labels in the overview,
 * and doesn't support a "detail" view. 
 * 
 * @author Erich Schubert
 */
public class VisualizationLabel extends VisualizationInfo {
  // TODO: use shared library
  StyleLibrary style = new PropertiesBasedStyleLibrary();
  
  /**
   * Label to display
   */
  String label;
  
  /**
   * Constructor
   * 
   * @param label Label to display
   * @param width Width
   * @param height Height
   */
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
    return true;
  }

  @Override
  @SuppressWarnings("unused")
  public Element build(SVGPlot plot, double width, double height) {
    throw new UnsupportedOperationException("Labels don't have a detail view.");
  }

  @Override
  protected Visualizer getVisualization() {
    // Should not be called, since we've overridden isVisible and thumbnailEnabled above.
    return null;
  }

  @Override
  public Element makeElement(SVGPlot plot) {
    CSSClass cls = new CSSClass(plot,"unmanaged");
    double fontsize = style.getTextSize("overview.labels") / StyleLibrary.SCALE;
    cls.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, SVGUtil.fmt(fontsize));
    cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getTextColor("overview.labels"));
    cls.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, style.getFontFamily("overview.labels"));
    
    Element text = plot.svgText(width/2, height/2 + .35*fontsize, this.label);
    SVGUtil.setAtt(text, SVGConstants.SVG_STYLE_ATTRIBUTE, cls.inlineCSS());
    SVGUtil.setAtt(text, SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE, SVGConstants.SVG_MIDDLE_VALUE);
    return text;
  }

  @Override
  public boolean hasDetails() {
    return false;
  }  
}