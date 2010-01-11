package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSetting;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.UnprojectedVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Pseudo-Visualizer, that lists the settings of the algorithm-
 * 
 * @author Erich Schubert
 */
public class SettingsVisualizer extends AbstractVisualizer implements UnprojectedVisualizer {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Settings";

  /**
   * Constructor, Parameterizable style - does nothing.
   */
  public SettingsVisualizer() {
    super();
  }

  /**
   * Initialization.
   * 
   * @param context context.
   */
  public void init(VisualizerContext context) {
    super.init(NAME, context);
  }

  @Override
  public Element visualize(SVGPlot svgp, double width, double height) {
    List<AttributeSettings> settings = ResultUtil.getGlobalAssociation(context.getResult(), AssociationID.META_SETTINGS);

    Element layer = svgp.svgElement(SVGConstants.SVG_G_TAG);
    
    // FIXME: use CSSClass and StyleLibrary

    int i = 0;
    for(AttributeSettings setting : settings) {
      Element object = svgp.svgText(0, i + 0.7, setting.getObject().getClass().getName());
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-weight: bold");
      layer.appendChild(object);
      i++;
      for(AttributeSetting set : setting.getSettings()) {
        Element label = svgp.svgText(0, i + 0.7, set.getName());
        label.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6");
        layer.appendChild(label);
        Element value = svgp.svgText(7.5, i + 0.7, set.getValue());
        value.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6");
        layer.appendChild(value);
        // only advance once, since we want these two to be in the same line.
        i++;
      }
    }

    int size = Math.max(i, 20);
    double scale = (1. / size);
    // scale
    // FIXME: use width, height
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "scale(" + (0.9 * scale) + ") translate(0.08 0.02)");

    return layer;
  }
}
