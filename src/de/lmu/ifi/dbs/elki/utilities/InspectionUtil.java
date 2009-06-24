package de.lmu.ifi.dbs.elki.utilities;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;

/**
 * A collection of inspection-related utility functions.
 * 
 * @author Erich Schubert
 * 
 */
public class InspectionUtil {
  /**
   * Default package ignores.
   */
  private static final String[] DEFAULT_IGNORES = {
  // Sun Java
  "java.", "com.sun.",
  // Batik classes
  "org.apache.",
  // W3C / SVG / XML classes
  "org.w3c.", "org.xml.",
  // JUnit
  "org.junit.", "junit.", "org.hamcrest."
  //
  };

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
    String[] classpath = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
    return findAllImplementations(classpath, c, DEFAULT_IGNORES, everything);
  }

  /**
   * Find all implementations of a given class.
   * 
   * @param classpath Classpath to use (JARs and folders supported)
   * @param c Class restriction
   * @param ignorepackages List of packages to ignore
   * @param everything include interfaces, abstract and private classes
   * @return List of found classes.
   */
  public static List<Class<?>> findAllImplementations(String[] classpath, Class<?> c, String[] ignorepackages, boolean everything) {
    // Collect iterators
    Vector<Iterable<String>> iters = new Vector<Iterable<String>>(classpath.length);
    for(String path : classpath) {
      File p = new File(path);
      if(path.endsWith(".jar")) {
        iters.add(new JarClassIterator(path));
      }
      else if(p.isDirectory()) {
        iters.add(new DirClassIterator(p));
      }
    }

    ArrayList<Class<?>> res = new ArrayList<Class<?>>();
    ClassLoader cl = ClassLoader.getSystemClassLoader();
    for(Iterable<String> iter : iters) {
      for(String classname : iter) {
        boolean ignore = false;
        for(String pkg : ignorepackages) {
          if(classname.startsWith(pkg)) {
            ignore = true;
            break;
          }
        }
        if(ignore) {
          continue;
        }
        try {
          Class<?> cls = cl.loadClass(classname);
          // skip abstract / private classes.
          if(!everything && (Modifier.isInterface(cls.getModifiers()) || Modifier.isAbstract(cls.getModifiers()) || Modifier.isPrivate(cls.getModifiers()))) {
            continue;
          }
          if(c.isAssignableFrom(cls)) {
            res.add(cls);
          }
        }
        catch(ClassNotFoundException e) {
          continue;
        }
        catch(NoClassDefFoundError e) {
          continue;
        }
        catch(Exception e) {
          continue;
        }
      }
    }
    return res;
  }

  static class JarClassIterator implements Iterator<String>, Iterable<String> {
    private Enumeration<JarEntry> jarentries;

    private String ne;

    /**
     * Constructor from Jar file.
     * 
     * @param path Jar file entries to iterate over.
     */
    public JarClassIterator(String path) {
      try {
        JarFile jf = new JarFile(path);
        this.jarentries = jf.entries();
        this.ne = findNext();
      }
      catch(IOException e) {
        LoggingUtil.exception(e);
        this.jarentries = null;
        this.ne = null;
      }
    }

    @Override
    public boolean hasNext() {
      // Do we have a next entry?
      return (ne != null);
    }

    /**
     * Find the next entry, since we need to skip some jar file entries.
     * 
     * @return next entry or null
     */
    private String findNext() {
      while(jarentries.hasMoreElements()) {
        JarEntry je = jarentries.nextElement();
        String name = je.getName();
        if(name.endsWith(".class")) {
          String classname = name.substring(0, name.length() - ".class".length());
          return classname.replace("/", ".");
        }
      }
      return null;
    }

    @Override
    public String next() {
      // Return the previously stored entry.
      String ret = ne;
      ne = findNext();
      return ret;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<String> iterator() {
      return this;
    }
  }

  static class DirClassIterator implements Iterator<String>, Iterable<String> {
    private String prefix;

    private Stack<File> set = new Stack<File>();

    private String cur;

    /**
     * Constructor from Directory
     * 
     * @param path Directory to iterate over
     */
    public DirClassIterator(File path) {
      this.prefix = path.getAbsolutePath();
      this.set.push(path);
      this.cur = findNext();
    }

    @Override
    public boolean hasNext() {
      // Do we have a next entry?
      return (cur != null);
    }

    /**
     * Find the next entry, since we need to skip some jar file entries.
     * 
     * @return next entry or null
     */
    private String findNext() {
      while(set.size() > 0) {
        File f = set.pop();
        // recurse into directories
        if(f.isDirectory()) {
          for(File newf : f.listFiles()) {
            set.push(newf);
          }
          continue;
        }
        String name = f.getAbsolutePath();
        if(name.startsWith(prefix)) {
          int l = prefix.length();
          if(name.charAt(l) == File.separatorChar) {
            l += 1;
          }
          name = name.substring(l);
        }
        else {
          LoggingUtil.warning("I was expecting all directories to start with '" + prefix + "' but '" + name + "' did not.");
        }
        if(name.endsWith(".class")) {
          String classname = name.substring(0, name.length() - ".class".length());
          return classname.replace(File.separator, ".");
        }
      }
      return null;
    }

    @Override
    public String next() {
      // Return the previously stored entry.
      String ret = this.cur;
      this.cur = findNext();
      return ret;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<String> iterator() {
      return this;
    }
  }
}
