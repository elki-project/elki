package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import java.util.Collection;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.result.SettingsResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.StaticVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;

/**
 * Pseudo-Visualizer, that lists the settings of the algorithm-
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has StaticVisualization oneway - - produces
 * @apiviz.has SettingsResult oneway - - visualizes
 */
public class SettingsVisualizer extends AbstractUnprojectedVisualizer<DatabaseObject> {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Settings";

  /**
   * Settings result to visualize
   */
  private final SettingsResult sr;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public SettingsVisualizer(SettingsResult sr) {
    super(NAME);
    super.metadata.put(Visualizer.META_GROUP, Visualizer.GROUP_METADATA);
    this.sr = sr;
  }

  @Override
  public Visualization visualize(SVGPlot svgp, double width, double height) {
    Collection<Pair<Object, Parameter<?, ?>>> settings = sr.getSettings();

    Element layer = svgp.svgElement(SVGConstants.SVG_G_TAG);

    // FIXME: use CSSClass and StyleLibrary

    int i = 0;
    Object last = null;
    for(Pair<Object, Parameter<?, ?>> setting : settings) {
      if(setting.first != last && setting.first != null) {
        String name;
        try {
          name = setting.first.getClass().getName();
          if(ClassParameter.class.isInstance(setting.first)) {
            name = ((ClassParameter<?>) setting.first).getValue().getName();
          }
        }
        catch(NullPointerException e) {
          name = "[null]";
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

    int cols = Math.max(30, (int) (i * height / width));
    int rows = i;
    final double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    final String transform = SVGUtil.makeMarginTransform(width, height, cols, rows, margin / StyleLibrary.SCALE);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    Integer level = this.getMetadata().getGenerics(Visualizer.META_LEVEL, Integer.class);
    return new StaticVisualization(context, svgp, level, layer, width, height);
  }

  @Override
  public Visualization makeThumbnail(SVGPlot svgp, double width, double height, @SuppressWarnings("unused") int tresolution) {
    // No thumbnails for this visualizer
    return visualize(svgp, width, height);
  }
}