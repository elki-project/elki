package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import java.util.Collection;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SettingsResult;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.StaticVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Pseudo-Visualizer, that lists the settings of the algorithm-
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses StaticVisualization oneway - - «create»
 * @apiviz.has SettingsResult oneway - - visualizes
 */
// TODO: make this a menu item instead of a "visualization"?
public class SettingsVisFactory extends AbstractVisFactory {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Settings";

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public SettingsVisFactory() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    SettingsResult sr = task.getResult();
    VisualizerContext context = task.getContext();
    SVGPlot svgp = task.getPlot();

    Collection<Pair<Object, Parameter<?, ?>>> settings = sr.getSettings();

    Element layer = svgp.svgElement(SVGConstants.SVG_G_TAG);

    // FIXME: use CSSClass and StyleLibrary

    int i = 0;
    Object last = null;
    for(Pair<Object, Parameter<?, ?>> setting : settings) {
      if(setting.first != last && setting.first != null) {
        String name;
        try {
          if(setting.first instanceof Class) {
            name = ((Class<?>) setting.first).getName();
          } else {
            name = setting.first.getClass().getName();
          }
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

    int cols = Math.max(30, (int) (i * task.getHeight() / task.getWidth()));
    int rows = i;
    final double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    final String transform = SVGUtil.makeMarginTransform(task.getWidth(), task.getHeight(), cols, rows, margin / StyleLibrary.SCALE);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    return new StaticVisualization(task, layer);
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    final IterableIterator<SettingsResult> settingsResults = ResultUtil.filteredResults(newResult, SettingsResult.class);
    for(SettingsResult sr : settingsResults) {
      final VisualizationTask task = new VisualizationTask(NAME, sr, null, this, null);
      task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_STATIC);
      baseResult.getHierarchy().add(sr, task);
    }
  }

  @Override
  public boolean allowThumbnails(@SuppressWarnings("unused") VisualizationTask task) {
    return false;
  }

  @Override
  public Class<? extends Projection> getProjectionType() {
    return null;
  }
}