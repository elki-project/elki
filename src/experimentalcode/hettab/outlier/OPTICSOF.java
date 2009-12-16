package experimentalcode.hettab.outlier;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

/**
 *
 * @author admin
 *
 * @param <O>
 * @param <D>
 */
public class OPTICSOF<O extends DatabaseObject, D extends Distance<D>> extends DistanceBasedAlgorithm<O, D, MultiResult> {

	 /**
	   * OptionID for {@link #EPSILON_PARAM}
	   */
	  public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("optics.epsilon", "The maximum radius of the neighborhood to be considered.");

	  /**
	   * Parameter to specify the maximum radius of the neighborhood to be
	   * considered, must be suitable to the distance function specified.
	   * <p>
	   * Key: {@code -optics.epsilon}
	   * </p>
	   */
	  private final PatternParameter EPSILON_PARAM = new PatternParameter(EPSILON_ID);

	  /**
	   * Hold the value of {@link #EPSILON_PARAM}.
	   */
	  private String epsilon;

	  /**
	   * OptionID for {@link #MINPTS_PARAM}
	   */
	  public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID("optics.minpts", "Threshold for minimum number of points in " + "the epsilon-neighborhood of a point.");

	  /**
	   * Parameter to specify the threshold for minimum number of points in the
	   * epsilon-neighborhood of a point, must be an integer greater than 0.
	   * <p>
	   * Key: {@code -optics.minpts}
	   * </p>
	   */
	  private final IntParameter MINPTS_PARAM = new IntParameter(MINPTS_ID, new GreaterConstraint(0));

	  /**
	   * Holds the value of {@link #MINPTS_PARAM}.
	   */
	  private int minpts;

	  /**
	   * Provides the result of the algorithm.
	   */
	  private MultiResult result;

	  /**
	   * Holds a set of processed ids.
	   */
	  private Set<Integer> processedIDs;


    /**
     *
     */
	public Description getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 *
	 */
	public ClusterOrderResult<D> getResult() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 *
	 */
	@Override
	protected MultiResult runInTime(Database<O> database)
			throws IllegalStateException {
		HashMap<Integer, List<DistanceResultPair<D>>> nMinPts = new HashMap<Integer, List<DistanceResultPair<D>>>();
		HashMap<Integer, Double> coreDist = new HashMap<Integer, Double>();

		for(Integer id : database){
			List<DistanceResultPair<D>> neighbors = database.kNNQueryForID(id, minpts, getDistanceFunction());
			nMinPts.put(id,neighbors);
					}
		return null;
	}

}
