package de.lmu.ifi.dbs.elki.data.images;

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

import java.io.File;
import java.io.IOException;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Interface for color histogram implementations.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses File
 */
public interface ComputeColorHistogram extends Parameterizable {
  /**
   * Compute a color histogram given a file name.
   * 
   * The mask file (which may be null) is expected to use >50% transparent or
   * black to mask pixels, Non-transparent white to keep pixels. Alpha values
   * are not used.
   * 
   * @param file File name
   * @param mask Mask file (optional)
   * @return Color histogram
   * @throws IOException on file read errors.
   */
  public double[] computeColorHistogram(File file, File mask) throws IOException;
}