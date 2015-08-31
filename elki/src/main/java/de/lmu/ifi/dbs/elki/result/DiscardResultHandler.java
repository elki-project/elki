package de.lmu.ifi.dbs.elki.result;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

/**
 * A dummy result handler that discards the actual result, for use in
 * benchmarks.
 * 
 * @author Erich Schubert
 */
public class DiscardResultHandler implements ResultHandler {
  /**
   * Default constructor.
   */
  public DiscardResultHandler() {
    // empty constructor
  }

  @Override
  public void processNewResult(ResultHierarchy hier, Result newResult) {
    // always ignore the new result.
  }
}