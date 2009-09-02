package de.lmu.ifi.dbs.elki.utilities;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * <p>
 * Utils for handling class instantiation especially with respect to Java
 * generics.
 * </p>
 * 
 * <p>
 * Due to the way generics are implemented - via erasure - type safety cannot be
 * guaranteed properly at compile time here. These classes collect such cases
 * using helper functions, so that we have to suppress these warnings only in
 * one place.
 * </p>
 * 
 * <p>
 * Note that many of these situations are still type safe, i.e. an <i>empty</i>
 * array of List<List<?>> can indeed be cast into a List<List<Whatever>>.
 * </p>
 * 
 * <p>
 * The only one potentially unsafe is {@link #instantiateGenerics}, since we
 * can't verify that the runtime type 'type' adhers to the compile time
 * restriction T. When T is not generic, such a check is possible, and then the
 * developer should use {@link #instantiate} instead.
 * </p>
 * 
 */
public final class ClassGenericsUtil {
  /**
   * <p>
   * Returns a new instance of the given type for the specified className.
   * </p>
   * 
   * <p>
   * If the Class for className is not found, the instantiation is tried using
   * the package of the given type as package of the given className.
   * </p>
   * 
   * @param <T> Class type for compile time type checking
   * @param type desired Class type of the Object to retrieve
   * @param className name of the class to instantiate
   * @return a new instance of the given type for the specified className
   * @throws UnableToComplyException if the instantiation cannot be performed
   *         successfully
   */
  public static <T> T instantiate(Class<T> type, String className) throws UnableToComplyException {
    T instance;
    try {
      try {
        instance = type.cast(Class.forName(className).newInstance());
      }
      catch(ClassNotFoundException e) {
        // try package of type
        instance = type.cast(Class.forName(type.getPackage().getName() + "." + className).newInstance());
      }
    }
    catch(InstantiationException e) {
      throw new UnableToComplyException(e);
    }
    catch(IllegalAccessException e) {
      throw new UnableToComplyException(e);
    }
    catch(ClassNotFoundException e) {
      throw new UnableToComplyException(e);
    }
    catch(ClassCastException e) {
      throw new UnableToComplyException(e);
    }
    return instance;
  }

  /**
   * <p>
   * Returns a new instance of the given type for the specified className.
   * </p>
   * 
   * <p>
   * If the Class for className is not found, the instantiation is tried using
   * the package of the given type as package of the given className.
   * </p>
   * 
   * <p>
   * This is a weaker type checked version of "{@link #instantiate}" for use
   * with Generics.
   * </p>
   * 
   * @param <T> Class type for compile time type checking
   * @param type desired Class type of the Object to retrieve
   * @param className name of the class to instantiate
   * @return a new instance of the given type for the specified className
   * @throws UnableToComplyException if the instantiation cannot be performed
   *         successfully
   */
  @SuppressWarnings("unchecked")
  public static <T> T instantiateGenerics(Class<?> type, String className) throws UnableToComplyException {
    T instance;
    // TODO: can we do a verification that type conforms to T somehow?
    // (probably not because generics are implemented via erasure.
    try {
      try {
        instance = ((Class<T>) type).cast(Class.forName(className).newInstance());
      }
      catch(ClassNotFoundException e) {
        // try package of type
        instance = ((Class<T>) type).cast(Class.forName(type.getPackage().getName() + "." + className).newInstance());
      }
    }
    catch(InstantiationException e) {
      throw new UnableToComplyException(e);
    }
    catch(IllegalAccessException e) {
      throw new UnableToComplyException(e);
    }
    catch(ClassNotFoundException e) {
      throw new UnableToComplyException(e);
    }
    catch(ClassCastException e) {
      throw new UnableToComplyException(e);
    }
    return instance;
  }

  /**
   * Create an array (of null values)
   * 
   * This is a common unchecked cast we have to do due to Java Generics
   * limitations.
   * 
   * @param <T> Type the array elements have
   * @param len array size
   * @param base template class for array creation.
   * @return new array of null pointers.
   */
  @SuppressWarnings("unchecked")
  public static <T> T[] newArrayOfNull(int len, Class<T> base) {
    return (T[]) java.lang.reflect.Array.newInstance(base, len);
  }

  /**
   * Convert a collection to an array.
   * 
   * @param <T> Type the array elements have
   * @param coll collection to convert.
   * @param base Template class for array creation.
   * @return new array with the collection contents.
   */
  @SuppressWarnings("unchecked")
  public static <B, T extends B> T[] toArray(Collection<T> coll, Class<B> base) {
    return coll.toArray((T[])newArray(base, 0));
  }

  /**
   * Create an array of <code>len</code> empty ArrayLists.
   * 
   * This is a common unchecked cast we have to do due to Java Generics
   * limitations.
   * 
   * @param <T> Type the list elements have
   * @param len array size
   * @return new array of ArrayLists
   */
  @SuppressWarnings("unchecked")
  public static <T> ArrayList<T>[] newArrayOfEmptyArrayList(int len) {
    ArrayList<T>[] result = new ArrayList[len];
    for(int i = 0; i < len; i++) {
      result[i] = new ArrayList();
    }
    return result;
  }

  /**
   * Create an array of <code>len</code> empty HashSets.
   * 
   * This is a common unchecked cast we have to do due to Java Generics
   * limitations.
   * 
   * @param <T> Type the set elements have
   * @param len array size
   * @return new array of HashSets
   */
  @SuppressWarnings("unchecked")
  public static <T> HashSet<T>[] newArrayOfEmptyHashSet(int len) {
    HashSet<T>[] result = new HashSet[len];
    for(int i = 0; i < len; i++) {
      result[i] = new HashSet();
    }
    return result;
  }

  /**
   * Cast the (erased) generics onto a class.
   * 
   * Note: this function is a hack - notice that it would allow you to up-cast
   * any class! Still it is preferable to have this cast in one place than in
   * dozens without any explanation.
   * 
   * The reason this is needed is the following: There is no
   * Class&lt;Set&lt;String&gt;&gt;.class. This method allows you to do <code>
   * Class&lt;Set&lt;String&gt;&gt; setclass = uglyCastIntoSubclass(Set.class);
   * </code>
   * 
   * We can't type check at runtime, since we don't have T.
   */
  @SuppressWarnings("unchecked")
  public static <D, T extends D> Class<T> uglyCastIntoSubclass(Class<D> cls) {
    return (Class<T>) cls;
  }

  /**
   * This class performs an ugly cast, from <code>Class&lt;F&gt;</code> to
   * <code>Class&lt;T&gt;</code>, where both F and T need to extend B.
   * 
   * The restrictions are there to avoid misuse of this cast helper.
   * 
   * While this sounds really ugly, the common use case will be something like
   * 
   * <pre>
   * BASE = Class&lt;Database&gt;
   * FROM = Class&lt;Database&gt;
   * TO = Class&lt;Database&lt;V&gt;&gt;
   * </pre>
   * 
   * i.e. the main goal is to add missing Generics to the compile time type.
   * 
   * @param <BASE> Base type
   * @param <TO> Destination type
   * @param <FROM> Source type
   * @param cls Class to be cast
   * @param base Base class for type checking.
   * @return Casted class.
   */
  @SuppressWarnings("unchecked")
  public static <BASE, FROM extends BASE, TO extends BASE> Class<TO> uglyCrossCast(Class<FROM> cls, Class<BASE> base) {
    if(!base.isAssignableFrom(cls)) {
      if(cls == null) {
        throw new ClassCastException("Attempted to use 'null' as class.");
      }
      throw new ClassCastException(cls.getName() + " is not a superclass of " + base);
    }
    return (Class<TO>) cls;
  }

  /**
   * Cast an object at a base class, but return a subclass (for Generics!).
   * 
   * The main goal of this is to allow casting an object from e.g. "
   * <code>List</code>" to "<code>List&lt;Something&gt;</code>" without having
   * to add SuppressWarnings everywhere.
   * 
   * @param <B> Base type to cast at
   * @param <T> Derived type returned
   * @param base Base class to cast at
   * @param obj Object
   * @return Cast object or null.
   */
  @SuppressWarnings("unchecked")
  public static <B, T extends B> T castWithGenericsOrNull(Class<B> base, Object obj) {
    try {
      return (T) base.cast(obj);
    }
    catch(ClassCastException e) {
      return null;
    }
  }

  /**
   * Generic newInstance that tries to clone an object.
   * 
   * @param <T> Object type, generic
   * @param obj Master copy - must not be null.
   * @return New instance, if possible
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   * @throws NoSuchMethodException
   */
  @SuppressWarnings("unchecked")
  public static <T> T newInstance(T obj) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    try {
      Object n = obj.getClass().getConstructor().newInstance();
      return (T) n;
    }
    catch(NullPointerException e) {
      throw new IllegalArgumentException("Null pointer exception in newInstance()", e);
    }
  }

  /**
   * Retrieve the component type of a given array. For cloning.
   * 
   * @param <T> Array type, generic
   * @param a Existing array
   * @return Component type of the given array.
   */
  @SuppressWarnings("unchecked")
  public static <T> Class<? extends T> getComponentType(T[] a) {
    Class<?> k = a.getClass().getComponentType();
    return (Class<? extends T>) k;
  }

  /**
   * Make a new array of the given class and size.
   * 
   * @param <T> Generic type
   * @param k Class
   * @param size Size
   * @return new array of the given type
   */
  @SuppressWarnings("unchecked")
  public static <T> T[] newArray(Class<? extends T> k, int size) {
    if(k.isPrimitive()) {
      throw new IllegalArgumentException("Argument cannot be primitive: " + k);
    }
    Object a = java.lang.reflect.Array.newInstance(k, size);
    return (T[]) a;
  }

  /**
   * Clone an array of the given type.
   * 
   * @param <T> Generic type
   * @param a existing array
   * @param size array size
   * @return new array
   */
  public static <T> T[] newArray(T[] a, int size) {
    return newArray(getComponentType(a), size);
  }

  /**
   * Clone a collection. Collection must have an empty constructor!
   * 
   * @param <T>
   * @param <C>
   * @param coll
   * @return
   */
  public static <T, C extends Collection<T>> C cloneCollection(C coll) {
    try {
      C copy = newInstance(coll);
      copy.addAll(coll);
      return copy;
    }
    catch(InstantiationException e) {
      throw new RuntimeException(e);
    }
    catch(IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    catch(InvocationTargetException e) {
      throw new RuntimeException(e);
    }
    catch(NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Transform a collection to an Array
   * 
   * @param <T> object type
   * @param c Collection
   * @param a Array to write to or replace (i.e. sample array)
   * @return
   */
  public static <T> T[] collectionToArray(Collection<T> c, T[] a) {
    if(a.length < c.size()) {
      a = newArray(a, c.size());
    }
    int i = 0;
    for(T x : c) {
      a[i] = x;
      i++;
    }
    if(i < a.length) {
      a[i] = null;
    }
    return a;
  }
}
