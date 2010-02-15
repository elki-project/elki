package experimentalcode.shared.algorithm.clustering;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
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
 * @param <O> Object type
 */
public class StarBasedReferencePoints<O extends NumberVector<O, ?>> extends AbstractLoggable implements ReferencePointsHeuristic<O> {
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
   * Constructor, Parameterizable style.
   */
  public StarBasedReferencePoints(Parameterization config) {
    super();
    if(config.grab(this, NOCENTER_FLAG)) {
      nocenter = NOCENTER_FLAG.getValue();
    }
    if(config.grab(this, SCALE_PARAM)) {
      scale = SCALE_PARAM.getValue();
    }
  }

  @Override
  public Collection<O> getReferencePoints(Database<O> db) {
    O prototype = db.get(db.iterator().next());

    int dim = db.dimensionality();

    // Compute minimum, maximum and centroid
    double[] centroid = new double[dim];
    double[] min = new double[dim];
    double[] max = new double[dim];
    for(int d = 0; d < dim; d++) {
      centroid[d] = 0;
      min[d] = Double.MAX_VALUE;
      max[d] = -Double.MAX_VALUE;
    }
    for(Integer objID : db) {
      O obj = db.get(objID);
      for(int d = 0; d < dim; d++) {
        double val = obj.doubleValue(d + 1);
        centroid[d] += val;
        min[d] = Math.min(min[d], val);
        max[d] = Math.max(max[d], val);
      }
    }
    // finish centroid, scale min, max
    for(int d = 0; d < dim; d++) {
      centroid[d] = centroid[d] / db.size();
      min[d] = (min[d] - centroid[d]) * scale + centroid[d];
      max[d] = (max[d] - centroid[d]) * scale + centroid[d];
    }

    ArrayList<O> result = new ArrayList<O>(2 * dim + 1);
    if(!nocenter) {
      result.add(prototype.newInstance(centroid));
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
      result.add(prototype.newInstance(vec));
      vec[i] = max[i];
      result.add(prototype.newInstance(vec));
    }

    return result;
  }
}
