/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.utilities;

import java.lang.reflect.InvocationTargetException;

import elki.logging.Logging;
import elki.utilities.exceptions.AbortException;
import elki.utilities.exceptions.ClassInstantiationException;
import elki.utilities.optionhandling.ParameterException;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Utilities for handling class instantiation, especially with respect to Java
 * generics.
 * <p>
 * Due to the way generics are implemented - via erasure - type safety cannot be
 * guaranteed properly at compile time here. These classes collect such cases
 * using helper functions, so that we have to suppress these warnings only in
 * one place.
 * <p>
 * Note that many of these situations are still type safe, i.e., an <i>empty</i>
 * array of {@code List<List<?>>} can indeed be cast into a
 * {@code List<List<Whatever>>}.
 *
 * @author Erich Schubert
 * @since 0.2
 */
public final class ClassGenericsUtil {
  /**
   * Static logger to use.
   */
  private static final Logging LOG = Logging.getLogger(ClassGenericsUtil.class);

  /**
   * Class loader.
   */
  static final ClassLoader CLASSLOADER = ClassGenericsUtil.class.getClassLoader();

  /**
   * Name for a static "parameterize" factory method.
   */
  public static final String FACTORY_METHOD_NAME = "parameterize";

  /**
   * Fake Constructor. Use static methods.
   */
  private ClassGenericsUtil() {
    // Do not instantiate
  }

  /**
   * Get a parameterizer for the given class.
   *
   * @param c Class
   * @return Parameterizer or null.
   */
  public static Parameterizer getParameterizer(Class<?> c) {
    for(Class<?> inner : c.getDeclaredClasses()) {
      if(Parameterizer.class.isAssignableFrom(inner)) {
        try {
          return inner.asSubclass(Parameterizer.class).getDeclaredConstructor().newInstance();
        }
        catch(Exception e) {
          LOG.warning("Non-usable Parameterizer in class: " + c.getName());
        }
      }
    }
    return null;
  }

  /**
   * Instantiate a parameterizable class. When using this, consider using
   * {@link Parameterization#descend}!
   *
   * @param <C> base type
   * @param r Base (restriction) class
   * @param c Class to instantiate
   * @param config Configuration to use for instantiation.
   * @return Instance
   * @throws ClassInstantiationException When a class cannot be instantiated.
   */
  public static <C> C tryInstantiate(Class<C> r, Class<?> c, Parameterization config) throws ClassInstantiationException {
    if(c == null) {
      throw new ClassInstantiationException("Trying to instantiate 'null' class!");
    }
    try {
      // Try a V3 parameterization class
      Parameterizer par = getParameterizer(c);
      return r.cast(par != null ? make(par, config) //
          // Try a default constructor.
          : c.getConstructor().newInstance());
    }
    catch(InstantiationException | InvocationTargetException
        | IllegalAccessException | NoSuchMethodException e) {
      throw new ClassInstantiationException(e);
    }
  }

  /**
   * Method to configure a class, then instantiate when the configuration step
   * was successful.
   * <p>
   * <b>Don't call this directly use unless you know what you are doing. <br>
   * Instead, use {@link Parameterization#tryInstantiate(Class)}!</b>
   * <p>
   * Otherwise, {@code null} will be returned, and the resulting errors can be
   * retrieved from the {@link Parameterization} parameter object. In general,
   * you should be checking the {@link Parameterization} object for errors
   * before accessing the returned value, since it may be {@code null}
   * unexpectedly otherwise.
   * 
   * @param config Parameterization
   * @return Instance or {@code null}
   */
  public static Object make(Parameterizer par, Parameterization config) {
    Object owner = par.getClass().getDeclaringClass();
    config = config.descend(owner != null ? owner : par);
    par.configure(config);

    if(config.hasErrors()) {
      return null;
    }
    Object ret = par.make();
    if(ret == null) {
      throw new AbortException("makeInstance() returned null!");
    }
    return ret;
  }

  /**
   * Force parameterization method.
   * <p>
   * Please use this only in "runner" classes such as unit tests, since the
   * error handling is not very flexible.
   *
   * @param <C> Type
   * @param c Class to instantiate
   * @param config Parameters
   * @return Instance
   * @throws AbortException on errors
   */
  @SuppressWarnings("unchecked")
  public static <C> C parameterizeOrAbort(Class<?> c, Parameterization config) {
    try {
      C ret = tryInstantiate((Class<C>) c, c, config);
      if(ret == null) {
        throw new AbortException("Could not instantiate class. Check parameters.");
      }
      return ret;
    }
    catch(Exception e) {
      if(config.hasErrors()) {
        for(ParameterException err : config.getErrors()) {
          LOG.warning(err.toString());
        }
      }
      throw e instanceof AbortException ? (AbortException) e : new AbortException("Instantiation failed", e);
    }
  }

  /**
   * Cast the (erased) generics onto a class.
   * <p>
   * Note: this function is a hack - notice that it would allow you to up-cast
   * any class! Still it is preferable to have this cast in one place than in
   * dozens without any explanation.
   * <p>
   * The reason this is needed is the following: There is no
   * <code>Class&lt;Set&lt;String&gt;&gt;.class</code>.
   * This method allows you to do
   * <code>Class&lt;Set&lt;String&gt;&gt; setclass = uglyCastIntoSubclass(Set.class);</code>
   * <p>
   * We can't type check at runtime, since we don't have T.
   *
   * @param cls Class type
   * @param <D> Base type
   * @param <T> Supertype
   * @return {@code cls} parameter, but cast to {@code Class<T>}
   */
  @SuppressWarnings("unchecked")
  public static <D, T extends D> Class<T> uglyCastIntoSubclass(Class<D> cls) {
    return (Class<T>) cls;
  }

  /**
   * Instantiate the first available implementation of an interface.
   * <p>
   * Only supports public and parameterless constructors.
   *
   * @param clz Class to instantiate.
   * @return Instance
   */
  public static <T> T instantiateLowlevel(Class<? extends T> clz) {
    Exception last = null;
    for(Class<?> c : ELKIServiceRegistry.findAllImplementations(clz)) {
      try {
        return clz.cast(c.getDeclaredConstructor().newInstance());
      }
      catch(Exception e) {
        last = e;
      }
    }
    throw new AbortException("Cannot find a usable implementation of " + clz.toString(), last);
  }

  /**
   * Try to load the default for a particular interface (must have a public
   * default constructor, used for factories).
   *
   * @param <T> Type
   * @param clz Interface to implement
   * @param def Name of default implementation
   * @return Instance
   */
  public static <T> T loadDefault(Class<T> clz, String def) {
    try {
      return clz.cast(ELKIServiceRegistry.findImplementation(clz, def).getDeclaredConstructor().newInstance());
    }
    catch(Exception e) {
      return instantiateLowlevel(clz);
    }
  }
}
