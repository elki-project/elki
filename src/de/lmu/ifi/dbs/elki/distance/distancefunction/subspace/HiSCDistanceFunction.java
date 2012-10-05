package de.lmu.ifi.dbs.elki.distance.distancefunction.subspace;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.preference.HiSCPreferenceVectorIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Distance function used in the HiSC algorithm.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has Instance
 * 
 * @param <V> the type of NumberVector to compute the distances in between
 */
public class HiSCDistanceFunction<V extends NumberVector<?>> extends AbstractPreferenceVectorBasedCorrelationDistanceFunction<V, HiSCPreferenceVectorIndex<V>> {
  /**
   * Logger for debug.
   */
  private static final Logging LOG = Logging.getLogger(HiSCDistanceFunction.class);

  /**
   * Constructor.
   * 
   * @param indexFactory HiSC index factory
   * @param epsilon Epsilon value
   */
  public HiSCDistanceFunction(HiSCPreferenceVectorIndex.Factory<V> indexFactory, double epsilon) {
    super(indexFactory, epsilon);
  }

  @Override
  public <T extends V> Instance<T> instantiate(Relation<T> database) {
    // We can't really avoid these warnings, due to a limitation in Java
    // Generics (AFAICT)
    @SuppressWarnings("unchecked")
    HiSCPreferenceVectorIndex<T> indexinst = (HiSCPreferenceVectorIndex<T>) indexFactory.instantiate((Relation<V>) database);
    return new Instance<T>(database, indexinst, getEpsilon(), this);
  }

  /**
   * The actual instance bound to a particular database.
   * 
   * @author Erich Schubert
   * 
   * @param <V> the type of NumberVector to compute the distances in between
   */
  public static class Instance<V extends NumberVector<?>> extends AbstractPreferenceVectorBasedCorrelationDistanceFunction.Instance<V, HiSCPreferenceVectorIndex<V>> {
    /**
     * Constructor.
     * 
     * @param database Database
     * @param index Preprocessed index
     * @param epsilon Epsilon
     * @param distanceFunction parent distance function
     */
    public Instance(Relation<V> database, HiSCPreferenceVectorIndex<V> index, double epsilon, HiSCDistanceFunction<? super V> distanceFunction) {
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
      double dist1 = weightedDistance(v1, v2, pv1);
      double dist2 = weightedDistance(v1, v2, pv2);

      if(Math.max(dist1, dist2) > epsilon) {
        subspaceDim++;
        if(LOG.isDebugging()) {
          //Representation<String> rep = rep.getObjectLabelQuery();
          StringBuilder msg = new StringBuilder();
          msg.append("\ndist1 ").append(dist1);
          msg.append("\ndist2 ").append(dist2);
          // msg.append("\nv1 ").append(rep.get(v1.getID()));
          // msg.append("\nv2 ").append(rep.get(v2.getID()));
          msg.append("\nsubspaceDim ").append(subspaceDim);
          msg.append("\ncommon pv ").append(FormatUtil.format(dim, commonPreferenceVector));
          LOG.debugFine(msg.toString());
        }
      }

      // flip commonPreferenceVector for distance computation in common subspace
      BitSet inverseCommonPreferenceVector = (BitSet) commonPreferenceVector.clone();
      inverseCommonPreferenceVector.flip(0, dim);

      return new PreferenceVectorBasedCorrelationDistance(RelationUtil.dimensionality(relation), subspaceDim, weightedDistance(v1, v2, inverseCommonPreferenceVector), commonPreferenceVector);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractPreferenceVectorBasedCorrelationDistanceFunction.Parameterizer<HiSCPreferenceVectorIndex.Factory<V>> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      Class<HiSCPreferenceVectorIndex.Factory<V>> cls = ClassGenericsUtil.uglyCastIntoSubclass(HiSCPreferenceVectorIndex.Factory.class);
      factory = config.tryInstantiate(cls);
    }

    @Override
    protected HiSCDistanceFunction<V> makeInstance() {
      return new HiSCDistanceFunction<V>(factory, epsilon);
    }
  }
}