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
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * A collection of inspection-related utility functions.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class ELKIServiceScanner {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(ELKIServiceScanner.class);

  /**
   * Class loader
   */
  private static final URLClassLoader CLASSLOADER = (URLClassLoader) ClassLoader.getSystemClassLoader();

  /**
   * Factory class postfix.
   */
  public static final String FACTORY_POSTFIX = "$Factory";

  /**
   * (Non-weak) cache for all "frequently scanned" classes.
   */
  private static List<Class<?>> MASTER_CACHE = null;

  /**
   * Static methods only.
   */
  private ELKIServiceScanner() {
    // Do not automatically scan.
  }

  /**
   * Load classes via linear scanning.
   *
   * @param restrictionClass Class to find subclasses for.
   */
  public static void load(Class<?> restrictionClass) {
    if(MASTER_CACHE == null) {
      initialize();
    }
    if(MASTER_CACHE.size() == 0) {
      return;
    }
    Iterator<Class<?>> iter = MASTER_CACHE.iterator();
    while(iter.hasNext()) {
      Class<?> clazz = iter.next();
      // Skip other classes.
      if(!restrictionClass.isAssignableFrom(clazz)) {
        continue;
      }
      // skip abstract / private classes.
      if(Modifier.isInterface(clazz.getModifiers()) || Modifier.isAbstract(clazz.getModifiers()) || Modifier.isPrivate(clazz.getModifiers())) {
        continue;
      }
      boolean instantiable = false;
      try {
        instantiable = clazz.getConstructor() != null;
      }
      catch(Exception | Error e) {
        // ignore
      }
      try {
        instantiable = instantiable || ClassGenericsUtil.getParameterizer(clazz) != null;
      }
      catch(Exception | Error e) {
        // ignore
      }
      if(!instantiable) {
        continue;
      }
      ELKIServiceRegistry.register(restrictionClass, clazz);
    }
  }

  /**
   * Get a list with all classes in the working folder (not including jars!)
   *
   * @return Classes.
   */
  public static Iterator<Class<?>> nonindexedClasses() {
    if(MASTER_CACHE == null) {
      initialize();
    }
    if(MASTER_CACHE.size() == 0) {
      return Collections.emptyIterator();
    }
    return MASTER_CACHE.iterator();
  }

  /**
   * Perform a full (slow) scan for classes.
   */
  private synchronized static void initialize() {
    if(MASTER_CACHE != null) {
      return;
    }
    Enumeration<URL> cps;
    try {
      cps = CLASSLOADER.getResources("");
    }
    catch(IOException e) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
      return;
    }
    List<Class<?>> res = new ArrayList<>();
    while(cps.hasMoreElements()) {
      URL u = cps.nextElement();
      // Scan file sources only.
      if(!"file".equals(u.getProtocol())) {
        continue;
      }
      File path;
      try {
        path = new File(u.toURI());
      }
      catch(URISyntaxException e) {
        LOG.warning("Incorrect classpath entry: " + u);
        continue;
      }
      Iterator<String> it = new DirClassIterator(path);
      while(it.hasNext()) {
        String classname = it.next();
        try {
          Class<?> cls = CLASSLOADER.loadClass(classname);
          // skip classes where we can't get a full name.
          if(cls.getCanonicalName() == null) {
            continue;
          }
          res.add(cls);
        }
        catch(Exception | Error e) {
          continue;
        }
      }
    }
    MASTER_CACHE = Collections.unmodifiableList(res);
    if(LOG.isDebuggingFinest() && MASTER_CACHE.size() > 0) {
      LOG.debugFinest("Classes found by scanning the development classpath: " + MASTER_CACHE.size());
    }
  }

  /**
   * Class to iterate over a directory tree.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  static class DirClassIterator implements Iterator<String> {
    private static final String CLASS_EXT = ".class";

    private static final String FACTORY_FILE_EXT = FACTORY_POSTFIX + CLASS_EXT;

    private static final int CLASS_EXT_LENGTH = CLASS_EXT.length();

    private String prefix;

    private ArrayList<String> files = new ArrayList<>(100);

    private ArrayList<File> folders = new ArrayList<>(100);

    /**
     * Constructor from Directory
     *
     * @param path Directory to iterate over
     */
    public DirClassIterator(File path) {
      this.prefix = path.getAbsolutePath();
      if(prefix.charAt(prefix.length() - 1) != File.separatorChar) {
        prefix = prefix + File.separatorChar;
      }

      this.folders.add(path);
    }

    @Override
    public boolean hasNext() {
      if(files.size() == 0) {
        findNext();
      }
      return (files.size() > 0);
    }

    /**
     * Find the next entry, since we need to skip some directories.
     */
    private void findNext() {
      while(folders.size() > 0) {
        File path = folders.remove(folders.size() - 1);
        // recurse into directories
        if(path.isDirectory()) {
          for(String localname : path.list()) {
            // Ignore unix-hidden files/dirs
            if(localname.charAt(0) == '.') {
              continue;
            }
            // Classes
            if(localname.endsWith(CLASS_EXT)) {
              if(localname.indexOf('$') >= 0) {
                if(!localname.endsWith(FACTORY_FILE_EXT)) {
                  continue;
                }
              }
              final String fullname = new File(path, localname).toString();
              files.add(fullname.substring(prefix.length(), fullname.length() - CLASS_EXT_LENGTH).replace(File.separatorChar, '.'));
              continue;
            }
            // Recurse into directories
            File newf = new File(path, localname);
            if(newf.isDirectory()) {
              folders.add(newf);
            }
          }
        }
      }
    }

    @Override
    public String next() {
      if(files.size() == 0) {
        findNext();
      }
      if(files.size() > 0) {
        return files.remove(files.size() - 1);
      }
      return null;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Sort classes by their class name. Package first, then class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class ClassSorter implements Comparator<Class<?>> {
    @Override
    public int compare(Class<?> o1, Class<?> o2) {
      Package p1 = o1.getPackage();
      Package p2 = o2.getPackage();
      if(p1 == null) {
        return -1;
      }
      if(p2 == null) {
        return 1;
      }
      int pkgcmp = p1.getName().compareTo(p2.getName());
      if(pkgcmp != 0) {
        return pkgcmp;
      }
      return o1.getCanonicalName().compareTo(o2.getCanonicalName());
    }
  }
}
