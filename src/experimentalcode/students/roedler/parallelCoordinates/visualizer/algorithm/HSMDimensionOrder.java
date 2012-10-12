package experimentalcode.students.roedler.parallelCoordinates.visualizer.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.projector.ParallelPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.parallel.AbstractParallelVisualization;
import experimentalcode.students.roedler.parallelCoordinates.gui.MenuOwner;
import experimentalcode.students.roedler.parallelCoordinates.gui.SubMenu;

/**
 * HSM algorithm to arrange dimensions
 * 
 * Reference: <br>
 * Andrada Tatu, Georgia Albuquerque, Martin Eisemann, Peter Bak, Holger
 * Theisel, Marcus A. Magnor, and Daniel A. Keim. Automated Analytical Methods
 * to Support Visual Exploration of High- Dimensional Data. <br>
 * IEEE Trans. Vis. Comput. Graph., pages 584–597, 2011.
 * 
 * @author Robert Rödler
 */
public class HSMDimensionOrder extends AbstractParallelVisualization<NumberVector<?>> implements MenuOwner {
  /**
   * Generic tags to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String CLUSTERORDER = "HSM Dimension Order";

  /**
   * the result we work on
   */
  private Clustering<Model> clustering;

  private int sum;

  private int mode = 2;

  /**
   * Angular resolution
   */
  private final static int steps;

  /**
   * Precomputed sinus and cosinus lookup tables.
   */
  private final static double[] cost, sint;

  /**
   * Precompute sinus and cosinus tables.
   */
  static {
    steps = 180;
    cost = new double[steps];
    sint = new double[steps];
    {
      double step = Math.toRadians(360. / steps);
      double ang = 0.;
      for(int i = 0; i < steps; i++, ang += step) {
        cost[i] = Math.cos(ang);
        sint[i] = Math.sin(ang);
      }
    }
  }

  /**
   * The Logger for this class
   */
  private static final Logging logger = Logging.getLogger(HSMDimensionOrder.class);

  /**
   * Constructor.
   * 
   * @param task VisualizationTask
   */
  public HSMDimensionOrder(VisualizationTask task) {
    super(task);
    clustering = task.getResult();
    sum = 0;
    incrementalRedraw();
  }

  @Override
  protected void redraw() {
  }

  private void arrange(int par) {
    final int dim = RelationUtil.dimensionality(relation);
    final int resolution = 500;
    Matrix hsmmat = new Matrix(dim, dim, 0.);
    int[][] pic = new int[resolution][resolution];

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

    int progress = 0;
    int max = ((dim * dim) - dim) / 2;

    for(int i = 0; i < dim - 1; i++) {
      // if (!proj.isAxisVisible(i - 1)) {
      // continue;
      // }

      for(int j = i + 1; j < dim; j++) {
        // if (!proj.isAxisVisible(j - 1)) { continue; }

        for(int m = 0; m < resolution; m++) {
          Arrays.fill(pic[m], 0);
        }

        for(DBIDIter id = ids.iter(); id.valid(); id.advance()) {
          NumberVector<?> obj = relation.get(id);
          double xi = proj.getAxisScale(i - 1).getScaled(obj.doubleValue(i));
          double xj = proj.getAxisScale(j - 1).getScaled(obj.doubleValue(j));
          drawLine(0, (int) (resolution * xi), resolution - 1, (int) (resolution * xj), pic);
        }

        int[][] hough = houghTransformation(pic);

        double median = (double) sum / (double) (hough[0].length * hough.length);
        double bigCells = (double) sumMatrix(splitMatrixIntoCells(hough, mode, median));

        hsmmat.set(i, j, 1. - (bigCells / 2500.));
        hsmmat.set(j, i, 1. - (bigCells / 2500.));

        if(logger.isVerbose()) {
          progress++;
          logger.verbose("HSM Progress " + progress + " von " + max);
        }
        else {
          progress++;
          System.out.println("HSM Progress " + progress + " von " + max);
        }
      }
    }
    end = System.nanoTime();

    if(logger.isVerbose()) {
      logger.verbose("Runtime HSMDimensionOrder: " + (end - start) / 1000000. + " ms for a dataset with " + ids.size() + " objects and " + dim + " dimensions");
    }

    ArrayList<Integer> arrange = new ArrayList<Integer>();

    int[] first = getMax(hsmmat);
    arrange.add(first[0]);
    arrange.add(1, first[1]);
    int[] pos;
    pos = first.clone();

    blank(hsmmat, pos[0]);
    blank(hsmmat, pos[1]);
    int tmp1, tmp2;

    for(int i = 2; i < dim; i++) {
      tmp1 = getMax(hsmmat, pos[0]);
      tmp2 = getMax(hsmmat, pos[1]);

      if(Math.abs(hsmmat.get(pos[0], tmp1)) > Math.abs(hsmmat.get(pos[1], tmp2))) {
        arrange.add(0, tmp1);
        blank(hsmmat, tmp1);
        pos[0] = tmp1;
      }
      else {
        arrange.add(arrange.size(), tmp2);
        blank(hsmmat, tmp2);
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

  private int sumMatrix(int[][] mat) {
    int ret = 0;
    for(int i = 0; i < mat[0].length; i++) {
      for(int j = 0; j < mat.length; j++) {
        if(mat[i][j] == 1) {
          ret++;
        }
      }
    }
    return ret;
  }

  private int[][] splitMatrixIntoCells(int[][] mat, int mode, double median) {
    int[][] ret = new int[50][50];

    double stepX = mat[0].length / 50.;
    double stepY = mat.length / 50.;

    for(int i = 0; i < 50; i++) {
      for(int j = 0; j < 50; j++) {
        ret[i][j] = 0;
        double sum = 0.;
        int cells = 0;
        boolean bigger = true;

        for(int k = (int) (i * stepY); k < (int) ((i + 1) * stepY); k++) {
          for(int l = (int) (j * stepX); l < (int) ((j + 1) * stepX); l++) {
            if(mode == 1 && mat[k][l] > median) {
              ret[i][j] = 1;
              break;
            }
            if(mode == 2 && mat[k][l] <= median) {
              bigger = false;
              break;
            }
            sum += mat[k][l];
            cells++;
          }
        }
        if(mode == 2 && bigger) {
          ret[i][j] = 1;
        }
        if(mode == 3 && ((sum / (double) cells) > median)) {
          ret[i][j] = 1;
        }
      }
    }
    return ret;
  }

  private int[][] houghTransformation(int[][] mat) {

    // int max = (int) Math.sqrt(Math.pow((double) mat.getRowDimensionality() /
    // 2., 2.) + Math.pow((double) mat.getColumnDimensionality() / 2., 2.));
    final int max = (int) Math.sqrt(Math.pow((double) mat.length, 2.) + Math.pow((double) mat[0].length, 2.));

    int[][] ret = new int[max][steps];
    for(int i = 0; i < ret.length; i++) {
      Arrays.fill(ret[0], 0);
    }

    sum = 0;

    for(int x = 0; x < mat.length; x++) {
      for(int y = 0; y < mat[0].length; y++) {
        if(mat[x][y] > 0) {
          for(int i = 0; i < steps; i++) {
            final int d = (int) (x * cost[i] + y * sint[i]);

            if(d > 0 && d < max) {
              ret[d][i] += mat[x][y];
              sum += mat[x][y];
            }
          }
        }
      }
    }

    return ret;
  }

  // Bresenham algorithm, copied from Wikipedia
  private static void drawLine(int x0, int y0, int x1, int y1, int[][] pic) {
    if(y0 == pic[0].length) {
      y0--;
    }
    if(y1 == pic[0].length) {
      y1--;
    }
    int dx = +Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
    int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
    int err = dx + dy;

    for(;;) {
      pic[x0][y0] = 1;
      if(x0 == x1 && y0 == y1) {
        break;
      }

      final int e2 = err << 1;
      if(e2 > dy) {
        err += dy;
        x0 += sx;
      }
      if(e2 < dx) {
        err += dx;
        y0 += sy;
      }
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
    private static final String NAME = "HSM Dimension Order";

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new HSMDimensionOrder(task);
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
            task.level = VisualizationTask.LEVEL_DATA + 3;
            baseResult.getHierarchy().add(c, task);
            baseResult.getHierarchy().add(p, task);
          }
        }
      }
    }
  }
}
