package info.kfgodel.diamond.objects;

/**
 * This type serves as a test object for field accessors
 * Created by kfgodel on 23/10/14.
 */
public class FieldAccessorTestObject {

  private int privateField;
  int defaultField;
  protected int protectedField;
  public int publicField;

  public int getPrivateField() {
    return privateField;
  }

  public void setPrivateField(int privateField) {
    this.privateField = privateField;
  }

  public int getPublicField() {
    return publicField;
  }

  public int setPublicField(int publicField) {
    this.publicField = publicField;
    return this.publicField;
  }
}
