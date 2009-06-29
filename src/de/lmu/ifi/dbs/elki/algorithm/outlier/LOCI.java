package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDatabase;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromAssociation;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.CPair;

/**
 * Fast Outlier Detection Using the "Local Correlation Integral".
 * 
 * Exact implementation only, not aLOCI.
 * 
 * Outlier detection using multiple epsilon neighborhoods.
 * 
 * Based on:
 * S. Papadimitriou, H. Kitagawa, P. B. Gibbons and C. Faloutsos:
 * LOCI: Fast Outlier Detection Using the Local Correlation Integral.
 * In: Proc. 19th IEEE Int. Conf. on Data Engineering (ICDE '03), Bangalore, India, 2003.
 * 
 * @author Erich Schubert
 *
 * @param <O>
 */
public class LOCI<O extends DatabaseObject> extends DistanceBasedAlgorithm<O, DoubleDistance, MultiResult> {
  /**
   * OptionID for {@link #RMAX_PARAM}
   */
  public static final OptionID RMAX_ID = OptionID.getOrCreateOptionID(
      "loci.rmax",
      "The maximum radius of the neighborhood to be considered."
  );

  /**
   * OptionID for {@link #NMIN_PARAM}
   */
  public static final OptionID NMIN_ID = OptionID.getOrCreateOptionID(
      "loci.nmin",
      "Minimum neighborhood size to be considered."
  );

  /**
   * OptionID for {@link #ALPHA_PARAM}
   */
  public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID(
      "loci.alpha",
      "Scaling factor for averaging neighborhood"
  );

  /**
   * Parameter to specify the maximum radius of the neighborhood to be considered,
   * must be suitable to the distance function specified.
   * <p>Key: {@code -loci.rmax} </p>
   */
  private final PatternParameter RMAX_PARAM = new PatternParameter(RMAX_ID);

  /**
   * Holds the value of {@link #RMAX_PARAM}.
   */
  private String rmax;

  /**
   * Parameter to specify the minimum neighborhood size
   * <p>Key: {@code -loci.nmin} </p>
   * <p>Default: {@code 20}</p>
   */
  private final IntParameter NMIN_PARAM = new IntParameter(NMIN_ID, null, 20);

  /**
   * Holds the value of {@link #NMIN_PARAM}.
   */
  private double nmin;

  /**
   * Parameter to specify the averaging neighborhood scaling
   * <p>Key: {@code -loci.alpha} </p>
   * <p>Default: {@code 0.5}</p>
   */
  private final DoubleParameter ALPHA_PARAM = new DoubleParameter(ALPHA_ID, 0.5);

  /**
   * Holds the value of {@link #ALPHA_PARAM}.
   */
  private double alpha;

  /**
   * Provides the result of the algorithm.
   */
  MultiResult result;

  /**
   * The LOCI critical distances of an object.
   */
  public static final AssociationID<List<CPair<Double,Integer>>> LOCI_CRITICALDIST = AssociationID.getOrCreateAssociationIDGenerics("loci-cdist", List.class);

  /**
   * The LOCI MDEF / SigmaMDEF maximum values radius
   */
  public static final AssociationID<Double> LOCI_MDEF_CRITICAL_RADIUS = AssociationID.getOrCreateAssociationID("loci.mdefrad", Double.class);

  /**
   * The LOCI MDEF / SigmaMDEF maximum value (normalized MDEF)
   */
  public static final AssociationID<Double> LOCI_MDEF_NORM = AssociationID.getOrCreateAssociationID("loci.mdefnorm", Double.class);

  /**
   * Constructor, adding options to option handler.
   */
  public LOCI() {
    super();
    // maximum query range
    addOption(RMAX_PARAM);
    // minimum neighborhood size
    addOption(NMIN_PARAM);
    // scaling factor for averaging range
    addOption(ALPHA_PARAM);
  }
  
  /**
   * Calls the super method
   * and sets additionally the values of the parameter
   * {@link #RMAX_PARAM}, {@link #NMIN_PARAM} and {@link #ALPHA_PARAM} 
   */
  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
      List<String> remainingParameters = super.setParameters(args);

      // maximum query radius
      rmax = RMAX_PARAM.getValue();

      // minimum neighborhood size
      nmin = NMIN_PARAM.getValue();

      // averaging range scaling
      alpha = ALPHA_PARAM.getValue();

      return remainingParameters;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   */
  @Override
  protected MultiResult runInTime(Database<O> database) throws IllegalStateException {
    getDistanceFunction().setDatabase(database, isVerbose(), isTime());
    // LOCI preprocessing step
    for (Integer id : database.getIDs()) {
      List<DistanceResultPair<DoubleDistance>> neighbors = database.rangeQuery(id, rmax, getDistanceFunction());
      // build list of critical distances
      ArrayList<CPair<Double,Integer>> cdist = new ArrayList<CPair<Double,Integer>>(neighbors.size() * 2);
      {
        int i = 0;
        for (DistanceResultPair<DoubleDistance> r : neighbors) {
          cdist.add(new CPair<Double,Integer>(r.getDistance().getValue(), i));
          cdist.add(new CPair<Double,Integer>(r.getDistance().getValue() / alpha, null));
          i++;
        }
      }
      Collections.sort(cdist);
      // fill the gaps to have fast lookups of number of neighbors at a given distance.
      int lastk = 0;
      for (CPair<Double,Integer> c : cdist) {
        if (c.second == null) 
          c.second = lastk;
        else
          lastk = c.second;
      }
      
      database.associate(LOCI_CRITICALDIST, id, cdist);
    }
    // LOCI main step
    for (Integer id : database.getIDs()) {
      double maxmdefnorm = 0.0;
      double maxnormr = 0;
      List<CPair<Double,Integer>> cdist = database.getAssociation(LOCI_CRITICALDIST, id);
      for (CPair<Double,Integer> c : cdist) {
        double alpha_r = alpha * c.first;
        // compute n(p_i, \alpha * r) from list
        int n_alphar = 0;
        for (CPair<Double,Integer> c2 : cdist) {
          if (c2.first <= alpha_r) n_alphar=c2.second;
          else break;
        }
        // compute \hat{n}(p_i, r, \alpha)
        double nhat_r_alpha = 0.0;
        double sigma_nhat_r_alpha = 0.0;
        // note that the query range is c.first
        List<DistanceResultPair<DoubleDistance>> rneighbors = database.rangeQuery(id, Double.toString(c.first), getDistanceFunction());
        if (rneighbors.size() < nmin) continue;
        for (DistanceResultPair<DoubleDistance> rn : rneighbors) {
          List<CPair<Double,Integer>> rncdist = database.getAssociation(LOCI_CRITICALDIST, rn.getID());
          int rn_alphar = 0;
          for (CPair<Double,Integer> c2 : rncdist) {
            if (c2.first <= alpha_r) rn_alphar=c2.second;
            else break;
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
        
        if (mdefnorm > maxmdefnorm) {
          maxmdefnorm = mdefnorm;
          maxnormr = c.first;
        }
      }
      // TODO: when nmin was never fulfilled, the values will remain 0.
      database.associate(LOCI_MDEF_NORM, id, maxmdefnorm);
      database.associate(LOCI_MDEF_CRITICAL_RADIUS, id, maxnormr);
    }
    result = new MultiResult();
    result.addResult(new AnnotationFromDatabase<Double, O>(database,LOCI_MDEF_NORM));
    result.addResult(new AnnotationFromDatabase<Double, O>(database,LOCI_MDEF_CRITICAL_RADIUS));
    result.addResult(new OrderingFromAssociation<Double, O>(database,LOCI_MDEF_NORM, true));
    return result;
  }

  /**
   * Get algorithm description.
   */
  public Description getDescription() {
    return new Description(
        "LOCI",
        "Fast Outlier Detection Using the Local Correlation Integral",
        "Algorithm to compute outliers based on the Local Correlation Integral",
        "S. Papadimitriou, H. Kitagawa, P. B. Gibbons and C. Faloutsos: " +
            "LOCI: Fast Outlier Detection Using the Local Correlation Integral. " +
            "In: Proc. 19th IEEE Int. Conf. on Data Engineering (ICDE '03), Bangalore, India, 2003.");
  }

  /**
   * Return result.
   */
  public MultiResult getResult() {
    return result;
  }
}
