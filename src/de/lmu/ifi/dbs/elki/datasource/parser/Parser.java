package de.lmu.ifi.dbs.elki.datasource.parser;

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

import java.io.InputStream;

import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtilFrequentlyScanned;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * A Parser shall provide a ParsingResult by parsing an InputStream.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.landmark
 * @apiviz.uses InputStream
 * @apiviz.uses MultipleObjectsBundle oneway - - «create»
 */
public interface Parser extends Parameterizable, InspectionUtilFrequentlyScanned {
  /**
   * Returns a list of the objects parsed from the specified input stream.
   * 
   * @param in the stream to parse objects from
   * @return a list containing those objects parsed from the input stream
   */
  MultipleObjectsBundle parse(InputStream in);
}
