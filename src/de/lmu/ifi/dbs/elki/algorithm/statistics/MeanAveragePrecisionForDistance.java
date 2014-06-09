package de.lmu.ifi.dbs.elki.algorithm.statistics;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.type.AlternativeTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.roc.ROC;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * Evaluate a distance functions performance by computing the mean average
 * precision, when ranking the objects by distance.
 * 
 * TODO: refactor akin to ROC AUC evaluation.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public class MeanAveragePrecisionForDistance<O> extends AbstractDistanceBasedAlgorithm<O, MeanAveragePrecisionForDistance.MAPResult> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(MeanAveragePrecisionForDistance.class);

  /**
   * Relative number of object to use in sampling.
   */
  private double sampling = 1.0;

  /**
   * Random sampling seed.
   */
  private RandomFactory random = null;

  /**
   * Include query object in evaluation.
   */
  private boolean includeSelf;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param sampling Sampling rate
   * @param random Random sampling generator
   * @param includeSelf Include query object in evaluation
   */
  public MeanAveragePrecisionForDistance(DistanceFunction<? super O> distanceFunction, double sampling, RandomFactory random, boolean includeSelf) {
    super(distanceFunction);
    this.sampling = sampling;
    this.random = random;
    this.includeSelf = includeSelf;
  }

  /**
   * Run the algorithm
   * 
   * @param database Database to run on (for kNN queries)
   * @param relation Relation for distance computations
   * @param lrelation Relation for class label comparison
   * @return Vectors containing mean and standard deviation.
   */
  public MAPResult run(Database database, Relation<O> relation, Relation<?> lrelation) {
    final DistanceQuery<O> distQuery = database.getDistanceQuery(relation, getDistanceFunction());

    final DBIDs ids;
    if(sampling < 1.) {
      int size = Math.max(1, (int) (sampling * relation.size()));
      ids = DBIDUtil.randomSample(relation.getDBIDs(), size, random);
    }
    else if(sampling > 1.) {
      ids = DBIDUtil.randomSample(relation.getDBIDs(), (int) sampling, random);
    }
    else {
      ids = relation.getDBIDs();
    }

    // For storing the positive neighbors.
    ModifiableDBIDs posn = DBIDUtil.newHashSet();
    // Distance storage.
    ModifiableDoubleDBIDList nlist = DBIDUtil.newDistanceDBIDList(relation.size());

    // Statistics tracking
    Mean mean = new Mean(), map = new Mean(), mroc = new Mean();

    FiniteProgress objloop = LOG.isVerbose() ? new FiniteProgress("Processing query objects", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      Object label = lrelation.get(iter);
      findMatches(posn, lrelation, label);
      computeDistances(nlist, iter, distQuery, relation);
      computeAP(iter, posn, nlist, mean);
      map.put(mean.getCount() > 0 ? mean.getMean() : 0.);
      // We may as well compute ROC AUC while we're at it.
      mroc.put(ROC.computeROCAUCDistanceResult(posn, nlist));
      LOG.incrementProcessed(objloop);
    }
    LOG.ensureCompleted(objloop);

    return new MAPResult(map.getMean(), mroc.getMean());
  }

  /**
   * Test whether two relation agree.
   * 
   * @param ref Reference object
   * @param test Test object
   * @return {@code true} if the objects match
   */
  protected static boolean match(Object ref, Object test) {
    if(ref == null) {
      return false;
    }
    // Cheap and fast, may hold for class labels!
    if(ref == test) {
      return true;
    }
    if(ref instanceof LabelList && test instanceof LabelList) {
      final LabelList lref = (LabelList) ref;
      final LabelList ltest = (LabelList) test;
      final int s1 = lref.size(), s2 = ltest.size();
      if(s1 == 0 || s2 == 0) {
        return false;
      }
      for(int i = 0; i < s1; i++) {
        String l1 = lref.get(i);
        if(l1 == null) {
          continue;
        }
        for(int j = 0; j < s2; j++) {
          if(l1.equals(ltest.get(j))) {
            return true;
          }
        }
      }
    }
    // Fallback to equality, e.g. on class labels
    return ref.equals(test);
  }

  /**
   * Find all matching objects.
   * 
   * @param posn Output set.
   * @param lrelation Label relation
   * @param label Query object label
   */
  private void findMatches(ModifiableDBIDs posn, Relation<?> lrelation, Object label) {
    posn.clear();
    for(DBIDIter ri = lrelation.iterDBIDs(); ri.valid(); ri.advance()) {
      if(match(label, lrelation.get(ri))) {
        posn.add(ri);
      }
    }
  }

  /**
   * Compute the distances to the neighbor objects.
   * 
   * @param nlist Neighbor list (output)
   * @param iter Query object
   * @param distQuery Distance function
   * @param relation Data relation
   */
  private void computeDistances(ModifiableDoubleDBIDList nlist, DBIDIter iter, final DistanceQuery<O> distQuery, Relation<O> relation) {
    nlist.clear();
    O qo = relation.get(iter);
    for(DBIDIter ri = relation.iterDBIDs(); ri.valid(); ri.advance()) {
      nlist.add(distQuery.distance(qo, ri), ri);
    }
    nlist.sort();
  }

  /**
   * Compute the average precision.
   * 
   * @param query Query object
   * @param posn Positive neighbors
   * @param nlist Neighbor list (sorted)
   * @param mean Output mean
   */
  private void computeAP(DBIDIter query, DBIDs posn, ModifiableDoubleDBIDList nlist, Mean mean) {
    mean.reset();
    int positive = 0, i = 0, npos = 0;
    double prev = Double.NaN;
    for(DoubleDBIDListIter ri = nlist.iter(); ri.valid(); ri.advance()) {
      if(!includeSelf && DBIDUtil.equal(query, ri)) {
        // Do not increment i.
        continue;
      }
      i++;
      if(posn.contains(ri)) {
        positive++;
        npos++;
      }
      // Tie handling:
      if(ri.doubleValue() == prev) {
        continue;
      }
      // We may have seen positives earlier.
      if(npos > 0) {
        mean.put(positive / (double) i, npos);
        npos = 0;
      }
      prev = ri.doubleValue();
    }
    // Ties at maximum distance and last object:
    if(npos > 0) {
      mean.put(positive / (double) i, npos);
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    TypeInformation cls = new AlternativeTypeInformation(TypeUtil.CLASSLABEL, TypeUtil.LABELLIST);
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction(), cls);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Result object for MAP scores.
   * 
   * @author Erich Schubert
   */
  public static class MAPResult implements Result, TextWriteable {
    /**
     * MAP value
     */
    private double map;

    /**
     * ROC AUC value
     */
    private double rocauc;

    /**
     * Constructor.
     * 
     * @param map MAP value
     * @param rocauc ROC AUC value
     */
    public MAPResult(double map, double rocauc) {
      super();
      this.map = map;
      this.rocauc = rocauc;
    }

    /**
     * @return the area under curve
     */
    public double getROCAUC() {
      return rocauc;
    }

    /**
     * @return the medium average precision
     */
    public double getMAP() {
      return map;
    }

    @Override
    public String getLongName() {
      return "MAP score";
    }

    @Override
    public String getShortName() {
      return "map-score";
    }

    @Override
    public void writeToText(TextWriterStream out, String label) {
      out.inlinePrintNoQuotes("MAP");
      out.inlinePrint(map);
      out.flush();
      out.inlinePrintNoQuotes("ROCAUC");
      out.inlinePrint(rocauc);
      out.flush();
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   * 
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Parameter to enable sampling.
     */
    public static final OptionID SAMPLING_ID = new OptionID("map.sampling", "Relative amount of object to sample.");

    /**
     * Parameter to control the sampling random seed.
     */
    public static final OptionID SEED_ID = new OptionID("map.sampling-seed", "Random seed for deterministic sampling.");

    /**
     * Parameter to include the query object.
     */
    public static final OptionID INCLUDESELF_ID = new OptionID("map.includeself", "Include the query object in the evaluation.");

    /**
     * Relative amount of data to sample.
     */
    protected double sampling = 1.0;

    /**
     * Random sampling seed.
     */
    protected RandomFactory seed = null;

    /**
     * Include query object in evaluation.
     */
    protected boolean includeSelf;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter samplingP = new DoubleParameter(SAMPLING_ID);
      samplingP.addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      samplingP.addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE);
      samplingP.setOptional(true);
      if(config.grab(samplingP)) {
        sampling = samplingP.getValue();
      }
      final RandomParameter rndP = new RandomParameter(SEED_ID);
      rndP.setOptional(true);
      if(config.grab(rndP)) {
        seed = rndP.getValue();
      }
      final Flag includeP = new Flag(INCLUDESELF_ID);
      if(config.grab(includeP)) {
        includeSelf = includeP.isTrue();
      }
    }

    @Override
    protected MeanAveragePrecisionForDistance<O> makeInstance() {
      return new MeanAveragePrecisionForDistance<>(distanceFunction, sampling, seed, includeSelf);
    }
  }
}
