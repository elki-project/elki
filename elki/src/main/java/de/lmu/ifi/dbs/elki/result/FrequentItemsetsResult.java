package de.lmu.ifi.dbs.elki.result;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.algorithm.itemsetmining.Itemset;
import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Result class for Apriori Algorithm.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public class FrequentItemsetsResult extends BasicResult implements TextWriteable {
  /**
   * The supports of all frequent itemsets.
   */
  private List<Itemset> itemsets;

  /**
   * Metadata of the data relation, for item labels.
   */
  private VectorFieldTypeInformation<BitVector> meta;

  /**
   * Constructor.
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param itemsets Frequent itemsets
   * @param meta Metadata
   */
  public FrequentItemsetsResult(String name, String shortname, List<Itemset> itemsets, VectorFieldTypeInformation<BitVector> meta) {
    super(name, shortname);
    this.itemsets = itemsets;
    this.meta = meta;
  }

  /**
   * Returns the frequent item sets.
   * 
   * @return the frequent item sets.
   */
  public List<Itemset> getItemsets() {
    return itemsets;
  }

  @Override
  public void writeToText(TextWriterStream out, String label) {
    for(Itemset itemset : itemsets) {
      out.inlinePrintNoQuotes(itemset.appendTo(new StringBuilder(), meta));
      out.flush();
    }
  }
}
