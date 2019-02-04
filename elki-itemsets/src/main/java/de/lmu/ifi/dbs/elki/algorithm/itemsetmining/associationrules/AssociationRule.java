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
package de.lmu.ifi.dbs.elki.algorithm.itemsetmining.associationrules;

import de.lmu.ifi.dbs.elki.algorithm.itemsetmining.Itemset;
import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;

/**
 * Association Rule
 * 
 * @author Frederic Sautter
 * @since 0.7.5
 */
public class AssociationRule implements Comparable<AssociationRule> {
  /**
   * Consequent itemset
   */
  private Itemset consequent;

  /**
   * Consequent itemset
   */
  private Itemset antecedent;

  /**
   * Union of consequent and consequent
   */
  private Itemset union;

  /**
   * Measure of the Rule.
   */
  private double measure;

  /**
   * Constructor.
   *
   * @param union Union of consequent and consequent
   * @param consequent Consequent of the rule
   * @param antecedent Antecedent of the rule
   * @param measure Value of the interest measure
   */
  public AssociationRule(Itemset union, Itemset consequent, Itemset antecedent, double measure) {
    super();
    this.union = union;
    this.consequent = consequent;
    this.antecedent = antecedent;
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
    return this.antecedent;
  }

  @Override
  public int compareTo(AssociationRule o) {
    return this.union.compareTo(o.union);
  }
  
  @Override
  public String toString() {
    return appendTo(new StringBuilder(), null).toString();
  }

  /**
   * Append to a string buffer.
   * 
   * @param buf Buffer
   * @param meta Relation metadata (for labels)
   * @return String buffer for chaining.
   */
  public StringBuilder appendTo(StringBuilder buf, VectorFieldTypeInformation<BitVector> meta) {
    this.antecedent.appendTo(buf, meta);
    buf.append(" --> ");
    this.consequent.appendItemsTo(buf, meta);
    buf.append(": ");
    buf.append(union.getSupport());
    buf.append(" : ");
    buf.append(this.measure);
    return buf;
  }
}
