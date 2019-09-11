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
package elki.timeseries;

import java.util.ArrayList;
import java.util.List;

import elki.database.ids.DBIDRef;
import elki.result.textwriter.TextWriteable;
import elki.result.textwriter.TextWriterStream;

/**
 * Change point detection result Used by change or trend detection algorithms
 * 
 * TODO: we need access to the data labels / timestamp information!
 *
 * @author Sebastian Rühl
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @has - - - ChangePoint
 */
public class ChangePoints implements TextWriteable {
  /**
   * Change points.
   */
  List<ChangePoint> changepoints = new ArrayList<>();

  /**
   * Result constructor.
   */
  public ChangePoints() {
    super();
  }

  @Override
  public void writeToText(TextWriterStream out, String label) {
    StringBuilder buf = new StringBuilder();
    for(ChangePoint cp : changepoints) {
      buf.setLength(0);
      out.inlinePrintNoQuotes(cp.appendTo(buf));
      out.flush();
    }
  }

  /**
   * Add a change point to the result.
   * 
   * @param iter Time reference
   * @param column Column
   * @param score Score
   */
  public void add(DBIDRef iter, int column, double score) {
    changepoints.add(new ChangePoint(iter, column, score));
  }
}
