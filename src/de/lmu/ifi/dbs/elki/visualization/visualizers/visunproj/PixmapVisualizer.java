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

import java.awt.image.RenderedImage;
import java.util.Collection;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.PixmapResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Visualize an arbitrary pixmap result.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has PixmapResult oneway - 1 visualizes
 */
public class PixmapVisualizer extends AbstractVisualization {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Pixmap Visualizer";

  /**
   * The actual pixmap result.
   */
  private PixmapResult result;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   */
  public PixmapVisualizer(VisualizationTask task) {
    super(task);
    this.result = task.getResult();
  }

  @Override
  protected void redraw() {
    // TODO: Use width, height, imgratio, number of OPTICS plots!
    double scale = StyleLibrary.SCALE;

    final double sizex = scale;
    final double sizey = scale * task.getHeight() / task.getWidth();
    final double margin = 0.0; // context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    final String transform = SVGUtil.makeMarginTransform(task.getWidth(), task.getHeight(), sizex, sizey, margin);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    RenderedImage img = result.getImage();
    // is ratio, target ratio
    double iratio = img.getHeight() / img.getWidth();
    double tratio = task.getHeight() / task.getWidth();
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

  /**
   * Factory class for pixmap visualizers.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses PixmapVisualizer oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      Collection<PixmapResult> prs = ResultUtil.filterResults(result, PixmapResult.class);
      for(PixmapResult pr : prs) {
        // Add plots, attach visualizer
        final VisualizationTask task = new VisualizationTask(NAME, pr, null, this);
        task.width = pr.getImage().getWidth() / pr.getImage().getHeight();
        task.height = 1.0;
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_STATIC);
        baseResult.getHierarchy().add(pr, task);
      }
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new PixmapVisualizer(task);
    }

    @Override
    public boolean allowThumbnails(VisualizationTask task) {
      // Don't use thumbnails
      return false;
    }
  }
}