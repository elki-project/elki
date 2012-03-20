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
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Pseudo-Visualizer, that gives the key for a clustering.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses StaticVisualization oneway - - «create»
 * @apiviz.has Clustering oneway - - visualizes
 */
public class KeyVisFactory extends AbstractVisualization {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Cluster Key";

  /**
   * Clustering to display
   */
  private Clustering<Model> clustering;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   */
  public KeyVisFactory(VisualizationTask task) {
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
    final List<Cluster<Model>> allcs = clustering.getAllClusters();

    MarkerLibrary ml = context.getStyleLibrary().markers();
    layer = svgp.svgElement(SVGConstants.SVG_G_TAG);
    int i = 0;

    // Add a label for the clustering.
    {
      Element label = svgp.svgText(0.1, i + 0.7, clustering.getLongName());
      label.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.3");
      layer.appendChild(label);
      i++;
    }

    // TODO: multi-column layout!
    for(Cluster<Model> c : allcs) {
      ml.useMarker(svgp, layer, 0.3, i + 0.5, i, 0.3);
      Element label = svgp.svgText(0.7, i + 0.7, c.getNameAutomatic());
      label.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6");
      layer.appendChild(label);
      i++;
    }

    // Add a button to set style policy
    {
      StylingPolicy sp = context.getStyleResult().getStylingPolicy();
      if(sp instanceof ClusterStylingPolicy && ((ClusterStylingPolicy) sp).getClustering() == clustering) {
        Element button = svgp.svgRect(.1, i + .1, 3.8, .9);
        SVGUtil.setAtt(button, SVGConstants.SVG_STYLE_ATTRIBUTE, SVGConstants.CSS_FILL_PROPERTY + ": silver");
        layer.appendChild(button);
        Element label = svgp.svgText(.2, i + .7, "Active style");
        label.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-color: darkgray");
        layer.appendChild(label);
      }
      else {
        Element button = svgp.svgRect(.1, i + .1, 3.8, .9);
        SVGUtil.setAtt(button, SVGConstants.SVG_STYLE_ATTRIBUTE, SVGConstants.CSS_FILL_PROPERTY + ": silver");
        layer.appendChild(button);
        Element label = svgp.svgText(.2, i + .7, "Set style");
        label.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6");
        EventTarget etr = (EventTarget) button;
        etr.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, new EventListener() {
          @Override
          public void handleEvent(Event evt) {
            setStylePolicy();
          }
        }, false);
        layer.appendChild(label);
      }
      i++;
    }

    int cols = Math.max(6, (int) (i * task.getHeight() / task.getWidth()));
    int rows = i;
    final double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    final String transform = SVGUtil.makeMarginTransform(task.getWidth(), task.getHeight(), cols, rows, margin / StyleLibrary.SCALE);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);
  }

  /**
   * Trigger a style change.
   */
  protected void setStylePolicy() {
    context.getStyleResult().setStylingPolicy(new ClusterStylingPolicy(clustering, context.getStyleLibrary()));
    context.getHierarchy().resultChanged(context.getStyleResult());
  }

  public static class Factory extends AbstractVisFactory {

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result newResult) {
      // Find clusterings we can visualize:
      Collection<Clustering<?>> clusterings = ResultUtil.filterResults(newResult, Clustering.class);
      for(Clustering<?> c : clusterings) {
        if(c.getAllClusters().size() > 0) {
          final VisualizationTask task = new VisualizationTask(NAME, c, null, this);
          task.width = 1.0;
          task.height = 1.0;
          task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_STATIC);
          baseResult.getHierarchy().add(c, task);
        }
      }
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new KeyVisFactory(task);
    }

    @Override
    public boolean allowThumbnails(VisualizationTask task) {
      return false;
    }
  }
}