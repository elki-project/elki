package de.lmu.ifi.dbs.elki.evaluation.paircounting;

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
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.MeanVariance;

/**
 * Class storing the contingency table and related data on two clusterings.
 * 
 * @author Erich Schubert
 */
public class ClusterContingencyTable {
  /**
   * Noise cluster handling
   */
  protected boolean breakNoiseClusters = false;

  /**
   * Self pairing
   */
  protected boolean selfPairing = true;

  /**
   * Number of clusters in first
   */
  protected int size1 = -1;

  /**
   * Number of clusters in second
   */
  protected int size2 = -1;

  /**
   * Contingency matrix
   */
  protected int[][] contingency = null;

  /**
   * Noise flags
   */
  protected BitSet noise1 = null;

  /**
   * Noise flags
   */
  protected BitSet noise2 = null;

  /**
   * Pair counting measures
   */
  protected PairCounting paircount = null;

  /**
   * Entropy-based measures
   */
  protected Entropy entropy = null;

  /**
   * Set matching purity measures
   */
  protected SetMatchingPurity smp = null;

  /**
   * Edit-Distance measures
   */
  protected EditDistance edit = null;

  /**
   * BCubed measures
   */
  protected BCubed bcubed = null;

  /**
   * Constructor.
   * 
   * @param selfPairing Build self-pairs
   * @param breakNoiseClusters Break noise clusters into individual objects
   */
  public ClusterContingencyTable(boolean selfPairing, boolean breakNoiseClusters) {
    super();
    this.selfPairing = selfPairing;
    this.breakNoiseClusters = breakNoiseClusters;
  }

  /**
   * Process two clustering results.
   * 
   * @param result1 First clustering
   * @param result2 Second clustering
   */
  public void process(Clustering<?> result1, Clustering<?> result2) {
    // Get the clusters
    final List<? extends Cluster<?>> cs1 = result1.getAllClusters();
    final List<? extends Cluster<?>> cs2 = result2.getAllClusters();

    // Initialize
    size1 = cs1.size();
    size2 = cs2.size();
    contingency = new int[size1 + 2][size2 + 2];
    noise1 = new BitSet(size1);
    noise2 = new BitSet(size2);

    // Fill main part of matrix
    {
      {
        final Iterator<? extends Cluster<?>> it2 = cs2.iterator();
        for(int i2 = 0; it2.hasNext(); i2++) {
          final Cluster<?> c2 = it2.next();
          if(c2.isNoise()) {
            noise2.set(i2);
          }
          contingency[size1 + 1][i2] = c2.size();
          contingency[size1 + 1][size2] += c2.size();
        }
      }
      final Iterator<? extends Cluster<?>> it1 = cs1.iterator();
      for(int i1 = 0; it1.hasNext(); i1++) {
        final Cluster<?> c1 = it1.next();
        if(c1.isNoise()) {
          noise1.set(i1);
        }
        final DBIDs ids = DBIDUtil.ensureSet(c1.getIDs());
        contingency[i1][size2 + 1] = c1.size();
        contingency[size1][size2 + 1] += c1.size();

        final Iterator<? extends Cluster<?>> it2 = cs2.iterator();
        for(int i2 = 0; it2.hasNext(); i2++) {
          final Cluster<?> c2 = it2.next();
          int count = 0;
          for(DBID id : c2.getIDs()) {
            if(ids.contains(id)) {
              count++;
            }
          }
          contingency[i1][i2] = count;
          contingency[i1][size2] += count;
          contingency[size1][i2] += count;
          contingency[size1][size2] += count;
        }
      }
    }
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    if(contingency != null) {
      for(int i1 = 0; i1 < size1 + 2; i1++) {
        if(i1 >= size1) {
          buf.append("------\n");
        }
        for(int i2 = 0; i2 < size2 + 2; i2++) {
          if(i2 >= size2) {
            buf.append("| ");
          }
          buf.append(contingency[i1][i2]).append(" ");
        }
        buf.append("\n");
      }
    }
    // if(pairconfuse != null) {
    // buf.append(FormatUtil.format(pairconfuse));
    // }
    return buf.toString();
  }

  /**
   * Get (compute) the pair counting measures.
   * 
   * @return Pair counting measures
   */
  public PairCounting getPaircount() {
    if(paircount == null) {
      paircount = new PairCounting();
    }
    return paircount;
  }

  /**
   * Get (compute) the entropy based measures
   * 
   * @return Entropy based measures
   */
  public Entropy getEntropy() {
    if(entropy == null) {
      entropy = new Entropy();
    }
    return entropy;
  }

  /**
   * Get (compute) the edit-distance based measures
   * 
   * @return Edit-distance based measures
   */
  public EditDistance getEdit() {
    if(edit == null) {
      edit = new EditDistance();
    }
    return edit;
  }

  /**
   * The BCubed based measures
   * 
   * @return BCubed measures
   */
  public BCubed getBCubed() {
    if(bcubed == null) {
      bcubed = new BCubed();
    }
    return bcubed;
  }

  /**
   * Pair-counting measures.
   * 
   * @author Erich Schubert
   */
  public class PairCounting {
    /**
     * Pair counting confusion matrix (flat: inBoth, inFirst, inSecond, inNone)
     */
    protected long[] pairconfuse = null;

    /**
     * Compute the pair sizes
     */
    protected void computePairSizes() {
      assert (contingency != null) : "No clustering loaded.";
      // Aggregations
      long inBoth = 0, in1 = 0, in2 = 0, total = 0;
      // Process first clustering:
      {
        for(int i1 = 0; i1 < size1; i1++) {
          final int size = contingency[i1][size2 + 1];
          if(breakNoiseClusters && noise1.get(i1)) {
            if(selfPairing) {
              in1 += size;
            } // else: 0
          }
          else {
            if(selfPairing) {
              in1 += size * size;
            }
            else {
              in1 += size * (size - 1);
            }
          }
        }
      }
      // Process second clustering:
      {
        for(int i2 = 0; i2 < size2; i2++) {
          final int size = contingency[size1 + 1][i2];
          if(breakNoiseClusters && noise2.get(i2)) {
            if(selfPairing) {
              in2 += size;
            } // else: 0
          }
          else {
            if(selfPairing) {
              in2 += size * size;
            }
            else {
              in2 += size * (size - 1);
            }
          }
        }
      }
      // Process combinations
      for(int i1 = 0; i1 < size1; i1++) {
        for(int i2 = 0; i2 < size2; i2++) {
          final int size = contingency[i1][i2];
          if(breakNoiseClusters && (noise1.get(i1) || noise2.get(i2))) {
            if(selfPairing) {
              inBoth += size;
            } // else: 0
          }
          else {
            if(selfPairing) {
              inBoth += size * size;
            }
            else {
              inBoth += size * (size - 1);
            }
          }
        }
      }
      // The official sum
      int tsize = contingency[size1][size2];
      if(contingency[size1][size2 + 1] != tsize || contingency[size1 + 1][size2] != tsize) {
        LoggingUtil.warning("PairCounting F-Measure is not well defined for overlapping and incomplete clusterings.");
      }
      if(tsize >= Math.sqrt(Long.MAX_VALUE)) {
        LoggingUtil.warning("Your data set size probably is too big for this implementation, which uses only long precision.");
      }
      if(selfPairing) {
        total = tsize * tsize;
      }
      else {
        total = tsize * (tsize - 1);
      }
      long inFirst = in1 - inBoth, inSecond = in2 - inBoth;
      long inNone = total - (inBoth + inFirst + inSecond);
      pairconfuse = new long[] { inBoth, inFirst, inSecond, inNone };
    }

    /**
     * Get the pair-counting confusion matrix.
     * 
     * @return Confusion matrix
     */
    public long[] getPairConfusionMatrix() {
      if(pairconfuse == null) {
        computePairSizes();
      }
      return pairconfuse;
    }

    /**
     * Get the pair-counting F-Measure
     * 
     * @param beta Beta value.
     * @return F-Measure
     */
    public double fMeasure(double beta) {
      final double beta2 = beta * beta;
      getPairConfusionMatrix();
      double fmeasure = ((1 + beta2) * pairconfuse[0]) / ((1 + beta2) * pairconfuse[0] + beta2 * pairconfuse[1] + pairconfuse[2]);
      return fmeasure;
    }

    /**
     * Get the pair-counting F1-Measure.
     * 
     * @return F1-Measure
     */
    public double f1Measure() {
      return fMeasure(1.0);
    }

    /**
     * Computes the pair-counting precision.
     * 
     * @return pair-counting precision
     */
    public double precision() {
      getPairConfusionMatrix();
      return ((double) pairconfuse[0]) / (pairconfuse[0] + pairconfuse[2]);
    }

    /**
     * Computes the pair-counting recall.
     * 
     * @return pair-counting recall
     */
    public double recall() {
      getPairConfusionMatrix();
      return ((double) pairconfuse[0]) / (pairconfuse[0] + pairconfuse[1]);
    }

    /**
     * Computes the pair-counting Fowlkes-mallows.
     * 
     * @return pair-counting Fowlkes-mallows
     */
    public double fowlkesMallows() {
      return Math.sqrt(precision() * recall());
    }

    /**
     * Computes the Rand index (RI).
     * 
     * @return The Rand index (RI).
     */
    public double randIndex() {
      getPairConfusionMatrix();
      final double sum = pairconfuse[0] + pairconfuse[1] + pairconfuse[2] + pairconfuse[3];
      return (pairconfuse[0] + pairconfuse[3]) / sum;
    }

    /**
     * Computes the adjusted Rand index (ARI).
     * 
     * @return The adjusted Rand index (ARI).
     */
    public double adjustedRandIndex() {
      getPairConfusionMatrix();
      final double nom = pairconfuse[0] * pairconfuse[3] - pairconfuse[1] * pairconfuse[2];
      final long d1 = (pairconfuse[0] + pairconfuse[1]) * (pairconfuse[1] + pairconfuse[3]);
      final long d2 = (pairconfuse[0] + pairconfuse[2]) * (pairconfuse[2] + pairconfuse[3]);
      return 2 * nom / (d1 + d2);
    }

    /**
     * Computes the Jaccard index
     * 
     * @return The Jaccard index
     */
    public double jaccard() {
      getPairConfusionMatrix();
      final double sum = pairconfuse[0] + pairconfuse[1] + pairconfuse[2];
      return pairconfuse[0] / sum;
    }

    /**
     * Computes the Mirkin index
     * 
     * @return The Mirkin index
     */
    public long mirkin() {
      getPairConfusionMatrix();
      return 2 * (pairconfuse[1] + pairconfuse[2]);
    }
  }

  /**
   * Entropy based measures
   * 
   * @author Sascha Goldhofer
   */
  public class Entropy {
    /**
     * Entropy in first
     */
    protected double entropyFirst = -1.0;

    /**
     * Entropy in second
     */
    protected double entropySecond = -1.0;

    /**
     * Joint entropy
     */
    protected double entropyJoint = -1.0;

    /**
     * Get the entropy of the first clustering using Log_2. (not normalized, 0 =
     * unequal)
     * 
     * @return Entropy of first clustering
     */
    public double entropyFirst() {
      if(entropyFirst < 0) {
        entropyFirst = 0.0;
        // iterate over first clustering
        for(int i1 = 0; i1 < size1; i1++) {
          if(contingency[i1][size2] > 0) {
            double probability = 1.0 * contingency[i1][size2] / contingency[size1][size2];
            entropyFirst += probability * Math.log(probability);
          }
        }
        entropyFirst = -entropyFirst;
      }

      return entropyFirst;
    }

    /**
     * Get the entropy of the second clustering using Log_2. (not normalized, 0
     * = unequal)
     * 
     * @return Entropy of second clustering
     */
    public double entropySecond() {
      if(entropySecond < 0) {
        entropySecond = 0.0;
        // iterate over first clustering
        for(int i2 = 0; i2 < size2; i2++) {
          if(contingency[size1][i2] > 0) {
            double probability = 1.0 * contingency[size1][i2] / contingency[size1][size2];
            entropySecond += probability * Math.log(probability);
          }
        }
        entropySecond = -entropySecond;
      }
      return entropySecond;
    }

    /**
     * Get the joint entropy of both clusterings (not normalized, 0 = unequal)
     * 
     * @return Joint entropy of both clusterings
     */
    public double entropyJoint() {
      if(entropyJoint == -1.0) {
        entropyJoint = 0.0;
        for(int i1 = 0; i1 < size1; i1++) {
          for(int i2 = 0; i2 < size2; i2++) {
            if(contingency[i1][i2] > 0) {
              double probability = 1.0 * contingency[i1][i2] / contingency[size1][size2];
              entropyJoint += probability * Math.log(probability);
            }
          }
        }
        entropyJoint = -entropyJoint;
      }
      return entropyJoint;
    }

    /**
     * Get the conditional entropy of the first clustering. (not normalized, 0 =
     * equal)
     * 
     * @return Conditional entropy of first clustering
     */
    public double entropyConditionalFirst() {
      return (entropyJoint() - entropySecond());
    }

    /**
     * Get the conditional entropy of the first clustering. (not normalized, 0 =
     * equal)
     * 
     * @return Conditional entropy of second clustering
     */
    public double entropyConditionalSecond() {
      return (entropyJoint() - entropyFirst());
    }

    /**
     * Get Powers entropy (not normalized, 0 = unequal)
     * 
     * @return Powers
     */
    public double entropyPowers() {
      return (2 * entropyJoint() / (entropyFirst() + entropySecond()));
    }

    /**
     * Get the mutual information (not normalized, 0 = unequal)
     * 
     * @return Mutual information
     */
    public double entropyMutualInformation() {
      return (entropyFirst() + entropySecond() - entropyJoint());
    }

    /**
     * Get the joint-normalized mutual information
     * 
     * @return Joint Normalized Mutual information
     */
    public double entropyNMIJoint() {
      return (entropyMutualInformation() / entropyJoint());
    }

    /**
     * Get the min-normalized mutual information
     * 
     * @return Min Normalized Mutual information
     */
    public double entropyNMIMin() {
      return (entropyMutualInformation() / Math.min(entropyFirst(), entropySecond()));
    }

    /**
     * Get the max-normalized mutual information
     * 
     * @return Max Normalized Mutual information
     */
    public double entropyNMIMax() {
      return (entropyMutualInformation() / Math.max(entropyFirst(), entropySecond()));
    }

    /**
     * Get the sum-normalized mutual information
     * 
     * @return Sum Normalized Mutual information
     */
    public double entropyNMISum() {
      return (2 * entropyMutualInformation() / (entropyFirst() + entropySecond()));
    }

    /**
     * Get the sqrt-normalized mutual information
     * 
     * @return Sqrt Normalized Mutual information
     */
    public double entropyNMISqrt() {
      return (entropyMutualInformation() / Math.sqrt(entropyFirst() * entropySecond()));
    }

    /**
     * Get the variation of information (not normalized, 0 = equal)
     * 
     * @return Variation of information
     */
    public double variationOfInformation() {
      return (2 * entropyJoint() - (entropyFirst() + entropySecond()));
    }

    /**
     * Get the normalized variation of information (normalized, 0 = equal)
     * 
     * @return Normalized Variation of information
     */
    public double normalizedVariationOfInformation() {
      return (1.0 - (entropyMutualInformation() / entropyJoint()));
    }

    /**
     * Get the entropy F1-Measure
     */
    public double f1Measure() {
      return Util.f1Measure(entropyFirst, entropySecond);
    }
  }

  /**
   * Set matching purity measures
   * 
   * @author Sascha Goldhofer
   */
  public class SetMatchingPurity {
    /**
     * Result cache
     */
    protected double smPurity = -1.0, smInversePurity = -1.0;

    /**
     * Get the set matchings purity (first:second clustering) (normalized, 1 =
     * equal)
     * 
     * @return purity
     */
    public double purity() {
      if(smPurity < 0) {
        smPurity = 0.0;
        // iterate first clustering
        for(int i1 = 0; i1 < size1; i1++) {
          double precisionMax = 0.0;
          for(int i2 = 0; i2 < size2; i2++) {
            precisionMax = Math.max(precisionMax, (1.0 * contingency[i1][i2]));
            // / contingency[i1][size2]));
          }
          smPurity += (precisionMax / contingency[size1][size2]);
          // * contingency[i1][size2]/contingency[size1][size2];
        }
      }
      return smPurity;
    }

    /**
     * Get the set matchings inverse purity (second:first clustering)
     * (normalized, 1 = equal)
     * 
     * @return Inverse purity
     */
    public double inversePurity() {
      if(smInversePurity < 0) {
        smInversePurity = 0.0;
        // iterate second clustering
        for(int i2 = 0; i2 < size2; i2++) {
          double recallMax = 0.0;
          for(int i1 = 0; i1 < size1; i1++) {
            recallMax = Math.max(recallMax, (1.0 * contingency[i1][i2]));
            // / contingency[i1][size2]));
          }
          smInversePurity += (recallMax / contingency[size1][size2]);
          // * contingency[i1][size2]/contingency[size1][size2];
        }
      }
      return smInversePurity;
    }

    /**
     * Get the set matching F1-Measure
     * 
     * @return Set Matching F1-Measure
     */
    public double f1Measure() {
      return Util.f1Measure(purity(), inversePurity());
    }
  }

  /**
   * Edit distance measures
   * 
   * @author Sascha Goldhofer
   */
  public class EditDistance {
    /**
     * Edit operations for first clustering to second clustering.
     */
    int editFirst = -1;

    /**
     * Edit operations for second clustering to first clustering.
     */
    int editSecond = -1;

    /**
     * Get the baseline editing Operations ( = total Objects)
     * 
     * @return worst case amount of operations
     */
    public int editOperationsBaseline() {
      // total objects merge operations...
      return contingency[size1][size2];
    }

    /**
     * Get the editing operations required to transform first clustering to
     * second clustering
     * 
     * @return Editing operations used to transform first into second clustering
     */
    public int editOperationsFirst() {
      if(editFirst == -1) {
        editFirst = 0;

        // iterate over first clustering
        for(int i1 = 0; i1 < size1; i1++) {
          // get largest cell
          int largestLabelSet = 0;
          for(int i2 = 0; i2 < size2; i2++) {
            largestLabelSet = Math.max(largestLabelSet, contingency[i1][i2]);
          }

          // merge: found (largest) cluster to second clusterings cluster
          editFirst++;
          // move: wrong objects from this cluster to correct cluster (of second
          // clustering)
          editFirst += contingency[i1][size2] - largestLabelSet;
        }
      }
      return editFirst;
    }

    /**
     * Get the editing operations required to transform second clustering to
     * first clustering
     * 
     * @return Editing operations used to transform second into first clustering
     */
    public int editOperationsSecond() {
      if(editSecond == -1) {
        editSecond = 0;

        // iterate over second clustering
        for(int i2 = 0; i2 < size2; i2++) {
          // get largest cell
          int largestLabelSet = 0;
          for(int i1 = 0; i1 < size1; i1++) {
            largestLabelSet = Math.max(largestLabelSet, contingency[i1][i2]);
          }

          // merge: found (largest) cluster to second clusterings cluster
          editSecond++;
          // move: wrong objects from this cluster to correct cluster (of second
          // clustering)
          editSecond += contingency[size1][i2] - largestLabelSet;
        }
      }
      return editSecond;
    }

    /**
     * Get the editing distance to transform second clustering to first
     * clustering (normalized, 1=equal)
     * 
     * @return Editing distance first into second clustering
     */
    public double editDistanceFirst() {
      return 1.0 * editOperationsFirst() / editOperationsBaseline();
    }

    /**
     * Get the editing distance to transform second clustering to first
     * clustering (normalized, 1=equal)
     * 
     * @return Editing distance second into first clustering
     */
    public double editDistanceSecond() {
      return 1.0 * editOperationsSecond() / editOperationsBaseline();
    }

    /**
     * Get the edit distance F1-Measure
     * 
     * @return Edit Distance F1-Measure
     */
    public double f1Measure() {
      return Util.f1Measure(editDistanceFirst(), editDistanceSecond());
    }
  }

  /**
   * BCubed measures
   * 
   * @author Sascha Goldhofer
   */
  public class BCubed {
    /**
     * Result cache
     */
    protected double bCubedPrecision = -1.0, bCubedRecall = -1.0;

    /**
     * Get the BCubed Precision (first clustering) (normalized, 0 = unequal)
     * 
     * @return BCubed Precision
     */
    public double precision() {
      if(bCubedPrecision < 0) {
        bCubedPrecision = 0.0;
        bCubedRecall = 0.0;

        for(int i1 = 0; i1 < size1; i1++) {
          for(int i2 = 0; i2 < size2; i2++) {
            // precision of one item
            double precision = 1.0 * contingency[i1][i2] / contingency[i1][size2];
            // precision for all items in cluster
            bCubedPrecision += (precision * contingency[i1][i2]);

            // recall of one item
            double recall = 1.0 * contingency[i1][i2] / contingency[size1][i2];
            // recall for all items in cluster
            bCubedRecall += (recall * contingency[i1][i2]);
          }
        }

        bCubedPrecision = bCubedPrecision / contingency[size1][size2];
        bCubedRecall = bCubedRecall / contingency[size1][size2];
      }
      return bCubedPrecision;
    }

    /**
     * Get the BCubed Recall (first clustering) (normalized, 0 = unequal)
     * 
     * @return BCubed Recall
     */
    public double recall() {
      if(bCubedRecall < 0) {
        precision();
      }
      return bCubedRecall;
    }

    /**
     * Get the BCubed F1-Measure
     * 
     * @return BCubed F1-Measure
     */
    public double f1Measure() {
      return Util.f1Measure(precision(), recall());
    }
  }

  /**
   * Compute the average Gini for each cluster (in both clusterings -
   * symmetric).
   * 
   * @return Mean and variance of Gini
   */
  public MeanVariance averageSymmetricGini() {
    MeanVariance mv = new MeanVariance();
    for(int i1 = 0; i1 < size1; i1++) {
      double purity = 0.0;
      if(contingency[i1][size2] > 0) {
        final double cs = contingency[i1][size2]; // sum, as double.
        for(int i2 = 0; i2 < size2; i2++) {
          double rel = contingency[i1][i2] / cs;
          purity += rel * rel;
        }
      }
      mv.put(purity);
    }
    for(int i2 = 0; i2 < size2; i2++) {
      double purity = 0.0;
      if(contingency[size1][i2] > 0) {
        final double cs = contingency[size1][i2]; // sum, as double.
        for(int i1 = 0; i1 < size1; i1++) {
          double rel = contingency[i1][i2] / cs;
          purity += rel * rel;
        }
      }
      mv.put(purity);
    }
    return mv;
  }

  /**
   * Utility class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static final class Util {
    /**
     * F-Measure
     * 
     * @param precision Precision
     * @param recall Recall
     * @param beta Beta value
     * @return F-Measure
     */
    public static double fMeasure(double precision, double recall, double beta) {
      final double beta2 = beta * beta;
      return (1 + beta2) * precision * recall / (beta2 * precision + recall);
    }

    /**
     * F1-Measure (F-Measure with beta = 1)
     * 
     * @param precision Precision
     * @param recall Recall
     * @return F-Measure
     */
    public static double f1Measure(double precision, double recall) {
      return 2 * precision * recall / (precision + recall);
    }
  }
}