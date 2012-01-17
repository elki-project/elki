package de.lmu.ifi.dbs.elki.data.type;

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
 * Exception thrown when no supported data type was found.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses TypeInformation oneway - -
 */
public class NoSupportedDataTypeException extends IllegalStateException {
  /**
   * Serial version.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor.
   */
  public NoSupportedDataTypeException(TypeInformation type) {
    super("No data type found satisfying: " + type.toString());
  }

  /**
   * Constructor with string message. If possible, use the type parameter instead!
   *
   * @param string Error message
   */
  public NoSupportedDataTypeException(String string) {
    super(string);
  }
}