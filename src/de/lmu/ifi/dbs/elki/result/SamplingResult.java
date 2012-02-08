package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
 * Wrapper for storing the current database sample.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf DBIDs
 */
public class SamplingResult implements Result {
  /**
   * The actual selection
   */
  DBIDs sample = null;

  /**
   * Constructor.
   */
  public SamplingResult() {
    super();
  }

  /**
   * @return the current sample
   */
  public DBIDs getSample() {
    return sample;
  }

  /**
   * Note: trigger a resultchanged event!
   * 
   * @param sample the new sample
   */
  public void setSample(DBIDs sample) {
    this.sample = sample;
  }

  @Override
  public String getLongName() {
    return "Sample";
  }

  @Override
  public String getShortName() {
    return "sample";
  }
}