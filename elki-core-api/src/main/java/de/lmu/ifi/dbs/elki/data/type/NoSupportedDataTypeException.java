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
package de.lmu.ifi.dbs.elki.data.type;

import java.util.Collection;

/**
 * Exception thrown when no supported data type was found.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @assoc - - - TypeInformation
 */
public class NoSupportedDataTypeException extends IllegalStateException {
  /**
   * Serial version.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Available types
   */
  private Collection<TypeInformation> types = null;

  /**
   * Constructor.
   *
   * @param type Requested type
   * @param types Available types.
   */
  public NoSupportedDataTypeException(TypeInformation type, Collection<TypeInformation> types) {
    super("No data type found satisfying: " + type.toString());
    this.types = types;
  }

  /**
   * Constructor with string message. If possible, use the type parameter
   * instead!
   *
   * @param string Error message
   */
  public NoSupportedDataTypeException(String string) {
    super(string);
  }

  @Override
  public String getMessage() {
    StringBuilder buf = new StringBuilder(super.getMessage());
    if(types != null) {
      buf.append("\nAvailable types:");
      for(TypeInformation type : types) {
        buf.append(' ').append(type.toString());
      }
    }
    return buf.toString();
  }
}
