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
package de.lmu.ifi.dbs.elki.utilities;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;

import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Class that emulates the behavior of an java ServiceLoader, except that the
 * classes are <em>not</em> automatically instantiated. This is more lazy, but
 * also we need to do the instantiations our way with the parameterizable API.
 *
 * @author Erich Schubert
 * @since 0.5.0
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
    load(parent, ELKIServiceLoader.class.getClassLoader());
  }

  /**
   * Load the service file.
   */
  public static void load(Class<?> parent, ClassLoader cl) {
    char[] buf = new char[0x4000];
    try {
      String fullName = RESOURCE_PREFIX + parent.getName();
      Enumeration<URL> configfiles = cl.getResources(fullName);
      while(configfiles.hasMoreElements()) {
        URL nextElement = configfiles.nextElement();
        URLConnection conn = nextElement.openConnection();
        conn.setUseCaches(false);
        try (
            InputStreamReader is = new InputStreamReader(conn.getInputStream(), "UTF-8");) {
          int start = 0, cur = 0, valid = is.read(buf, 0, buf.length);
          char c;
          while(cur < valid) {
            // Find newline or end
            while(cur < valid && (c = buf[cur]) != '\n' && c != '\r') {
              cur++;
            }
            if(cur == valid && is.ready()) {
              // Move consumed buffer contents:
              if(start > 0) {
                System.arraycopy(buf, start, buf, 0, valid - start);
                valid -= start;
                cur -= start;
                start = 0;
              }
              else if(valid == buf.length) {
                throw new IOException("Buffer size exceeded. Maximum line length in service files is: " + buf.length + " in file: " + fullName);
              }
              valid = is.read(buf, valid, buf.length - valid);
              continue;
            }
            parseLine(parent, buf, start, cur);
            while(cur < valid && ((c = buf[cur]) == '\n' || c == '\r')) {
              cur++;
            }
            start = cur;
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
   * @param parent Parent class
   * @param line Line to read
   */
  private static void parseLine(Class<?> parent, char[] line, int begin, int end) {
    while(begin < end && line[begin] == ' ') {
      begin++;
    }
    if(begin >= end || line[begin] == '#') {
      return; // Empty/comment lines are okay, continue
    }
    // Find end of class name:
    int cend = begin + 1;
    while(cend < end && line[cend] != ' ') {
      cend++;
    }
    // Class name:
    String cname = new String(line, begin, cend - begin);
    ELKIServiceRegistry.register(parent, cname);
    for(int abegin = cend + 1, aend = -1; abegin < end; abegin = aend + 1) {
      // Skip whitespace:
      while(abegin < end && line[abegin] == ' ') {
        abegin++;
      }
      // Find next whitespace:
      aend = abegin + 1;
      while(aend < end && line[aend] != ' ') {
        aend++;
      }
      ELKIServiceRegistry.registerAlias(parent, new String(line, abegin, aend - abegin), cname);
    }
    return;
  }
}
