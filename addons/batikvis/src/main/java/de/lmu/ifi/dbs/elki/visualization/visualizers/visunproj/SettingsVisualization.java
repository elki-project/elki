package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Collection;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.result.SettingsResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackedParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.StaticVisualizationInstance;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Pseudo-Visualizer, that lists the settings of the algorithm-
 *
 * @author Erich Schubert
 *
 * @apiviz.stereotype factory
 * @apiviz.uses StaticVisualizationInstance oneway - - «create»
 * @apiviz.has SettingsResult oneway - - visualizes
 */
// TODO: make this a menu item instead of a "visualization"?
public class SettingsVisualization extends AbstractVisFactory {
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
  public Visualization makeVisualization(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    SettingsResult sr = task.getResult();
    VisualizerContext context = task.getContext();

    Collection<TrackedParameter> settings = sr.getSettings();

    Element layer = plot.svgElement(SVGConstants.SVG_G_TAG);

    // FIXME: use CSSClass and StyleLibrary

    int i = 0;
    Object last = null;
    for(TrackedParameter setting : settings) {
      if(setting.getOwner() != last && setting.getOwner() != null) {
        String name;
        try {
          if(setting.getOwner() instanceof Class) {
            name = ((Class<?>) setting.getOwner()).getName();
          }
          else {
            name = setting.getOwner().getClass().getName();
          }
          if(ClassParameter.class.isInstance(setting.getOwner())) {
            name = ((ClassParameter<?>) setting.getOwner()).getValue().getName();
          }
        }
        catch(NullPointerException e) {
          name = "[null]";
        }
        Element object = plot.svgText(0, i + 0.7, name);
        object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-weight: bold");
        layer.appendChild(object);
        i++;
        last = setting.getOwner();
      }
      // get name and value
      String name = setting.getParameter().getOptionID().getName();
      String value = "[unset]";
      try {
        if(setting.getParameter().isDefined()) {
          value = setting.getParameter().getValueAsString();
        }
      }
      catch(NullPointerException e) {
        value = "[null]";
      }

      Element label = plot.svgText(0, i + 0.7, name);
      label.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6");
      layer.appendChild(label);
      Element vale = plot.svgText(7.5, i + 0.7, value);
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

    return new StaticVisualizationInstance(task, plot, width, height, layer);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    Hierarchy.Iter<SettingsResult> it = VisualizationTree.filterResults(context, start, SettingsResult.class);
    for(; it.valid(); it.advance()) {
      SettingsResult sr = it.get();
      final VisualizationTask task = new VisualizationTask(NAME, context, sr, null, SettingsVisualization.this);
      task.reqwidth = 1.0;
      task.reqheight = 1.0;
      task.level = VisualizationTask.LEVEL_STATIC;
      task.initDefaultVisibility(false);
      context.addVis(sr, task);
    }
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    return false;
  }
}