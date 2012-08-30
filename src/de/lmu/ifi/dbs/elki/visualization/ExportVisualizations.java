package de.lmu.ifi.dbs.elki.visualization;

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
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.batik.util.SVGConstants;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter.FileType;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.PlotItem;
import de.lmu.ifi.dbs.elki.visualization.projector.Projector;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;

/**
 * Class that automatically generates all visualizations and exports them into
 * SVG files.
 * 
 * @author Erich Schubert
 *
 * @apiviz.composedOf VisualizerParameterizer
 */
// TODO: make more parameterizable, wrt. what to skip
public class ExportVisualizations implements ResultHandler {
  /**
   * Get a logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ExportVisualizations.class);

  /**
   * Parameter to specify the canvas ratio
   * <p>
   * Key: {@code -vis.ratio}
   * </p>
   * <p>
   * Default value: 1.33
   * </p>
   */
  public static final OptionID RATIO_ID = OptionID.getOrCreateOptionID("vis.ratio", "The width/heigh ratio of the output.");

  /**
   * Parameter to specify the output folder
   * <p>
   * Key: {@code -vis.output}
   * </p>
   */
  public static final OptionID FOLDER_ID = OptionID.getOrCreateOptionID("vis.output", "The output folder.");

  /**
   * Output folder
   */
  File output;

  /**
   * Visualization manager.
   */
  VisualizerParameterizer manager;

  /**
   * Ratio for canvas
   */
  double ratio;

  /**
   * Base result
   */
  HierarchicalResult baseResult = null;

  /**
   * Visualizer context
   */
  VisualizerContext context = null;

  /**
   * Output counter
   */
  int counter = 0;

  /**
   * Constructor.
   * 
   * @param output Output folder
   * @param manager Parameterizer
   * @param ratio Canvas ratio
   */
  protected ExportVisualizations(File output, VisualizerParameterizer manager, double ratio) {
    super();
    this.output = output;
    this.manager = manager;
    this.ratio = ratio;
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    if(output.isFile()) {
      throw new AbortException("Output folder cannot be an existing file.");
    }
    if(!output.exists()) {
      if(!output.mkdirs()) {
        throw new AbortException("Could not create output directory.");
      }
    }
    if(this.baseResult != baseResult) {
      this.baseResult = baseResult;
      context = null;
      counter = 0;
      LOG.verbose("Note: Reusing visualization exporter for more than one result is untested.");
    }
    if(context == null) {
      context = manager.newContext(baseResult);
    }

    // Projected visualizations
    ArrayList<Projector> projectors = ResultUtil.filterResults(baseResult, Projector.class);
    for(Projector proj : projectors) {
      // TODO: allow selecting individual projections only.
      Collection<PlotItem> items = proj.arrange();
      for(PlotItem item : items) {
        processItem(item);
      }
    }
    ResultHierarchy hier = baseResult.getHierarchy();
    ArrayList<VisualizationTask> tasks = ResultUtil.filterResults(baseResult, VisualizationTask.class);
    for(VisualizationTask task : tasks) {
      boolean isprojected = false;
      for(Result parent : hier.getParents(task)) {
        if(parent instanceof Projector) {
          isprojected = true;
          break;
        }
      }
      if(isprojected) {
        continue;
      }
      PlotItem pi = new PlotItem(ratio, 1.0, null);
      pi.add(task);
      processItem(pi);
    }
  }

  private void processItem(PlotItem item) {
    final double height = 1;
    final double width = ratio * height;
    // Descend into subitems
    for(Iterator<PlotItem> iter = item.subitems.iterator(); iter.hasNext();) {
      PlotItem subitem = iter.next();
      processItem(subitem);
    }
    if(item.taskSize() <= 0) {
      return;
    }
    item.sort();

    SVGPlot svgp = new SVGPlot();
    svgp.getRoot().setAttribute(SVGConstants.SVG_WIDTH_ATTRIBUTE, "20cm");
    svgp.getRoot().setAttribute(SVGConstants.SVG_HEIGHT_ATTRIBUTE, (20 / ratio) + "cm");
    svgp.getRoot().setAttribute(SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "0 0 " + width + " " + height);

    ArrayList<Visualization> layers = new ArrayList<Visualization>();
    for(Iterator<VisualizationTask> iter = item.tasks.iterator(); iter.hasNext();) {
      VisualizationTask task = iter.next();
      {
        Boolean dis = task.getGenerics(VisualizationTask.META_NODETAIL, Boolean.class);
        if(dis != null && dis == true) {
          continue;
        }
      }
      {
        Boolean dis = task.getGenerics(VisualizationTask.META_NOEXPORT, Boolean.class);
        if(dis != null && dis == true) {
          continue;
        }
      }
      if(!VisualizerUtil.isVisible(task)) {
        continue;
      }
      try {
        Visualization v = task.getFactory().makeVisualization(task.clone(svgp, context, item.proj, width, height));
        layers.add(v);
      }
      catch(Exception e) {
        if(Logging.getLogger(task.getFactory().getClass()).isDebugging()) {
          LoggingUtil.exception("Visualization failed.", e);
        }
        else {
          LoggingUtil.warning("Visualizer " + task.getFactory().getClass().getName() + " failed - enable debugging to see details.");
        }
      }
    }
    if(layers.size() <= 0) {
      return;
    }
    for(Visualization layer : layers) {
      if(layer.getLayer() != null) {
        svgp.getRoot().appendChild(layer.getLayer());
      }
      else {
        LoggingUtil.warning("NULL layer seen.");
      }
    }
    svgp.updateStyleElement();

    // TODO: generate names...
    File outname = new File(output, "plot-" + counter + ".svg");
    try {
      svgp.saveAsSVG(outname);
    }
    catch(Exception e) {
      LOG.warning("Export of visualization failed.", e);
    }
    for(Visualization layer : layers) {
      layer.destroy();
    }
    counter++;
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Visualization manager.
     */
    VisualizerParameterizer manager;

    /**
     * Output folder
     */
    File output;

    /**
     * Ratio for canvas
     */
    double ratio;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      FileParameter outputP = new FileParameter(FOLDER_ID, FileType.OUTPUT_FILE);
      if(config.grab(outputP)) {
        output = outputP.getValue();
      }

      DoubleParameter ratioP = new DoubleParameter(RATIO_ID, new GreaterConstraint(0.0), 1.33);
      if(config.grab(ratioP)) {
        ratio = ratioP.getValue();
      }

      manager = config.tryInstantiate(VisualizerParameterizer.class);
    }

    @Override
    protected ExportVisualizations makeInstance() {
      return new ExportVisualizations(output, manager, ratio);
    }
  }
}
