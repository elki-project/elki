package de.lmu.ifi.dbs.elki.result.combinators;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.utilities.pairs.SimplePair;

/**
 * AnnotationCombiner is a class that combines multiple Annotation Results into one.
 * 
 * @author Erich Schubert
 *
 * @param <T>
 */
public class AnnotationCombiner<T> implements AnnotationResult<T> {
  /**
   * Results to combine
   */
  private Collection<AnnotationResult<T>> results = new ArrayList<AnnotationResult<T>>();
  
  /**
   * Constructor
   */
  public AnnotationCombiner() {
    this(new ArrayList<AnnotationResult<T>>());
  }
  
  /**
   * Constructor with a list of results to combine.
   * 
   * @param results
   */
  public AnnotationCombiner(Collection<AnnotationResult<T>> results) {
    super();
    this.results = results;
  }

  /**
   * Add an annotation result to the result list.
   * 
   * @param result
   */
  public void addAnnotationResult(AnnotationResult<T> result) {
    this.results.add(result);
  }

  /**
   * Retrieve all annotations and return them as combined array.
   */
  @Override
  public SimplePair<String, T>[] getAnnotations(Integer objID) {
    ArrayList<SimplePair<String, T>> annotations = new ArrayList<SimplePair<String, T>>();
    for (AnnotationResult<T> result : results) {
        SimplePair<String, T>[] newannotations = result.getAnnotations(objID);
        for (SimplePair<String, T> newann : newannotations)
          annotations.add(newann);
      }
    SimplePair<String, T>[] result = SimplePair.newArray(0);
    return annotations.toArray(result);
  }
}
