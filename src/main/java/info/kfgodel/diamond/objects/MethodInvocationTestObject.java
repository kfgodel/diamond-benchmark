package info.kfgodel.diamond.objects;

/**
 * This type serves for testing method invocations
 * Created by kfgodel on 25/10/14.
 */
public class MethodInvocationTestObject {

  public int publicMethod() {
    return 1;
  }

  protected int protectedMethod() {
    return 2;
  }

  int defaultMethod() {
    return 3;
  }

  private int privateMethod() {
    return 4;
  }

  public void exceptionMethod() {
    throw new RuntimeException("I don't finish successfully");
  }

  @Override
  public String toString() {
    return "a test instance";
  }

  public MethodInvocationTestObject methodA() {
    return this;
  }

  public MethodInvocationTestObject methodB() {
    return this;
  }

  public MethodInvocationTestObject methodC() {
    return this;
  }

  public int methodD() {
    return 4;
  }
}
