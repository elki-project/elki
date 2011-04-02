package experimentalcode.lucia;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.outlier.DBOutlierDetection;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.evaluation.roc.ComputeROCCurve;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Tests the DBOutlierDetection algorithm. 
 * @author lucia
 * 
 */
public class TestDBOutlierDetection extends OutlierTest implements JUnit4Test{
	// the following values depend on the data set used!
	static String dataset = "src/experimentalcode/lucia/datensaetze/holzFeuerWasser.csv";
	static double d = 0.175;
	static double p = 0.98;

	@Test
	public void testDBOutlierDetection() throws UnableToComplyException {
		ArrayList<Pair<Double, DBID>> pair_scoresIds = new ArrayList<Pair<Double, DBID>>();

		Database<DoubleVector> db = getDatabase(dataset);

		
		//Parameterization
		ListParameterization params = new ListParameterization();
		params.addParameter(DBOutlierDetection.D_ID, d);
		params.addParameter(DBOutlierDetection.P_ID, p);
		params.addParameter(ComputeROCCurve.POSITIVE_CLASS_NAME_ID, "Noise");

		
		// run DBOutlierDetection
		OutlierResult result = runDBOutlierDetection(db, params);
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
			System.out.println("DBOutlierDetection(d="+ d + " und p=" + p + ") ROC AUC: " + actual);
			Assert.assertEquals("ROC AUC not right.", 0.98151795, actual, 0.00001);
		}
	}
	
	private static OutlierResult runDBOutlierDetection(Database<DoubleVector> db, ListParameterization params) {
		// setup algorithm
		DBOutlierDetection<DoubleVector, DoubleDistance> dboutlierdetection = null;
		Class<DBOutlierDetection<DoubleVector, DoubleDistance>>dbOutlierDetectioncls = ClassGenericsUtil.uglyCastIntoSubclass(DBOutlierDetection.class);
		dboutlierdetection = params.tryInstantiate(dbOutlierDetectioncls, dbOutlierDetectioncls);
		params.failOnErrors();

		// run DBOutlierDetection on database
		return dboutlierdetection.run(db);
	}

}
