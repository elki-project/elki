package experimentalcode.shared.algorithm.clustering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.output.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

public class GridBasedReferencePoints<O extends NumberVector<O,?>> extends AbstractParameterizable implements ReferencePointsHeuristic<O> {
  /**
   * OptionID for {@link #GRID_PARAM}
   */
  public static final OptionID GRID_ID = OptionID.getOrCreateOptionID("grid.steps", "The number of splits to do.");

  /**
   * Parameter to specify the sample size.
   * <p>
   * Key: {@code -grid.steps}
   * </p>
   */
  private final IntParameter GRID_PARAM = new IntParameter(GRID_ID, new GreaterConstraint(1), 2);

  /**
   * Holds the value of {@link #GRID_PARAM}.
   */
  private int gridres;
  
  public GridBasedReferencePoints() {
    super();
    addOption(GRID_PARAM);
  }

  @Override
  public Collection<O> getReferencePoints(Database<O> db) {
    Pair<O, O> minmax = DatabaseUtil.computeMinMax(db);
    
    assert(gridres >= 2);
    
    int dim = db.dimensionality();
    int gridpoints = (int)Math.pow(gridres, dim);
    
    ArrayList<O> result = new ArrayList<O>(gridpoints);
    double[] delta = new double[dim];
    for (int d = 0; d < dim; d++) {
      delta[d] = (minmax.second.getValue(d+1).doubleValue() - minmax.first.getValue(d+1).doubleValue()) / (gridres - 1);
    }
    
    double[] vec = new double[dim];
    for (int i = 0; i < gridpoints; i++) {
      int acc = i;
      for (int d = 0; d < dim; d++) {
        int coord = acc % gridres;
        acc = acc / gridres;
        vec[d] = minmax.first.getValue(d+1).doubleValue() + coord * delta[d];
      }
      O newp = minmax.first.newInstance(vec);
      logger.debug("New reference point: " + FormatUtil.format(vec));
      result.add(newp);
    }

    return result;
  }

  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);
    
    gridres = GRID_PARAM.getValue();
    
    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }
}
