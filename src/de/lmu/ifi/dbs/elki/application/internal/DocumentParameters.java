package de.lmu.ifi.dbs.elki.application.internal;

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

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.HashMapList;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtil;
import de.lmu.ifi.dbs.elki.utilities.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterizable;
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
 */
public class DocumentParameters {

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
   * @param args
   */
  public static void main(String[] args) {
    if(args.length != 2) {
      LoggingUtil.warning("I need exactly two file names to operate!");
      System.exit(1);
    }
    if(!args[0].endsWith(".html")) {
      LoggingUtil.warning("First file name doesn't end with .html!");
      System.exit(1);
    }
    if(!args[1].endsWith(".html")) {
      LoggingUtil.warning("Second file name doesn't end with .html!");
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
      LoggingUtil.exception(e);
      System.exit(1);
    }

    {
      FileOutputStream byclassfo;
      try {
        byclassfo = new FileOutputStream(byclsname);
      }
      catch(FileNotFoundException e) {
        LoggingUtil.exception("Can't create output stream!", e);
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
        LoggingUtil.exception("IO Exception writing output.", e);
        throw new RuntimeException(e);
      }
    }

    {
      FileOutputStream byoptfo;
      try {
        byoptfo = new FileOutputStream(byoptname);
      }
      catch(FileNotFoundException e) {
        LoggingUtil.exception("Can't create output stream!", e);
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
        LoggingUtil.exception("IO Exception writing output.", e);
        throw new RuntimeException(e);
      }
    }
  }

  private static void buildParameterIndex(HashMapList<Class<?>, Parameter<?, ?>> byclass, HashMapList<OptionID, Pair<Parameter<?, ?>, Class<?>>> byopt) {
    ArrayList<Pair<Object, Parameter<?, ?>>> options = new ArrayList<Pair<Object, Parameter<?, ?>>>();
    ExecutorService es = Executors.newSingleThreadExecutor();
    for(final Class<?> cls : InspectionUtil.findAllImplementations(Object.class, false)) {
      // Special cases we need to skip...
      if (cls.getCanonicalName() == "experimentalcode.elke.AlgorithmTest") {
        continue;
      }
      final Constructor<?> constructor;
      try {
        constructor = cls.getConstructor(Parameterization.class);
      }
      catch(Exception e) {
        // Not parameterizable.
        continue;
      }

      UnParameterization config = new UnParameterization();
      final TrackParameters track = new TrackParameters(config);
      // LoggingUtil.warning("Instantiating " + cls.getName());
      FutureTask<?> instantiator = new FutureTask<Object>(new Runnable() {
        @Override
        public void run() {
          try {
            constructor.newInstance(track);
          }
          catch(java.lang.reflect.InvocationTargetException e) {
            if(e.getCause() instanceof RuntimeException) {
              throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException(e.getCause());
          }
          catch(RuntimeException e) {
            throw e;
          }
          catch(Exception e) {
            throw new RuntimeException(e);
          }
        }
      }, null);
      es.submit(instantiator);
      try {
        // Wait up to one second.
        instantiator.get(100000L, TimeUnit.MILLISECONDS);
        options.addAll(track.getAllParameters());
      }
      catch(TimeoutException e) {
        de.lmu.ifi.dbs.elki.logging.LoggingUtil.warning("Timeout on instantiating " + cls.getName());
        es.shutdownNow();
        throw new RuntimeException(e);
      }
      catch(java.util.concurrent.ExecutionException e) {
        de.lmu.ifi.dbs.elki.logging.LoggingUtil.warning("Error instantiating " + cls.getName());
        es.shutdownNow();
        if(e.getCause() instanceof RuntimeException) {
          throw (RuntimeException) e.getCause();
        }
        throw new RuntimeException(e.getCause());
      }
      catch(Exception e) {
        de.lmu.ifi.dbs.elki.logging.LoggingUtil.warning("Error instantiating " + cls.getName());
        es.shutdownNow();
        throw new RuntimeException(e);
      }
    }

    for(Pair<Object, Parameter<?, ?>> pp : options) {
      Class<?> c = pp.first.getClass();
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
            if(pair.second == c) {
              inlist = true;
              break;
            }
          }
        }
        if(!inlist) {
          byopt.add(o.getOptionID(), new Pair<Parameter<?, ?>, Class<?>>(o, c));
        }
      }
      if(!byopt.contains(o.getOptionID(), new Pair<Parameter<?, ?>, Class<?>>(o, c))) {
        byopt.add(o.getOptionID(), new Pair<Parameter<?, ?>, Class<?>>(o, c));
      }
    }
    es.shutdownNow();
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
    Collections.sort(classes, new SortByName());

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
        HTMLUtil.appendMultilineText(htmldoc, elemp, opt.getShortDescription());
        elemdd.appendChild(elemp);
        // class restriction?
        if(opt instanceof ClassParameter<?>) {
          appendClassRestriction(htmldoc, (ClassParameter<?>) opt, elemdd);
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
      Parameter<?, ?> firstopt = byopt.get(oid).get(0).getFirst();
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
      if(firstopt instanceof ClassParameter<?>) {
        appendClassRestriction(htmldoc, (ClassParameter<?>) firstopt, optdd);
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
            if(!cls.getRestrictionClass().equals(((ClassParameter<?>) firstopt).getRestrictionClass())) {
              appendClassRestriction(htmldoc, cls, classli);
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

  private static void appendClassRestriction(Document htmldoc, ClassParameter<?> opt, Element elemdd) {
    Element p = htmldoc.createElement(HTMLUtil.HTML_P_TAG);
    p.appendChild(htmldoc.createTextNode(HEADER_CLASS_RESTRICTION));
    if(opt.getRestrictionClass().isInterface()) {
      p.appendChild(htmldoc.createTextNode(HEADER_CLASS_RESTRICTION_IMPLEMENTING));
    }
    else {
      p.appendChild(htmldoc.createTextNode(HEADER_CLASS_RESTRICTION_EXTENDING));
    }
    Element defa = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
    defa.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, linkForClassName(opt.getRestrictionClass().getName()));
    defa.setTextContent(opt.getRestrictionClass().getName());
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
        String prefix = opt.getRestrictionClass().getPackage().getName() + ".";
        Element p = htmldoc.createElement(HTMLUtil.HTML_P_TAG);
        p.appendChild(htmldoc.createTextNode(HEADER_KNOWN_IMPLEMENTATIONS));
        elemdd.appendChild(p);
        Element ul = htmldoc.createElement(HTMLUtil.HTML_UL_TAG);
        for(Class<?> c : iter) {
          Element li = htmldoc.createElement(HTMLUtil.HTML_LI_TAG);
          Element defa = htmldoc.createElement(HTMLUtil.HTML_A_TAG);
          defa.setAttribute(HTMLUtil.HTML_HREF_ATTRIBUTE, linkForClassName(c.getName()));
          String visname = c.getName();
          if(visname.startsWith(prefix)) {
            visname = visname.substring(prefix.length());
          }
          defa.setTextContent(visname);
          li.appendChild(defa);
          ul.appendChild(li);
        }
        elemdd.appendChild(ul);
      }
    }
  }

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

  private static void appendNoDefaultValue(Document htmldoc, Element optdd) {
    Element p = htmldoc.createElement(HTMLUtil.HTML_P_TAG);
    p.appendChild(htmldoc.createTextNode(HEADER_DEFAULT_VALUE));
    p.appendChild(htmldoc.createTextNode(NO_DEFAULT_VALUE));
    optdd.appendChild(p);
  }

  private static String linkForClassName(String name) {
    String link = name.replace(".", "/") + ".html";
    return link;
  }

  protected static class SortByName implements Comparator<Class<?>> {
    @Override
    public int compare(Class<?> o1, Class<?> o2) {
      return o1.getName().compareTo(o2.getName());
    }
  }

  protected static class SortByOption implements Comparator<OptionID> {
    @Override
    public int compare(OptionID o1, OptionID o2) {
      return o1.getName().compareToIgnoreCase(o2.getName());
    }
  }
}
