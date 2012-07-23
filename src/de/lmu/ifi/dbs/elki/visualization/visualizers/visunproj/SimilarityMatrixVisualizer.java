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

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.evaluation.similaritymatrix.ComputeSimilarityMatrixImage;
import de.lmu.ifi.dbs.elki.evaluation.similaritymatrix.ComputeSimilarityMatrixImage.SimilarityMatrix;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Visualize a similarity matrix with object labels
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class SimilarityMatrixVisualizer extends AbstractVisFactory {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Similarity Matrix Visualizer";

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public SimilarityMatrixVisualizer() {
    super();
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    Collection<ComputeSimilarityMatrixImage.SimilarityMatrix> prs = ResultUtil.filterResults(result, ComputeSimilarityMatrixImage.SimilarityMatrix.class);
    for(ComputeSimilarityMatrixImage.SimilarityMatrix pr : prs) {
      // Add plots, attach visualizer
      final VisualizationTask task = new VisualizationTask(NAME, pr, null, this);
      task.width = 1.0;
      task.height = 1.0;
      task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_STATIC);
      baseResult.getHierarchy().add(pr, task);
    }
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    return new Instance(task);
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    // Don't use thumbnails
    return false;
  }

  /**
   * Instance
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has SimilarityMatrix oneway - 1 visualizes
   */
  public class Instance extends AbstractVisualization {
    /**
     * The actual pixmap result.
     */
    private SimilarityMatrix result;

    /**
     * Constructor.
     * 
     * @param task Visualization task
     */
    public Instance(VisualizationTask task) {
      super(task);
      this.result = task.getResult();
    }

    @Override
    protected void redraw() {
      // TODO: Use width, height, imgratio, number of OPTICS plots!
      double scale = StyleLibrary.SCALE;

      final double sizex = scale;
      final double sizey = scale * task.getHeight() / task.getWidth();
      final double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
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
      SVGUtil.setAtt(itag, SVGConstants.SVG_X_ATTRIBUTE, margin * 0.75);
      SVGUtil.setAtt(itag, SVGConstants.SVG_Y_ATTRIBUTE, margin * 0.75);
      SVGUtil.setAtt(itag, SVGConstants.SVG_WIDTH_ATTRIBUTE, scale * zoom * iratio);
      SVGUtil.setAtt(itag, SVGConstants.SVG_HEIGHT_ATTRIBUTE, scale * zoom);
      itag.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_QNAME, result.getAsFile().toURI().toString());
      layer.appendChild(itag);

      // Add object labels
      final int size = result.getIDs().size();
      final double hlsize = scale * zoom * iratio / size;
      final double vlsize = scale * zoom / size;
      int i = 0;
      final Relation<String> lrep = DatabaseUtil.guessObjectLabelRepresentation(result.getRelation().getDatabase());
      for(DBIDIter id = result.getIDs().iter(); id.valid(); id.advance()) {
        String label = lrep.get(id);
        if(label != null) {
          // Label on horizontal axis
          final double hlx = margin * 0.75 + hlsize * (i + .8);
          final double hly = margin * 0.7;
          Element lbl = svgp.svgText(hlx, hly, label);
          SVGUtil.setAtt(lbl, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "rotate(-90," + hlx + "," + hly + ")");
          SVGUtil.setAtt(lbl, SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: " + hlsize * 0.8);
          layer.appendChild(lbl);
          // Label on vertical axis
          Element lbl2 = svgp.svgText(margin * 0.7, margin * 0.75 + vlsize * (i + .8), label);
          SVGUtil.setAtt(lbl2, SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE, SVGConstants.SVG_END_VALUE);
          SVGUtil.setAtt(lbl2, SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: " + vlsize * 0.8);
          layer.appendChild(lbl2);
        }
        i++;
      }
    }
  }
}