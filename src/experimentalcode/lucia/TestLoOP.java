package experimentalcode.lucia;

import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.outlier.LoOP;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.evaluation.roc.ComputeROCCurve;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;


/**
 * Tests the LoOP algorithm. 
 * @author lucia
 * 
 */
public class TestLoOP extends OutlierTest{
	// the following values depend on the data set used!
  static String dataset = "src/experimentalcode/lucia/datensaetze/gauss3D.csv";

	static int k = 15;


	@Test
	public void testLoOP() throws UnableToComplyException {

	  //Get Database
		Database<DoubleVector> db = getDatabase(dataset);

		//Parameterization
		ListParameterization params = new ListParameterization();
		params.addParameter(LoOP.KCOMP_ID, k);
		params.addParameter(ComputeROCCurve.POSITIVE_CLASS_NAME_ID, "Noise");
		
		// run LoOP
		OutlierResult result = runLoOP(db, params);
		AnnotationResult<Double> scores = result.getScores();

		
	  //check Outlier Score of Point 273
    int id = 273;
    double score = scores.getValueFor(DBIDUtil.importInteger(id));
//    System.out.println("Outlier Score of the Point with id " + id + ": " + score);
    Assert.assertEquals("Outlier Score of Point with id " + id + " not right.", 0.39805457858293325, score, 0.0001);

		
		//get ROC AUC
		List<Double> auc = getROCAUC(db, result, params);
		Iterator<Double> iter = auc.listIterator();
		double actual;
		while(iter.hasNext()){
			actual = iter.next();
//			System.out.println("LoOP(k="+ k + ") ROC AUC: " + actual);
			Assert.assertEquals("ROC AUC not right.", 0.94168519, actual, 0.00001);
		}
	}
	
	private static OutlierResult runLoOP(Database<DoubleVector> db, ListParameterization params) {
		// setup algorithm
		LoOP<DoubleVector, DoubleDistance> loop = null;
		Class<LoOP<DoubleVector, DoubleDistance>> loopcls = ClassGenericsUtil.uglyCastIntoSubclass(LoOP.class);
		loop = params.tryInstantiate(loopcls, loopcls);
		params.failOnErrors();

		// run LoOP on database
		return loop.run(db);
	}
}
