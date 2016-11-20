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

import de.lmu.ifi.dbs.elki.algorithm.itemsetmining.Itemset;
import de.lmu.ifi.dbs.elki.algorithm.itemsetmining.OneItemset;
import de.lmu.ifi.dbs.elki.algorithm.itemsetmining.SparseItemset;
import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;

/**
 * Association Rule
 * 
 * @author Frederic Sautter
 *
 */
public class AssociationRule implements Comparable<AssociationRule> {
  
  /**
   * Consequent itemset
   */
  private Itemset consequent;
  
  /**
   * Union of consequent and consequent
   */
  private Itemset union;
  
  /**
   * Support of the antecedent
   */
  private int anteSupport;
  
  /**
   * Measure of the Rule.
   */
  private double measure;

  /**
   * Constructor.
   *
   * @param union Union of consequent and consequent
   * @param consequent Consequent of the rule
   * @param anteSupport Support of the antecedent
   * @param measure Value of the interest measure
   */
  public AssociationRule(Itemset union, Itemset consequent, int anteSupport, double measure) {
    // TODO Auto-generated constructor stub
    this.union = union;
    this.consequent = consequent;
    this.anteSupport = anteSupport;
    this.measure = measure;
  }
  
  
  
  /**
   * Get the consequent of the rule.
   * 
   * @return consequent
   */
  public Itemset getConsequent() {
    return this.consequent;
  }
  
  /**
   * Get the union of consequent and consequent of the rule.
   * 
   * @return union
   */
  public Itemset getUnion() {
    return this.union;
  }
  
  /**
   * Get the antecedent support.
   * 
   * @return confidence
   */
  public int getAnteSupport() {
    return this.anteSupport;
  }
  
  /**
   * Get the rule measure.
   * 
   * @return measure
   */
  public double getMeasure() {
    return this.measure;
  }
  
  /**
   * Get the antecedent of the rule.
   * 
   * @return antecedent
   */
  public Itemset getAntecedent() {
    int[] itemset = union.toSparseRep();
    int[] con = consequent.toSparseRep();
    int[] antecedent = new int[itemset.length - con.length];
    int i = 0;
    int index = 0;
    for(int item : itemset) {
      if(item != con[i]) {
          antecedent[index] = item;
          index++;
      } else if(i != con.length - 1) {
          i++;
      }
    }
    if(antecedent.length == 1) {
      return new OneItemset(antecedent[0], anteSupport);
    } else {
      return new SparseItemset(antecedent, anteSupport);
    }
  }

  @Override
  public int compareTo(AssociationRule o) {
    // TODO Auto-generated method stub
    return this.union.compareTo(o.union);
  }
  
  /**
   * Append to a string buffer.
   * 
   * @param buf Buffer
   * @param meta Relation metadata (for labels)
   * @return String buffer for chaining.
   */
  public StringBuilder appendTo(StringBuilder buf, VectorFieldTypeInformation<BitVector> meta){
    this.getAntecedent().appendTo(buf, meta);
    buf.append(" --> ");
    this.consequent.appendTo(buf, meta);
    buf.append(" : ");
    buf.append(this.measure);
    return buf;
  }

}
