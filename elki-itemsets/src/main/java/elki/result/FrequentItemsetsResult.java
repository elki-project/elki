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
package elki.result;

import java.util.List;

import elki.algorithm.itemsetmining.Itemset;
import elki.data.BitVector;
import elki.data.type.VectorFieldTypeInformation;
import elki.result.textwriter.TextWriteable;
import elki.result.textwriter.TextWriterStream;

/**
 * Result class for frequent itemset mining algorithms.
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @has - - - Itemset
 */
public class FrequentItemsetsResult implements TextWriteable {
  /**
   * The supports of all frequent itemsets.
   */
  private List<Itemset> itemsets;

  /**
   * Metadata of the data relation, for item labels.
   */
  private VectorFieldTypeInformation<BitVector> meta;

  /**
   * Total number of transactions.
   */
  private int total;

  /**
   * Constructor.
   *
   * @param itemsets Frequent itemsets (sorted, by size then lexicographically)
   * @param meta Metadata
   * @param total Total number of transactions
   */
  public FrequentItemsetsResult(List<Itemset> itemsets, VectorFieldTypeInformation<BitVector> meta, int total) {
    super();
    this.itemsets = itemsets;
    this.meta = meta;
    this.total = total;
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
    StringBuilder buf = new StringBuilder();
    for(Itemset itemset : itemsets) {
      buf.setLength(0); // Reuse
      out.inlinePrintNoQuotes(itemset.appendTo(buf, meta));
      out.flush();
    }
  }

  /**
   * Get the metadata used for serialization.
   *
   * @return Metadata
   */
  public VectorFieldTypeInformation<BitVector> getMeta() {
    return meta;
  }

  /**
   * Get the total number of transactions.
   *
   * @return Total transactions
   */
  public int getTotal() {
    return total;
  }
}
