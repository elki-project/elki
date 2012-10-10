package de.lmu.ifi.dbs.elki.utilities.documentation;

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
 * Utilities for extracting documentation from class annotations.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.utilities.documentation.Title
 * @apiviz.uses de.lmu.ifi.dbs.elki.utilities.documentation.Description
 * @apiviz.uses de.lmu.ifi.dbs.elki.utilities.documentation.Reference
 */
public final class DocumentationUtil {
  /**
   * Get a useful title from a class, either by reading the
   * "title" annotation, or by using the class name.
   * 
   * @param c Class
   * @return title
   */
  public static String getTitle(Class<?> c) {
    Title title = c.getAnnotation(Title.class);
    if(title != null && title.value().length() > 0) {
      return title.value();
    }
    return c.getSimpleName();
  }
  
  /**
   * Get a class description if defined, an empty string otherwise.
   * 
   * @param c Class
   * @return description or the emtpy string
   */
  public static String getDescription(Class<?> c) {
    Description desc = c.getAnnotation(Description.class);
    if (desc != null) {
      return desc.value();
    }
    return "";
  }
  
  /**
   * Get the reference annotation of a class, or {@code null}.
   * 
   * @param c Class
   * @return Reference or {@code null}
   */
  public static Reference getReference(Class<?> c) {
    Reference ref = c.getAnnotation(Reference.class);
    return ref;
  }
}