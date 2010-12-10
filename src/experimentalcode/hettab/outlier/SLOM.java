package experimentalcode.hettab.outlier;


import java.util.List;
import java.util.Vector;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.Heap;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * 
 * @author hettab
 * 
 * @param <O>
 * @param <D>
 */

public class SLOM<V extends NumberVector<V, ?>> extends
		AbstractDistanceBasedAlgorithm<V, DoubleDistance, OutlierResult>
		implements OutlierAlgorithm<V, OutlierResult> {

	/**
	 * The logger for this class.
	 */
	private static final Logging logger = Logging.getLogger(SLOM.class);

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
	 * Parameter to specify the position of non spatial attribute
	 */
	public static final OptionID NONSPATIAL_ID = OptionID.getOrCreateOptionID(
			"nonspatial.a", "position of non spatial attribut");

	/**
	 * Parameter to specify the position of spatial attribute
	 */
	public static final OptionID SPATIAL_ID = OptionID.getOrCreateOptionID(
			"spatial.a", "position of non spatial attribut");

	/**
	 * The parameter k
	 */
	private int k;

	/**
	 * position of spatial attribute
	 */
	private List<Integer> spatialDim;

	/**
	 * position of non spatial attribute
	 */
	private List<Integer> nonSpatialDim;

	/**
	 * The association id to associate the SLOM_SCORE of an object for the INFLO
	 * algorithm.
	 */
	public static final AssociationID<Double> SLOM_SCORE = AssociationID
			.getOrCreateAssociationID("SLOM", Double.class);

	/**
	 * 
	 * @param distanceFunction
	 * @param k
	 * @param nonSpatialDim
	 * @param spatialDim
	 */
	protected SLOM(DistanceFunction<V, DoubleDistance> distanceFunction, int k,
			List<Integer> nonSpatialDim, List<Integer> spatialDim) {
		super(distanceFunction);
		this.k = k;
		this.nonSpatialDim = nonSpatialDim;
		this.spatialDim = spatialDim;
	}

	/**
	 * 
	 */
	@Override
	protected OutlierResult runInTime(Database<V> database)
			throws IllegalStateException {
		WritableDataStore<Double> sloms = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, Double.class);
		MinMax<Double> slomminmax = new MinMax<Double>();
		for (DBID id : database) {
           double d = getModifiedDistance(database, id);
           slomminmax.put(d);
           sloms.put(id, d);
		}
		    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("Influence Outlier Score", "info-outlier", SLOM_SCORE, sloms);
		    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(slomminmax.getMin(), slomminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
		    return new OutlierResult(scoreMeta, scoreResult);
	}
    
	/**
	 * 
	 * @param database
	 * @param id
	 * @return
	 */
	public double getModifiedDistance(Database<V> database, DBID id) {

		Vector<DBID> neighbors = getSpatialNeighborhood(database, id);
		//
		Vector<Double> projectedDimV1 = new Vector<Double>(nonSpatialDim.size());
		V v1 = database.get(id);
		//
		double maxDist = 0;
		double sum = 0;
		//
		for (int i = 0; i < spatialDim.size(); i++) {
			projectedDimV1.add(v1.getValue(nonSpatialDim.get(i)).doubleValue());
		}

		for (DBID neighbor : neighbors) {
			V v2 = database.get(neighbor);
			Vector<Double> projectedDimV2 = new Vector<Double>(
					nonSpatialDim.size());
			for (int i = 0; i < nonSpatialDim.size(); i++) {
				projectedDimV2.add(v2.getValue(nonSpatialDim.get(i))
						.doubleValue());
			}
			DistanceResultPair<DoubleDistance> dPair = new DistanceResultPair<DoubleDistance>(
					EuclideanDistanceFunction.STATIC.distance(v1, v2), neighbor);
			double candidate = dPair.first.doubleValue();
			sum = sum + candidate;
			if (maxDist < candidate) {
				maxDist = candidate;
			}
		}
		return (sum - maxDist) / (neighbors.size() - 1);
	}

	/**
	 * 
	 * @param database
	 * @param id
	 * @param dim
	 * @return
	 */
	public Vector<DBID> getSpatialNeighborhood(Database<V> database, DBID id) {
		Heap<DistanceResultPair<DoubleDistance>> heap = new Heap<DistanceResultPair<DoubleDistance>>(
				k);
		Vector<Double> projectedDimV1 = new Vector<Double>(spatialDim.size());
		V v1 = database.get(id);
		//
		for (int i = 0; i < spatialDim.size(); i++) {
			projectedDimV1.add(v1.getValue(spatialDim.get(i)).doubleValue());
		}
		//
		for (DBID dbid : database) {
			V v2 = database.get(dbid);
			Vector<Double> projectedDimV2 = new Vector<Double>(
					spatialDim.size());
			for (int i = 0; i < spatialDim.size(); i++) {
				projectedDimV2
						.add(v2.getValue(spatialDim.get(i)).doubleValue());
			}
			DistanceResultPair<DoubleDistance> dPair = new DistanceResultPair<DoubleDistance>(
					EuclideanDistanceFunction.STATIC.distance(v1, v2), dbid);
			heap.add(dPair);
		}
		// get the spatial neighbours
		Vector<DBID> neighbours = new Vector<DBID>();
		for (DistanceResultPair<DoubleDistance> pair : heap) {
			neighbours.add(pair.second);
		}
		return neighbours;
	}

	/**
	 * Factory method for {@link Parameterizable}
	 * 
	 * @param config
	 *            Parameterization
	 * @return KNN outlier detection algorithm
	 */
	public static <V extends NumberVector<V, ?>> SLOM<V> parameterize(
			Parameterization config) {
		int k = getParameterK(config);
		List<Integer> nonSpatialDim = getParameterNonSpatialDim(config);
		List<Integer> spatialDim = getParameterNonSpatialDim(config);
		DistanceFunction<V, DoubleDistance> distanceFunction = getParameterDistanceFunction(config);
		if (config.hasErrors()) {
			return null;
		}
		return new SLOM<V>(distanceFunction, k, nonSpatialDim, spatialDim);
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

	/**
	 * 
	 * @param config
	 * @return
	 */
	protected static List<Integer> getParameterNonSpatialDim(
			Parameterization config) {
		final IntListParameter param = new IntListParameter(NONSPATIAL_ID,
				false);
		if (config.grab(param)) {
			return param.getValue();
		}
		return null;
	}

	/**
	 * 
	 * @param config
	 * @return
	 */
	protected static List<Integer> getParameterSpatialDim(
			Parameterization config) {
		final IntListParameter param = new IntListParameter(SPATIAL_ID, false);
		if (config.grab(param)) {
			return param.getValue();
		}
		return null;
	}

	@Override
	protected Logging getLogger() {
		return logger;
	}

}
