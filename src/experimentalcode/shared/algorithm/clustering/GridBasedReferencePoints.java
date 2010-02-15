package experimentalcode.shared.algorithm.clustering;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Grid-based strategy to pick reference points.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public class GridBasedReferencePoints<O extends NumberVector<O, ?>> extends AbstractParameterizable implements ReferencePointsHeuristic<O> {
  /**
   * OptionID for {@link #GRID_PARAM}
   */
  public static final OptionID GRID_ID = OptionID.getOrCreateOptionID("grid.size", "The number of partitions in each dimension. Points will be placed on the edges of the grid, except for a grid size of 0, where only the mean is generated as reference point.");

  /**
   * Parameter to specify the grid resolution.
   * <p>
   * Key: {@code -grid.size}
   * </p>
   */
  private final IntParameter GRID_PARAM = new IntParameter(GRID_ID, new GreaterEqualConstraint(0), 1);

  /**
   * OptionID for {@link #GRID_SCALE_PARAM}
   */
  public static final OptionID GRID_SCALE_ID = OptionID.getOrCreateOptionID("grid.scale", "Scale the grid by the given factor. This can be used to obtain reference points outside the used data space.");

  /**
   * Parameter to specify the extra scaling of the space, to allow
   * out-of-data-space reference points.
   * <p>
   * Key: {@code -grid.oversize}
   * </p>
   */
  private final DoubleParameter GRID_SCALE_PARAM = new DoubleParameter(GRID_SCALE_ID, new GreaterEqualConstraint(0.0), 1.0);

  /**
   * Holds the value of {@link #GRID_PARAM}.
   */
  protected int gridres;

  /**
   * Holds the value of {@link #GRID_SCALE_PARAM}.
   */
  protected double gridscale;

  /**
   * Constructor, Parameterizable style.
   */
  public GridBasedReferencePoints(Parameterization config) {
    super();
    if(config.grab(this, GRID_PARAM)) {
      gridres = GRID_PARAM.getValue();
    }
    if(config.grab(this, GRID_SCALE_PARAM)) {
      gridscale = GRID_SCALE_PARAM.getValue();
    }
  }

  @Override
  public Collection<O> getReferencePoints(Database<O> db) {
    Pair<O, O> minmax = DatabaseUtil.computeMinMax(db);
    O prototype = minmax.first;

    int dim = db.dimensionality();

    // Compute mean from minmax.
    double[] mean = new double[dim];
    for(int d = 0; d < dim; d++) {
      mean[d] = (minmax.first.doubleValue(d + 1) + minmax.second.doubleValue(d + 1)) / 2;
    }

    int gridpoints = Math.max(1, (int) Math.pow(gridres + 1, dim));
    ArrayList<O> result = new ArrayList<O>(gridpoints);
    double[] delta = new double[dim];
    if(gridres > 0) {
      double halfgrid = gridres / 2.0;
      for(int d = 0; d < dim; d++) {
        delta[d] = (minmax.second.doubleValue(d + 1) - minmax.first.doubleValue(d + 1)) / gridres;
      }

      double[] vec = new double[dim];
      for(int i = 0; i < gridpoints; i++) {
        int acc = i;
        for(int d = 0; d < dim; d++) {
          int coord = acc % (gridres + 1);
          acc = acc / (gridres + 1);
          vec[d] = mean[d] + (coord - halfgrid) * delta[d] * gridscale;
        }
        O newp = prototype.newInstance(vec);
        // logger.debug("New reference point: " + FormatUtil.format(vec));
        result.add(newp);
      }
    }
    else {
      result.add(prototype.newInstance(mean));
      // logger.debug("New reference point: " + FormatUtil.format(mean));
    }

    return result;
  }
}
