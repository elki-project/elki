package de.lmu.ifi.dbs.elki.application.internal;

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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Comment;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.ELKIServiceLoader;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.HashMapList;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.UnParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.utilities.xml.HTMLUtil;

/**
 * Class to generate HTML parameter descriptions for all classes implementing
 * the {@link Parameterizable} interface. Used in documentation generation only.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses Parameterizable
 * @apiviz.uses Parameter
 */
public class DocumentParameters {
  static final Logging logger = Logging.getLogger(DocumentParameters.class);

  private static final String HEADER_PARAMETER_FOR = "Parameter for: ";

  private static final String HEADER_DEFAULT_VALUE = "Default: ";

  private static final String NO_DEFAULT_VALUE = "No default value.";

  private static final String HEADER_CLASS_RESTRICTION = "Class Restriction: ";

  private static final String HEADER_CLASS_RESTRICTION_IMPLEMENTING = "implements ";

  private static final String HEADER_CLASS_RESTRICTION_EXTENDING = "extends ";

  private static final String NO_CLASS_RESTRICTION = "No class restriction.";

  private static final String CSSFILE = "stylesheet.css";

  private static final String MODIFICATION_WARNING = "WARNING: THIS DOCUMENT IS AUTOMATICALLY GENERATED. MODIFICATIONS MAY GET LOST.";

  private static final String HEADER_KNOWN_IMPLEMENTATIONS = "Known implementations: ";

  /**
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    if(args.length != 2) {
      logger.warning("I need exactly two file names to operate!");
      System.exit(1);
    }
    if(!args[0].endsWith(".html")) {
      logger.warning("First file name doesn't end with .html!");
      System.exit(1);
    }
    if(!args[1].endsWith(".html")) {
      logger.warning("Second file name doesn't end with .html!");
      System.exit(1);
    }
    File byclsname = new File(args[0]);
    File byoptname = new File(args[1]);

    HashMapList<Class<?>, Parameter<?, ?>> byclass = new HashMapList<Class<?>, Parameter<?, ?>>();
    HashMapList<OptionID, Pair<Parameter<?, ?>, Class<?>>> byopt = new HashMapList<OptionID, Pair<Parameter<?, ?>, Class<?>>>();
    try {
      buildParameterIndex(byclass, byopt);
    }
    catch(Exception e) {
      logger.exception(e);
      System.exit(1);
    }

    {
      FileOutputStream byclassfo;
      try {
        byclassfo = new FileOutputStream(byclsname);
      }
      catch(FileNotFoundException e) {
        logger.exception("Can't create output stream!", e);
        throw new RuntimeException(e);
      }
      OutputStream byclassstream = new BufferedOutputStream(byclassfo);
      Document byclassdoc = makeByClassOverview(byclass);
      try {
        HTMLUtil.writeXHTML(byclassdoc, byclassstream);
        byclassstream.flush();
        byclassstream.close();
        byclassfo.close();
      }
      catch(IOException e) {
        logger.exception("IO Exception writing output.", e);
        throw new RuntimeException(e);
      }
    }

    {
      FileOutputStream byoptfo;
      try {
        byoptfo = new FileOutputStream(byoptname);
      }
      catch(FileNotFoundException e) {
        logger.exception("Can't create output stream!", e);
        throw new RuntimeException(e);
      }
      OutputStream byoptstream = new BufferedOutputStream(byoptfo);
      Document byoptdoc = makeByOptOverview(byopt);
      try {
        HTMLUtil.writeXHTML(byoptdoc, byoptfo);
        byoptstream.flush();
        byoptstream.close();
        byoptfo.close();
      }
      catch(IOException e) {
        logger.exception("IO Exception writing output.", e);
        throw new RuntimeException(e);
      }
    }
  }

  private static void buildParameterIndex(HashMapList<Class<?>, Parameter<?, ?>> byclass, HashMapList<OptionID, Pair<Parameter<?, ?>, Class<?>>> byopt) {
    final ArrayList<Pair<Object, Parameter<?, ?>>> options = new ArrayList<Pair<Object, Parameter<?, ?>>>();
    ExecutorService es = Executors.newSingleThreadExecutor();
    for(final Class<?> cls : InspectionUtil.findAllImplementations(Parameterizable.class, false)) {
      // Doesn't have a proper name?
      if(cls.getCanonicalName() == null) {
        continue;
      }
      // Special cases we need to skip...
      if(cls.getCanonicalName() == "experimentalcode.elke.AlgorithmTest") {
        continue;
      }

      UnParameterization config = new UnParameterization();
      final TrackParameters track = new TrackParameters(config);
      // LoggingUtil.warning("Instantiating " + cls.getName());
      FutureTask<?> instantiator = new FutureTask<Object>(new Runnable() {
        @Override
        public void run() {
          // Try a V3 style parameterizer first.
          Parameterizer par = ClassGenericsUtil.getParameterizer(cls);
          if(par != null) {
            par.configure(track);
          }
          else {
            try {
              ClassGenericsUtil.tryInstantiate(Object.class, cls, track);
            }
            catch(java.lang.NoSuchMethodException e) {
              logger.warning("Could not instantiate class " + cls.getName() + " - no appropriate constructor or parameterizer found.");
            }
            catch(java.lang.reflect.InvocationTargetException e) {
              if(e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
              }
              if(e.getCause() instanceof Error) {
                throw (Error) e.getCause();
              }
              throw new RuntimeException(e.getCause());
            }
            catch(RuntimeException e) {
              throw e;
            }
            catch(Exception e) {
              throw new RuntimeException(e);
            }
            catch(java.lang.Error e) {
              throw new RuntimeException(e);
            }
          }
          for(Pair<Object, Parameter<?, ?>> pair : track.getAllParameters()) {
            if(pair.first == null) {
              pair.first = cls;
            }
            options.add(pair);
          }
        }
      }, null);
      es.submit(instantiator);
      try {
        // Wait up to one second.
        instantiator.get(100000L, TimeUnit.MILLISECONDS);
      }
      catch(TimeoutException e) {
        de.lmu.ifi.dbs.elki.logging.LoggingUtil.warning("Timeout on instantiating " + cls.getName());
        es.shutdownNow();
        throw new RuntimeException(e);
      }
      catch(java.util.concurrent.ExecutionException e) {
        de.lmu.ifi.dbs.elki.logging.LoggingUtil.warning("Error instantiating " + cls.getName(), e.getCause());
        /*
         * es.shutdownNow(); if(e.getCause() instanceof RuntimeException) {
         * throw (RuntimeException) e.getCause(); } throw new
         * RuntimeException(e.getCause());
         */
        continue;
      }
      catch(Exception e) {
        /*
         * de.lmu.ifi.dbs.elki.logging.LoggingUtil.warning("Error instantiating "
         * + cls.getName()); es.shutdownNow(); throw new RuntimeException(e);
         */
        continue;
      }
    }

    logger.debug("Documenting " + options.size() + " parameter instances.");
    for(Pair<Object, Parameter<?, ?>> pp : options) {
      if(pp.first == null || pp.second == null) {
        logger.debugFiner("Null: " + pp.first + " " + pp.second);
        continue;
      }
      Class<?> c;
      if(pp.first instanceof Class) {
        c = (Class<?>) pp.first;
      }
      else {
        c = pp.first.getClass();
      }
      Parameter<?, ?> o = pp.second;

      // just collect unique occurrences
      {
        List<Parameter<?, ?>> byc = byclass.get(c);
        boolean inlist = false;
        if(byc != null) {
          for(Parameter<?, ?> par : byc) {
            if(par.getOptionID() == o.getOptionID()) {
              inlist = true;
              break;
            }
          }
        }
        if(!inlist) {
          byclass.add(c, o);
        }
      }
      {
        List<Pair<Parameter<?, ?>, Class<?>>> byo = byopt.get(o.getOptionID());
        boolean inlist = false;
        if(byo != null) {
          for(Pair<Parameter<?, ?>, Class<?>> pair : byo) {
            if(pair.second.equals(c)) {
              inlist = true;
              break;
            }
          }
        }
        if(!inlist) {
          byopt.add(o.getOptionID(), new Pair<Parameter<?, ?>, Class<?>>(o, c));
        }
      }
    }
    es.shutdownNow();
    logger.debug("byClass: " + byclass.size() + " byOpt: " + byopt.size());
  }

  protected static Constructor<?> getConstructor(final Class<?> cls) {
    try {
      return cls.getConstructor(Parameterization.class);
    }
    catch(java.lang.NoClassDefFoundError e) {
      // Class not actually found
    }
    catch(RuntimeException e) {
      // Not parameterizable, usually not even found ...
      logger.warning("RuntimeException: ", e);
    }
    catch(Exception e) {
      // Not parameterizable.
    }
    catch(java.lang.Error e) {
      // Not parameterizable.
      logger.warning("Error: ", e);
    }
    return null;
  }

  private static Document makeByClassOverview(HashMapList<Class<?>, Parameter<?, ?>> byclass) {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();
    }
    catch(ParserConfigurationException e1) {
      throw new RuntimeException(e1);
    }
    DOMImplementation impl = builder.getDOMImplementation();
    Document htmldoc = impl.createDocument(HTMLUtil.HTML_NAMESPACE, HTMLUtil.HTML_HTML_TAG, null);
    // head
    Element head = htmldoc.createElement(HTMLUtil.HTML_HEAD_TAG);
    htmldoc.getDocumentElement().appendChild(head);
    // body
    Element body = htmldoc.createElement(HTMLUtil.HTML_BODY_TAG);
    htmldoc.getDocumentElement().appendChild(body);
    // modification warnings
    {
      Comment warn = htmldoc.createComment(MODIFICATION_WARNING);
      head.appendChild(warn);
      Comment warn2 = htmldoc.createComment(MODIFICATION_WARNING);
      body.appendChild(warn2);
    }
    // meta with charset information
    {
      Element meta = htmldoc.createElement(HTMLUtil.HTML_META_TAG);
      meta.setAttribute(HTMLUtil.HTML_HTTP_EQUIV_ATTRIBUTE, HTMLUtil.HTML_HTTP_EQUIV_CONTENT_TYPE);
      meta.setAttribute(HTMLUtil.HTML_CONTENT_ATTRIBUTE, HTMLUtil.CONTENT_TYPE_HTML_UTF8);
      head.appendChild(meta);
    }
    // stylesheet
    {
      Element css = htmldoc.createElement(HTMLUtil.HTML_LINK_TAG);
      css.setAttribute(HTMLUtil.HTML_REL_ATTRIBUTE, HTMLUtil.HTML_REL_STYLESHEET);
      css.setAttribute(HTMLUtil.HTML_TYPE_ATTRIBUTE, HTMLUtil.CONTENT_TYPE_CSS);
      css.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, CSSFILE);
      head.appendChild(css);
    }
    // title
    {
      Element title = htmldoc.createElement(HTMLUtil.HTML_TITLE_TAG);
      title.setTextContent("Command line parameter overview.");
      head.appendChild(title);
    }
    // Heading
    {
      Element h1 = htmldoc.createElement(HTMLUtil.HTML_H1_TAG);
      h1.setTextContent("ELKI command line parameter overview:");
      body.appendChild(h1);
    }

    // Main definition list
    Element maindl = htmldoc.createElement(HTMLUtil.HTML_DL_TAG);
    body.appendChild(maindl);

    List<Class<?>> classes = new ArrayList<Class<?>>(byclass.keySet());
    Collections.sort(classes, new InspectionUtil.ClassSorter());

    for(Class<?> cls : classes) {
      // DT = definition term
      Element classdt = htmldoc.createElement(HTMLUtil.HTML_DT_TAG);
      // Anchor for references
      {
        Element classan = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
        classan.setAttribute(HTMLUtil.HTML_NAME_ATTRIBUTE, cls.getName());
        classdt.appendChild(classan);
      }
      // Link back to original class
      {
        Element classa = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
        classa.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, linkForClassName(cls.getName()));
        classa.setTextContent(cls.getName());
        classdt.appendChild(classa);
      }
      maindl.appendChild(classdt);
      // DD = definition description
      Element classdd = htmldoc.createElement(HTMLUtil.HTML_DD_TAG);
      maindl.appendChild(classdd);
      // nested definition list for options
      Element classdl = htmldoc.createElement(HTMLUtil.HTML_DL_TAG);
      classdd.appendChild(classdl);
      for(Parameter<?, ?> opt : byclass.get(cls)) {
        // DT definition term: option name, in TT for typewriter optics
        Element elemdt = htmldoc.createElement(HTMLUtil.HTML_DT_TAG);
        {
          Element elemtt = htmldoc.createElement(HTMLUtil.HTML_TT_TAG);
          elemtt.setTextContent(SerializedParameterization.OPTION_PREFIX + opt.getName() + " " + opt.getSyntax());
          elemdt.appendChild(elemtt);
        }
        classdl.appendChild(elemdt);
        // DD definition description - put the option description here.
        Element elemdd = htmldoc.createElement(HTMLUtil.HTML_DD_TAG);
        Element elemp = htmldoc.createElement(HTMLUtil.HTML_P_TAG);
        if(opt.getShortDescription() != null) {
          HTMLUtil.appendMultilineText(htmldoc, elemp, opt.getShortDescription());
        }
        elemdd.appendChild(elemp);
        // class restriction?
        if(opt instanceof ClassParameter<?>) {
          appendClassRestriction(htmldoc, ((ClassParameter<?>) opt).getRestrictionClass(), elemdd);
        }
        // default value? completions?
        appendDefaultValueIfSet(htmldoc, opt, elemdd);
        // known values?
        if(opt instanceof ClassParameter<?>) {
          appendKnownImplementationsIfNonempty(htmldoc, (ClassParameter<?>) opt, elemdd);
        }
        classdl.appendChild(elemdd);
      }
    }
    return htmldoc;
  }

  private static Document makeByOptOverview(HashMapList<OptionID, Pair<Parameter<?, ?>, Class<?>>> byopt) {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();
    }
    catch(ParserConfigurationException e1) {
      throw new RuntimeException(e1);
    }
    DOMImplementation impl = builder.getDOMImplementation();
    Document htmldoc = impl.createDocument(HTMLUtil.HTML_NAMESPACE, HTMLUtil.HTML_HTML_TAG, null);
    // head
    Element head = htmldoc.createElement(HTMLUtil.HTML_HEAD_TAG);
    htmldoc.getDocumentElement().appendChild(head);
    // body
    Element body = htmldoc.createElement(HTMLUtil.HTML_BODY_TAG);
    htmldoc.getDocumentElement().appendChild(body);
    // modification warnings
    {
      Comment warn = htmldoc.createComment(MODIFICATION_WARNING);
      head.appendChild(warn);
      Comment warn2 = htmldoc.createComment(MODIFICATION_WARNING);
      body.appendChild(warn2);
    }
    // meta with charset information
    {
      Element meta = htmldoc.createElement(HTMLUtil.HTML_META_TAG);
      meta.setAttribute(HTMLUtil.HTML_HTTP_EQUIV_ATTRIBUTE, HTMLUtil.HTML_HTTP_EQUIV_CONTENT_TYPE);
      meta.setAttribute(HTMLUtil.HTML_CONTENT_ATTRIBUTE, HTMLUtil.CONTENT_TYPE_HTML_UTF8);
      head.appendChild(meta);
    }
    // stylesheet
    {
      Element css = htmldoc.createElement(HTMLUtil.HTML_LINK_TAG);
      css.setAttribute(HTMLUtil.HTML_REL_ATTRIBUTE, HTMLUtil.HTML_REL_STYLESHEET);
      css.setAttribute(HTMLUtil.HTML_TYPE_ATTRIBUTE, HTMLUtil.CONTENT_TYPE_CSS);
      css.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, CSSFILE);
      head.appendChild(css);
    }
    // title
    {
      Element title = htmldoc.createElement(HTMLUtil.HTML_TITLE_TAG);
      title.setTextContent("Command line parameter overview - by option");
      head.appendChild(title);
    }
    // Heading
    {
      Element h1 = htmldoc.createElement(HTMLUtil.HTML_H1_TAG);
      h1.setTextContent("ELKI command line parameter overview:");
      body.appendChild(h1);
    }

    // Main definition list
    Element maindl = htmldoc.createElement(HTMLUtil.HTML_DL_TAG);
    body.appendChild(maindl);

    List<OptionID> opts = new ArrayList<OptionID>(byopt.keySet());
    Collections.sort(opts, new SortByOption());

    for(OptionID oid : opts) {
      final Parameter<?, ?> firstopt = byopt.get(oid).get(0).getFirst();
      // DT = definition term
      Element optdt = htmldoc.createElement(HTMLUtil.HTML_DT_TAG);
      // Anchor for references
      {
        Element optan = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
        optan.setAttribute(HTMLUtil.HTML_NAME_ATTRIBUTE, firstopt.getName());
        optdt.appendChild(optan);
      }
      // option name
      {
        Element elemtt = htmldoc.createElement(HTMLUtil.HTML_TT_TAG);
        elemtt.setTextContent(SerializedParameterization.OPTION_PREFIX + firstopt.getName() + " " + firstopt.getSyntax());
        optdt.appendChild(elemtt);
      }
      maindl.appendChild(optdt);
      // DD = definition description
      Element optdd = htmldoc.createElement(HTMLUtil.HTML_DD_TAG);
      {
        Element elemp = htmldoc.createElement(HTMLUtil.HTML_P_TAG);
        HTMLUtil.appendMultilineText(htmldoc, elemp, firstopt.getShortDescription());
        optdd.appendChild(elemp);
      }
      // class restriction?
      Class<?> superclass = null;
      if(firstopt instanceof ClassParameter<?>) {
        // Find superclass heuristically
        superclass = ((ClassParameter<?>) firstopt).getRestrictionClass();
        for (Pair<Parameter<?, ?>, Class<?>> clinst : byopt.get(oid)) {
          ClassParameter<?> cls = (ClassParameter<?>) clinst.getFirst();
          if (!cls.getRestrictionClass().equals(superclass) && cls.getRestrictionClass().isAssignableFrom(superclass)) {
            superclass = cls.getRestrictionClass();
          }
        }
        appendClassRestriction(htmldoc, superclass, optdd);
      }
      // default value?
      appendDefaultValueIfSet(htmldoc, firstopt, optdd);
      // known values?
      if(firstopt instanceof ClassParameter<?>) {
        appendKnownImplementationsIfNonempty(htmldoc, (ClassParameter<?>) firstopt, optdd);
      }
      maindl.appendChild(optdd);
      // nested definition list for options
      Element classesul = htmldoc.createElement(HTMLUtil.HTML_UL_TAG);
      {
        Element p = htmldoc.createElement(HTMLUtil.HTML_P_TAG);
        p.appendChild(htmldoc.createTextNode(HEADER_PARAMETER_FOR));
        optdd.appendChild(p);
      }
      optdd.appendChild(classesul);
      for(Pair<Parameter<?, ?>, Class<?>> clinst : byopt.get(oid)) {
        // DT definition term: option name, in TT for typewriter optics
        Element classli = htmldoc.createElement(HTMLUtil.HTML_LI_TAG);

        // Link back to original class
        {
          Element classa = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
          classa.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, linkForClassName(clinst.getSecond().getName()));
          classa.setTextContent(clinst.getSecond().getName());
          classli.appendChild(classa);
        }
        if(clinst.getFirst() instanceof ClassParameter<?> && firstopt instanceof ClassParameter<?>) {
          ClassParameter<?> cls = (ClassParameter<?>) clinst.getFirst();
          if(cls.getRestrictionClass() != null) {
            // TODO: if it is null, it could still be different!
            if(!cls.getRestrictionClass().equals(superclass)) {
              appendClassRestriction(htmldoc, cls.getRestrictionClass(), classli);
            }
          }
          else {
            appendNoClassRestriction(htmldoc, classli);
          }
        }
        Parameter<?, ?> param = clinst.getFirst();
        if(param.getDefaultValue() != null) {
          if(!param.getDefaultValue().equals(firstopt.getDefaultValue())) {
            appendDefaultValueIfSet(htmldoc, param, classli);
          }
        }
        else {
          if(firstopt.getDefaultValue() != null) {
            appendNoDefaultValue(htmldoc, classli);
          }
        }

        classesul.appendChild(classli);
      }
    }
    return htmldoc;
  }

  private static void appendDefaultClassLink(Document htmldoc, Parameter<?, ?> opt, Element p) {
    Element defa = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
    defa.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, linkForClassName(((ClassParameter<?>) opt).getDefaultValue().getCanonicalName()));
    defa.setTextContent(((ClassParameter<?>) opt).getDefaultValue().getCanonicalName());
    p.appendChild(defa);
  }

  private static void appendClassRestriction(Document htmldoc, Class<?> restriction, Element elemdd) {
    if(restriction == null) {
      logger.warning("No restriction class!");
      return;
    }
    Element p = htmldoc.createElement(HTMLUtil.HTML_P_TAG);
    p.appendChild(htmldoc.createTextNode(HEADER_CLASS_RESTRICTION));
    if(restriction.isInterface()) {
      p.appendChild(htmldoc.createTextNode(HEADER_CLASS_RESTRICTION_IMPLEMENTING));
    }
    else {
      p.appendChild(htmldoc.createTextNode(HEADER_CLASS_RESTRICTION_EXTENDING));
    }
    Element defa = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
    defa.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, linkForClassName(restriction.getName()));
    defa.setTextContent(restriction.getName());
    p.appendChild(defa);
    elemdd.appendChild(p);
  }

  private static void appendNoClassRestriction(Document htmldoc, Element elemdd) {
    Element p = htmldoc.createElement(HTMLUtil.HTML_P_TAG);
    p.appendChild(htmldoc.createTextNode(HEADER_CLASS_RESTRICTION));
    p.appendChild(htmldoc.createTextNode(NO_CLASS_RESTRICTION));
    elemdd.appendChild(p);
  }

  private static void appendKnownImplementationsIfNonempty(Document htmldoc, ClassParameter<?> opt, Element elemdd) {
    if(opt.getRestrictionClass() != Object.class) {
      IterableIterator<Class<?>> iter = opt.getKnownImplementations();
      if(iter.hasNext()) {
        Element p = htmldoc.createElement(HTMLUtil.HTML_P_TAG);
        p.appendChild(htmldoc.createTextNode(HEADER_KNOWN_IMPLEMENTATIONS));
        elemdd.appendChild(p);
        Element ul = htmldoc.createElement(HTMLUtil.HTML_UL_TAG);
        for(Class<?> c : iter) {
          Element li = htmldoc.createElement(HTMLUtil.HTML_LI_TAG);
          Element defa = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
          defa.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, linkForClassName(c.getName()));
          defa.setTextContent(ClassParameter.canonicalClassName(c, opt.getRestrictionClass()));
          li.appendChild(defa);
          ul.appendChild(li);
        }
        elemdd.appendChild(ul);
      }
      // Report when not in properties file.
      Iterator<Class<?>> clss = new ELKIServiceLoader(opt.getRestrictionClass());
      if (!clss.hasNext()) {
        logger.warning(opt.getRestrictionClass().getName() + " not in properties. No autocompletion available in release GUI.");
      }
    }
  }

  /**
   * Append string containing the default value.
   * 
   * @param htmldoc Document
   * @param par Parameter
   * @param optdd HTML Element
   */
  private static void appendDefaultValueIfSet(Document htmldoc, Parameter<?, ?> par, Element optdd) {
    if(par.hasDefaultValue()) {
      Element p = htmldoc.createElement(HTMLUtil.HTML_P_TAG);
      p.appendChild(htmldoc.createTextNode(HEADER_DEFAULT_VALUE));
      if(par instanceof ClassParameter<?>) {
        appendDefaultClassLink(htmldoc, par, p);
      }
      else {
        Object def = par.getDefaultValue();
        p.appendChild(htmldoc.createTextNode(def.toString()));
      }
      optdd.appendChild(p);
    }
  }

  /**
   * Append string that there is not default value.
   * 
   * @param htmldoc Document
   * @param optdd HTML Element
   */
  private static void appendNoDefaultValue(Document htmldoc, Element optdd) {
    Element p = htmldoc.createElement(HTMLUtil.HTML_P_TAG);
    p.appendChild(htmldoc.createTextNode(HEADER_DEFAULT_VALUE));
    p.appendChild(htmldoc.createTextNode(NO_DEFAULT_VALUE));
    optdd.appendChild(p);
  }

  /**
   * Return a link for the class name
   * 
   * @param name Class name
   * @return (relative) link destination
   */
  private static String linkForClassName(String name) {
    String link = name.replace(".", "/") + ".html";
    return link;
  }

  /**
   * Sort parameters by their option
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected static class SortByOption implements Comparator<OptionID> {
    @Override
    public int compare(OptionID o1, OptionID o2) {
      return o1.getName().compareToIgnoreCase(o2.getName());
    }
  }
}
