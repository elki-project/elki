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
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.style.ClusterStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.marker.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGButton;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Visualizer, displaying the key for a clustering.
 *
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class KeyVisualization extends AbstractVisFactory {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Cluster Key";

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    // Find clusterings we can visualize:
    Collection<Clustering<?>> clusterings = ResultUtil.filterResults(newResult, Clustering.class);
    for(Clustering<?> c : clusterings) {
      final int numc = c.getAllClusters().size();
      if(numc > 0) {
        // FIXME: compute from labels?
        final double maxwidth = 10.;
        final VisualizationTask task = new VisualizationTask(NAME, c, null, this);
        final int cols = getPreferredColumns(1.0, 1.0, numc, maxwidth);
        final int rows = (int) Math.ceil(numc / (double) cols);
        final double div = Math.max(2. + rows, cols * maxwidth);
        task.width = cols * maxwidth / div;
        task.height = (2. + rows) / div;
        task.level = VisualizationTask.LEVEL_STATIC;
        task.nodetail = true;
        baseResult.getHierarchy().add(c, task);
      }
    }
  }

  /**
   * Compute the preferred number of columns.
   * 
   * @param width Target width
   * @param height Target height
   * @param numc Number of clusters
   * @param maxwidth Max width of entries
   * @return Preferred number of columns
   */
  public static int getPreferredColumns(double width, double height, int numc, double maxwidth) {
    // Maximum width (compared to height) of labels - guess.
    // FIXME: do we really need to do this three-step computation?
    // Number of rows we'd use in a squared layout:
    final double rows = Math.ceil(Math.pow(numc * maxwidth, height / (width + height)));
    // Given this number of rows (plus two for header), use this many columns:
    return (int) Math.ceil(numc / (rows + 2));
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    return new Instance(task);
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    return false;
  }

  /**
   * Instance
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has Clustering oneway - - visualizes
   */
  public class Instance extends AbstractVisualization {
    /**
     * Clustering to display
     */
    private Clustering<Model> clustering;

    /**
     * Constructor.
     * 
     * @param task Visualization task
     */
    public Instance(VisualizationTask task) {
      super(task);
      this.clustering = task.getResult();
      context.addResultListener(this);
    }

    @Override
    public void destroy() {
      context.removeResultListener(this);
      super.destroy();
    }

    @Override
    public void resultChanged(Result current) {
      super.resultChanged(current);
      if(current == context.getStyleResult()) {
        incrementalRedraw();
      }
    }

    @Override
    protected void redraw() {
      SVGPlot svgp = task.getPlot();
      StyleLibrary style = context.getStyleLibrary();
      MarkerLibrary ml = style.markers();

      // Maximum width (compared to height) of labels - guess.
      // FIXME: compute from labels?
      final double maxwidth = 10.;

      final List<Cluster<Model>> allcs = clustering.getAllClusters();
      final int numc = allcs.size();
      final int cols = getPreferredColumns(task.getWidth(), task.getHeight(), numc, maxwidth);
      final int rows = 2 + (int) Math.ceil(numc / (double) cols);
      // We use a coordinate system based on rows, so columns are at c*maxwidth

      layer = svgp.svgElement(SVGConstants.SVG_G_TAG);
      // Add a label for the clustering.
      {
        Element label = svgp.svgText(0.1, 0.7, clustering.getLongName());
        label.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.4; fill: " + style.getTextColor(StyleLibrary.DEFAULT));
        layer.appendChild(label);
      }

      int i = 0;
      for(Cluster<Model> c : allcs) {
        final int col = i / rows;
        final int row = i % rows;
        ml.useMarker(svgp, layer, 0.3 + maxwidth * col, row + 1.5, i, 0.3);
        Element label = svgp.svgText(0.7 + maxwidth * col, row + 1.7, c.getNameAutomatic());
        label.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; fill: " + style.getTextColor(StyleLibrary.DEFAULT));
        layer.appendChild(label);
        i++;
      }

      // Add a button to set style policy
      {
        StylingPolicy sp = context.getStyleResult().getStylingPolicy();
        if(sp instanceof ClusterStylingPolicy && ((ClusterStylingPolicy) sp).getClustering() == clustering) {
          // Don't show the button when active. May confuse people more than the
          // disappearing button?

          // SVGButton button = new SVGButton(.1, rows + 1.1, 3.8, .7, .2);
          // button.setTitle("Active style", "darkgray");
          // layer.appendChild(button.render(svgp));
        }
        else {
          SVGButton button = new SVGButton(.1, rows + 1.1, 3.8, .7, .2);
          button.setTitle("Set style", "black");
          Element elem = button.render(svgp);
          // Attach listener
          EventTarget etr = (EventTarget) elem;
          etr.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, new EventListener() {
            @Override
            public void handleEvent(Event evt) {
              setStylePolicy();
            }
          }, false);
          layer.appendChild(elem);
        }
      }

      // int rows = i + 2;
      // int cols = Math.max(6, (int) (rows * task.getHeight() /
      // task.getWidth()));
      final double margin = style.getSize(StyleLibrary.MARGIN);
      final String transform = SVGUtil.makeMarginTransform(task.getWidth(), task.getHeight(), cols * maxwidth, rows, margin / StyleLibrary.SCALE);
      SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);
    }

    /**
     * Trigger a style change.
     */
    protected void setStylePolicy() {
      context.getStyleResult().setStylingPolicy(new ClusterStylingPolicy(clustering, context.getStyleLibrary()));
      context.getHierarchy().resultChanged(context.getStyleResult());
    }
  }
}