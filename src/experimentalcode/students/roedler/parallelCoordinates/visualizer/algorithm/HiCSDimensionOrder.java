package experimentalcode.students.roedler.parallelCoordinates.visualizer.algorithm;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.data.VectorUtil.SortDBIDsBySingleDimension;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.statistics.tests.GoodnessOfFitTest;
import de.lmu.ifi.dbs.elki.math.statistics.tests.KolmogorovSmirnovTest;
import de.lmu.ifi.dbs.elki.math.statistics.tests.WelchTTest;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.TopBoundedHeap;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.projector.ParallelPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.parallel.AbstractParallelVisualization;
import experimentalcode.students.roedler.parallelCoordinates.gui.MenuOwner;
import experimentalcode.students.roedler.parallelCoordinates.gui.SubMenu;

/**
 * HiCS algorithm to arrange dimensions
 * 
 * Fabian Keller, Emmanuel M¨uller, and Klemens B¨ohm. HiCS:
 * High Contrast Subspaces for Density-Based Outlier Ranking. <br>
 * In ICDE, pages 1037–1048, 2012.
 * 
 * @author Robert Rödler
 */
public class HiCSDimensionOrder extends AbstractParallelVisualization<NumberVector<?>> implements MenuOwner {
  /**
   * Generic tags to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String CLUSTERORDER = "HiCS Dimension Order";

  private Clustering<Model> clustering;

  
  /**
   * Maximum number of retries.
   */
  private static final int MAX_RETRIES = 100;
  
  /**
   * Monte-Carlo iterations
   */
  private int m = 50;

  /**
   * Alpha threshold
   */
  private double alpha = 0.1;

  /**
   * Statistical test to use
   */
  private GoodnessOfFitTest statTest;

  /**
   * Candidates limit
   */
  private int cutoff = 400;
  
  /**
   * Random generator
   */
  private Random random;
  
  /**
   * The Logger for this class
   */
  private static final Logging logger = Logging.getLogger(HiCSDimensionOrder.class);
  
  /**
   * Constructor.
   * 
   * @param task VisualizationTask
   */
  public HiCSDimensionOrder(VisualizationTask task) {
    super(task);
    clustering = task.getResult();
   // this.statTest = new WelchTTest();
    this.statTest = new KolmogorovSmirnovTest();
    this.random = new Random();
    incrementalRedraw();
  }

  @Override
  protected void redraw() {
  }

  private void arrange(int par) {
    int dim = DatabaseUtil.dimensionality(relation);
    Matrix hicsmat = new Matrix(dim, dim, 0.);
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
    
    ArrayList<ArrayDBIDs> subspaceIndex = buildOneDimIndexes(relation, ids);
    Set<HiCSSubspace> subspaces = calculateSubspaces(relation, subspaceIndex);
    
    for (HiCSSubspace hics : subspaces){
      hicsmat.set(hics.nextSetBit(0), hics.nextSetBit(hics.nextSetBit(0) + 1), hics.contrast);
      hicsmat.set(hics.nextSetBit(hics.nextSetBit(0) + 1), hics.nextSetBit(0), hics.contrast);
    }
    
    end = System.nanoTime();
    
    if (logger.isVerbose()){
      logger.verbose("Runtime HiCSDimensionOrder: " + (end - start)/1000000. + " ms for a dataset with " + ids.size() + " objects and " + dim + " dimensions");
    }
    
    ArrayList<Integer> arrange = new ArrayList<Integer>();

    int[] first = getMax(hicsmat);
    arrange.add(first[0]);
    arrange.add(1, first[1]);
    int[] pos;
    pos = first.clone();

    blank(hicsmat, pos[0]);
    blank(hicsmat, pos[1]);
    int tmp1, tmp2;

    for(int i = 2; i < dim; i++) {
      tmp1 = getMax(hicsmat, pos[0]);
      tmp2 = getMax(hicsmat, pos[1]);

      if(Math.abs(hicsmat.get(pos[0], tmp1)) > Math.abs(hicsmat.get(pos[1], tmp2))) {
        arrange.add(0, tmp1);
        blank(hicsmat, tmp1);
        pos[0] = tmp1;
      }
      else {
        arrange.add(arrange.size(), tmp2);
        blank(hicsmat, tmp2);
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
  
  
  /**
   * Identifies high contrast subspaces in a given full-dimensional database
   * 
   * @param relation the relation the HiCS should be evaluated for
   * @param subspaceIndex Subspace indexes
   * @return a set of high contrast subspaces
   */
  private Set<HiCSSubspace> calculateSubspaces(Relation<? extends NumberVector<?>> relation, ArrayList<ArrayDBIDs> subspaceIndex) {
    final int dbdim = DatabaseUtil.dimensionality(relation);

    TreeSet<HiCSSubspace> subspaceList = new TreeSet<HiCSSubspace>(HiCSSubspace.SORT_BY_SUBSPACE);
    TopBoundedHeap<HiCSSubspace> dDimensionalList = new TopBoundedHeap<HiCSSubspace>(cutoff, HiCSSubspace.SORT_BY_CONTRAST_ASC);
    
    // compute two-element sets of subspaces
    for(int i = 0; i < dbdim; i++) {
      for(int j = i + 1; j < dbdim; j++) {
        HiCSSubspace ts = new HiCSSubspace();
        ts.set(i);
        ts.set(j);
        calculateContrast(relation, ts, subspaceIndex);
        dDimensionalList.add(ts);
      }
    }
    subspaceList.addAll(dDimensionalList);
 
    return subspaceList;
  }
  
  
  /**
   * Calculates "index structures" for every attribute, i.e. sorts a
   * ModifiableArray of every DBID in the database for every dimension and
   * stores them in a list
   * 
   * @param relation Relation to index
   * @return List of sorted objects
   */
  private ArrayList<ArrayDBIDs> buildOneDimIndexes(Relation<? extends NumberVector<?>> relation, DBIDs ids) {
    final int dim = DatabaseUtil.dimensionality(relation);
    ArrayList<ArrayDBIDs> subspaceIndex = new ArrayList<ArrayDBIDs>(dim + 1);

    SortDBIDsBySingleDimension comp = new VectorUtil.SortDBIDsBySingleDimension(relation);
    for(int i = 1; i <= dim; i++) {
    //  ArrayModifiableDBIDs amDBIDs = DBIDUtil.newArray(relation.getDBIDs());
      ArrayModifiableDBIDs amDBIDs = DBIDUtil.newArray(ids);
      comp.setDimension(i);
      amDBIDs.sort(comp);
      subspaceIndex.add(amDBIDs);
    }

    return subspaceIndex;
  }
  
  /**
   * Calculates the actual contrast of a given subspace
   * 
   * @param relation
   * @param subspace
   * @param subspaceIndex Subspace indexes
   */
  private void calculateContrast(Relation<? extends NumberVector<?>> relation, HiCSSubspace subspace, ArrayList<ArrayDBIDs> subspaceIndex) {
    final int card = subspace.cardinality();
    final double alpha1 = Math.pow(alpha, (1.0 / card));
    final int windowsize = (int) (relation.size() * alpha1);

    int retries = 0;
    double deviationSum = 0.0;
    for(int i = 0; i < m; i++) {
      // Choose a random set bit.
      int chosen = -1;
      for(int tmp = random.nextInt(card); tmp >= 0; tmp--) {
        chosen = subspace.nextSetBit(chosen + 1);
      }
      // initialize sample
      DBIDs conditionalSample = relation.getDBIDs();

      for(int j = subspace.nextSetBit(0); j >= 0; j = subspace.nextSetBit(j + 1)) {
        if(j == chosen) {
          continue;
        }
        ArrayDBIDs sortedIndices = subspaceIndex.get(j);
        ArrayModifiableDBIDs indexBlock = DBIDUtil.newArray();
        // initialize index block
        int start = random.nextInt(relation.size() - windowsize);
        for(int k = start; k < start + windowsize; k++) {
          indexBlock.add(sortedIndices.get(k)); // select index block
        }

        conditionalSample = DBIDUtil.intersection(conditionalSample, indexBlock);
      }
      if(conditionalSample.size() < 10) {
        retries++;
 
        if(retries < MAX_RETRIES) {
          i--;
          continue;
        }
      }
      // Project conditional set
      double[] sampleValues = new double[conditionalSample.size()];
      {
        int l = 0;
        for(DBIDIter id = conditionalSample.iter(); id.valid(); id.advance()) {
          sampleValues[l] = relation.get(id).doubleValue(chosen + 1);
          l++;
        }
      }
      // Project full set
      double[] fullValues = new double[relation.size()];
      {
        int l = 0;
        for(DBIDIter id = subspaceIndex.get(chosen).iter(); id.valid(); id.advance()) {
          fullValues[l] = relation.get(id).doubleValue(chosen + 1);
          l++;
        }
      }
      double contrast = statTest.deviation(fullValues, sampleValues);
      if(Double.isNaN(contrast)) {
        i--;
        continue;
      }
      deviationSum += contrast;
    }
    subspace.contrast = deviationSum / m;
  }

  /**
   * BitSet that holds a contrast value as field. Used for the representation of
   * a subspace in HiCS
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class HiCSSubspace extends BitSet {
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    /**
     * The HiCS contrast value
     */
    protected double contrast;

    /**
     * Constructor.
     */
    public HiCSSubspace() {
      super();
    }

    @Override
    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("[contrast=").append(contrast);
      for(int i = nextSetBit(0); i >= 0; i = nextSetBit(i + 1)) {
        buf.append(" ").append(i + 1);
      }
      buf.append("]");
      return buf.toString();
    }
    
    /**
     * Sort subspaces by their actual subspace.
     */
    public static Comparator<HiCSSubspace> SORT_BY_CONTRAST_ASC = new Comparator<HiCSSubspace>() {
      @Override
      public int compare(HiCSSubspace o1, HiCSSubspace o2) {
        if(o1.contrast == o2.contrast) {
          return 0;
        }
        return o1.contrast > o2.contrast ? 1 : -1;
      }
    };

    /**
     * Sort subspaces by their actual subspace.
     */
    public static Comparator<HiCSSubspace> SORT_BY_CONTRAST_DESC = new Comparator<HiCSSubspace>() {
      @Override
      public int compare(HiCSSubspace o1, HiCSSubspace o2) {
        if(o1.contrast == o2.contrast) {
          return 0;
        }
        return o1.contrast < o2.contrast ? 1 : -1;
      }
    };

    /**
     * Sort subspaces by their actual subspace.
     */
    public static Comparator<HiCSSubspace> SORT_BY_SUBSPACE = new Comparator<HiCSSubspace>() {
      @Override
      public int compare(HiCSSubspace o1, HiCSSubspace o2) {
        int dim1 = o1.nextSetBit(0);
        int dim2 = o2.nextSetBit(0);
        while(dim1 >= 0 && dim2 >= 0) {
          if(dim1 < dim2) {
            return -1;
          }
          else if(dim1 > dim2) {
            return 1;
          }
          dim1 = o1.nextSetBit(dim1 + 1);
          dim2 = o2.nextSetBit(dim2 + 1);
        }
        return 0;
      }
    };
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
    private static final String NAME = "HiCS Dimension Order";

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new HiCSDimensionOrder(task);
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
