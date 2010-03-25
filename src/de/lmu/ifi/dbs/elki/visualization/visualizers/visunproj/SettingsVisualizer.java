package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import java.util.ArrayList;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SettingsResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
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
    List<Pair<Object, Parameter<?, ?>>> settings = new ArrayList<Pair<Object, Parameter<?, ?>>>();
    for(SettingsResult sr : ResultUtil.getSettingsResults(context.getResult())) {
      settings.addAll(sr.getSettings());
    }

    Element layer = svgp.svgElement(SVGConstants.SVG_G_TAG);

    // FIXME: use CSSClass and StyleLibrary

    int i = 0;
    Object last = null;
    for(Pair<Object, Parameter<?, ?>> setting : settings) {
      if(setting.first != last) {
        String name = setting.first.getClass().getName();
        if(ClassParameter.class.isInstance(setting.first)) {
          name = ((ClassParameter<?>) setting.first).getValue().getName();
        }
        Element object = svgp.svgText(0, i + 0.7, name);
        object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-weight: bold");
        layer.appendChild(object);
        i++;
        last = setting.first;
      }
      // get name and value
      String name = setting.second.getOptionID().getName();
      String value = "[unset]";
      try {
        if(setting.second.isDefined()) {
          value = setting.second.getValueAsString();
        }
      }
      catch(NullPointerException e) {
        value = "[null]";
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

    int cols = 20;
    int rows = i;
    final double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    final String transform = SVGUtil.makeMarginTransform(width, height, cols, rows, margin);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    return layer;
  }
}
