package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.StaticVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Trivial "visualizer" that displays a static label.
 * The visualizer is meant to be used for dimension labels in the overview.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses StaticVisualization oneway - - «create»
 */
public class LabelVisFactory extends AbstractUnprojectedVisFactory<DatabaseObject> {
  /**
   * The label to render
   */
  private String label = "undefined";

  /**
   * Formal constructor, to satisfy Parameterizable API
   */
  public LabelVisFactory() {
    super("Static label");
    super.metadata.put(VisFactory.META_NODETAIL, true);
  }

  /**
   * The actually used constructor - with a static label.
   * 
   * @param label Label to use
   */
  public LabelVisFactory(String label) {
    super("Static label");
    this.label = label;
  }

  @SuppressWarnings("unused")
  @Override
  public void addVisualizers(VisualizerContext<? extends DatabaseObject> context, AnyResult result) {
    // No auto discovery supported.
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    SVGPlot svgp = task.getPlot();
    VisualizerContext<DatabaseObject> context = task.getContext();
    CSSClass cls = new CSSClass(svgp, "unmanaged");
    StyleLibrary style = context.getStyleLibrary();
    double fontsize = style.getTextSize("overview.labels") / StyleLibrary.SCALE;
    cls.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, SVGUtil.fmt(fontsize));
    cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getTextColor("overview.labels"));
    cls.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, style.getFontFamily("overview.labels"));

    Element layer = svgp.svgText(task.getWidth() / 2, task.getHeight() / 2 + .35 * fontsize, this.label);
    SVGUtil.setAtt(layer, SVGConstants.SVG_STYLE_ATTRIBUTE, cls.inlineCSS());
    SVGUtil.setAtt(layer, SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE, SVGConstants.SVG_MIDDLE_VALUE);
    return new StaticVisualization(task, layer, this.getLevel());
  }

  @Override
  public boolean allowThumbnails(@SuppressWarnings("unused") VisualizationTask task) {
    return false;
  }
}
