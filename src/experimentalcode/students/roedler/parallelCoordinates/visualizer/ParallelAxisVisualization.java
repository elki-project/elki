package experimentalcode.students.roedler.parallelCoordinates.visualizer;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import experimentalcode.students.roedler.parallelCoordinates.projector.ParallelPlotProjector;
import experimentalcode.students.roedler.parallelCoordinates.svg.SVGParallelLinearAxis;


/**
 * Generates a SVG-Element containing axes, including labeling.
 * 
 * @author Robert Rödler
 * 
 * @apiviz.uses SVGSimpleLinearAxis
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
public class ParallelAxisVisualization<NV extends NumberVector<NV, ?>> extends ParallelVisualization<NV> implements DataStoreListener {
  
  /**
   * Constructor.
   * 
   * @param task VisualizationTask
   */
  public ParallelAxisVisualization(VisualizationTask task) {
    super(task);
    context.addDataStoreListener(this);
    incrementalRedraw();
  }
  
  @Override
  protected void redraw() {
    int dim = DatabaseUtil.dimensionality(rep);
    final double scale = StyleLibrary.SCALE;
    
    try {
      for(int i = 0; i < dim; i++){
        if (proj.isVisible(i)){
          SVGParallelLinearAxis.drawAxis(svgp, layer, proj.getScale(proj.getDimensionNumber(i)), proj.getXpos(i), 0, proj.getXpos(i), scale, true, context.getStyleLibrary(), proj.isInverted(i), scale);
        }
      }
      
    }
    catch(CSSNamingConflict e) {
      throw new RuntimeException("Conflict in CSS naming for axes.", e);
    }
  }
  
  @Override
  public void contentChanged(DataStoreEvent e) {
    synchronizedRedraw();
  }
  
  /**
   * Factory for axis visualizations
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses AxisVisualization oneway - - «create»
   * 
   * @param <NV>
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory {
    /**
     * A short name characterizing this Visualizer.
     */
    private static final String NAME = "Parallel Axes";

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new ParallelAxisVisualization<NV>(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      IterableIterator<ParallelPlotProjector<?>> ps = ResultUtil.filteredResults(result, ParallelPlotProjector.class);
      for(ParallelPlotProjector<?> p : ps) {
        final VisualizationTask task = new VisualizationTask(NAME, p, p.getRelation(), this);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_BACKGROUND);
        baseResult.getHierarchy().add(p, task);
      }
    }

    @Override
    public boolean allowThumbnails(VisualizationTask task) {
      // Don't use thumbnails
      return true;
    }

  }
}
