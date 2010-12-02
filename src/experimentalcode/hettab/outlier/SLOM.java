package experimentalcode.hettab.outlier;

import java.util.HashMap;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * 
 * @author hettab
 * 
 * @param <O>
 * @param <D>
 */

public class SLOM<O extends DatabaseObject, D extends NumberDistance<D, ?>>
		extends AbstractDistanceBasedAlgorithm<O, D, OutlierResult> implements
		OutlierAlgorithm<O, OutlierResult> {

	/**
	 * The logger for this class.
	 */
	private static final Logging logger = Logging
			.getLogger(SLOM.class);

	/**
	 * The association id to associate the KNNO_KNNDISTANCE of an object for the
	 * KNN outlier detection algorithm.
	 */
	public static final AssociationID<Double> KNNO_KNNDISTANCE = AssociationID
			.getOrCreateAssociationID("knno_knndistance", Double.class);

	/**
	 * Parameter to specify the k nearest neighbor
	 */
	public static final OptionID K_ID = OptionID.getOrCreateOptionID("knno.k",
			"k nearest neighbor");

	/**
	 * The parameter k
	 */
	private int k;
	/**
	 * The association id to associate the SLOM_SCORE of an object for the
	 * INFLO algorithm.
	 */
	public static final AssociationID<Double> SLOM_SCORE = AssociationID
			.getOrCreateAssociationID("SLOM", Double.class);

	protected SLOM(DistanceFunction<? super O, D> distanceFunction,
			int k) {
		super(distanceFunction);
		this.k = k;
	}

	@Override
	protected OutlierResult runInTime(Database<O> database)
			throws IllegalStateException {
		KNNQuery<O, D> knnQuery = database
				.getKNNQuery(getDistanceFunction(), k);

		WritableDataStore<Double> sloms = DataStoreUtil.makeStorage(
				database.getIDs(), DataStoreFactory.HINT_STATIC, Double.class);
		//
		HashMap<DBID, Double> modiDists = new HashMap<DBID, Double>();
		HashMap<DBID, Double> avg = new HashMap<DBID, Double>();

		for (DBID id : database.getIDs()) {
			// List of neighbours
			List<DistanceResultPair<D>> neighbours = knnQuery.getKNNForDBID(id,
					k);
			//
			double mDist = 0;
			// maximal distance
			double max = 0;
			//
			for (DistanceResultPair<D> pair : neighbours) {
				if (pair.getFirst().doubleValue() > max) {
					max = pair.getFirst().doubleValue();
					mDist = pair.getFirst().doubleValue();
				}
			}
			avg.put(id, mDist);
			Double modiDist = new Double(mDist - max / neighbours.size());
			modiDists.put(id, modiDist);
		}

		MinMax<Double> slom_minmax = new MinMax<Double>();
		for (DBID id : database.getIDs()) {
			List<DistanceResultPair<D>> neighbours = knnQuery.getKNNForDBID(id,
					k);
			double beta = 0;
			for (DistanceResultPair<D> pair : neighbours) {
				if (modiDists.get(pair.getID()).doubleValue() > avg.get(id)
						.doubleValue()) {
					beta++;
				} else {
					beta--;
				}
			}
			beta = Math.max(beta, 1) / (neighbours.size() - 1);
			beta = beta / (1 + modiDists.get(id).doubleValue());
			sloms.put(id, modiDists.get(id).doubleValue() * beta);
			slom_minmax.put(modiDists.get(id).doubleValue() * beta);
		}

		AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>(
				"spatial local Outlier Score", "slom-outlier", SLOM_SCORE,
				sloms);
		OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(
				slom_minmax.getMin(), slom_minmax.getMax(), 0.0,
				Double.POSITIVE_INFINITY, 1.0);
		return new OutlierResult(scoreMeta, scoreResult);

	}

	/**
	 * Factory method for {@link Parameterizable}
	 * 
	 * @param config
	 *            Parameterization
	 * @return KNN outlier detection algorithm
	 */
	public static <O extends DatabaseObject, D extends NumberDistance<D, ?>> SLOM<O, D> parameterize(
			Parameterization config) {
		int k = getParameterK(config);
		DistanceFunction<O, D> distanceFunction = getParameterDistanceFunction(config);
		if (config.hasErrors()) {
			return null;
		}
		return new SLOM<O, D>(distanceFunction, k);
	}

	/**
	 * Get the k Parameter for the knn query
	 * 
	 * @param config
	 *            Parameterization
	 * @return k parameter
	 */
	protected static int getParameterK(Parameterization config) {
		final IntParameter param = new IntParameter(K_ID,
				new GreaterConstraint(1));
		if (config.grab(param)) {
			return param.getValue();
		}
		return -1;
	}

	@Override
	protected Logging getLogger() {
		return logger;
	}

}
