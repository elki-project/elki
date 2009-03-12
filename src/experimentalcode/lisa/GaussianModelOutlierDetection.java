package experimentalcode.lisa;

import java.util.Iterator;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.Util;

public class GaussianModelOutlierDetection<V extends RealVector<V,N>, N extends Number> extends AbstractAlgorithm<V,MultiResult> {
	MultiResult result;
	
	@Override
	protected MultiResult runInTime(Database<V> database) throws IllegalStateException {
		V mean = Util.centroid(database);
		V meanNeg = mean.negativeVector();
		Matrix covarianzTransposed = Util.covarianceMatrix(database, mean).transpose();	
		//for each object compute mahalanobis distance
		 for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); ) {
             Integer id = iter.next();
             V x = database.get(id);
             V dist = x.plus(meanNeg);
  //           V distTransp = dist.;
             
		 }
		 
		 
		return null;
	}

	@Override
	public Description getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MultiResult getResult() {
		// TODO Auto-generated method stub
		return null;
	}
	

}
