package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromHashMap;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * OPTICSOF provides the Optics-of algorithm, an algorithm to find Local
 * Outliers in a database.
 * <p>
 * Reference:<br>
 * Markus M. Breunig, Hans-Peter Kriegel, Raymond T. N, Jörg Sander:<br />
 * OPTICS-OF: Identifying Local Outliers<br />
 * In Proc. of the 3rd European Conference on Principles of Knowledge Discovery
 * and Data Mining (PKDD), Prague, Czech Republic
 * 
 * @author Ahmed Hettab
 * 
 * @param <O> DatabaseObject
 */
@Title("OPTICS-OF: Identifying Local Outliers")
@Description("Algorithm to compute density-based local outlier factors in a database based on the neighborhood size parameter 'minpts'")
@Reference(authors = "M. M. Breunig, H.-P. Kriegel, R. Ng, and J. Sander", title = "OPTICS-OF: Identifying Local Outliers", booktitle = "Proc. of the 3rd European Conference on Principles of Knowledge Discovery and Data Mining (PKDD), Prague, Czech Republic", url = "http://dx.doi.org/10.1007/b72280")
public class OPTICSOF<O extends DatabaseObject> extends DistanceBasedAlgorithm<O, DoubleDistance, MultiResult> {
  /**
   * Parameter to specify the threshold MinPts
   * <p>
   * Key: {@code -optics.minpts}
   * </p>
   */
  private final IntParameter MINPTS_PARAM = new IntParameter(OPTICS.MINPTS_ID, new GreaterConstraint(0));

  /**
   * Holds the value of {@link #MINPTS_PARAM}.
   */
  private int minpts;

  /**
   * The association id to associate the OPTICS_OF_SCORE of an object for the OF
   * algorithm.
   */
  public static final AssociationID<Double> OPTICS_OF_SCORE = AssociationID.getOrCreateAssociationID("optics-of", Double.class);

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public OPTICSOF(Parameterization config) {
    super(config);
    // parameter minpts
    if(config.grab(MINPTS_PARAM)) {
      minpts = MINPTS_PARAM.getValue();
    }
  }

  /**
	 *
	 */
  @Override
  protected MultiResult runInTime(Database<O> database) throws IllegalStateException {
    getDistanceFunction().setDatabase(database);

    HashMap<Integer, List<DistanceResultPair<DoubleDistance>>> nMinPts = new HashMap<Integer, List<DistanceResultPair<DoubleDistance>>>();
    HashMap<Integer, Double> coreDistance = new HashMap<Integer, Double>();
    HashMap<Integer, Integer> minPtsNeighborhoodSize = new HashMap<Integer, Integer>();

    // Pass 1
    // N_minpts(id) and core-distance(id)

    for(Integer id : database) {
      List<DistanceResultPair<DoubleDistance>> minptsNegibours = database.kNNQueryForID(id, minpts, getDistanceFunction());
      Double d = minptsNegibours.get(minptsNegibours.size() - 1).getDistance().doubleValue();
      nMinPts.put(id, minptsNegibours);
      coreDistance.put(id, d);
      minPtsNeighborhoodSize.put(id, database.rangeQuery(id, d.toString(), getDistanceFunction()).size());
    }

    // Pass 2
    HashMap<Integer, List<Double>> reachDistance = new HashMap<Integer, List<Double>>();
    HashMap<Integer, Double> lrds = new HashMap<Integer, Double>();
    for(Integer id : database) {
      List<Double> core = new ArrayList<Double>();
      double lrd = 0;
      for(DistanceResultPair<DoubleDistance> neighPair : nMinPts.get(id)) {
        int idN = neighPair.getID();
        double coreDist = coreDistance.get(idN);
        double dist = getDistanceFunction().distance(id, idN).doubleValue();
        Double rd = Math.max(coreDist, dist);
        lrd = rd + lrd;
        core.add(rd);
      }
      lrd = (minPtsNeighborhoodSize.get(id) / lrd);
      reachDistance.put(id, core);
      lrds.put(id, lrd);
    }

    // Pass 3
    MinMax<Double> ofminmax = new MinMax<Double>();
    HashMap<Integer, Double> ofs = new HashMap<Integer, Double>();
    for(Integer id : database) {
      double of = 0;
      for(DistanceResultPair<DoubleDistance> pair : nMinPts.get(id)) {
        int idN = pair.getID();
        double lrd = lrds.get(id);
        double lrdN = lrds.get(idN);
        of = of + lrdN / lrd;
      }
      of = of / minPtsNeighborhoodSize.get(id);
      ofs.put(id, of);
      // update minimum and maximum
      ofminmax.put(of);
    }
    // Build result representation.
    AnnotationResult<Double> scoreResult = new AnnotationFromHashMap<Double>(OPTICS_OF_SCORE, ofs);
    OrderingResult orderingResult = new OrderingFromHashMap<Double>(ofs, false);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(ofminmax.getMin(), ofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    return new OutlierResult(scoreMeta, scoreResult, orderingResult);
  }
}