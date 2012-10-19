package de.lmu.ifi.dbs.elki.index.vafile;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DoubleDistanceDBIDList;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DoubleDistanceKNNHeap;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DoubleDistanceKNNList;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.AbstractRefiningIndex;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexFactory;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.DoubleMaxHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleObjPair;

/**
 * Vector-approximation file (VAFile)
 * 
 * Reference:
 * <p>
 * Weber, R. and Blott, S.<br>
 * An approximation based data structure for similarity search<br />
 * in: Report TR1997b, ETH Zentrum, Zurich, Switzerland
 * </p>
 * 
 * @author Thomas Bernecker
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * 
 * @apiviz.composedOf VectorApproximation
 * @apiviz.has VAFileRangeQuery
 * @apiviz.has VAFileKNNQuery
 * @apiviz.uses VALPNormDistance
 * 
 * @param <V> Vector type
 */
@Title("An approximation based data structure for similarity search")
@Reference(authors = "Weber, R. and Blott, S.", title = "An approximation based data structure for similarity search", booktitle = "Report TR1997b, ETH Zentrum, Zurich, Switzerland", url = "http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.40.480&rep=rep1&type=pdf")
public class VAFile<V extends NumberVector<?>> extends AbstractRefiningIndex<V> implements KNNIndex<V>, RangeIndex<V> {
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
    this.vectorApprox = new ArrayList<VectorApproximation>();
  }

  @Override
  protected void initialize(Relation<V> relation, DBIDs ids) {
    setPartitions(relation);
    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
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
    if((Math.log(partitions) / Math.log(2)) != (int) (Math.log(partitions) / Math.log(2))) {
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
    int approximation[] = new int[dv.getDimensionality()];
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

  @Override
  public long getReadOperations() {
    return getRandomReadOnly() + getScannedPages();
  }

  /**
   * Get the number of random read operations only.
   * 
   * @return Random read operations.
   */
  public long getRandomReadOnly() {
    return super.getReadOperations();
  }

  /**
   * Get the number of scanned bytes.
   * 
   * @return Numebr of scanned bytes.
   */
  public long getScannedPages() {
    int vacapacity = pageSize / VectorApproximation.byteOnDisk(splitPositions.length, partitions);
    int vasize = (int) Math.ceil((vectorApprox.size()) / (1.0 * vacapacity));
    return vasize * scans;
  }

  @Override
  public long getWriteOperations() {
    return -1;
  }

  @Override
  public void resetPageAccess() {
    super.resetPageAccess();
    scans = 0;
    // FIXME: writes
  }

  @Override
  public String getLongName() {
    return "VA-file index";
  }

  @Override
  public String getShortName() {
    return "va-file";
  }

  @SuppressWarnings("unchecked")
  @Override
  public <D extends Distance<D>> KNNQuery<V, D> getKNNQuery(DistanceQuery<V, D> distanceQuery, Object... hints) {
    for(Object hint : hints) {
      if(hint == DatabaseQuery.HINT_BULK) {
        // FIXME: support bulk?
        return null;
      }
    }
    DistanceFunction<? super V, ?> df = distanceQuery.getDistanceFunction();
    if(df instanceof LPNormDistanceFunction) {
      double p = ((LPNormDistanceFunction) df).getP();
      DistanceQuery<V, ?> ddq = (DistanceQuery<V, ?>) distanceQuery;
      KNNQuery<V, ?> dq = new VAFileKNNQuery((DistanceQuery<V, DoubleDistance>) ddq, p);
      return (KNNQuery<V, D>) dq;
    }
    // Not supported.
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <D extends Distance<D>> RangeQuery<V, D> getRangeQuery(DistanceQuery<V, D> distanceQuery, Object... hints) {
    DistanceFunction<? super V, ?> df = distanceQuery.getDistanceFunction();
    if(df instanceof LPNormDistanceFunction) {
      double p = ((LPNormDistanceFunction) df).getP();
      DistanceQuery<V, ?> ddq = (DistanceQuery<V, ?>) distanceQuery;
      RangeQuery<V, ?> dq = new VAFileRangeQuery((DistanceQuery<V, DoubleDistance>) ddq, p);
      return (RangeQuery<V, D>) dq;
    }
    // Not supported.
    return null;
  }

  /**
   * Range query for this index.
   * 
   * @author Erich Schubert
   */
  public class VAFileRangeQuery extends AbstractRefiningIndex<V>.AbstractRangeQuery<DoubleDistance> {
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

    public VAFileRangeQuery(DistanceQuery<V, DoubleDistance> distanceQuery, double p) {
      super(distanceQuery);
      this.p = p;
    }

    @Override
    public DoubleDistanceDBIDList getRangeForObject(V query, DoubleDistance range) {
      final double eps = range.doubleValue();
      // generate query approximation and lookup table
      VectorApproximation queryApprox = calculateApproximation(null, query);

      // Approximative distance function
      VALPNormDistance vadist = new VALPNormDistance(p, splitPositions, query, queryApprox);

      // Count a VA file scan
      scans += 1;

      DoubleDistanceDBIDList result = new DoubleDistanceDBIDList();
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
        final double dist = refine(va.id, query).doubleValue();
        if(dist <= eps) {
          result.add(dist, va.id);
        }
      }
      result.sort();
      return result;
    }
  }

  /**
   * KNN query for this index.
   * 
   * @author Erich Schubert
   */
  public class VAFileKNNQuery extends AbstractRefiningIndex<V>.AbstractKNNQuery<DoubleDistance> {
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
    public VAFileKNNQuery(DistanceQuery<V, DoubleDistance> distanceQuery, double p) {
      super(distanceQuery);
      this.p = p;
    }

    @Override
    public DoubleDistanceKNNList getKNNForObject(V query, int k) {
      // generate query approximation and lookup table
      VectorApproximation queryApprox = calculateApproximation(null, query);

      // Approximative distance function
      VALPNormDistance vadist = new VALPNormDistance(p, splitPositions, query, queryApprox);

      // Heap for the kth smallest maximum distance (yes, we need a max heap!)
      DoubleMaxHeap minMaxHeap = new DoubleMaxHeap(k+1);
      double minMaxDist = Double.POSITIVE_INFINITY;
      // Candidates with minDist <= kth maxDist
      ArrayList<DoubleObjPair<DBID>> candidates = new ArrayList<DoubleObjPair<DBID>>(vectorApprox.size());

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
        candidates.add(new DoubleObjPair<DBID>(minDist, va.id));

        // Update candidate pruning heap
        minMaxHeap.add(maxDist);
        while(minMaxHeap.size() > k) {
          minMaxHeap.poll();
        }
        if(minMaxHeap.size() > k) {
          minMaxDist = minMaxHeap.peek();
        }
      }
      // sort candidates by lower bound (minDist)
      Collections.sort(candidates);

      // refinement step
      DoubleDistanceKNNHeap result = new DoubleDistanceKNNHeap(k);

      // log.fine("candidates size " + candidates.size());
      // retrieve accurate distances
      for(DoubleObjPair<DBID> va : candidates) {
        // Stop when we are sure to have all elements
        if(result.size() >= k) {
          double kDist = result.doubleKNNDistance();
          if(va.first > kDist) {
            break;
          }
        }

        // refine the next element
        final double dist = refine(va.second, query).doubleValue();
        result.add(dist, va.second);
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
   * @apiviz.stereotype factory
   * @apiviz.has VAFile
   * 
   * @param <V> Vector type
   */
  public static class Factory<V extends NumberVector<?>> implements IndexFactory<V, VAFile<V>> {
    /**
     * Number of partitions to use in each dimension.
     * 
     * <pre>
     * -vafile.partitions 8
     * </pre>
     */
    public static final OptionID PARTITIONS_ID = OptionID.getOrCreateOptionID("vafile.partitions", "Number of partitions to use in each dimension.");

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
      return new VAFile<V>(pagesize, relation, numpart);
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return TypeUtil.NUMBER_VECTOR_FIELD;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
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
        IntParameter pagesizeP = new IntParameter(TreeIndexFactory.PAGE_SIZE_ID, 1024);
        pagesizeP.addConstraint(new GreaterConstraint(0));
        if(config.grab(pagesizeP)) {
          pagesize = pagesizeP.getValue();
        }
        IntParameter partitionsP = new IntParameter(Factory.PARTITIONS_ID);
        partitionsP.addConstraint(new GreaterConstraint(2));
        if(config.grab(partitionsP)) {
          numpart = partitionsP.getValue();
        }
      }

      @Override
      protected Factory<?> makeInstance() {
        return new Factory<NumberVector<?>>(pagesize, numpart);
      }
    }
  }
}