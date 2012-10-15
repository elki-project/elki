package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SettingsResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
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
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public SettingsVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    SettingsResult sr = task.getResult();
    VisualizerContext context = task.getContext();
    SVGPlot svgp = task.getPlot();

    Collection<Pair<Object, Parameter<?>>> settings = sr.getSettings();

    Element layer = svgp.svgElement(SVGConstants.SVG_G_TAG);

    // FIXME: use CSSClass and StyleLibrary

    int i = 0;
    Object last = null;
    for(Pair<Object, Parameter<?>> setting : settings) {
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

    return new StaticVisualizationInstance(task, layer);
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    Collection<SettingsResult> settingsResults = ResultUtil.filterResults(newResult, SettingsResult.class);
    for(SettingsResult sr : settingsResults) {
      final VisualizationTask task = new VisualizationTask(NAME, sr, null, this);
      task.width = 1.0;
      task.height = 1.0;
      task.level = VisualizationTask.LEVEL_STATIC;
      baseResult.getHierarchy().add(sr, task);
    }
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    return false;
  }
}