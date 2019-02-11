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
package de.lmu.ifi.dbs.elki.result;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.batik.util.SVGConstants;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter.FileType;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.RenderFlag;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.VisualizerParameterizer;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.PlotItem;
import de.lmu.ifi.dbs.elki.visualization.projector.Projector;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Class that automatically generates all visualizations and exports them into
 * SVG files. To configure the export, you <em>will</em> want to configure the
 * {@link VisualizerParameterizer}, in particular the pattern for choosing which
 * visualizers to run.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @composed - - - VisualizerParameterizer
 * @composed - - - Format
 */
@Alias("de.lmu.ifi.dbs.elki.visualization.ExportVisualizations")
public class ExportVisualizations implements ResultHandler {
  /**
   * Get a logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ExportVisualizations.class);

  /**
   * File format
   *
   * @author Erich Schubert
   */
  public enum Format {
    SVG, PNG, PDF, PS, EPS, JPEG
  }

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
  Result baseResult = null;

  /**
   * Visualizer context
   */
  VisualizerContext context = null;

  /**
   * Output counter.
   */
  Map<String, Integer> counter = new HashMap<>();

  /**
   * Output file format.
   */
  Format format;

  /**
   * Image width for pixel output.
   */
  int iwidth;

  /**
   * Constructor.
   *
   * @param output Output folder
   * @param manager Parameterizer
   * @param ratio Canvas ratio
   * @param format Output file format
   */
  public ExportVisualizations(File output, VisualizerParameterizer manager, double ratio, Format format) {
    this(output, manager, ratio, format, 1000);
  }

  /**
   * Constructor.
   *
   * @param output Output folder
   * @param manager Parameterizer
   * @param ratio Canvas ratio
   * @param format Output file format
   * @param iwidth Image width for pixel formats
   */
  public ExportVisualizations(File output, VisualizerParameterizer manager, double ratio, Format format, int iwidth) {
    super();
    this.output = output;
    this.manager = manager;
    this.ratio = ratio;
    this.format = format;
    this.iwidth = iwidth;
  }

  @Override
  public void processNewResult(ResultHierarchy hier, Result newResult) {
    if(output.isFile()) {
      throw new AbortException("Output folder cannot be an existing file.");
    }
    if(!output.exists() && !output.mkdirs()) {
      throw new AbortException("Could not create output directory.");
    }
    if(this.baseResult == null) {
      this.baseResult = newResult;
      context = null;
      counter = new HashMap<>();
      LOG.warning("Note: Reusing visualization exporter for more than one result is untested.");
    }
    if(context == null) {
      context = manager.newContext(hier, baseResult);
    }

    // Projected visualizations
    Hierarchy<Object> vistree = context.getVisHierarchy();
    for(It<Projector> iter2 = vistree.iterAll().filter(Projector.class); iter2.valid(); iter2.advance()) {
      // TODO: allow selecting individual projections only.
      Collection<PlotItem> items = iter2.get().arrange(context);
      for(PlotItem item : items) {
        processItem(item);
      }
    }
    for(It<VisualizationTask> iter2 = vistree.iterAll().filter(VisualizationTask.class); iter2.valid(); iter2.advance()) {
      VisualizationTask task = iter2.get();
      if(vistree.iterParents(task).filter(Projector.class).valid()) {
        continue;
      }
      PlotItem pi = new PlotItem(ratio, 1.0, null);
      pi.add(task);
      processItem(pi);
    }
  }

  private void processItem(PlotItem item) {
    // Descend into subitems
    for(Iterator<PlotItem> iter = item.subitems.iterator(); iter.hasNext();) {
      processItem(iter.next());
    }
    if(item.taskSize() <= 0) {
      return;
    }
    item.sort();
    final double width = item.w, height = item.h;

    VisualizationPlot svgp = new VisualizationPlot();
    svgp.getRoot().setAttribute(SVGConstants.SVG_WIDTH_ATTRIBUTE, "20cm");
    svgp.getRoot().setAttribute(SVGConstants.SVG_HEIGHT_ATTRIBUTE, (20 * height / width) + "cm");
    svgp.getRoot().setAttribute(SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "0 0 " + width + " " + height);

    ArrayList<Visualization> layers = new ArrayList<>();
    for(Iterator<VisualizationTask> iter = item.tasks.iterator(); iter.hasNext();) {
      VisualizationTask task = iter.next();
      if(task.has(RenderFlag.NO_DETAIL) || task.has(RenderFlag.NO_EXPORT) || !task.isVisible()) {
        continue;
      }
      try {
        Visualization v = task.getFactory().makeVisualization(context, task, svgp, width, height, item.proj);
        layers.add(v);
      }
      catch(Exception e) {
        if(Logging.getLogger(task.getFactory().getClass()).isDebugging()) {
          LOG.exception("Visualization failed.", e);
        }
        else {
          LOG.warning("Visualizer " + task.getFactory().getClass().getName() + " failed - enable debugging to see details.");
        }
      }
    }
    if(layers.isEmpty()) {
      return;
    }
    for(Visualization layer : layers) {
      if(layer.getLayer() == null) {
        LOG.warning("NULL layer seen.");
        continue;
      }
      svgp.getRoot().appendChild(layer.getLayer());
    }
    svgp.updateStyleElement();

    String prefix = null;
    prefix = (prefix == null && item.proj != null) ? item.proj.getMenuName() : prefix;
    prefix = (prefix == null && item.tasks.size() > 0) ? item.tasks.get(0).getMenuName() : prefix;
    prefix = (prefix != null ? prefix : "plot");
    // TODO: generate names...
    Integer count = counter.get(prefix);
    counter.put(prefix, count = count == null ? 1 : (count + 1));
    try {
      switch(format){
      case SVG: {
        File outname = new File(output, prefix + "-" + count + ".svg");
        svgp.saveAsSVG(outname);
        break;
      }
      case PNG: {
        File outname = new File(output, prefix + "-" + count + ".png");
        svgp.saveAsPNG(outname, (int) (iwidth * ratio), iwidth);
        break;
      }
      case PDF: {
        File outname = new File(output, prefix + "-" + count + ".pdf");
        svgp.saveAsPDF(outname);
        break;
      }
      case PS: {
        File outname = new File(output, prefix + "-" + count + ".ps");
        svgp.saveAsPS(outname);
        break;
      }
      case EPS: {
        File outname = new File(output, prefix + "-" + count + ".eps");
        svgp.saveAsEPS(outname);
        break;
      }
      case JPEG: {
        File outname = new File(output, prefix + "-" + count + ".jpg");
        svgp.saveAsJPEG(outname, (int) (iwidth * ratio), iwidth);
        break;
      }
      }
    }
    catch(Exception e) {
      LOG.warning("Export of visualization failed.", e);
    }
    for(Visualization layer : layers) {
      layer.destroy();
    }
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to specify the canvas ratio
     */
    public static final OptionID RATIO_ID = new OptionID("vis.ratio", "The width/heigh ratio of the output.");

    /**
     * Parameter to specify the output folder
     */
    public static final OptionID FOLDER_ID = new OptionID("vis.output", "The output folder.");

    /**
     * Parameter to specify the output format
     */
    public static final OptionID FORMAT_ID = new OptionID("vis.format", "File format. Note that some formats requrie additional libraries, only SVG and PNG are default.");

    /**
     * Parameter to specify the image width of pixel formats
     */
    public static final OptionID IWIDTH_ID = new OptionID("vis.width", "Image width for pixel formats.");

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

    /**
     * Output file format.
     */
    Format format;

    /**
     * Width of pixel output formats.
     */
    int iwidth = 1000;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      FileParameter outputP = new FileParameter(FOLDER_ID, FileType.OUTPUT_FILE);
      if(config.grab(outputP)) {
        output = outputP.getValue();
      }

      DoubleParameter ratioP = new DoubleParameter(RATIO_ID, 1.33) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(ratioP)) {
        ratio = ratioP.doubleValue();
      }

      EnumParameter<Format> formatP = new EnumParameter<>(FORMAT_ID, Format.class, Format.SVG);
      if(config.grab(formatP)) {
        format = formatP.getValue();
      }
      if(format == Format.PNG || format == Format.JPEG) {
        IntParameter iwidthP = new IntParameter(IWIDTH_ID, 1000);
        if(config.grab(iwidthP)) {
          iwidth = iwidthP.intValue();
        }
      }

      manager = config.tryInstantiate(VisualizerParameterizer.class);
    }

    @Override
    protected ExportVisualizations makeInstance() {
      return new ExportVisualizations(output, manager, ratio, format, iwidth);
    }
  }
}
