package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import java.awt.image.RenderedImage;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.result.PixmapResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Visualize an arbitrary pixmap result.
 *
 * @author Erich Schubert
 *
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class PixmapVisualizer extends AbstractVisFactory {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Pixmap Visualizer";

  /**
   * Constructor.
   */
  public PixmapVisualizer() {
    super();
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    Hierarchy.Iter<PixmapResult> it = VisualizationTree.filterResults(context, start, PixmapResult.class);
    for(; it.valid(); it.advance()) {
      PixmapResult pr = it.get();
      // Add plots, attach visualizer
      final VisualizationTask task = new VisualizationTask(NAME, context, pr, null, PixmapVisualizer.this);
      task.reqwidth = pr.getImage().getWidth() / (double) pr.getImage().getHeight();
      task.reqheight = 1.0;
      task.level = VisualizationTask.LEVEL_STATIC;
      context.addVis(pr, task);
    }
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task, SVGPlot plot, double width, double height, Projection proj) {
    return new Instance(task, plot, width, height, proj);
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    // Don't use thumbnails
    return false;
  }

  /**
   * Instance.
   *
   * @author Erich Schubert
   *
   * @apiviz.has PixmapResult oneway - 1 visualizes
   */
  public class Instance extends AbstractVisualization {
    /**
     * The actual pixmap result.
     */
    private PixmapResult result;

    /**
     * Constructor.
     *
     * @param task Visualization task
     */
    public Instance(VisualizationTask task, SVGPlot plot, double width, double height, Projection proj) {
      super(task, plot, width, height);
      this.result = task.getResult();
    }

    @Override
    protected void redraw() {
      // TODO: Use width, height, imgratio, number of OPTICS plots!
      double scale = StyleLibrary.SCALE;

      final double sizex = scale;
      final double sizey = scale * getHeight() / getWidth();
      final double margin = 0.0; // context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
      layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
      final String transform = SVGUtil.makeMarginTransform(getWidth(), getHeight(), sizex, sizey, margin);
      SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

      RenderedImage img = result.getImage();
      // is ratio, target ratio
      double iratio = img.getHeight() / img.getWidth();
      double tratio = getHeight() / getWidth();
      // We want to place a (iratio, 1.0) object on a (tratio, 1.0) screen.
      // Both dimensions must fit:
      double zoom = (iratio >= tratio) ? Math.min(tratio / iratio, 1.0) : Math.max(iratio / tratio, 1.0);

      Element itag = svgp.svgElement(SVGConstants.SVG_IMAGE_TAG);
      SVGUtil.setAtt(itag, SVGConstants.SVG_IMAGE_RENDERING_ATTRIBUTE, SVGConstants.SVG_OPTIMIZE_SPEED_VALUE);
      SVGUtil.setAtt(itag, SVGConstants.SVG_X_ATTRIBUTE, 0);
      SVGUtil.setAtt(itag, SVGConstants.SVG_Y_ATTRIBUTE, 0);
      SVGUtil.setAtt(itag, SVGConstants.SVG_WIDTH_ATTRIBUTE, scale * zoom * iratio);
      SVGUtil.setAtt(itag, SVGConstants.SVG_HEIGHT_ATTRIBUTE, scale * zoom);
      itag.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_QNAME, result.getAsFile().toURI().toString());

      layer.appendChild(itag);
    }
  }
}