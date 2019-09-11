/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.visualization.visualizers.visunproj;

import java.util.Collection;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import elki.result.SettingsResult;
import elki.result.SettingsResult.SettingInformation;
import elki.visualization.VisualizationTask;
import elki.visualization.VisualizationTree;
import elki.visualization.VisualizerContext;
import elki.visualization.gui.VisualizationPlot;
import elki.visualization.projections.Projection;
import elki.visualization.style.StyleLibrary;
import elki.visualization.svg.SVGUtil;
import elki.visualization.visualizers.StaticVisualizationInstance;
import elki.visualization.visualizers.VisFactory;
import elki.visualization.visualizers.Visualization;

/**
 * Pseudo-Visualizer, that lists the settings of the algorithm-
 *
 * @author Erich Schubert
 * @since 0.3
 *
 * @stereotype factory
 * @navassoc - create - StaticVisualizationInstance
 * @navhas - visualizes - SettingsResult
 */
// TODO: make this a menu item instead of a "visualization"?
public class SettingsVisualization implements VisFactory {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Settings";

  /**
   * Constructor.
   */
  public SettingsVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    SettingsResult sr = task.getResult();

    Collection<SettingInformation> settings = sr.getSettings();

    Element layer = plot.svgElement(SVGConstants.SVG_G_TAG);

    // FIXME: use CSSClass and StyleLibrary

    int i = 0;
    Object last = null;
    for(SettingInformation setting : settings) {
      if(setting.owner != last) {
        Element object = plot.svgText(0, i + 0.7, setting.owner);
        object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-weight: bold");
        layer.appendChild(object);
        i++;
        last = setting.owner;
      }
      Element label = plot.svgText(0, i + 0.7, setting.name);
      label.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6");
      layer.appendChild(label);
      Element vale = plot.svgText(7.5, i + 0.7, setting.value);
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

    return new StaticVisualizationInstance(context, task, plot, width, height, layer);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findNewResults(context, start).filter(SettingsResult.class).forEach(sr -> {
      context.addVis(sr, new VisualizationTask(this, NAME, sr, null) //
          .level(VisualizationTask.LEVEL_STATIC).visibility(false));
    });
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    return false;
  }
}
