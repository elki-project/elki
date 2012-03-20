package experimentalcode.students.roedler.parallelCoordinates.visualizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import experimentalcode.students.roedler.parallelCoordinates.projector.ParallelPlotProjector;
import experimentalcode.students.roedler.parallelCoordinates.utils.SampledResult;


/**
 * Generates data lines.
 * 
 * @author Robert Rödler
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
public class LineVisualization<NV extends NumberVector<NV, ?>> extends ParallelVisualization<NV> implements DataStoreListener {

  /**
   * Generic tags to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String DATALINE = "Dataline";
  
  /**
   * Constructor.
   * 
   * @param task VisualizationTask
   */
  public LineVisualization(VisualizationTask task) {
    super(task);
    incrementalRedraw();
    context.addDataStoreListener(this);
  }
  
  @Override
  protected void redraw() {
    addCSSClasses(svgp);
    int dim = DatabaseUtil.dimensionality(rep);
    
    SVGPath path;
    Vector yPos;
    
    Collection<SampledResult<Model>> samples = ResultUtil.filterResults(context.getResult(), SampledResult.class);
    if (samples.size() > 0){
      for(SampledResult<Model> c : samples) {
        Iterator<Cluster<Model>> ci = c.getAllClusters().iterator();
        for(int cnum = 0; cnum < c.getAllClusters().size(); cnum++) {
          Cluster<?> clus = ci.next();
          
          for(DBID id : clus.getIDs()) {
            path = new SVGPath();
            yPos = proj.projectDataToRenderSpace(rep.get(id));
            for (int i = 0; i < dim; i++){
              if (proj.isVisible(i)){
                path.drawTo(proj.getXpos(i), yPos.get(i));
              }
            }
            Element line = path.makeElement(svgp);
            SVGUtil.addCSSClass(line, DATALINE);
            layer.appendChild(line);
          }
        }
      }
    }
    else {
      for(DBID id : rep.iterDBIDs()) {
        path = new SVGPath();
        yPos = proj.projectDataToRenderSpace(rep.get(id));
        for (int i = 0; i < dim; i++){
          if (proj.isVisible(i)){
            path.drawTo(proj.getXpos(i), yPos.get(i));
          }
        }
        Element line = path.makeElement(svgp);
        SVGUtil.addCSSClass(line, DATALINE);
        layer.appendChild(line);
      }
    }
  
  }
  
  /**
   * Adds the required CSS-Classes
   * 
   * @param svgp SVG-Plot
   */
  private void addCSSClasses(SVGPlot svgp) {
    final StyleLibrary style = context.getStyleLibrary();
    // Class for the distance function
    if(!svgp.getCSSClassManager().contains(DATALINE)) {
      CSSClass cls = new CSSClass(this, DATALINE);
      cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_BLACK_VALUE);
      cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT) / (StyleLibrary.SCALE * 2.));
      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
      cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
      cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
      svgp.addCSSClassOrLogError(cls);
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
    private static final String NAME = "Data lines";

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new LineVisualization<NV>(task);
    }
    
    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      ArrayList<Clustering<?>> cs = ResultUtil.filterResults(result, Clustering.class);
      boolean hasClustering = (cs.size() > 0);

      IterableIterator<ParallelPlotProjector<?>> ps = ResultUtil.filteredResults(result, ParallelPlotProjector.class);
      for(ParallelPlotProjector<?> p : ps) {
        final VisualizationTask task = new VisualizationTask(NAME, p.getRelation(), p.getRelation(), this);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA + 12);
        if(hasClustering) {
          task.put(VisualizationTask.META_VISIBLE_DEFAULT, false);
        }
        // baseResult.getHierarchy().add(p.getRelation(), task);
        baseResult.getHierarchy().add(p, task);
      }
    }
  

    @Override
    public boolean allowThumbnails(VisualizationTask task) {
      // Don't use thumbnails
      return false;
    }
  }
}
