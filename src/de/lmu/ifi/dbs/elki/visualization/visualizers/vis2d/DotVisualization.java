package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.ArrayList;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;

/**
 * Generates a SVG-Element containing "dots" as markers representing the
 * Database's objects.
 * 
 * @author Remigius Wojdanowski
 * 
 * @apiviz.has NumberVector - - visualizes
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class DotVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> implements DataStoreListener<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Data Dots";

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String MARKER = "marker";

  /**
   * The data we visualize
   */
  private Database<? extends NV> database;

  /**
   * Constructor.
   * 
   * @param task Task to visualize
   */
  public DotVisualization(VisualizationTask task) {
    super(task);
    this.database = task.getResult();
    context.addDataStoreListener(this);
    incrementalRedraw();
  }

  @Override
  public void destroy() {
    super.destroy();
    context.removeDataStoreListener(this);
  }

  @Override
  public void redraw() {
    // draw data
    double dot_size = context.getStyleLibrary().getSize(StyleLibrary.DOTPLOT);
    for(DBID id : database) {
      try {
        double[] v = proj.fastProjectDataToRenderSpace(database.get(id));
        Element dot = svgp.svgCircle(v[0], v[1], dot_size);
        SVGUtil.addCSSClass(dot, MARKER);
        layer.appendChild(dot);
      }
      catch(ObjectNotFoundException e) {
        // ignore.
      }

    }
  }

  @Override
  public void contentChanged(@SuppressWarnings("unused") DataStoreEvent<NV> e) {
    synchronizedRedraw();
  }

  /**
   * The visualization factory
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses DotVisualization oneway - - «create»
   * 
   * @param <NV> Type of the NumberVector being visualized.
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory<NV> {
    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new DotVisualization<NV>(task);
    }

    @Override
    public void addVisualizers(VisualizerContext<? extends NV> context, Result result) {
      ArrayList<Database<?>> databases = ResultUtil.filterResults(result, Database.class);
      for(Database<?> database : databases) {
        if(!VisualizerUtil.isNumberVectorDatabase(database)) {
          return;
        }
        final VisualizationTask task = new VisualizationTask(NAME, context, database, this);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA + 1);
        context.addVisualizer(database, task);
      }
    }

    @Override
    public Object getVisualizationType() {
      return P2DVisualization.class;
    }
  }
}