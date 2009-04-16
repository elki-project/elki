package experimentalcode.lisa;

import java.util.Iterator;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.result.AnnotationsFromDatabase;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromAssociation;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;




public class KNNOutlierDetection <O extends DatabaseObject, D extends Distance<D>> extends DistanceBasedAlgorithm<O , D , MultiResult> {
	
	public static final OptionID K_ID = OptionID.getOrCreateOptionID(
		      "knno.k",
		      "kth nearest neighbor"
		  );

	public static final OptionID N_ID = OptionID.getOrCreateOptionID(
				      "knno.n",
				      "number of outliers that are searched"
				  );
		
		public static final AssociationID<Distance> KNNO_ODEGREE= AssociationID.getOrCreateAssociationID("knno_odegree", Distance.class);
		/**
		   * Parameter to specify the kth nearest neighbor,
		   * 
		   * <p>Key: {@code -knno.k} </p>
		   */
		  private final IntParameter K_PARAM = new IntParameter(K_ID);
		  
		  /**
		   * Parameter to specify the number of outliers
		   * 
		   * <p>Key: {@code -knno.n} </p>
		   */
		  private final IntParameter N_PARAM = new IntParameter(N_ID);
		  /**
		   * Holds the value of {@link #K_PARAM}.
		   */
		  private int k;
		  /**
		   * Holds the value of {@link #N_PARAM}.
		   */
		  private int n;
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
		    addOption(N_PARAM);
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
			  n = N_PARAM.getValue(); 
		      return remainingParameters;
		  }

		  /**
		   * Runs the algorithm in the timed evaluation part.
		   */

      @Override
		  protected MultiResult runInTime(Database<O> database) throws IllegalStateException {
			  getDistanceFunction().setDatabase(database, isVerbose(), isTime());
			  Iterator<Integer> iter = database.iterator();
			  Integer id;
			  //compute distance to the k nearest neighbor. n objects with the highest distance are flagged as outliers
			  while(iter.hasNext()){
				 id = iter.next();
				  //distance to the kth nearest neighbor
				 D dkn = database.kNNQueryForID(id,  k, getDistanceFunction()).get(k-1).getDistance();
				 if (logger.isDebugging()) {
				   logger.debugFine(dkn + "  dkn");
		        }
		      
				  database.associate(KNNO_ODEGREE, id, dkn);
			  }
			  AnnotationsFromDatabase<O, D> res1 = new AnnotationsFromDatabase<O, D>(database);
		       res1.addAssociation((AssociationID<D>)KNNO_ODEGREE);
		        // Ordering
		        OrderingFromAssociation<D, O> res2 = new OrderingFromAssociation<D, O>(database,(AssociationID<D>) KNNO_ODEGREE, true); 
		        // combine results.
		        result = new MultiResult();
		        result.addResult(res1);
		        result.addResult(res2);
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
			  
			  
			  