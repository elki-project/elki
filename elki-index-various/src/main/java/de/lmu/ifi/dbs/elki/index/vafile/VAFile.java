/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.index.vafile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.LPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.index.AbstractRefiningIndex;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.AbstractPageFileFactory;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.DoubleMaxHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

import net.jafama.FastMath;

/**
 * Vector-approximation file (VAFile)
 * <p>
 * Reference:
 * <p>
 * R. Weber, S. Blott<br>
 * An approximation based data structure for similarity search<br>
 * Report TR1997b, ETH Zentrum, Zurich, Switzerland
 * 
 * @author Thomas Bernecker
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @opt nodefillcolor LemonChiffon
 * 
 * @composed - - - VectorApproximation
 * @has - - - VAFileRangeQuery
 * @has - - - VAFileKNNQuery
 * @assoc - - - VALPNormDistance
 * 
 * @param <V> Vector type
 */
@Title("An approximation based data structure for similarity search")
@Reference(authors = "R. Weber, S. Blott", //
    title = "An approximation based data structure for similarity search", //
    booktitle = "Report TR1997b, ETH Zentrum, Zurich, Switzerland", //
    url = "http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.40.480&rep=rep1&type=pdf", //
    bibkey = "tr/ethz/WeberS97")
public class VAFile<V extends NumberVector> extends AbstractRefiningIndex<V> implements KNNIndex<V>, RangeIndex<V> {
  /**
   * Logging class.
   */
  private static final Logging LOG = Logging.getLogger(VAFile.class);

  /**
   * Approximation index.
   */
  private List<VectorApproximation> vectorApprox;

  /**
   * Number of partitions.
   */
  private int partitions;

  /**
   * Quantile grid we use.
   */
  private double[][] splitPositions;

  /**
   * Page size, for estimating the VA file size.
   */
  int pageSize;

  /**
   * Number of scans we performed.
   */
  int scans;

  /**
   * Constructor.
   * 
   * @param pageSize Page size of simulated index
   * @param relation Relation to index
   * @param partitions Number of partitions for each dimension.
   */
  public VAFile(int pageSize, Relation<V> relation, int partitions) {
    super(relation);
    this.partitions = partitions;
    this.pageSize = pageSize;
    this.scans = 0;
    this.vectorApprox = new ArrayList<>();
  }

  @Override
  public void initialize() {
    setPartitions(relation);
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      DBID id = DBIDUtil.deref(iter);
      vectorApprox.add(calculateApproximation(id, relation.get(id)));
    }
  }

  /**
   * Initialize the data set grid by computing quantiles.
   * 
   * @param relation Data relation
   * @throws IllegalArgumentException
   */
  public void setPartitions(Relation<V> relation) throws IllegalArgumentException {
    if((FastMath.log(partitions) / FastMath.log(2)) != (int) (FastMath.log(partitions) / FastMath.log(2))) {
      throw new IllegalArgumentException("Number of partitions must be a power of 2!");
    }

    final int dimensions = RelationUtil.dimensionality(relation);
    final int size = relation.size();
    splitPositions = new double[dimensions][partitions + 1];

    for(int d = 0; d < dimensions; d++) {
      double[] tempdata = new double[size];
      int j = 0;
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        tempdata[j] = relation.get(iditer).doubleValue(d);
        j += 1;
      }
      Arrays.sort(tempdata);

      for(int b = 0; b < partitions; b++) {
        int start = (int) (b * size / (double) partitions);
        splitPositions[d][b] = tempdata[start];
      }
      // make sure that last object will be included
      splitPositions[d][partitions] = tempdata[size - 1] + 0.000001;
    }
  }

  /**
   * Calculate the VA file position given the existing borders.
   * 
   * @param id Object ID
   * @param dv Data vector
   * @return Vector approximation
   */
  public VectorApproximation calculateApproximation(DBID id, V dv) {
    int[] approximation = new int[dv.getDimensionality()];
    for(int d = 0; d < splitPositions.length; d++) {
      final double val = dv.doubleValue(d);
      final int lastBorderIndex = splitPositions[d].length - 1;

      // Value is below data grid
      if(val < splitPositions[d][0]) {
        approximation[d] = 0;
        if(id != null) {
          LOG.warning("Vector outside of VAFile grid!");
        }
      } // Value is above data grid
      else if(val > splitPositions[d][lastBorderIndex]) {
        approximation[d] = lastBorderIndex - 1;
        if(id != null) {
          LOG.warning("Vector outside of VAFile grid!");
        }
      } // normal case
      else {
        // Search grid position
        int pos = Arrays.binarySearch(splitPositions[d], val);
        pos = (pos >= 0) ? pos : ((-pos) - 2);
        approximation[d] = pos;
      }
    }
    return new VectorApproximation(id, approximation);
  }

  /**
   * Get the number of scanned bytes.
   * 
   * @return Number of scanned bytes.
   */
  public long getScannedPages() {
    int vacapacity = pageSize / VectorApproximation.byteOnDisk(splitPositions.length, partitions);
    long vasize = (long) Math.ceil((vectorApprox.size()) / (1.0 * vacapacity));
    return vasize * scans;
  }

  @Override
  public Logging getLogger() {
    return LOG;
  }

  @Override
  public void logStatistics() {
    super.logStatistics();
    // FIXME:
    LOG.statistics("scanned pages:" + getScannedPages());
  }

  @Override
  public String getLongName() {
    return "VA-file index";
  }

  @Override
  public String getShortName() {
    return "va-file";
  }

  @Override
  public KNNQuery<V> getKNNQuery(DistanceQuery<V> distanceQuery, Object... hints) {
    DistanceFunction<? super V> df = distanceQuery.getDistanceFunction();
    if(df instanceof LPNormDistanceFunction) {
      double p = ((LPNormDistanceFunction) df).getP();
      return new VAFileKNNQuery(distanceQuery, p);
    }
    // Not supported.
    return null;
  }

  @Override
  public RangeQuery<V> getRangeQuery(DistanceQuery<V> distanceQuery, Object... hints) {
    DistanceFunction<? super V> df = distanceQuery.getDistanceFunction();
    if(df instanceof LPNormDistanceFunction) {
      double p = ((LPNormDistanceFunction) df).getP();
      return new VAFileRangeQuery(distanceQuery, p);
    }
    // Not supported.
    return null;
  }

  /**
   * Range query for this index.
   * 
   * @author Erich Schubert
   */
  public class VAFileRangeQuery extends AbstractRefiningIndex<V>.AbstractRangeQuery {
    /**
     * LP Norm p parameter.
     */
    final double p;

    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query object
     * @param p LP norm p
     */

    public VAFileRangeQuery(DistanceQuery<V> distanceQuery, double p) {
      super(distanceQuery);
      this.p = p;
    }

    @Override
    public void getRangeForObject(V query, double eps, ModifiableDoubleDBIDList result) {
      // generate query approximation and lookup table
      VectorApproximation queryApprox = calculateApproximation(null, query);

      // Approximative distance function
      VALPNormDistance vadist = new VALPNormDistance(p, splitPositions, query, queryApprox);

      // Count a VA file scan
      scans += 1;

      // Approximation step
      for(int i = 0; i < vectorApprox.size(); i++) {
        VectorApproximation va = vectorApprox.get(i);
        double minDist = vadist.getMinDist(va);

        if(minDist > eps) {
          continue;
        }

        // TODO: we don't need to refine always (maxDist < eps), if we are
        // interested in the DBID only! But this needs an API change.

        // refine the next element
        final double dist = refine(va.id, query);
        if(dist <= eps) {
          result.add(dist, va.id);
        }
      }
    }
  }

  /**
   * KNN query for this index.
   * 
   * @author Erich Schubert
   */
  public class VAFileKNNQuery extends AbstractRefiningIndex<V>.AbstractKNNQuery {
    /**
     * LP Norm p parameter.
     */
    final double p;

    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query object
     * @param p LP norm p
     */
    public VAFileKNNQuery(DistanceQuery<V> distanceQuery, double p) {
      super(distanceQuery);
      this.p = p;
    }

    @Override
    public KNNList getKNNForObject(V query, int k) {
      // generate query approximation and lookup table
      VectorApproximation queryApprox = calculateApproximation(null, query);

      // Approximative distance function
      VALPNormDistance vadist = new VALPNormDistance(p, splitPositions, query, queryApprox);

      // Heap for the kth smallest maximum distance (yes, we need a max heap!)
      DoubleMaxHeap minMaxHeap = new DoubleMaxHeap(k + 1);
      double minMaxDist = Double.POSITIVE_INFINITY;
      // Candidates with minDist <= kth maxDist
      ModifiableDoubleDBIDList candidates = DBIDUtil.newDistanceDBIDList(vectorApprox.size());

      // Count a VA file scan
      scans += 1;

      // Approximation step
      for(int i = 0; i < vectorApprox.size(); i++) {
        VectorApproximation va = vectorApprox.get(i);
        double minDist = vadist.getMinDist(va);
        double maxDist = vadist.getMaxDist(va);

        // Skip excess candidate generation:
        if(minDist > minMaxDist) {
          continue;
        }
        candidates.add(minDist, va.id);

        // Update candidate pruning heap
        minMaxHeap.add(maxDist, k);
        if(minMaxHeap.size() >= k) {
          minMaxDist = minMaxHeap.peek();
        }
      }
      // sort candidates by lower bound (minDist)
      candidates.sort();

      // refinement step
      KNNHeap result = DBIDUtil.newHeap(k);

      // log.fine("candidates size " + candidates.size());
      // retrieve accurate distances
      for(DoubleDBIDListIter iter = candidates.iter(); iter.valid(); iter.advance()) {
        // Stop when we are sure to have all elements
        if(result.size() >= k) {
          double kDist = result.getKNNDistance();
          if(iter.doubleValue() > kDist) {
            break;
          }
        }

        // refine the next element
        final double dist = refine(iter, query);
        result.insert(dist, iter);
      }
      if(LOG.isDebuggingFinest()) {
        LOG.finest("query = (" + query + ")");
        LOG.finest("database: " + vectorApprox.size() + ", candidates: " + candidates.size() + ", results: " + result.size());
      }

      return result.toKNNList();
    }
  }

  /**
   * Index factory class.
   * 
   * @author Erich Schubert
   * 
   * @stereotype factory
   * @has - - - VAFile
   * 
   * @param <V> Vector type
   */
  public static class Factory<V extends NumberVector> implements IndexFactory<V> {
    /**
     * Number of partitions to use in each dimension.
     * 
     * <pre>
     * -vafile.partitions 8
     * </pre>
     */
    public static final OptionID PARTITIONS_ID = new OptionID("vafile.partitions", "Number of partitions to use in each dimension.");

    /**
     * Page size.
     */
    int pagesize = 1;

    /**
     * Number of partitions.
     */
    int numpart = 2;

    /**
     * Constructor.
     * 
     * @param pagesize Page size
     * @param numpart Number of partitions
     */
    public Factory(int pagesize, int numpart) {
      super();
      this.pagesize = pagesize;
      this.numpart = numpart;
    }

    @Override
    public VAFile<V> instantiate(Relation<V> relation) {
      return new VAFile<>(pagesize, relation, numpart);
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return TypeUtil.NUMBER_VECTOR_FIELD;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     */
    public static class Parameterizer extends AbstractParameterizer {
      /**
       * Page size.
       */
      int pagesize = 1;

      /**
       * Number of partitions.
       */
      int numpart = 2;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        IntParameter pagesizeP = new IntParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 1024) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
        if(config.grab(pagesizeP)) {
          pagesize = pagesizeP.getValue();
        }
        IntParameter partitionsP = new IntParameter(Factory.PARTITIONS_ID) //
            .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
        if(config.grab(partitionsP)) {
          numpart = partitionsP.getValue();
        }
      }

      @Override
      protected Factory<?> makeInstance() {
        return new Factory<>(pagesize, numpart);
      }
    }
  }
}
