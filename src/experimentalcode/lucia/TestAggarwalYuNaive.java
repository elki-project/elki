package experimentalcode.lucia;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.outlier.AggarwalYuNaive;
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
 * Tests the AggarwalYuNaive algorithm. 
 * @author lucia
 */
public class TestAggarwalYuNaive extends OutlierTest implements JUnit4Test{
	// the following values depend on the data set used!
	static String dataset = "data/testdata/unittests/3clusters-and-noise-2d.csv";
	static int k = 2;
	static int phi = 8;


	@Test
	public void testAggarwalYuNaive() throws UnableToComplyException {
		ArrayList<Pair<Double, DBID>> pair_scoresIds = new ArrayList<Pair<Double, DBID>>();

		Database<DoubleVector> db = getDatabase(dataset);

		
		//Parameterization
		ListParameterization params = new ListParameterization();
		params.addParameter(AggarwalYuNaive.K_ID, k);
		params.addParameter(AggarwalYuNaive.PHI_ID, phi);
		params.addParameter(ComputeROCCurve.POSITIVE_CLASS_NAME_ID, "Noise");

		
		// run AggarwalYuNaive
		OutlierResult result = runAggarwalYuNaive(db, params);
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
			System.out.println("AggarwalYuNaive(k="+ k + " und phi="+phi+") ROC AUC: " + actual);
			Assert.assertEquals("ROC AUC not right.", 0.80311111, actual, 0.00001);
		}
	}
	private static OutlierResult runAggarwalYuNaive(Database<DoubleVector> db, ListParameterization params) {
		// setup algorithm
		AggarwalYuNaive<DoubleVector> aggarwalYuNaive = null;
		Class<AggarwalYuNaive<DoubleVector>> aggarwalYuNaivecls = ClassGenericsUtil.uglyCastIntoSubclass(AggarwalYuNaive.class);
		aggarwalYuNaive = params.tryInstantiate(aggarwalYuNaivecls, aggarwalYuNaivecls);
		params.failOnErrors();

		// run AggarwalYuNaive on database
		return aggarwalYuNaive.run(db);
	}
}
