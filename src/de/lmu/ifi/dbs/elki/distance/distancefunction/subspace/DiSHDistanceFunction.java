package de.lmu.ifi.dbs.elki.distance.distancefunction.subspace;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.preference.DiSHPreferenceVectorIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Distance function used in the DiSH algorithm.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has Instance
 */
public class DiSHDistanceFunction extends AbstractPreferenceVectorBasedCorrelationDistanceFunction<NumberVector<?, ?>, DiSHPreferenceVectorIndex<NumberVector<?, ?>>> {
  /**
   * Logger for debug.
   */
  static Logging logger = Logging.getLogger(DiSHDistanceFunction.class);

  /**
   * Constructor.
   * 
   * @param indexFactory
   * @param epsilon
   */
  public DiSHDistanceFunction(DiSHPreferenceVectorIndex.Factory<NumberVector<?, ?>> indexFactory, double epsilon) {
    super(indexFactory, epsilon);
  }

  @Override
  public VectorFieldTypeInformation<? super NumberVector<?, ?>> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  public <T extends NumberVector<?, ?>> Instance<T> instantiate(Relation<T> database) {
    // We can't really avoid these warnings, due to a limitation in Java
    // Generics (AFAICT)
    @SuppressWarnings("unchecked")
    DiSHPreferenceVectorIndex<T> indexinst = (DiSHPreferenceVectorIndex<T>) indexFactory.instantiate((Relation<NumberVector<?, ?>>) database);
    return new Instance<T>(database, indexinst, getEpsilon(), this);
  }

  /**
   * Get the minpts value.
   * 
   * @return the minpts parameter
   */
  public int getMinpts() {
    // TODO: get rid of this cast?
    return ((DiSHPreferenceVectorIndex.Factory<NumberVector<?, ?>>) indexFactory).getMinpts();
  }

  /**
   * The actual instance bound to a particular database.
   * 
   * @author Erich Schubert
   */
  public static class Instance<V extends NumberVector<?, ?>> extends AbstractPreferenceVectorBasedCorrelationDistanceFunction.Instance<V, DiSHPreferenceVectorIndex<V>> {
    /**
     * Constructor.
     * 
     * @param database Database
     * @param index Preprocessed index
     * @param epsilon Epsilon
     * @param distanceFunction parent distance function
     */
    public Instance(Relation<V> database, DiSHPreferenceVectorIndex<V> index, double epsilon, DiSHDistanceFunction distanceFunction) {
      super(database, index, epsilon, distanceFunction);
    }

    /**
     * Computes the correlation distance between the two specified vectors
     * according to the specified preference vectors.
     * 
     * @param v1 first vector
     * @param v2 second vector
     * @param pv1 the first preference vector
     * @param pv2 the second preference vector
     * @return the correlation distance between the two specified vectors
     */
    @Override
    public PreferenceVectorBasedCorrelationDistance correlationDistance(V v1, V v2, BitSet pv1, BitSet pv2) {
      BitSet commonPreferenceVector = (BitSet) pv1.clone();
      commonPreferenceVector.and(pv2);
      int dim = v1.getDimensionality();

      // number of zero values in commonPreferenceVector
      Integer subspaceDim = dim - commonPreferenceVector.cardinality();

      // special case: v1 and v2 are in parallel subspaces
      if(commonPreferenceVector.equals(pv1) || commonPreferenceVector.equals(pv2)) {
        double d = weightedDistance(v1, v2, commonPreferenceVector);
        if(d > 2 * epsilon) {
          subspaceDim++;
          if(logger.isDebugging()) {
            //Representation<String> rep = database.getObjectLabelQuery();
            StringBuffer msg = new StringBuffer();
            msg.append("d ").append(d);
            //msg.append("\nv1 ").append(rep.get(v1.getID()));
            //msg.append("\nv2 ").append(rep.get(v2.getID()));
            msg.append("\nsubspaceDim ").append(subspaceDim);
            msg.append("\ncommon pv ").append(FormatUtil.format(dim, commonPreferenceVector));
            logger.debugFine(msg.toString());
          }
        }
      }

      // flip commonPreferenceVector for distance computation in common subspace
      BitSet inverseCommonPreferenceVector = (BitSet) commonPreferenceVector.clone();
      inverseCommonPreferenceVector.flip(0, dim);

      return new PreferenceVectorBasedCorrelationDistance(DatabaseUtil.dimensionality(rep), subspaceDim, weightedDistance(v1, v2, inverseCommonPreferenceVector), commonPreferenceVector);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractPreferenceVectorBasedCorrelationDistanceFunction.Parameterizer<DiSHPreferenceVectorIndex.Factory<NumberVector<?, ?>>> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      Class<DiSHPreferenceVectorIndex.Factory<NumberVector<?, ?>>> cls = ClassGenericsUtil.uglyCastIntoSubclass(DiSHPreferenceVectorIndex.Factory.class);
      factory = config.tryInstantiate(cls);
    }

    @Override
    protected DiSHDistanceFunction makeInstance() {
      return new DiSHDistanceFunction(factory, epsilon);
    }
  }
}