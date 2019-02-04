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
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
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
 * @since 0.2
 */
public class ELKIServiceScanner {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(ELKIServiceScanner.class);

  /**
   * Class loader
   */
  private static final ClassLoader CLASSLOADER = ELKIServiceScanner.class.getClassLoader();

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
    if(MASTER_CACHE.isEmpty()) {
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
    if(MASTER_CACHE.isEmpty()) {
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
    try {
      Enumeration<URL> cps = CLASSLOADER.getResources("");
      List<Class<?>> res = new ArrayList<>();
      while(cps.hasMoreElements()) {
        URL u = cps.nextElement();
        // Scan file sources only.
        if(!"file".equals(u.getProtocol())) {
          continue;
        }
        try {
          Iterator<String> it = new DirClassIterator(new File(u.toURI()));
          while(it.hasNext()) {
            try {
              Class<?> cls = CLASSLOADER.loadClass(it.next());
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
        catch(URISyntaxException e) {
          LOG.warning("Incorrect classpath entry: " + u);
          continue;
        }
      }
      MASTER_CACHE = Collections.unmodifiableList(res);
      if(LOG.isDebuggingFinest()) {
        LOG.debugFinest("Classes found by scanning the development classpath: " + MASTER_CACHE.size());
      }
    }
    catch(IOException e) {
      LOG.exception(e);
      return;
    }
  }

  /**
   * Class to iterate over a directory tree.
   *
   * @author Erich Schubert
   */
  private static class DirClassIterator implements Iterator<String> {
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
      if(files.isEmpty()) {
        findNext();
      }
      return !files.isEmpty();
    }

    /**
     * Find the next entry, since we need to skip some directories.
     */
    private void findNext() {
      while(!folders.isEmpty()) {
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
              if(localname.indexOf('$') >= 0 && !localname.endsWith(FACTORY_FILE_EXT)) {
                continue;
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
      if(files.isEmpty()) {
        findNext();
      }
      return !files.isEmpty() ? files.remove(files.size() - 1) : null;
    }
  }

  /**
   * Sort classes by their class name. Package first, then class.
   */
  public static Comparator<Class<?>> SORT_BY_NAME = new Comparator<Class<?>>() {
    @Override
    public int compare(Class<?> o1, Class<?> o2) {
      return comparePackageClass(o1, o2);
    }
  };

  /**
   * Compare two classes, by package name first.
   *
   * @param o1 First class
   * @param o2 Second class
   * @return Comparison result
   */
  private static int comparePackageClass(Class<?> o1, Class<?> o2) {
    return o1.getPackage() == o2.getPackage() ? //
        o1.getCanonicalName().compareTo(o2.getCanonicalName()) //
        : o1.getPackage() == null ? -1 : o2.getPackage() == null ? +1 //
            : o1.getPackage().getName().compareTo(o2.getPackage().getName());
  }

  /**
   * Get the priority of a class, or its outer class.
   *
   * @param o1 Class
   * @return Priority
   */
  private static int classPriority(Class<?> o1) {
    Priority p = o1.getAnnotation(Priority.class);
    if(p == null) {
      Class<?> pa = o1.getDeclaringClass();
      p = (pa != null) ? pa.getAnnotation(Priority.class) : null;
    }
    return p != null ? p.value() : Priority.DEFAULT;
  }

  /**
   * Comparator to sort classes by priority, then alphabetic.
   */
  public static final Comparator<Class<?>> SORT_BY_PRIORITY = new Comparator<Class<?>>() {
    @Override
    public int compare(Class<?> o1, Class<?> o2) {
      int c = Integer.compare(classPriority(o2), classPriority(o1));
      c = c != 0 ? c : comparePackageClass(o1, o2);
      return c != 0 ? c : o1.getCanonicalName().compareTo(o2.getCanonicalName());
    }
  };
}
