package experimentalcode.lisa;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDatabase;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromAssociation;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.progress.FiniteProgress;


/**
 * Outlier Detection based on the distance of a point from its kth nearest neighbor. 
 * 
 * 
 * Based on:
 * S. Ramaswamy, R. Rastogi, K. Shim:
 * Efficient Algorithms for Mining Outliers from Large Data Sets.
 * In: Proc. of the Int. Conf. on Management of Data, Dallas, Texas, 2000.
 * 
 * 
 * @author lisa
 *
 * @param <O>
 */

public class KNNOutlierDetection <O extends DatabaseObject, D extends DoubleDistance> extends DistanceBasedAlgorithm<O , DoubleDistance , MultiResult> {
	
	public static final OptionID K_ID = OptionID.getOrCreateOptionID(
		      "knno.k",
		      "kth nearest neighbor"
		  );

		public static final AssociationID<Double> KNNO_KNNDISTANCE= AssociationID.getOrCreateAssociationID("knno_knndistance", Double.class);
		 public static final AssociationID<Double> KNNO_MAXODEGREE = AssociationID.getOrCreateAssociationID("knno_maxodegree", Double.class);
		/**
		   * Parameter to specify the kth nearest neighbor,
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
		    //debug = true;
		    // kth nearest neighbor
		    addOption(K_PARAM);
		    }
		  
		  /**
		   * Calls the super method
		   * and sets additionally the values of the parameter
		   * {@link #K_PARAM}, {@link #N_PARAM} 
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
		      "KNNOutlierDetection",
		      "Efficient Algorithms for Mining Outliers from Large Data Sets",
		      "Outlier Detection based on the distance of a point from its kth nearest neighbor.",
		      "S. Ramaswamy, R. Rastogi, K. Shim: " +
		      "Efficient Algorithms for Mining Outliers from Large Data Sets");
		}

		@Override
		public MultiResult getResult() {
			return result;
		}}
			  
			  
			  