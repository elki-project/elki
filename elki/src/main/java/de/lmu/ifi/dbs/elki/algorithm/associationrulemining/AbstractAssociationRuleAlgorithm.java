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

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.associationrulemining.interestingnessmeasure.AbstractInterestingnessMeasure;
import de.lmu.ifi.dbs.elki.algorithm.associationrulemining.interestingnessmeasure.Confidence;
import de.lmu.ifi.dbs.elki.algorithm.itemsetmining.AbstractFrequentItemsetAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.itemsetmining.FPGrowth;
import de.lmu.ifi.dbs.elki.result.AssociationRuleResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for association rule mining.
 * 
 * @author Frederic Sautter
 *
 */
public abstract class AbstractAssociationRuleAlgorithm extends AbstractAlgorithm<AssociationRuleResult> {
  
  /**
   * Frequent Itemset Algorithm to be used.
   */
  protected AbstractFrequentItemsetAlgorithm frequentItemAlgo;
  
  /**
   * Interestingness measure to be used.
   */
  protected AbstractInterestingnessMeasure interestMeasure;
  
  /**
   * Parameter for minimum interestingness measure.
   */
  protected double minmeasure = Double.MAX_VALUE ;
  
  /**
   * Parameter for maximum interestingness measure.
   */
  protected double maxmeasure = Double.MIN_VALUE;
  
  
  /**
   * Constructor.
   *
   * @param frequentItemAlgo FrequentItemset mining Algorithm
   * @param interestMeasure Interestingness measure
   * @param minmeasure Minimum threshold for interestingness measure
   * @param maxmeasure Maximum threshold for interestingness measure
   */
  public AbstractAssociationRuleAlgorithm(AbstractFrequentItemsetAlgorithm frequentItemAlgo, AbstractInterestingnessMeasure interestMeasure, double minmeasure, double maxmeasure) {
    super();
    this.frequentItemAlgo = frequentItemAlgo;
    this.interestMeasure = interestMeasure;
    this.minmeasure = minmeasure;
    this.maxmeasure = maxmeasure;
  }
  
  /**
   * Constructor
   * 
   * @param frequentItemAlgo FrequentItemset mining Algorithm
   * @param interestMeasure Interestingness measure
   * @param minmeasure Minimum threshold for interestingness measure
   */
  public AbstractAssociationRuleAlgorithm(AbstractFrequentItemsetAlgorithm frequentItemAlgo, AbstractInterestingnessMeasure interestMeasure, double minmeasure) {
    this(frequentItemAlgo, interestMeasure, minmeasure, Double.MIN_VALUE);
  }
  
  
  
  public abstract static class Parameterizer extends AbstractParameterizer {
  
    /**
     * Parameter to specify the frequentItemsetAlgorithm to be used.
     */
    public static final OptionID FREQUENTITEMALGO_ID = new OptionID("associationrulemining.frequentItemAlgo", //
        "Algorithm to be used for frequent itemset mining.");
    
    /**
     * Parameter to specify the interestingness measure to be used.
     */
    public static final OptionID INTERESTMEASURE_ID = new OptionID("associationrulemining.interestMeasure", //
        "Interestingness measure to be used");
    
    /**
     * Parameter to specify the minimum threshold for the interestingness measure.
     */
    public static final OptionID MINMEASURE_ID = new OptionID("associationrulemining.minmeasure", //
        "Minimum threshold for specified interstingness measure");
    
    /**
     * Parameter to specify the maximum threshold for the interestingness measure.
     */
    public static final OptionID MAXMEASURE_ID = new OptionID("associationrulemining.maxmeasure", //
        "Maximum threshold for specified interstingness measure");
    
    
    /**
     * Parameter for frequent itemset mining.
     */
    protected AbstractFrequentItemsetAlgorithm frequentItemAlgo;
    
    /**
     * Parameter for interestingness measure.
     */
    protected AbstractInterestingnessMeasure interestMeasure;
    
    /**
     * Parameter for minimum interestingness measure.
     */
    protected double minmeasure;
    
    /**
     * Parameter for maximum interestingness measure.
     */
    protected double maxmeasure;
    
    
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<AbstractFrequentItemsetAlgorithm> frequentItemAlgoP = new ObjectParameter<>(FREQUENTITEMALGO_ID, AbstractFrequentItemsetAlgorithm.class, FPGrowth.class);
      if(config.grab(frequentItemAlgoP)) {
        frequentItemAlgo = frequentItemAlgoP.instantiateClass(config);
      }
      ObjectParameter<AbstractInterestingnessMeasure> interestMeasureP = new ObjectParameter<>(INTERESTMEASURE_ID, AbstractInterestingnessMeasure.class, Confidence.class);
      if(config.grab(interestMeasureP)) {
        interestMeasure = interestMeasureP.instantiateClass(config);
      }
      DoubleParameter minmeasureP = new DoubleParameter(MINMEASURE_ID);
      if (config.grab(minmeasureP)) {
        minmeasure = minmeasureP.getValue();
      }
      DoubleParameter maxmeasureP = new DoubleParameter(MAXMEASURE_ID).setOptional(true);
      if (config.grab(maxmeasureP)) {
        maxmeasure = maxmeasureP.getValue();
      }
    }
  }
}
