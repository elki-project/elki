/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2026
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

import elki.result.textwriter.TextWriteable;
import elki.result.textwriter.TextWriterStream;
import elki.sequencemining.GSP.Sequence;

/**
 * Result class for frequent subsequence mining algorithms.
 *
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @has - - - Sequence
 */
public class FrequentSubsequencesResult implements TextWriteable {
  /**
   * Frequent subsequences.
   */
  private final List<Sequence> subsequences;

  /**
   * Total number of transactions.
   */
  private final int total;

  /**
   * Constructor.
   *
   * @param subsequences Frequent subsequences
   * @param total Number of transactions
   */
  public FrequentSubsequencesResult(List<Sequence> subsequences, int total) {
    this.subsequences = subsequences;
    this.total = total;
  }

  /**
   * Get frequent subsequences.
   *
   * @return Frequent subsequences
   */
  public List<Sequence> getSubsequences() {
    return subsequences;
  }

  /**
   * Get total number of transactions.
   *
   * @return Total number of transactions
   */
  public int getTotal() {
    return total;
  }

  @Override
  public void writeToText(TextWriterStream out, String label) {
    StringBuilder buf = new StringBuilder();
    for(Sequence seq : subsequences) {
      buf.setLength(0);
      buf.append(seq).append(" #SUP: ").append(seq.getSupport());
      out.inlinePrintNoQuotes(buf);
      out.flush();
    }
  }
}
