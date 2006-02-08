package de.lmu.ifi.dbs.distance.multirepresented;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.distance.AbstractDistanceFunction;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.*;
import java.util.regex.Pattern;

/**
 * A combination tree is a distance function for multi-represented objects fulfilling two conditions:
 * The leafs represent the different representations and the inner nodes represent the union
 * or intersection operator.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class CombinationTree<M extends MetricalObject<M>, O extends MultiRepresentedObject<M>, D extends Distance<D>> extends AbstractDistanceFunction<O, D> {
  /**
   * The default distance function.
   */
  public static final String DEFAULT_DISTANCE_FUNCTION = EuklideanDistanceFunction.class.getName();

  /**
   * The union operator.
   */
  public static final char UNION_OPERATOR = 'U';

  /**
   * The intersection operator.
   */
  public static final char INTERSECTION_OPERATOR = 'I';

  /**
   * The representation operator prefix.
   */
  public static final char REPRESENTATION_PREFIX = 'R';

  /**
   * The separator in the representation operator.
   */
  public static String REPRESENTATION_SEPARATOR = ":";

  /**
   * The separator in the intersection and union operator.
   */
  public static final char OPERATOR_SEPARATOR = ',';

  /**
   * Left parenthesis.
   */
  public static final char LEFT_PARENTHESIS = '(';

  /**
   * Right parenthesis.
   */
  public static final char RIGHT_PARENTHESIS = ')';

  /**
   * Option string for parameter tree.
   */
  public static final String TREE_P = "tree";

  /**
   * Description for parameter tree.
   */
  public static final String TREE_D = "<prefix>a prefix notation of the combination tree: \n" +
                                      "  - Representations are operands in the combination tree: " +
                                      REPRESENTATION_PREFIX + REPRESENTATION_SEPARATOR + "<index>" + REPRESENTATION_SEPARATOR + "<classname> \n" +
                                      "    where <index> denotes the index of the representation, \n" +
                                      "    and <classname> denotes the distance function to determine the distances in this representation \n" +
                                      "    must implement " + DistanceFunction.class.getName() + ". (Default: " + DEFAULT_DISTANCE_FUNCTION + ") \n" +
                                      "  - if <op1> and <op2> are operands then \n" +
                                      "        - " + UNION_OPERATOR + LEFT_PARENTHESIS + "<op1>" + OPERATOR_SEPARATOR + "<op2>" + RIGHT_PARENTHESIS + "\n" +
                                      "        - " + INTERSECTION_OPERATOR + LEFT_PARENTHESIS + "<op1>" + OPERATOR_SEPARATOR + "<op2>" + RIGHT_PARENTHESIS + "\n" +
                                      "    are also operands in the combination tree, \n" +
                                      "    where <" + UNION_OPERATOR + "> denotes the union operator and <" + INTERSECTION_OPERATOR + "> denotes the intersection operator.";


  /**
   * The split pattern for splitting the operators and operands of the input string.
   */
  private static Pattern OPERATOR_SPLIT = Pattern.compile(" +");

  /**
   * The split pattern for splitting the operands of the input string.
   */
  private static Pattern OPERAND_SPLIT = Pattern.compile(REPRESENTATION_SEPARATOR);
  /**
   * The pattern for an operator.
   */
  private static Pattern OPERATOR_PATTERN = Pattern.compile("["+UNION_OPERATOR+INTERSECTION_OPERATOR+"]");

  /**
   * The pattern for an operand.
   */
  private static Pattern OPERAND_PATTERN = Pattern.compile(REPRESENTATION_PREFIX + REPRESENTATION_SEPARATOR + "\\d+" + REPRESENTATION_SEPARATOR + "[^" + REPRESENTATION_SEPARATOR + "]+" +
                                                           "|"+REPRESENTATION_PREFIX + REPRESENTATION_SEPARATOR + "\\d+");
  /**
   * The root node of this combination tree.
   */
  private CombinationTreeNode<M, O, D> root;

  /**
   * The distance function of this combination tree.
   */
  private DistanceFunction<M, D> distanceFunction;

  /**
   * Provides a combination tree and
   * adds parameter for distance function and breadth first order description to parameter map
   * and initializes the option handler.
   */
  public CombinationTree() {
    super();

    parameterToDescription.put(TREE_P + OptionHandler.EXPECTS_VALUE, TREE_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * Provides a distance suitable to this DistanceFunction based on the given
   * pattern.
   *
   * @param pattern A pattern defining a distance suitable to this
   *                DistanceFunction
   * @return a distance suitable to this DistanceFunction based on the given
   *         pattern
   * @throws IllegalArgumentException if the given pattern is not compatible with the requirements
   *                                  of this DistanceFunction
   */
  public D valueOf(String pattern) throws IllegalArgumentException {
    return distanceFunction.valueOf(pattern);
  }

  /**
   * Provides an infinite distance.
   *
   * @return an infinite distance
   */
  public D infiniteDistance() {
    return distanceFunction.infiniteDistance();
  }

  /**
   * Provides a null distance.
   *
   * @return a null distance
   */
  public D nullDistance() {
    return distanceFunction.nullDistance();
  }

  /**
   * Provides an undefined distance.
   *
   * @return an undefined distance
   */
  public D undefinedDistance() {
    return distanceFunction.undefinedDistance();
  }

  /**
   * Computes the distance between two given MetricalObjects according to this
   * distance function.
   *
   * @param o1 first MetricalObject
   * @param o2 second MetricalObject
   * @return the distance between two given MetricalObjects according to this
   *         distance function
   */
  public D distance(O o1, O o2) {
    return root.distance(o1, o2);
  }

  /**
   * Returns a description of the class and the required parameters.
   * <p/>
   * This description should be suitable for a usage description.
   *
   * @return String a description of the class and the required parameters
   *         // todo
   */
  public String description() {
    return "Distance function for multirepresented objects.";
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);

    String prefix = optionHandler.getOptionValue(TREE_P);
    root = parsePrefix(prefix);

//    System.out.println("ROOT: " + root);

    return remainingParameters;
  }

  /**
   * Returns the setting of the attributes of the parameterizable.
   *
   * @return the setting of the attributes of the parameterizable
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();

    AttributeSettings settings = result.get(0);
    settings.addSetting(TREE_P, root.toString());

    result.addAll(distanceFunction.getAttributeSettings());
    return result;
  }

  /**
   * Test, ob der Buchstabe c ein erlaubter Operator ist.
   */
  private static boolean isOperator(String c) {
    return OPERATOR_PATTERN.matcher(c).matches();
  }

  /**
   * Test, ob der Buchstabe c ein erlaubter Operand ist.
   */
  private static boolean isOperand(String c) {
    return OPERAND_PATTERN.matcher(c).matches();
  }

  /**
   * Transformiert einen arithmetischen Ausdruck von Praefix-
   * in Postfix-Notation. Erlaubte Operatoren sind +, -, *, /
   * und erlaubte Operanden sind alle Kleinbuchstaben.
   */
  public CombinationTreeNode<M,O,D> parsePrefix(String prefixExpression) {
    String pre = removeOuterParentheses(prefixExpression);

    if (OPERAND_PATTERN.matcher(pre).matches()){
      return parseOperand(pre);
    }

    char operator = pre.charAt(0);
    if (! OPERATOR_PATTERN.matcher(""+operator).matches()) {
      throw new IllegalArgumentException("First expression in " + prefixExpression + " is neither a operand nor an operator");
    }

    pre = pre.substring(1);
    pre = removeOuterParentheses(pre);
    String[] operands = operands(pre);

    Operator<M,O,D> node = parseOperator(operator);

    CombinationTreeNode<M,O,D> leftChild = parsePrefix(operands[0]);
    CombinationTreeNode<M,O,D> rightChild = parsePrefix(operands[1]);

    node.addLeftChild(leftChild);
    node.addRightChild(rightChild);

    return node;
  }

  private String removeOuterParentheses(String s) {
    String result;
    if (s.charAt(0) == LEFT_PARENTHESIS) {
      if (s.charAt(s.length()-1) != RIGHT_PARENTHESIS)
        throw new IllegalArgumentException(s + " starts with " + LEFT_PARENTHESIS +
                                           ", but " + RIGHT_PARENTHESIS + " is missing");
      result = s.substring(1, s.length() - 1);
    }
    else result = s;
    return result;
  }

  private String[] operands(String s) {
    int lp = 0;
    int rp = 0;
    int index = -1;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == LEFT_PARENTHESIS) lp++;
      else if (c == RIGHT_PARENTHESIS) rp++;
      else if (c == OPERATOR_SEPARATOR) {
        if (lp == rp) {
          index = i;
          break;
        }
      }
    }
    if (index == -1) throw new IllegalArgumentException(s + " contains no operands!");
    return new String[] {s.substring(0, index), s.substring(index+1, s.length())};
  }

  private Representation<M,O,D> parseOperand(String operand) {
    if (! OPERAND_PATTERN.matcher(operand).matches())
      throw new IllegalArgumentException(operand + " is no operand!");

    String[] split = OPERAND_SPLIT.split(operand);

    int index = Integer.parseInt(split[1]);

    String distanceFunctionClass = split.length == 3? split[2] : DEFAULT_DISTANCE_FUNCTION;
    //noinspection unchecked
    DistanceFunction<M,D> distanceFunction = Util.instantiate(DistanceFunction.class, distanceFunctionClass);

    if (this.distanceFunction == null) this.distanceFunction = distanceFunction;

    return new Representation<M,O,D>(distanceFunction, index-1);
  }

  private Operator<M,O,D> parseOperator(char operator) {
    if (operator == UNION_OPERATOR)
      return new Operator<M,O,D>(Operator.UNION);

    if (operator == INTERSECTION_OPERATOR)
      return new Operator<M,O,D>(Operator.INTERSECTION);

    throw new IllegalArgumentException(operator + " is no operator!");
  }

  public static List<String> preToPostfix1(String prefixExpression) {
    // operator stack
    Stack<String> stack = new Stack<String>();

    // marks operators on the stack of which the first argument is already transformed
    final String flag = ".";
    stack.push(flag);
    List<String> postfix = new ArrayList<String>();

    // remove paranthesis and commas
    String pre = prefixExpression.replace('(', ' ');
    pre = pre.replace(')', ' ');
    pre = pre.replace(',', ' ');
    String[] prefix = OPERATOR_SPLIT.split(pre);

    prefixExpression = "";
    for (int i = 0; i <prefix.length; i++) {
      if (i != 0) prefixExpression += " " + prefix[i];
      else prefixExpression += prefix[i];
    }

    // parse
    for (int i = 0; i < prefix.length; i++) {
      String c = prefix[i];
      if (isOperator(c)) {
        stack.push(c);
      }
      else if (isOperand(c)) {
        postfix.add(c);
        while (stack.peek().equals(flag)) {
          stack.pop();
          if (stack.isEmpty()) {
            // input was correct
            if (i == prefix.length - 1) {
              return postfix;
            }
            // input was not correct
            else  {
              throw new IllegalArgumentException("Operator is missing: " + prefixExpression);
            }
          }
          // add operator to output
          postfix.add(stack.pop());
        }
        // mark the operator
        stack.push(flag);
      }
      else throw new IllegalArgumentException(c + " is neither operator nor operand.");
    }
    // input was not correct
    throw new IllegalArgumentException("Operator is missing: " + prefixExpression);
  }
}
