package de.lmu.ifi.dbs.elki.result;
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

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.associationrulemining.AssociationRule;
import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Result class for association rule mining
 * 
 * @author Frederic Sautter
 *
 */
public class AssociationRuleResult extends BasicResult implements TextWriteable {
  
  /**
   * Association rules together with the interestingness measure.
   */
  private List<AssociationRule> rules;
  
  /**
   * Metadata of the data relation, for item labels.
   */
  private VectorFieldTypeInformation<BitVector> meta;
  
  
  /**
   * 
   * Constructor.
   *
   * @param name
   * @param shortname
   */
  public AssociationRuleResult(String name, String shortname, List<AssociationRule> rules, VectorFieldTypeInformation<BitVector> meta) {
    super(name, shortname);
    this.rules = rules;
    this.meta = meta;
    // TODO Auto-generated constructor stub
  }
  
  
  
  /**
   * Returns the rules.
   * 
   * @return the rules.
   */
  public List<AssociationRule> getRules() {
    return rules;
  }
  
  /**
   * Returns Metadata of the data relation, for item labels.
   * 
   * @return meta
   */
  public VectorFieldTypeInformation<BitVector> getMeta() {
    return this.meta;
  }

  @Override
  public void writeToText(TextWriterStream out, String label) {
    // TODO Auto-generated method stub
    for(AssociationRule rule : rules) {
      out.inlinePrintNoQuotes(rule.appendTo(new StringBuilder(), meta));
      out.flush();
    }
  }

}
