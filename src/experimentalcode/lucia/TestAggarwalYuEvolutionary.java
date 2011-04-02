package experimentalcode.lucia;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.outlier.AggarwalYuEvolutionary;
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
 * Tests the AggarwalYuEvolutionary algorithm. 
 * @author lucia
 * 
 */
public class TestAggarwalYuEvolutionary extends OutlierTest implements JUnit4Test{
	// the following values depend on the data set used!
	  static String dataset = "src/experimentalcode/lucia/datensaetze/gauss3D.csv";
	
	static int k = 2;
	static int phi = 8;
	static int m = 5;


	@Test
	public void testAggarwalYuEvolutionary() throws UnableToComplyException {
		ArrayList<Pair<Double, DBID>> pair_scoresIds = new ArrayList<Pair<Double, DBID>>();

		Database<DoubleVector> db = getDatabase(dataset);

		
		//Parameterization
		ListParameterization params = new ListParameterization();
		params.addParameter(AggarwalYuEvolutionary.K_ID, k);
		params.addParameter(AggarwalYuEvolutionary.PHI_ID, phi);
		params.addParameter(AggarwalYuEvolutionary.M_ID, m);
		params.addParameter(ComputeROCCurve.POSITIVE_CLASS_NAME_ID, "Noise");

		
		// run AggarwalYuEvolutionary
		OutlierResult result = runAggarwalYuEvolutionary(db, params);
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
			System.out.println("AggarwalYuEvolutionary(k="+ k + " und phi="+phi+" und m="+m+") ROC AUC: " + actual);
			Assert.assertEquals("ROC AUC not right.", 0.53457407, actual, 0.1);
		}
	}
	
	private static OutlierResult runAggarwalYuEvolutionary(Database<DoubleVector> db, ListParameterization params) {
		// setup algorithm
		AggarwalYuEvolutionary<DoubleVector> aggarwalYuEvolutionary = null;
		Class<AggarwalYuEvolutionary<DoubleVector>> aggarwalYuEvolutionarycls = ClassGenericsUtil.uglyCastIntoSubclass(AggarwalYuEvolutionary.class);
		aggarwalYuEvolutionary = params.tryInstantiate(aggarwalYuEvolutionarycls, aggarwalYuEvolutionarycls);
		params.failOnErrors();

		// run AggarwalYuEvolutionary on database
		return aggarwalYuEvolutionary.run(db);
	}
}
