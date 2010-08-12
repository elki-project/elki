package de.lmu.ifi.dbs.elki.utilities.referencepoints;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * Star-based strategy to pick reference points.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Object type
 */
public class StarBasedReferencePoints<V extends NumberVector<V, ?>> implements ReferencePointsHeuristic<V> {
  /**
   * OptionID for {@link #NOCENTER_FLAG}
   */
  public static final OptionID NOCENTER_ID = OptionID.getOrCreateOptionID("star.nocenter", "Do not use the center as extra reference point.");

  /**
   * Parameter to specify the grid resolution.
   * <p>
   * Key: {@code -star.nocenter}
   * </p>
   */
  private final Flag NOCENTER_FLAG = new Flag(NOCENTER_ID);

  /**
   * OptionID for {@link #SCALE_PARAM}
   */
  public static final OptionID SCALE_ID = OptionID.getOrCreateOptionID("star.scale", "Scale the reference points by the given factor. This can be used to obtain reference points outside the used data space.");

  /**
   * Parameter to specify the extra scaling of the space, to allow
   * out-of-data-space reference points.
   * <p>
   * Key: {@code -star.scale}
   * </p>
   */
  private final DoubleParameter SCALE_PARAM = new DoubleParameter(SCALE_ID, new GreaterEqualConstraint(0.0), 1.0);

  /**
   * Holds the value of {@link #NOCENTER_FLAG}.
   */
  protected boolean nocenter;

  /**
   * Holds the value of {@link #SCALE_PARAM}.
   */
  protected double scale;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public StarBasedReferencePoints(Parameterization config) {
    super();
    config = config.descend(this);
    if(config.grab(NOCENTER_FLAG)) {
      nocenter = NOCENTER_FLAG.getValue();
    }
    if(config.grab(SCALE_PARAM)) {
      scale = SCALE_PARAM.getValue();
    }
  }

  @Override
  public <T extends V> Collection<V> getReferencePoints(Database<T> db) {
    Database<V> database = DatabaseUtil.databaseUglyVectorCast(db);
    V factory = database.getObjectFactory();

    int dim = database.dimensionality();

    // Compute minimum, maximum and centroid
    double[] centroid = new double[dim];
    double[] min = new double[dim];
    double[] max = new double[dim];
    for(int d = 0; d < dim; d++) {
      centroid[d] = 0;
      min[d] = Double.MAX_VALUE;
      max[d] = -Double.MAX_VALUE;
    }
    for(DBID objID : database) {
      V obj = database.get(objID);
      for(int d = 0; d < dim; d++) {
        double val = obj.doubleValue(d + 1);
        centroid[d] += val;
        min[d] = Math.min(min[d], val);
        max[d] = Math.max(max[d], val);
      }
    }
    // finish centroid, scale min, max
    for(int d = 0; d < dim; d++) {
      centroid[d] = centroid[d] / database.size();
      min[d] = (min[d] - centroid[d]) * scale + centroid[d];
      max[d] = (max[d] - centroid[d]) * scale + centroid[d];
    }

    ArrayList<V> result = new ArrayList<V>(2 * dim + 1);
    if(!nocenter) {
      result.add(factory.newInstance(centroid));
    }
    // Plus axis end points through centroid
    double[] vec = new double[dim];
    for(int i = 0; i < dim; i++) {
      for(int d = 0; d < dim; d++) {
        if(d != i) {
          vec[d] = centroid[d];
        }
      }
      vec[i] = min[i];
      result.add(factory.newInstance(vec));
      vec[i] = max[i];
      result.add(factory.newInstance(vec));
    }

    return result;
  }
}