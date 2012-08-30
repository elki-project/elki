package de.lmu.ifi.dbs.elki.utilities;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Class that emulates the behavior of an java ServiceLoader, except that the
 * classes are <em>not</em> automatically instantiated. This is more lazy, but
 * also we need to do the instantiations our way with the parameterizable API.
 * 
 * @author Erich Schubert
 */
public class ELKIServiceLoader implements Iterator<Class<?>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ELKIServiceLoader.class);

  /**
   * Prefix for the ELKI functionality discovery.
   */
  public static final String PREFIX = "META-INF/elki/";

  /**
   * Comment character
   */
  public static final char COMMENT_CHAR = '#';

  /**
   * Parent class
   */
  private Class<?> parent;

  /**
   * Classloader
   */
  private ClassLoader cl;

  /**
   * Enumeration of configuration files
   */
  private Enumeration<URL> configfiles;

  /**
   * Current iterator
   */
  private Iterator<Class<?>> curiter = null;

  /**
   * Next class to return
   */
  private Class<?> nextclass;

  /**
   * Constructor.
   * 
   * @param parent Parent class
   * @param cl Classloader to use
   */
  public ELKIServiceLoader(Class<?> parent, ClassLoader cl) {
    this.parent = parent;
    this.cl = cl;
  }

  /**
   * Constructor, using the system class loader.
   * 
   * @param parent Parent class
   */
  public ELKIServiceLoader(Class<?> parent) {
    this(parent, ClassLoader.getSystemClassLoader());
    getServiceFiles(parent);
  }

  /**
   * Get services files for a given class.
   * 
   * @param parent Parent class
   */
  private void getServiceFiles(Class<?> parent) {
    try {
      String fullName = PREFIX + parent.getName();
      configfiles = cl.getResources(fullName);
    }
    catch(IOException x) {
      throw new AbortException("Could not load service configuration files.", x);
    }
  }

  @Override
  public boolean hasNext() {
    if(nextclass != null) {
      return true;
    }
    // Find next iterator
    while((curiter == null) || !curiter.hasNext()) {
      if(!configfiles.hasMoreElements()) {
        return false;
      }
      curiter = parseFile(configfiles.nextElement());
    }
    nextclass = curiter.next();
    return true;
  }

  private Iterator<Class<?>> parseFile(URL nextElement) {
    ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
    try {
      BufferedReader r = new BufferedReader(new InputStreamReader(nextElement.openStream(), "utf-8"));
      while(parseLine(r.readLine(), classes, nextElement)) {
        // Continue
      }
    }
    catch(IOException x) {
      throw new AbortException("Error reading configuration file", x);
    }
    return classes.iterator();
  }

  private boolean parseLine(String line, ArrayList<Class<?>> classes, URL nextElement) throws IOException {
    if(line == null) {
      return false;
    }
    // Ignore comments, trim whitespace
    {
      int begin = 0;
      int end = line.indexOf(COMMENT_CHAR);
      if(end < 0) {
        end = line.length();
      }
      while(begin < end && line.charAt(begin) == ' ') {
        begin++;
      }
      while(end - 1 > begin && line.charAt(end - 1) == ' ') {
        end--;
      }
      if(begin > 0 || end < line.length()) {
        line = line.substring(begin, end);
      }
    }
    if(line.length() <= 0) {
      return true; // Empty/comment lines are okay, continue
    }
    // Try to load the class
    try {
      Class<?> cls = cl.loadClass(line);
      // Should not happen. Check anyway.
      if(cls == null) {
        assert (cls != null);
        return true;
      }
      if(parent.isAssignableFrom(cls)) {
        classes.add(cls);
      }
      else {
        LOG.warning("Class " + line + " does not implement " + parent + " but listed in service file " + nextElement);
      }
    }
    catch(ClassNotFoundException e) {
      LOG.warning("Class not found: " + line + "; listed in service file " + nextElement, e);
    }
    return true;
  }

  @Override
  public Class<?> next() {
    Class<?> ret = nextclass;
    nextclass = null;
    return ret;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}