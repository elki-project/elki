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
package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.StaticVisualizationInstance;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Trivial "visualizer" that displays a static label. The visualizer is meant to
 * be used for dimension labels in the overview.
 *
 * @author Erich Schubert
 * @since 0.3
 *
 * @stereotype factory
 * @navassoc - create - StaticVisualizationInstance
 */
public class LabelVisualization implements VisFactory {
  /**
   * The label to render
   */
  private String label = "undefined";

  /**
   * Flag to indicate rotated labels (90 deg to the left)
   */
  private boolean rotated = false;

  /**
   * Constructor. Solely for API purposes (Parameterizable!)
   */
  public LabelVisualization() {
    super();
  }

  /**
   * The actually used constructor - with a static label.
   *
   * @param label Label to use
   */
  public LabelVisualization(String label) {
    this(label, false);
  }

  /**
   * Constructor.
   *
   * @param label Label to use
   * @param rotated Rotated 90 deg to the left
   */
  public LabelVisualization(String label, boolean rotated) {
    super();
    this.label = label;
    this.rotated = rotated;
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    // No auto discovery supported.
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    CSSClass cls = new CSSClass(plot, "unmanaged");
    StyleLibrary style = context.getStyleLibrary();
    double fontsize = style.getTextSize("overview.labels") / StyleLibrary.SCALE;
    cls.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, SVGUtil.fmt(fontsize));
    cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getTextColor("overview.labels"));
    cls.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, style.getFontFamily("overview.labels"));

    Element layer;
    if(!rotated) {
      layer = plot.svgText(width * .5, height * .5 + .35 * fontsize, this.label);
      SVGUtil.setAtt(layer, SVGConstants.SVG_STYLE_ATTRIBUTE, cls.inlineCSS());
      SVGUtil.setAtt(layer, SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE, SVGConstants.SVG_MIDDLE_VALUE);
    }
    else {
      layer = plot.svgText(height * -.5, width * .5 + .35 * fontsize, this.label);
      SVGUtil.setAtt(layer, SVGConstants.SVG_STYLE_ATTRIBUTE, cls.inlineCSS());
      SVGUtil.setAtt(layer, SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE, SVGConstants.SVG_MIDDLE_VALUE);
      SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "rotate(-90)");
    }
    return new StaticVisualizationInstance(context, task, plot, width, height, layer);
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    return false;
  }
}
