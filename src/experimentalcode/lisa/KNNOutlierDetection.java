package experimentalcode.lisa;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDatabase;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromAssociation;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;


/**
 * <p> Outlier Detection based on the distance of an object to its k nearest neighbor.</p>  
 * 
 * <p>Reference:<br>
 * S. Ramaswamy, R. Rastogi, K. Shim: Efficient Algorithms for Mining Outliers from Large Data Sets.</br>
 * In: Proc. of the Int. Conf. on Management of Data, Dallas, Texas, 2000.</p>
 * 
 * @author Lisa Reichert
 *
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */

public class KNNOutlierDetection <O extends DatabaseObject, D extends DoubleDistance> extends DistanceBasedAlgorithm<O , DoubleDistance , MultiResult> {
  /**
   * The association id to associate the KNNO_KNNDISTANCE of an object for the
   *KNN outlier detection algorithm.
   */
  public static final AssociationID<Double> KNNO_KNNDISTANCE= AssociationID.getOrCreateAssociationID("knno_knndistance", Double.class);
  /**
   * The association id to associate the KNNO_MAXODEGREE. Needed for the visualization. 
   */
  public static final AssociationID<Double> KNNO_MAXODEGREE = AssociationID.getOrCreateAssociationID("knno_maxodegree", Double.class);

  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID(
          "knno.k",
          "k nearest neighbor"
      );
  
  /**
		   * Parameter to specify the k nearest neighbor,
		   * 
		   * <p>Key: {@code -knno.k} </p>
		   */
		  private final IntParameter K_PARAM = new IntParameter(K_ID);
		  
		  
		  /**
		   * Holds the value of {@link #K_PARAM}.
		   */
		  private int k;

		  /**
		   * Provides the result of the algorithm.
		   */
		  MultiResult result;

		  /**
		   * Constructor, adding options to option handler.
		   */
		  public KNNOutlierDetection() {
		    super();
		    // kth nearest neighbor
		    addOption(K_PARAM);
		    }
		  
		  /**
		   * Calls the super method
		   * and sets additionally the values of the parameter
		   * {@link #K_PARAM} 
		   */
		  @Override
		  public List<String> setParameters(List<String> args) throws ParameterException {
		      List<String> remainingParameters = super.setParameters(args);
		      k = K_PARAM.getValue();
			    return remainingParameters;
		  }

		  /**
		   * Runs the algorithm in the timed evaluation part.
		   */

      @Override
		  protected MultiResult runInTime(Database<O> database) throws IllegalStateException {
			  double maxodegree = 0;
        getDistanceFunction().setDatabase(database, isVerbose(), isTime());

        if(this.isVerbose()) {
          this.verbose("computing outlier degree(distance to the k nearest neighbor");
        }
        FiniteProgress progressKNNDistance = new FiniteProgress("KNNOD_KNNDISTANCE for objects", database.size());
        int counter = 0;
        
				// compute distance to the k nearest neighbor. 
        for(Integer id : database){
          counter++;
				  //distance to the kth nearest neighbor
				 Double dkn = database.kNNQueryForID(id,  k, getDistanceFunction()).get(k-1).getDistance().getValue();

		      if(dkn > maxodegree) {
		        maxodegree = dkn;
		      }
				  database.associate(KNNO_KNNDISTANCE, id, dkn);

          if(this.isVerbose()) {
            progressKNNDistance.setProcessed(counter);
            this.progress(progressKNNDistance);
          }
        }
			  AnnotationFromDatabase<Double, O> res1 = new AnnotationFromDatabase<Double, O>(database, KNNO_KNNDISTANCE);
		        // Ordering
		        OrderingFromAssociation<Double, O> res2 = new OrderingFromAssociation<Double, O>(database,KNNO_KNNDISTANCE, true); 
		        // combine results.
		        result = new MultiResult();
		        result.addResult(res1);
		        result.addResult(res2);
		        ResultUtil.setGlobalAssociation(result, KNNO_MAXODEGREE, maxodegree);
		        return result;
				

			 
		  }

		@Override
		public Description getDescription() {
		  return new Description(
		      "KNN outlier detection",
		      "Efficient Algorithms for Mining Outliers from Large Data Sets",
		      "Outlier Detection based on the distance of an object to its k nearest neighbor.",
		      "S. Ramaswamy, R. Rastogi, K. Shim: " +
		      "Efficient Algorithms for Mining Outliers from Large Data Sets. " +
		      "In: Proc. of the Int. Conf. on Management of Data, Dallas, Texas, 2000.");
		}

		@Override
		public MultiResult getResult() {
			return result;
		}}
			  
			  
			  