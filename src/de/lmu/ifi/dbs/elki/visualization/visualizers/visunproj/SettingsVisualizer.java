package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
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
    List<Pair<Parameterizable, Option<?>>> settings = ResultUtil.getGlobalAssociation(context.getResult(), AssociationID.META_SETTINGS);

    Element layer = svgp.svgElement(SVGConstants.SVG_G_TAG);

    // FIXME: use CSSClass and StyleLibrary

    int i = 0;
    Parameterizable last = null;
    for(Pair<Parameterizable, Option<?>> setting : settings) {
      if(setting.first != last) {
        Element object = svgp.svgText(0, i + 0.7, setting.first.getClass().getName());
        object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-weight: bold");
        layer.appendChild(object);
        i++;
        last = setting.first;
      }
      // get name and value
      String name = setting.second.getOptionID().getName();
      String value;
      try {
        value = setting.second.getValue().toString();
      }
      catch(NullPointerException e) {
        value = "[null]";
      }
      catch(UnusedParameterException e) {
        value = "[unset]";
      }

      Element label = svgp.svgText(0, i + 0.7, name);
      label.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6");
      layer.appendChild(label);
      Element vale = svgp.svgText(7.5, i + 0.7, value);
      vale.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6");
      layer.appendChild(vale);
      // only advance once, since we want these two to be in the same line.
      i++;
    }

    int size = Math.max(i, 20);
    double scale = (1. / size);
    // scale
    // FIXME: use width, height
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "scale(" + (0.9 * scale) + ") translate(0.08 0.02)");

    return layer;
  }
}
