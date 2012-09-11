package experimentalcode.students.roedler.parallelCoordinates.visualizer.algorithm;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.projector.ParallelPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.parallel.AbstractParallelVisualization;
import experimentalcode.students.roedler.parallelCoordinates.gui.MenuOwner;
import experimentalcode.students.roedler.parallelCoordinates.gui.SubMenu;

/**
 * SURFING algorithm to arrange dimensions but with k=N
 * 
 * Reference: <br>
 * Christian Baumgartner, Claudia Plant, Karin Kailing, Hans-
 * Peter Kriegel, and Peer Kr¨oger. Subspace Selection for Clustering
 * High-Dimensional Data.<br> 
 * In ICDM, pages 11–18, 2004.
 * 
 * @author Robert Rödler
 */
public class SURFINGDimensionOrder extends AbstractParallelVisualization<NumberVector<?>> implements MenuOwner {
  /**
   * Generic tags to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String CLUSTERORDER = "SURFING Dimension Order";

  private Clustering<Model> clustering;

  /**
   * The Logger for this class
   */
  private static final Logging logger = Logging.getLogger(SURFINGDimensionOrder.class);
  
  /**
   * Constructor.
   * 
   * @param task VisualizationTask
   */
  public SURFINGDimensionOrder(VisualizationTask task) {
    super(task);
    clustering = task.getResult();
    incrementalRedraw();
  }

  @Override
  protected void redraw() {
  }

  private void arrange(int par) {
    int dim = DatabaseUtil.dimensionality(relation);
    Matrix surfmat = new Matrix(dim, dim, 0.);
    long start, end;

    DBIDs ids = null;
    
    switch(par){
      case -2: {
        ids = context.getSelection().getSelectedIds();
        break;
      }
      case -1: {
        ids = ResultUtil.getSamplingResult(relation).getSample();
        break;
      }
      default: {
        ids = clustering.getAllClusters().get(par).getIDs();
      }
    }
    start = System.nanoTime();
    
    for (int i = 0; i < dim - 1; i++){
      for (int j = i + 1; j < dim; j++){
        double[] knns = new double[ids.size()];
        double sum = 0.;
        int knn = 0;
        for(DBIDIter id1 = ids.iter(); id1.valid(); id1.advance()) {
          double idsum = 0.;
          double x, y;
          
          for(DBIDIter id2 = ids.iter(); id2.valid(); id2.advance()) {
            if (DBIDUtil.equal(id1, id2)) { break; }
            x = relation.get(id1).doubleValue(i) - relation.get(id2).doubleValue(i);
            y = relation.get(id1).doubleValue(j) - relation.get(id2).doubleValue(j);
            idsum += Math.sqrt(x * x + y * y);
          }
          
          sum += idsum  / (double) ids.size();
          knns[knn] = idsum  / (double) ids.size();
          knn++;
        }
        double median = sum / (double) ids.size();
        double diff = 0.;
        
        for (int k = 0; k < knns.length; k++){
          diff += Math.abs(median - knns[k]);
        }
        diff *= 0.5;
        
        int below = 0;
        for (int l = 0; l < knns.length; l++){
          if (knns[l] < median) { 
            below++;
          }
        }
        double quality;
        if (below == 0){
          quality = 0.;
        }
        else {
          quality = diff / ((double)below * median);
        }
        surfmat.set(i, j, quality);
        surfmat.set(j, i, quality);
      }
    }
    end = System.nanoTime();
    
    if (logger.isVerbose()){
      logger.verbose("Runtime SURFINGDimensionOrder: " + (end - start)/1000000. + " ms for a dataset with " + ids.size() + " objects and " + dim + " dimensions");
    }
    
    ArrayList<Integer> arrange = new ArrayList<Integer>();

    int[] first = getMax(surfmat);
    arrange.add(first[0]);
    arrange.add(1, first[1]);
    int[] pos;
    pos = first.clone();

    blank(surfmat, pos[0]);
    blank(surfmat, pos[1]);
    int tmp1, tmp2;

    for(int i = 2; i < dim; i++) {
      tmp1 = getMax(surfmat, pos[0]);
      tmp2 = getMax(surfmat, pos[1]);

      if(Math.abs(surfmat.get(pos[0], tmp1)) > Math.abs(surfmat.get(pos[1], tmp2))) {
        arrange.add(0, tmp1);
        blank(surfmat, tmp1);
        pos[0] = tmp1;
      }
      else {
        arrange.add(arrange.size(), tmp2);
        blank(surfmat, tmp2);
        pos[1] = tmp2;
      }
    }
    
    for(int i = 0; i < arrange.size(); i++) {
      System.out.print("" + arrange.get(i) + "  ");
    }

    for(int i = 0; i < dim; i++) {
      proj.moveAxis(proj.getAxisForDim(arrange.get(i)), i);
    }
    
    context.getHierarchy().resultChanged(proj);
  }

  private int[] getMax(Matrix mat) {
    int[] pos = new int[2];
    double max = 0.;

    for(int i = 0; i < mat.getColumnDimensionality(); i++) {
      for(int j = 0; j < mat.getColumnDimensionality(); j++) {
        if(Math.abs(mat.get(i, j)) > max) {
          max = Math.abs(mat.get(i, j));
          pos[0] = i;
          pos[1] = j;
        }
      }
    }
    mat.set(pos[0], pos[1], 0.);
    mat.set(pos[1], pos[0], 0.);
    return pos;
  }

  private int getMax(Matrix mat, int row) {
    int pos = 0;
    double max = 0.;

    for(int i = 0; i < mat.getColumnDimensionality(); i++) {
      if(Math.abs(mat.get(row, i)) > max) {
        max = Math.abs(mat.get(row, i));
        pos = i;
      }
    }
    
    return pos;
  }

  private void blank(Matrix mat, int dim) {
    for(int i = 0; i < mat.getColumnDimensionality(); i++) {
      mat.set(i, dim, 0.);
    }
  }

  @Override
  public SubMenu getMenu() {
    SubMenu myMenu = new SubMenu(CLUSTERORDER, this);

    myMenu.addItem("arrange", Integer.toString(-1));

    int clus = clustering.getAllClusters().size();
    if(clus > 0) {
      for(int num = 0; num < clus; num++) {
        myMenu.addItem("arrange Cluster " + num, Integer.toString(num));
      }
    }
    myMenu.addItem("arrange selected", "-2");

    return myMenu;
  }

  @Override
  public void menuPressed(String id, boolean checked) {
    int iid = Integer.parseInt(id);
    arrange(iid);
    context.contentChanged(null);
  }

  /**
   * Factory
   * 
   * @author Robert Rödler
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses AxisVisualization oneway - - «create»
   * 
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * A short name characterizing this Visualizer.
     */
    private static final String NAME = "SURFING Dimension Order";

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new SURFINGDimensionOrder(task);
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
            task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA + 3);
            baseResult.getHierarchy().add(c, task);
            baseResult.getHierarchy().add(p, task);
          }
        }
      }
    }
  }
}
