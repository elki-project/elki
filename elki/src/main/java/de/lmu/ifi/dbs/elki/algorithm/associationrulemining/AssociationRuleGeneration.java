package de.lmu.ifi.dbs.elki.algorithm.associationrulemining;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.associationrulemining.interestingnessmeasure.AbstractInterestingnessMeasure;
import de.lmu.ifi.dbs.elki.algorithm.associationrulemining.interestingnessmeasure.Confidence;
import de.lmu.ifi.dbs.elki.algorithm.itemsetmining.AbstractFrequentItemsetAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.itemsetmining.Itemset;
import de.lmu.ifi.dbs.elki.algorithm.itemsetmining.OneItemset;
import de.lmu.ifi.dbs.elki.algorithm.itemsetmining.SparseItemset;
import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.AssociationRuleResult;
import de.lmu.ifi.dbs.elki.result.FrequentItemsetsResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * This algorithm calls a specified frequent itemset algorithm
 * and calculates all association rules, having a interest value between
 * then the specified boundaries form the obtained frequent itemsets
 * 
 * Reference:
 * <p>
 * Mohammed J Zaki and Wagner Meira Jr.</br>
 * Data mining and analysis: fundamental concepts and algorithms</br>
 * Cambridge University Press, 2014</br>
 * </p>
 * 
 * @author Frederic Sautter
 *
 */
@Reference(authors = "Mohammed J Zaki and Wagner Meira Jr.", //
title = "Data mining and analysis: fundamental concepts and algorithms", //
booktitle = "Cambridge University Press, 2014")
public class AssociationRuleGeneration extends AbstractAssociationRuleAlgorithm {

  /**
   * Constructor.
   *
   * @param frequentItemAlgo FrequentItemset mining Algorithm
   * @param interestMeasure Interestingness measure
   * @param minmeasure Minimum threshold for interestingness measure
   * @param maxmeasure Maximum threshold for interestingness measure
   */
  public AssociationRuleGeneration(AbstractFrequentItemsetAlgorithm frequentItemAlgo, AbstractInterestingnessMeasure interestMeasure, double minmeasure, double maxmeasure) {
    super(frequentItemAlgo, interestMeasure, minmeasure, maxmeasure);
  }

  /**
   * Constructor
   * 
   * @param frequentItemAlgo FrequentItemset mining Algorithm
   * @param interestMeasure Interestingness measure
   * @param minmeasure Minimum threshold for interestingness measure
   */
  public AssociationRuleGeneration(AbstractFrequentItemsetAlgorithm frequentItemAlgo, AbstractInterestingnessMeasure interestMeasure, double minmeasure) {
    super(frequentItemAlgo, interestMeasure, minmeasure);
  }
  
  
  
  public AssociationRuleResult run(Database database, final Relation<BitVector> relation) {
    
    // Total number of transactions in transaction database;
    final int totalTransactions = relation.size();
    
    // Run frequent itemset mining
    FrequentItemsetsResult frequentResult = frequentItemAlgo.run(database);
    
    // Generate HashMap of resulting ArrayList with items in reverse order
    LinkedHashMap<Itemset, Itemset> frequentItems = frequentResult.getRevHashMapRep();
    
    // Meta data for item tags
    VectorFieldTypeInformation<BitVector> meta = frequentResult.getMeta();
    
    // Output List
    List<AssociationRule> rules = new ArrayList<AssociationRule>();
    
    // Iterate through all frequent itemsets
    for(Itemset itemset : frequentItems.keySet()) {
      int[] itemsetSparse = itemset.toSparseRep();
      
      // stops if OneItemsets are reached
      if (itemsetSparse.length > 1) {
        List<Itemset> subsets = new ArrayList<Itemset>();
        boolean[] pruning = new boolean[(1 << itemsetSparse.length) - 2];
        calcSubsets(itemsetSparse, subsets, pruning, frequentItems);
      
        // Iterate through subsets
        for(int i = 0; i < pruning.length; i++) {
          if (pruning[i] == true) {
            Itemset antecedent = subsets.get(i);
            Itemset consequent = subsets.get(subsets.size() - i - 1);
            double measure = this.interestMeasure.measure(totalTransactions, antecedent.getSupport(), consequent.getSupport(), itemset.getSupport());
            
            if(measure >= minmeasure && measure <= this.maxmeasure) {
              rules.add(new AssociationRule(itemset, consequent, antecedent.getSupport(), measure));
            } else {
              if (this.interestMeasure instanceof Confidence) {
                prune(antecedent,pruning, subsets);
              }
            }
          }
        }
      } else {
        break;
      }
    }
    return new AssociationRuleResult("AssociationRule-Generation", "arule-generation", rules, meta);
  }
  
  /**
   * Fills a list with all subsets of a given itemset.
   * 
   * @author Frederic Sautter
   *
   * @param itemset Sparse representation of an itemset.
   * @param subsets List to fill with subsets
   * @param pruning Array for pruning
   * @param map HashMap where the itemsets are stored
   */
  private static void calcSubsets(int[] itemset, List<Itemset> subsets, boolean[] pruning, HashMap<Itemset, Itemset> map) {
    long creator = (1 << itemset.length) - 2;
    int index = 0;
    while(creator > 0) {
      int[] subset = bitToSparse(creator, itemset);
      if (subset.length == 1) {
        subsets.add(map.get(new OneItemset(subset[0])));
      } else {
        subsets.add(map.get(new SparseItemset(subset)));
      }
      pruning[index] = true;
      index++;
      creator--;
    }
  }
  
  /**
   * Returns the Subset of a set in a SparseItemset representation
   * 
   * @param bitset Subset of set in a bitset representation
   * @param set Itemset
   * 
   * @return itemset in SparseItemset representation.
   */
  private static int[] bitToSparse(long bitset, int[] set) {
    int[] subset = new int[BitsUtil.cardinality(bitset)];
    int index = 0;
    for(int i = 0; i < set.length; i++) {
      if((1 << i & bitset) != 0) {
        subset[index] = set[i];
        index++;
      }
    }
    return subset;
  }
  
  /**
   * Prunes the subset list according to the confidence monotonicity
   * 
   * @param superset Itemset, whose subsets are pruned
   * @param prining Array of bools for pruning
   * @param list subset list
   */
  private static void prune(Itemset superset, boolean[] pruning, List<Itemset> subsets) {
    int[] itemset = superset.toSparseRep();
    long creator = (1 << itemset.length) - 2;
    int index;
    while(creator > 0) {
      index = subsets.indexOf(new SparseItemset(bitToSparse(creator, itemset)));
      pruning[index] = false;
      creator--;
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.BIT_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    // TODO Auto-generated method stub
    return null;
  }
  
  /**
   * Parameterization class.
   *
   * @author Frederic Sautter
   */
  public static class Parameterizer extends AbstractAssociationRuleAlgorithm.Parameterizer {
    @Override
    protected AssociationRuleGeneration makeInstance() {
      return new AssociationRuleGeneration(frequentItemAlgo, interestMeasure, minmeasure, maxmeasure);
    }
  }

}
