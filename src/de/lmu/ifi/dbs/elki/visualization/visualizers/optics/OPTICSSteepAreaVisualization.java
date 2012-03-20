package de.lmu.ifi.dbs.elki.visualization.visualizers.optics;

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

import java.awt.Color;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICSXi;
import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICSXi.SteepAreaResult;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSDistanceAdapter;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSPlot;
import de.lmu.ifi.dbs.elki.visualization.projector.OPTICSProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Visualize the steep areas found in an OPTICS plot
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses 
 *              de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICSXi.SteepAreaResult
 */
public class OPTICSSteepAreaVisualization<D extends Distance<D>> extends AbstractOPTICSVisualization<D> {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "OPTICS Steep Areas";

  /**
   * CSS class for markers
   */
  protected static final String CSS_STEEP_UP = "opticsSteepUp";

  /**
   * CSS class for markers
   */
  protected static final String CSS_STEEP_DOWN = "opticsSteepDown";

  /**
   * Our clustering
   */
  OPTICSXi.SteepAreaResult areas;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   */
  public OPTICSSteepAreaVisualization(VisualizationTask task) {
    super(task);
    this.areas = findSteepAreaResult(this.optics.getResult());
    context.addResultListener(this);
    incrementalRedraw();
  }

  /**
   * Find the OPTICS clustering child of a cluster order.
   * 
   * @param co Cluster order
   * @return OPTICS clustering
   */
  protected static OPTICSXi.SteepAreaResult findSteepAreaResult(ClusterOrderResult<?> co) {
    for(Result r : co.getHierarchy().getChildren(co)) {
      if(OPTICSXi.SteepAreaResult.class.isInstance(r)) {
        return (OPTICSXi.SteepAreaResult) r;
      }
    }
    return null;
  }

  @Override
  protected void redraw() {
    makeLayerElement();
    addCSSClasses();
    
    final OPTICSPlot<D> opticsplot = optics.getOPTICSPlot(context);
    final List<ClusterOrderEntry<D>> co = getClusterOrder();
    final OPTICSDistanceAdapter<D> adapter = opticsplot.getDistanceAdapter();
    final LinearScale scale = opticsplot.getScale();
    
    for(OPTICSXi.SteepArea area : areas) {
      final int st = area.getStartIndex();
      final int en = area.getEndIndex();
      // Note: make sure we are using doubles!
      final double x1 = (st + .25) / co.size();
      final double x2 = (en + .75) / co.size();
      final double d1 = adapter.getDoubleForEntry(co.get(st));
      final double d2 = adapter.getDoubleForEntry(co.get(en));
      final double y1 = (!Double.isInfinite(d1) && !Double.isNaN(d1)) ? (1. - scale.getScaled(d1)) : 0.;
      final double y2 = (!Double.isInfinite(d2) && !Double.isNaN(d2)) ? (1. - scale.getScaled(d2)) : 0.;
      Element e = svgp.svgLine(plotwidth * x1, plotheight * y1, plotwidth * x2, plotheight * y2);
      if(area instanceof OPTICSXi.SteepDownArea) {
        SVGUtil.addCSSClass(e, CSS_STEEP_DOWN);
      }
      else {
        SVGUtil.addCSSClass(e, CSS_STEEP_UP);
      }
      layer.appendChild(e);
    }
  }

  /**
   * Adds the required CSS-Classes
   */
  private void addCSSClasses() {
    // Class for the markers
    if(!svgp.getCSSClassManager().contains(CSS_STEEP_DOWN)) {
      final CSSClass cls = new CSSClass(this, CSS_STEEP_DOWN);
      Color color = SVGUtil.stringToColor(context.getStyleLibrary().getColor(StyleLibrary.PLOT));
      if(color == null) {
        color = Color.BLACK;
      }
      color = new Color((int) (color.getRed() * 0.8), (int) (color.getGreen() * 0.8 + 0.2 * 256), (int) (color.getBlue() * 0.8));
      cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGUtil.colorToString(color));
      cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
      svgp.addCSSClassOrLogError(cls);
    }
    if(!svgp.getCSSClassManager().contains(CSS_STEEP_UP)) {
      final CSSClass cls = new CSSClass(this, CSS_STEEP_UP);
      Color color = SVGUtil.stringToColor(context.getStyleLibrary().getColor(StyleLibrary.PLOT));
      if(color == null) {
        color = Color.BLACK;
      }
      color = new Color((int) (color.getRed() * 0.8 + 0.2 * 256), (int) (color.getGreen() * 0.8), (int) (color.getBlue() * 0.8));
      cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGUtil.colorToString(color));
      cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
      svgp.addCSSClassOrLogError(cls);
    }
  }

  @Override
  public void resultChanged(Result current) {
    if(current instanceof SelectionResult) {
      synchronizedRedraw();
      return;
    }
    super.resultChanged(current);
  }

  /**
   * Factory class for OPTICS plot selections.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses OPTICSPlotSelectionVisualization oneway - - «create»
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
      IterableIterator<OPTICSProjector<?>> ops = ResultUtil.filteredResults(result, OPTICSProjector.class);
      for(OPTICSProjector<?> p : ops) {
        final SteepAreaResult steep = findSteepAreaResult(p.getResult());
        if(steep != null) {
          final VisualizationTask task = new VisualizationTask(NAME, p, null, this);
          task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA + 1);
          task.put(VisualizationTask.META_VISIBLE_DEFAULT, false);
          baseResult.getHierarchy().add(p, task);
          baseResult.getHierarchy().add(steep, task);
        }
      }
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new OPTICSSteepAreaVisualization<DoubleDistance>(task);
    }

    @Override
    public boolean allowThumbnails(VisualizationTask task) {
      // Don't use thumbnails
      return false;
    }
  }
}