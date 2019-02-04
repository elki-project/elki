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
package de.lmu.ifi.dbs.elki.data.model;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Base interface for Model classes.
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @opt nodefillcolor LemonChiffon
 */
public interface Model {
  /**
   * Type information, for relation selection.
   */
  SimpleTypeInformation<Model> TYPE = new SimpleTypeInformation<>(Model.class);

  /**
   * Default implementation of
   * {@link de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable#writeToText}.
   * 
   * Note: we deliberately do not implement TextWriteable!
   * 
   * @param out Output steam
   * @param label Optional label to prefix
   */
  // actually @Override, for TextWriteable.
  default void writeToText(TextWriterStream out, String label) {
    if(label != null) {
      out.commentPrintLn(label);
    }
    out.commentPrintLn("Model class: " + getClass().getName());
  }
}