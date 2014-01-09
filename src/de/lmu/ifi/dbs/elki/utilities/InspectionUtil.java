package de.lmu.ifi.dbs.elki.utilities;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import gnu.trove.set.hash.THashSet;

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
import java.util.WeakHashMap;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * A collection of inspection-related utility functions.
 * 
 * @author Erich Schubert
 */
public class InspectionUtil {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(InspectionUtil.class);

  /**
   * Class loader
   */
  private static final ClassLoader CLASSLOADER = ClassLoader.getSystemClassLoader();

  /**
   * Default package ignores.
   */
  private static final String[] DEFAULT_IGNORES = {
      // Sun Java
  "java.", "com.sun.",
      // Batik classes
  "org.apache.",
      // W3C / SVG / XML classes
  "org.w3c.", "org.xml.", "javax.xml.",
      // JUnit
  "org.junit.", "junit.", "org.hamcrest.",
      // Eclipse
  "org.eclipse.",
      // ApiViz
  "org.jboss.apiviz.",
      // JabRef
  "spin.", "osxadapter.", "antlr.", "ca.odell.", "com.jgoodies.", "com.michaelbaranov.", "com.mysql.", "gnu.dtools.", "net.sf.ext.", "net.sf.jabref.", "org.antlr.", "org.gjt.", "org.java.plugin.", "org.jempbox.", "org.pdfbox.", "wsi.ra.",
      // GNU trove
  "gnu.trove.",
      // Java OpenGL
  "jogamp.", "com.jogamp.", "javax.media.", "jogl.util."
  //
  };

  /**
   * If we have a non-static classpath, we do more extensive scanning for user
   * extensions.
   */
  public static final boolean NONSTATIC_CLASSPATH;

  /**
   * Factory class postfix.
   */
  public static final String FACTORY_POSTFIX = "$Factory";

  // Check for non-jar entries in classpath.
  static {
    String[] classpath = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
    boolean hasnonstatic = false;
    for (String path : classpath) {
      if (!path.endsWith(".jar")) {
        hasnonstatic = true;
      }
    }
    NONSTATIC_CLASSPATH = hasnonstatic;
  }

  /**
   * Weak hash map for class lookups
   */
  private static WeakHashMap<Class<?>, List<Class<?>>> CLASS_CACHE = new WeakHashMap<>();

  /**
   * (Non-weak) cache for all "frequently scanned" classes.
   */
  private static List<Class<?>> MASTER_CACHE = null;

  /**
   * Cached version of "findAllImplementations". For Parameterizable classes
   * only!
   * 
   * @param c Class to scan for
   * @return Found implementations
   */
  public static List<Class<?>> cachedFindAllImplementations(Class<?> c) {
    if (c == null) {
      return Collections.emptyList();
    }
    List<Class<?>> res = CLASS_CACHE.get(c);
    if (res == null) {
      res = findAllImplementations(c, false);
      CLASS_CACHE.put(c, res);
    }
    return res;
  }

  /**
   * Find all implementations of a given class in the classpath.
   * 
   * Note: returned classes may be abstract.
   * 
   * @param c Class restriction
   * @param everything include interfaces, abstract and private classes
   * @return List of found classes.
   */
  public static List<Class<?>> findAllImplementations(Class<?> c, boolean everything) {
    ArrayList<Class<?>> list = new ArrayList<>();
    // Add all from service files (i.e. jars)
    {
      Iterator<Class<?>> iter = new ELKIServiceLoader(c);
      while (iter.hasNext()) {
        list.add(iter.next());
      }
    }
    if (!InspectionUtil.NONSTATIC_CLASSPATH) {
      if (list.size() == 0) {
        LOG.warning("No implementations for " + c.getName() + " were found using index files.");
      }
    } else {
      // Duplicate checking
      THashSet<Class<?>> dupes = new THashSet<>(list);
      // Build cache on first use:
      if (MASTER_CACHE == null) {
        MASTER_CACHE = slowScan();
      }
      Iterator<Class<?>> iter = MASTER_CACHE.iterator();
      while (iter.hasNext()) {
        Class<?> cls = iter.next();
        // skip abstract / private classes.
        if (!everything && (Modifier.isInterface(cls.getModifiers()) || Modifier.isAbstract(cls.getModifiers()) || Modifier.isPrivate(cls.getModifiers()))) {
          continue;
        }
        if (c.isAssignableFrom(cls) && !dupes.contains(cls)) {
          list.add(cls);
          dupes.add(cls);
        }
      }
    }
    return list;
  }

  /**
   * Find an implementation of the given interface / super class, given a
   * relative class name or alias name.
   * 
   * @param restrictionClass Restriction class
   * @param value Class name, relative class name, or nickname.
   * @return Class found or {@code null}
   */
  @SuppressWarnings("unchecked")
  public static <C> Class<? extends C> findImplementation(Class<? super C> restrictionClass, String value) {
    // Try exact class factory first.
    try {
      return (Class<? extends C>) CLASSLOADER.loadClass(value + FACTORY_POSTFIX);
    } catch (ClassNotFoundException e) {
      // Ignore, retry
    }
    try {
      return (Class<? extends C>) CLASSLOADER.loadClass(value);
    } catch (ClassNotFoundException e) {
      // Ignore, retry
    }
    final String completedName = restrictionClass.getPackage().getName() + "." + value;
    // Try factory for guessed name next
    try {
      return (Class<? extends C>) CLASSLOADER.loadClass(completedName + FACTORY_POSTFIX);
    } catch (ClassNotFoundException e) {
      // Ignore, retry
    }
    // Last try: guessed name prefix only
    try {
      return (Class<? extends C>) CLASSLOADER.loadClass(completedName);
    } catch (ClassNotFoundException e) {
      // Ignore, retry
    }
    // Try aliases:
    for (Class<?> c : InspectionUtil.cachedFindAllImplementations(restrictionClass)) {
      if (c.isAnnotationPresent(Alias.class)) {
        Alias aliases = c.getAnnotation(Alias.class);
        for (String alias : aliases.value()) {
          if (alias.equalsIgnoreCase(value) || alias.equalsIgnoreCase(completedName)) {
            return (Class<? extends C>) c;
          }
        }
      }
    }
    return null;
  }

  /**
   * Perform a full (slow) scan for classes.
   * 
   * @return List with the scan result
   */
  private static List<Class<?>> slowScan() {
    ArrayList<Class<?>> res = new ArrayList<>();
    try {
      Enumeration<URL> cps = CLASSLOADER.getResources("");
      while (cps.hasMoreElements()) {
        URL u = cps.nextElement();
        // Scan file sources only.
        if ("file".equals(u.getProtocol())) {
          File path;
          try {
            path = new File(u.toURI());
          } catch (URISyntaxException e) {
            LOG.exception("Error in classpath: " + u, e);
            continue;
          }
          Iterator<String> it = new DirClassIterator(path, DEFAULT_IGNORES);
          while (it.hasNext()) {
            String classname = it.next();
            try {
              Class<?> cls = CLASSLOADER.loadClass(classname);
              // skip classes where we can't get a full name.
              if (cls.getCanonicalName() == null) {
                continue;
              }
              res.add(cls);
            } catch (ClassNotFoundException e) {
              continue;
            } catch (NoClassDefFoundError e) {
              continue;
            } catch (Exception e) {
              continue;
            } catch (Error e) {
              continue;
            }
          }
        }
      }
    } catch (IOException e) {
      LOG.exception(e);
    }
    Collections.sort(res, new ClassSorter());
    return res;
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

    private ArrayList<Pair<File, String>> folders = new ArrayList<>(100);

    private String[] ignorepackages;

    /**
     * Constructor from Directory
     * 
     * @param path Directory to iterate over
     */
    public DirClassIterator(File path, String[] ignorepackages) {
      this.ignorepackages = ignorepackages;
      this.prefix = path.getAbsolutePath();
      if (prefix.charAt(prefix.length() - 1) != File.separatorChar) {
        prefix = prefix + File.separatorChar;
      }

      this.folders.add(new Pair<>(path, ""));
    }

    @Override
    public boolean hasNext() {
      if (files.size() == 0) {
        findNext();
      }
      return (files.size() > 0);
    }

    /**
     * Find the next entry, since we need to skip some directories.
     */
    private void findNext() {
      while (folders.size() > 0) {
        Pair<File, String> pair = folders.remove(folders.size() - 1);
        // recurse into directories
        if (pair.first.isDirectory()) {
          nextfile: for (String localname : pair.first.list()) {
            // Ignore unix-hidden files/dirs
            if (localname.charAt(0) == '.') {
              continue;
            }
            // Classes
            if (localname.endsWith(CLASS_EXT)) {
              if (localname.indexOf('$') >= 0) {
                if (!localname.endsWith(FACTORY_FILE_EXT)) {
                  continue;
                }
              }
              files.add(pair.second + localname.substring(0, localname.length() - CLASS_EXT_LENGTH));
              continue;
            }
            // Recurse into directories
            File newf = new File(pair.first, localname);
            if (newf.isDirectory()) {
              String newpref = pair.second + localname + '.';
              for (String ignore : ignorepackages) {
                if (ignore.equals(newpref)) {
                  continue nextfile;
                }
              }
              folders.add(new Pair<>(newf, newpref));
            }
          }
        }
      }
    }

    @Override
    public String next() {
      if (files.size() == 0) {
        findNext();
      }
      if (files.size() > 0) {
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
      if (p1 == null) {
        return -1;
      }
      if (p2 == null) {
        return 1;
      }
      int pkgcmp = p1.getName().compareTo(p2.getName());
      if (pkgcmp != 0) {
        return pkgcmp;
      }
      return o1.getCanonicalName().compareTo(o2.getCanonicalName());
    }
  }
}
