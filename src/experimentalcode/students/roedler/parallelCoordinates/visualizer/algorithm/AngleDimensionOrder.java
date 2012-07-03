package experimentalcode.students.roedler.parallelCoordinates.visualizer.algorithm;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
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
 * Arrange dimensions
 * 
 * @author Robert Rödler
 */
public class AngleDimensionOrder extends AbstractParallelVisualization<NumberVector<?, ?>> implements MenuOwner {
  /**
   * Generic tags to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String CLUSTERORDER = "Angle Dimension Order";

  private Clustering<Model> clustering;

  /**
   * Constructor.
   * 
   * @param task VisualizationTask
   */
  public AngleDimensionOrder(VisualizationTask task) {
    super(task);
    clustering = task.getResult();
    incrementalRedraw();
  }

  @Override
  protected void redraw() {
  }

  private void arrange(int par) {
    int dim = DatabaseUtil.dimensionality(relation);
    Matrix angmat = new Matrix(dim, dim, 0.);

    int[] angles;
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
    
    double temp;
    
    for (int i = 1; i < dim; i++){
    //  if (!proj.isAxisVisible(i - 1)) { 
    //    continue; 
    //  }
      
      for (int j = i + 1; j <= dim; j++){
    //    if (!proj.isAxisVisible(j - 1)) { continue; }
        
        angles = new int[40];
        
        for(DBIDIter id = ids.iter(); id.valid(); id.advance()) {
          double dif = proj.fastProjectDataToRenderSpace(relation.get(id).doubleValue(j), j - 1) - proj.fastProjectDataToRenderSpace(relation.get(id).doubleValue(i), i - 1);
          int div =(int) (dif / 5.);
          
          if (dif > 0.) {
            if (div >= 20) { div = 19; }
            angles[20 + div]++;
          }
          else {
            if (div <= -20) { div = -19; }
            angles[19 + div]++;
          }
        }
        
        double entropy = 0.;
        
        for (int l = 0; l < angles.length; l++){
          temp = (double)angles[l] / (double)ids.size();

          if (temp == 0.) { temp++; }
           entropy += (temp * (Math.log(temp)));
        }
    
        entropy /= Math.log(40);
        
        angmat.set(i - 1, j - 1, 1 + entropy);
        angmat.set(j - 1, i - 1, 1 + entropy);
      }
    }
    
    
    ArrayList<Integer> arrange = new ArrayList<Integer>();

    int[] first = getMax(angmat);
    arrange.add(first[0]);
    arrange.add(1, first[1]);
    int[] pos;
    pos = first.clone();

    blank(angmat, pos[0]);
    blank(angmat, pos[1]);
    int tmp1, tmp2;

    for(int i = 2; i < dim; i++) {
      tmp1 = getMax(angmat, pos[0]);
      tmp2 = getMax(angmat, pos[1]);

      if(Math.abs(angmat.get(pos[0], tmp1)) > Math.abs(angmat.get(pos[1], tmp2))) {
        arrange.add(0, tmp1);
        blank(angmat, tmp1);
        pos[0] = tmp1;
      }
      else {
        arrange.add(arrange.size(), tmp2);
        blank(angmat, tmp2);
        pos[1] = tmp2;
      }
    }
    
    for(int i = 0; i < arrange.size(); i++) {
      System.out.print("" + arrange.get(i) + "  ");
    }

    for(int i = 0; i < dim; i++) {
      proj.moveAxis(proj.getAxisForDim(arrange.get(i)), i);
    }
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
    private static final String NAME = "Angle Dimension Order";

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new AngleDimensionOrder(task);
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
