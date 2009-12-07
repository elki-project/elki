package experimentalcode.lisa;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDatabase;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromAssociation;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.Description;
/**
 * outlier have smallest GMOD_PROB. lowest probability. 
 * @author lisa
 *
 * @param <V>
 */
public class GaussianModelOutlierDetection<V extends NumberVector<V,Double>> extends AbstractAlgorithm<V,MultiResult> {
	
  MultiResult result;
	
	public static final AssociationID<Double> GMOD_PROB = AssociationID.getOrCreateAssociationID("gmod.prob", Double.class);
	public static final AssociationID<Double> GMOD_MAXPROB = AssociationID.getOrCreateAssociationID("gmod.maxprob", Double.class);
	@Override
	protected MultiResult runInTime(Database<V> database) throws IllegalStateException {
		double maxProb = 0;
	  V mean = DatabaseUtil.centroid(database);
		debugFine(mean.toString());
		Matrix covarianceMatrix = DatabaseUtil.covarianceMatrix(database, mean);
		debugFine(covarianceMatrix.toString());
		Matrix covarianceTransposed = covarianceMatrix.inverse();
		double covarianceDet = covarianceMatrix.det();
		double fakt = (1.0/(Math.sqrt(Math.pow(2*Math.PI, database.dimensionality())*(covarianceDet)))); 
		//for each object compute mahalanobis distance
		 for (Integer id : database) {
             V x = database.get(id);
             Vector x_minus_mean = x.minus(mean).getColumnVector();
             double mDist = x_minus_mean.transposeTimes(covarianceTransposed).times(x_minus_mean).get(0,0);
             double prob = fakt * Math.exp(- mDist/2.0);
             if(prob > maxProb) {
               maxProb = prob;
             }
             
             database.associate(GMOD_PROB, id, prob); 
		 }


		 AnnotationFromDatabase<Double, V> res1 = new AnnotationFromDatabase<Double, V>(database, GMOD_PROB);
	        // Ordering
	        OrderingFromAssociation<Double, V> res2 = new OrderingFromAssociation<Double, V>(database, GMOD_PROB, true); 
	        // combine results.
	        result = new MultiResult();
	        ResultUtil.setGlobalAssociation(result, GMOD_MAXPROB, maxProb);
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
	}
	

}
