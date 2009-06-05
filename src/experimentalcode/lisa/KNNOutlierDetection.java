package experimentalcode.lisa;

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


/**
 * Outlier Detection based on the distance of a point from its kth nearest neighbor. 
 * 
 * 
 * Based on:
 * S. Ramaswamy, R. Rastogi, K. Shim:
 * Efficient Algorithms for Mining Outliers from Large Data Sets
 * @author lisa
 *
 * @param <O>
 */

public class KNNOutlierDetection <O extends DatabaseObject, D extends DoubleDistance> extends DistanceBasedAlgorithm<O , DoubleDistance , MultiResult> {
	
	public static final OptionID K_ID = OptionID.getOrCreateOptionID(
		      "knno.k",
		      "kth nearest neighbor"
		  );

	/*public static final OptionID N_ID = OptionID.getOrCreateOptionID(
				      "knno.n",
				      "number of outliers that are searched"
				  );*/
		
		public static final AssociationID<Double> KNNO_ODEGREE= AssociationID.getOrCreateAssociationID("knno_odegree", Double.class);
		 public static final AssociationID<Double> KNNO_MAXODEGREE = AssociationID.getOrCreateAssociationID("knno_maxodegree", Double.class);
		/**
		   * Parameter to specify the kth nearest neighbor,
		   * 
		   * <p>Key: {@code -knno.k} </p>
		   */
		  private final IntParameter K_PARAM = new IntParameter(K_ID);
		  
		  /*
		   * Parameter to specify the number of outliers
		   * 
		   * <p>Key: {@code -knno.n} </p>
		   /
		  private final IntParameter N_PARAM = new IntParameter(N_ID);*/
		  /**
		   * Holds the value of {@link #K_PARAM}.
		   */
		  private int k;
		  /**
		   * Holds the value of {@link #N_PARAM}.
		   */
		  //private int n;
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
		    // number of outliers
		    //addOption(N_PARAM);
		    }
		  
		  /**
		   * Calls the super method
		   * and sets additionally the values of the parameter
		   * {@link #K_PARAM}, {@link #N_PARAM} 
		   */
		  @Override
		  public String[] setParameters(String[] args) throws ParameterException {
		      String[] remainingParameters = super.setParameters(args);
		      k = K_PARAM.getValue();
			 // n = N_PARAM.getValue(); 
		      return remainingParameters;
		  }

		  /**
		   * Runs the algorithm in the timed evaluation part.
		   */

      @Override
		  protected MultiResult runInTime(Database<O> database) throws IllegalStateException {
			  double maxodegree = 0;
        getDistanceFunction().setDatabase(database, isVerbose(), isTime());
			  
				// compute distance to the k nearest neighbor. n objects with the highest distance are flagged as outliers
        for(Integer id : database){
				  //distance to the kth nearest neighbor
				 Double dkn = database.kNNQueryForID(id,  k, getDistanceFunction()).get(k-1).getDistance().getValue();

				 if (logger.isDebugging()) {
				   logger.debugFine(dkn + "  dkn");
		        }
		      if(dkn > maxodegree) {
		        maxodegree = dkn;
		      }
				  database.associate(KNNO_ODEGREE, id, dkn);
			  }
			  AnnotationFromDatabase<Double, O> res1 = new AnnotationFromDatabase<Double, O>(database, KNNO_ODEGREE);
		        // Ordering
		        OrderingFromAssociation<Double, O> res2 = new OrderingFromAssociation<Double, O>(database,KNNO_ODEGREE, true); 
		        // combine results.
		        result = new MultiResult();
		        result.addResult(res1);
		        result.addResult(res2);
		        ResultUtil.setGlobalAssociation(result, KNNO_MAXODEGREE, maxodegree);
		        return result;
				

			 
		  }

		@Override
		public Description getDescription() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public MultiResult getResult() {
			return result;
		}}
			  
			  
			  