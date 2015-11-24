package de.lmu.ifi.dbs.elki.utilities;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;

import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.BufferedLineReader;

/**
 * Class that emulates the behavior of an java ServiceLoader, except that the
 * classes are <em>not</em> automatically instantiated. This is more lazy, but
 * also we need to do the instantiations our way with the parameterizable API.
 *
 * @author Erich Schubert
 */
public class ELKIServiceLoader {
  /**
   * Resource name prefix for the ELKI functionality discovery.
   *
   * Note: resources are always separated with /, even on Windows.
   */
  public static final String RESOURCE_PREFIX = "META-INF/elki/";

  /**
   * File name prefix for the ELKI functionality discovery.
   */
  public static final String FILENAME_PREFIX = "META-INF" + File.separator + "elki" + File.separator;

  /**
   * Comment character
   */
  public static final char COMMENT_CHAR = '#';

  /**
   * Constructor - do not use.
   */
  private ELKIServiceLoader() {
    // Do not use.
  }

  /**
   * Load the service file.
   */
  public static void load(Class<?> parent) {
    load(parent, ClassLoader.getSystemClassLoader());
  }

  /**
   * Load the service file.
   */
  public static void load(Class<?> parent, ClassLoader cl) {
    try {
      String fullName = RESOURCE_PREFIX + parent.getName();
      Enumeration<URL> configfiles = cl.getResources(fullName);
      while(configfiles.hasMoreElements()) {
        URL nextElement = configfiles.nextElement();
        try (
            InputStreamReader is = new InputStreamReader(nextElement.openStream(), "utf-8");
            BufferedLineReader r = new BufferedLineReader(is)) {
          while(r.nextLine()) {
            parseLine(parent, r.getBuffer(), nextElement);
          }
        }
        catch(IOException x) {
          throw new AbortException("Error reading configuration file", x);
        }
      }
    }
    catch(IOException x) {
      throw new AbortException("Could not load service configuration files.", x);
    }
  }

  /**
   * Parse a single line from a service registry file.
   *
   * @param parent PArent class
   * @param line Line to read
   * @param nam File name for error reporting
   */
  private static void parseLine(Class<?> parent, CharSequence line, URL nam) {
    if(line == null) {
      return;
    }
    int begin = 0, end = line.length();
    while(begin < end && line.charAt(begin) == ' ') {
      begin++;
    }
    if(begin >= end || line.charAt(begin) == '#') {
      return; // Empty/comment lines are okay, continue
    }
    assert(begin == 0 || line.charAt(begin - 1) == ' ');
    // Find end of class name:
    int cend = begin + 1;
    while(cend < end && line.charAt(cend) != ' ') {
      cend++;
    }
    // Class name:
    String cname = line.subSequence(begin, cend).toString();
    ELKIServiceRegistry.register(parent, cname);
    for(int abegin = cend + 1, aend = -1; abegin < end; abegin = aend + 1) {
      // Skip whitespace:
      while(abegin < end && line.charAt(abegin) == ' ') {
        abegin++;
      }
      // Find next whitespace:
      aend = abegin + 1;
      while(aend < end && line.charAt(aend) != ' ') {
        aend++;
      }
      if(abegin < aend) {
        ELKIServiceRegistry.registerAlias(parent, line.subSequence(abegin, aend).toString(), cname);
      }
    }
    return;
  }
}
