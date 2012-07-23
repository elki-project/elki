package de.lmu.ifi.dbs.elki.visualization.visualizers.parallel.cluster;

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
import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projector.ParallelPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.parallel.AbstractParallelVisualization;

/**
 * Generates a SVG-Element that visualizes the area covered by a cluster.
 * 
 * @author Robert Rödler
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
// TODO: make parameterizable: rounded.
public class ClusterOutlineVisualization extends AbstractVisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Cluster Outline";

  /**
   * Currently unused option to enable/disable rounding
   */
  public static final OptionID ROUNDED_ID = OptionID.getOrCreateOptionID("parallel.clusteroutline.rounded", "Draw lines rounded");

  /**
   * Currently, always enabled.
   */
  private boolean rounded = true;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public ClusterOutlineVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    return new Instance(task, rounded);
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    // Find clusterings we can visualize:
    Collection<Clustering<?>> clusterings = ResultUtil.filterResults(result, Clustering.class);
    for(Clustering<?> c : clusterings) {
      if(c.getAllClusters().size() > 0) {
        Collection<ParallelPlotProjector<?>> ps = ResultUtil.filterResults(baseResult, ParallelPlotProjector.class);
        for(ParallelPlotProjector<?> p : ps) {
          final VisualizationTask task = new VisualizationTask(NAME, c, p.getRelation(), this);
          task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA - 1);
          task.put(VisualizationTask.META_VISIBLE_DEFAULT, false);
          baseResult.getHierarchy().add(c, task);
          baseResult.getHierarchy().add(p, task);
        }
      }
    }
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    // Don't use thumbnails
    return false;
  }

  /**
   * Instance
   * 
   * @author Robert Rödler
   * @author Erich Schubert
   */
  public class Instance extends AbstractParallelVisualization<NumberVector<?, ?>> implements DataStoreListener {
    /**
     * Generic tags to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
     */
    public static final String CLUSTERAREA = "Clusteroutline";

    /**
     * The result we visualize
     */
    private Clustering<Model> clustering;

    /**
     * Flag for using rounded shapes
     */
    boolean rounded = true;

    /**
     * Constructor.
     * 
     * @param task VisualizationTask
     */
    public Instance(VisualizationTask task, boolean rounded) {
      super(task);
      this.clustering = task.getResult();
      this.rounded = rounded;
      context.addDataStoreListener(this);
      context.addResultListener(this);
      incrementalRedraw();
    }

    @Override
    public void destroy() {
      context.removeDataStoreListener(this);
      context.removeResultListener(this);
      super.destroy();
    }

    @Override
    protected void redraw() {
      addCSSClasses(svgp);
      int dim = proj.getVisibleDimensions();

      DoubleMinMax[] mms = DoubleMinMax.newArray(dim);
      DoubleMinMax[] midmm = DoubleMinMax.newArray(dim - 1);

      Iterator<Cluster<Model>> ci = clustering.getAllClusters().iterator();
      for(int cnum = 0; cnum < clustering.getAllClusters().size(); cnum++) {
        Cluster<?> clus = ci.next();
        for(int i = 0; i < dim; i++) {
          mms[i].reset();
          if(i < dim - 1) {
            midmm[i].reset();
          }
        }

        // Process points
        // TODO: do this just once, cache the result somewhere appropriately?
        for(DBIDIter id = clus.getIDs().iter(); id.valid(); id.advance()) {
          double[] yPos = proj.fastProjectDataToRenderSpace(relation.get(id));
          for(int i = 0; i < dim; i++) {
            mms[i].put(yPos[i]);
            if(i > 0) {
              midmm[i - 1].put((yPos[i] + yPos[i - 1]) / 2.);
            }
          }
        }

        SVGPath path = new SVGPath();
        if(!rounded) {
          // Straight lines
          for(int i = 0; i < dim; i++) {
            path.drawTo(getVisibleAxisX(i), mms[i].getMax());
            if(i < dim - 1) {
              path.drawTo(getVisibleAxisX(i + .5), midmm[i].getMax());
            }
          }
          for(int i = dim - 1; i >= 0; i--) {
            if(i < dim - 1) {
              path.drawTo(getVisibleAxisX(i + .5), midmm[i].getMin());
            }
            path.drawTo(getVisibleAxisX(i), mms[i].getMin());
          }
          path.close();
        }
        else {
          // Maxima
          path.drawTo(getVisibleAxisX(0), mms[0].getMax());
          for(int i = 1; i < dim; i++) {
            path.quadTo(getVisibleAxisX(i - .5), midmm[i - 1].getMax(), getVisibleAxisX(i), mms[i].getMax());
          }
          // Minima
          path.drawTo(getVisibleAxisX(dim - 1), mms[dim - 1].getMin());
          for(int i = dim - 1; i > 0; i--) {
            path.quadTo(getVisibleAxisX(i - .5), midmm[i - 1].getMin(), getVisibleAxisX(i - 1), mms[i - 1].getMin());
          }
          path.close();
        }

        Element intervals = path.makeElement(svgp);
        SVGUtil.addCSSClass(intervals, CLUSTERAREA + cnum);
        layer.appendChild(intervals);
      }
    }

    /**
     * Adds the required CSS-Classes
     * 
     * @param svgp SVG-Plot
     */
    private void addCSSClasses(SVGPlot svgp) {
      if(!svgp.getCSSClassManager().contains(CLUSTERAREA)) {
        ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);
        int clusterID = 0;

        for(@SuppressWarnings("unused")
        Cluster<?> cluster : clustering.getAllClusters()) {
          CSSClass cls = new CSSClass(this, CLUSTERAREA + clusterID);
          // cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY,
          // context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT) / 2.0);
          cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, 0.5);
          final String color;
          if(clustering.getAllClusters().size() == 1) {
            color = SVGConstants.CSS_BLACK_VALUE;
          }
          else {
            color = colors.getColor(clusterID);
          }
          // cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, color);
          cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, color);

          svgp.addCSSClassOrLogError(cls);
          clusterID++;
        }
      }
    }
  }
}