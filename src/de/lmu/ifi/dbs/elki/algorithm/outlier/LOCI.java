package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableRecordStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromDataStore;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DistanceParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair;

/**
 * Fast Outlier Detection Using the "Local Correlation Integral".
 * 
 * Exact implementation only, not aLOCI.
 * 
 * Outlier detection using multiple epsilon neighborhoods.
 * 
 * Based on: S. Papadimitriou, H. Kitagawa, P. B. Gibbons and C. Faloutsos:
 * LOCI: Fast Outlier Detection Using the Local Correlation Integral. In: Proc.
 * 19th IEEE Int. Conf. on Data Engineering (ICDE '03), Bangalore, India, 2003.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
@Title("LOCI: Fast Outlier Detection Using the Local Correlation Integral")
@Description("Algorithm to compute outliers based on the Local Correlation Integral")
@Reference(authors = "S. Papadimitriou, H. Kitagawa, P. B. Gibbons, C. Faloutsos", title = "LOCI: Fast Outlier Detection Using the Local Correlation Integral", booktitle = "Proc. 19th IEEE Int. Conf. on Data Engineering (ICDE '03), Bangalore, India, 2003", url = "http://dx.doi.org/10.1109/ICDE.2003.1260802")
public class LOCI<O extends DatabaseObject, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, OutlierResult> {
  /**
   * OptionID for {@link #RMAX_PARAM}
   */
  public static final OptionID RMAX_ID = OptionID.getOrCreateOptionID("loci.rmax", "The maximum radius of the neighborhood to be considered.");

  /**
   * OptionID for {@link #NMIN_PARAM}
   */
  public static final OptionID NMIN_ID = OptionID.getOrCreateOptionID("loci.nmin", "Minimum neighborhood size to be considered.");

  /**
   * OptionID for {@link #ALPHA_PARAM}
   */
  public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("loci.alpha", "Scaling factor for averaging neighborhood");

  /**
   * Parameter to specify the maximum radius of the neighborhood to be
   * considered, must be suitable to the distance function specified.
   * <p>
   * Key: {@code -loci.rmax}
   * </p>
   */
  private final DistanceParameter<D> RMAX_PARAM = new DistanceParameter<D>(RMAX_ID, getDistanceFactory());

  /**
   * Holds the value of {@link #RMAX_PARAM}.
   */
  private D rmax;

  /**
   * Parameter to specify the minimum neighborhood size
   * <p>
   * Key: {@code -loci.nmin}
   * </p>
   * <p>
   * Default: {@code 20}
   * </p>
   */
  private final IntParameter NMIN_PARAM = new IntParameter(NMIN_ID, 20);

  /**
   * Holds the value of {@link #NMIN_PARAM}.
   */
  private double nmin;

  /**
   * Parameter to specify the averaging neighborhood scaling
   * <p>
   * Key: {@code -loci.alpha}
   * </p>
   * <p>
   * Default: {@code 0.5}
   * </p>
   */
  private final DoubleParameter ALPHA_PARAM = new DoubleParameter(ALPHA_ID, 0.5);

  /**
   * Holds the value of {@link #ALPHA_PARAM}.
   */
  private double alpha;

  /**
   * The LOCI MDEF / SigmaMDEF maximum values radius
   */
  public static final AssociationID<Double> LOCI_MDEF_CRITICAL_RADIUS = AssociationID.getOrCreateAssociationID("loci.mdefrad", Double.class);

  /**
   * The LOCI MDEF / SigmaMDEF maximum value (normalized MDEF)
   */
  public static final AssociationID<Double> LOCI_MDEF_NORM = AssociationID.getOrCreateAssociationID("loci.mdefnorm", Double.class);

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public LOCI(Parameterization config) {
    super(config);
    // maximum query range
    if(config.grab(RMAX_PARAM)) {
      rmax = RMAX_PARAM.getValue();
    }
    // minimum neighborhood size
    if(config.grab(NMIN_PARAM)) {
      nmin = NMIN_PARAM.getValue();
    }
    // scaling factor for averaging range
    if(config.grab(ALPHA_PARAM)) {
      alpha = ALPHA_PARAM.getValue();
    }
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   */
  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    DistanceQuery<O, D> distFunc = getDistanceQuery(database);

    FiniteProgress progressPreproc = logger.isVerbose() ? new FiniteProgress("LOCI preprocessing", database.size(), logger) : null;
    // LOCI preprocessing step
    WritableDataStore<ArrayList<DoubleIntPair>> interestingDistances = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_SORTED, ArrayList.class);
    for(DBID id : database.getIDs()) {
      List<DistanceResultPair<D>> neighbors = database.rangeQuery(id, rmax, distFunc);
      // build list of critical distances
      ArrayList<DoubleIntPair> cdist = new ArrayList<DoubleIntPair>(neighbors.size() * 2);
      {
        int i = 0;
        for(DistanceResultPair<D> r : neighbors) {
          assert(i != Integer.MIN_VALUE);
          cdist.add(new DoubleIntPair(r.getDistance().doubleValue(), i));
          cdist.add(new DoubleIntPair(r.getDistance().doubleValue() / alpha, Integer.MIN_VALUE));
          i++;
        }
      }
      Collections.sort(cdist);
      // fill the gaps to have fast lookups of number of neighbors at a given
      // distance.
      int lastk = 0;
      for(DoubleIntPair c : cdist) {
        if(c.second == Integer.MIN_VALUE) {
          c.second = lastk;
        }
        else {
          lastk = c.second;
        }
      }

      interestingDistances.put(id, cdist);
      if(progressPreproc != null) {
        progressPreproc.incrementProcessed(logger);
      }
    }
    if(progressPreproc != null) {
      progressPreproc.ensureCompleted(logger);
    }
    // LOCI main step
    FiniteProgress progressLOCI = logger.isVerbose() ? new FiniteProgress("LOCI scores", database.size(), logger) : null;
    WritableRecordStore store = DataStoreUtil.makeRecordStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, Double.class, Double.class);
    WritableDataStore<Double> mdef_norm = store.getStorage(0, Double.class);
    WritableDataStore<Double> mdef_radius = store.getStorage(1, Double.class);
    for(DBID id : database.getIDs()) {
      double maxmdefnorm = 0.0;
      double maxnormr = 0;
      List<DoubleIntPair> cdist = interestingDistances.get(id);
      double maxdist = cdist.get(cdist.size()-1).first;
      // Compute the largest neighborhood we will need.
      List<DistanceResultPair<D>> maxneighbors = database.rangeQuery(id, Double.toString(maxdist), distFunc);
      for(DoubleIntPair c : cdist) {
        double alpha_r = alpha * c.first;
        // compute n(p_i, \alpha * r) from list
        int n_alphar = 0;
        for(DoubleIntPair c2 : cdist) {
          if(c2.first <= alpha_r) {
            n_alphar = c2.second;
          }
          else {
            break;
          }
        }
        // compute \hat{n}(p_i, r, \alpha)
        double nhat_r_alpha = 0.0;
        double sigma_nhat_r_alpha = 0.0;
        // note that the query range is c.first
        //List<DistanceResultPair<D>> rneighbors = database.rangeQuery(id, Double.toString(c.first), getDistanceFunction());
        List<DistanceResultPair<D>> rneighbors = null;
        for (int i = 0; i < maxneighbors.size(); i++) {
          DistanceResultPair<D> ne = maxneighbors.get(i);
          if (ne.getDistance().doubleValue() > c.first) {
            if (i >= nmin) {
              rneighbors = maxneighbors.subList(0, i);
            } else {
              rneighbors = null;
            }
            break;
          }
        }
        if (rneighbors == null) {
          continue;
        }
        // redundant check.
        if(rneighbors.size() < nmin) {
          continue;
        }
        for(DistanceResultPair<D> rn : rneighbors) {
          List<DoubleIntPair> rncdist = interestingDistances.get(rn.getID());
          int rn_alphar = 0;
          for(DoubleIntPair c2 : rncdist) {
            if(c2.first <= alpha_r) {
              rn_alphar = c2.second;
            }
            else {
              break;
            }
          }
          nhat_r_alpha = nhat_r_alpha + rn_alphar;
          sigma_nhat_r_alpha = sigma_nhat_r_alpha + (rn_alphar * rn_alphar);
        }
        // finalize average and deviation
        nhat_r_alpha = nhat_r_alpha / rneighbors.size();
        sigma_nhat_r_alpha = Math.sqrt(sigma_nhat_r_alpha / rneighbors.size() - nhat_r_alpha * nhat_r_alpha);
        double mdef = 1.0 - (n_alphar / nhat_r_alpha);
        double sigmamdef = sigma_nhat_r_alpha / nhat_r_alpha;
        double mdefnorm = mdef / sigmamdef;

        if(mdefnorm > maxmdefnorm) {
          maxmdefnorm = mdefnorm;
          maxnormr = c.first;
        }
      }
      // FIXME: when nmin was never fulfilled, the values will remain 0.
      mdef_norm.put(id, maxmdefnorm);
      mdef_radius.put(id, maxnormr);
      if(progressLOCI != null) {
        progressLOCI.incrementProcessed(logger);
      }
    }
    if(progressLOCI != null) {
      progressLOCI.ensureCompleted(logger);
    }
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>(LOCI_MDEF_NORM, mdef_norm);
    OrderingResult orderingResult = new OrderingFromDataStore<Double>(mdef_norm, true);
    // TODO: actually provide min and max?
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(Double.NaN, Double.NaN, 0.0, Double.POSITIVE_INFINITY, 0.0);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult, orderingResult);
    result.addResult(new AnnotationFromDataStore<Double>(LOCI_MDEF_CRITICAL_RADIUS, mdef_radius));
    return result;
  }
}