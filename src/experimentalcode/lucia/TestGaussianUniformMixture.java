package experimentalcode.lucia;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.outlier.GaussianUniformMixture;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.evaluation.roc.ComputeROCCurve;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Tests the GaussianUniformMixture algorithm. 
 * @author lucia
 * 
 */
public class TestGaussianUniformMixture extends OutlierTest implements JUnit4Test{
	// the following values depend on the data set used!
	static String dataset = "data/testdata/unittests/subspace-hierarchy.csv";
	static double l = 0.01;


	@Test
	public void testGaussianUniformMixture() throws UnableToComplyException {
		ArrayList<Pair<Double, DBID>> pair_scoresIds = new ArrayList<Pair<Double, DBID>>();

		Database<DoubleVector> db = getDatabase(dataset);

		
		//Parameterization
		ListParameterization params = new ListParameterization();
		params.addParameter(GaussianUniformMixture.L_ID, l);
		params.addParameter(ComputeROCCurve.POSITIVE_CLASS_NAME_ID, "Noise");

		
		// run GaussianUniformMixture
		OutlierResult result = runGaussianUniformMixture(db, params);
		AnnotationResult<Double> scores = result.getScores();

		for(DBID id : db.getIDs()) {
			pair_scoresIds.add(new Pair<Double, DBID>(scores.getValueFor(id),id));
		}

		//get ROC AUC
		List<Double> auc = getROCAUC(db, result, params);
		Iterator<Double> iter = auc.listIterator();
		double actual;
		while(iter.hasNext()){
			actual = iter.next();
			System.out.println("GaussianUniformMixture(l="+ l + ") ROC AUC: " + actual);
			Assert.assertEquals("ROC AUC not right.", 0.9804999999999999, actual, 0.00001);
		}
	}

	private static OutlierResult runGaussianUniformMixture(Database<DoubleVector> db, ListParameterization params) {
		// setup algorithm
		GaussianUniformMixture<DoubleVector> gaussianUniformMixture = null;
		Class<GaussianUniformMixture<DoubleVector>> gaussianUniformMixturecls = ClassGenericsUtil.uglyCastIntoSubclass(GaussianUniformMixture.class);
		gaussianUniformMixture = params.tryInstantiate(gaussianUniformMixturecls, gaussianUniformMixturecls);
		params.failOnErrors();

		// run GaussianUniformMixture on database
		return gaussianUniformMixture.run(db);
	}
}
