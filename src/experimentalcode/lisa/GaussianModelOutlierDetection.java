package experimentalcode.lisa;

import java.util.Iterator;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.AnnotationsFromDatabase;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromAssociation;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.Description;

public class GaussianModelOutlierDetection<V extends RealVector<V,Double>> extends AbstractAlgorithm<V,MultiResult> {
	MultiResult result;
	public static final AssociationID<Double> GMOD_MDIST = AssociationID.getOrCreateAssociationID("gmod.mdist", Double.class);
	@Override
	protected MultiResult runInTime(Database<V> database) throws IllegalStateException {
		V mean = DatabaseUtil.centroid(database);
		V meanNeg = mean.negativeVector();
		Matrix covarianceTransposed = DatabaseUtil.covarianceMatrix(database, mean).transpose();	
		//for each object compute mahalanobis distance
		 for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); ) {
             Integer id = iter.next();
             V x = database.get(id);
             Vector x_minus_mean = x.plus(meanNeg).getColumnVector();
             double mDist = x_minus_mean.transpose().times(covarianceTransposed).times(x_minus_mean).get(0,0);
             database.associate(GMOD_MDIST, id, mDist); 
		 }
		 AnnotationsFromDatabase<V, Double> res1 = new AnnotationsFromDatabase<V, Double>(database);
	        res1.addAssociation("MDIST", GMOD_MDIST);
	        // Ordering
	        OrderingFromAssociation<Double, V> res2 = new OrderingFromAssociation<Double, V>(database, GMOD_MDIST, true); 
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
	}
	

}
