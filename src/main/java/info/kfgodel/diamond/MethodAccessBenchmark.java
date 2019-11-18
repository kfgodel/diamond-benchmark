package info.kfgodel.diamond;

import ar.com.kfgodel.diamond.api.Diamond;
import ar.com.kfgodel.diamond.api.methods.TypeMethod;
import ar.com.kfgodel.diamond.unit.testobjects.accessors.FieldAccessorTestObject;
import ar.com.kfgodel.diamond.unit.testobjects.accessors.MethodInvocationTestObject;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class MethodAccessBenchmark {

  private static final String FIELD_NAME = "publicField";

  private final MethodInvocationTestObject object = new MethodInvocationTestObject();


  private final Method nativeMethodA = getDeclaredMethod("methodA");
  private final Method nativeMethodB = getDeclaredMethod("methodB");
  private final Method nativeMethodC = getDeclaredMethod("methodC");
  private final Method nativeMethodD = getDeclaredMethod("methodD");

  private final MethodHandle unreflectedMethodA = unreflect(nativeMethodA);
  private final MethodHandle unreflectedMethodB = unreflect(nativeMethodB);
  private final MethodHandle unreflectedMethodC = unreflect(nativeMethodC);
  private final MethodHandle unreflectedMethodD = unreflect(nativeMethodD);

  private final MethodHandle convertedMethodA = convertUnreflected(unreflectedMethodA);
  private final MethodHandle convertedMethodB = convertUnreflected(unreflectedMethodB);
  private final MethodHandle convertedMethodC = convertUnreflected(unreflectedMethodC);
  private final MethodHandle convertedMethodD = convertUnreflected(unreflectedMethodD);

  private final MethodHandle methodAHandle = getHandle("methodA", MethodInvocationTestObject.class);
  private final MethodHandle methodBHandle = getHandle("methodB",MethodInvocationTestObject.class);
  private final MethodHandle methodCHandle = getHandle("methodC",MethodInvocationTestObject.class);
  private final MethodHandle methodDHandle = getHandle("methodD", int.class);

  private final TypeMethod diamondMethodA = diamondOf("methodA");
  private final TypeMethod diamondMethodB = diamondOf("methodB");
  private final TypeMethod diamondMethodC = diamondOf("methodC");
  private final TypeMethod diamondMethodD = diamondOf("methodD");

  @Benchmark
  public void noOpZero() {
    // This is a reference test so we know what the best case is on this machine
  }

  @Benchmark
  public int directMethodAccess() {
    return object.methodA().methodB().methodC().methodD();
  }

  @Benchmark
  public int usingConvertedUnreflectedMethodHandles() {
    try {
      Object resultA = convertedMethodA.invokeExact(object);
      Object resultB = convertedMethodB.invokeExact(resultA);
      Object resultC = convertedMethodC.invokeExact(resultB);
      Object resultD = convertedMethodD.invokeExact(resultC);
      return ((Number) resultD).intValue();
    } catch (Throwable e) {
      throw new RuntimeException("Unexpected test error", e);
    }
  }

  @Benchmark
  public int invokingInexactMethodHandles() {
    try {
      Object resultA = methodAHandle.invoke(object);
      Object resultB = methodBHandle.invoke(resultA);
      Object resultC = methodCHandle.invoke(resultB);
      return ((int) methodDHandle.invoke(resultC));
    } catch (Throwable e) {
      throw new RuntimeException("Unexpected test error", e);
    }
  }

  @Benchmark
  public int invokingExactMethodHandles() {
    try {
      MethodInvocationTestObject resultA = (MethodInvocationTestObject) methodAHandle.invokeExact(object);
      MethodInvocationTestObject resultB = (MethodInvocationTestObject) methodBHandle.invokeExact(resultA);
      MethodInvocationTestObject resultC = (MethodInvocationTestObject) methodCHandle.invokeExact(resultB);
      return ((int) methodDHandle.invokeExact(resultC));
    } catch (Throwable e) {
      throw new RuntimeException("Unexpected test error", e);
    }
  }

  @Benchmark
  public int invokingExactUnreflectedMethodHandles() {
    try {
      MethodInvocationTestObject resultA = (MethodInvocationTestObject) unreflectedMethodA.invokeExact(object);
      MethodInvocationTestObject resultB = (MethodInvocationTestObject) unreflectedMethodB.invokeExact(resultA);
      MethodInvocationTestObject resultC = (MethodInvocationTestObject) unreflectedMethodC.invokeExact(resultB);
      return ((int) unreflectedMethodD.invokeExact(resultC));
    } catch (Throwable e) {
      throw new RuntimeException("Unexpected test error", e);
    }
  }

  @Benchmark
  public int usingNativeReflection() {
    try {
      Object resultA = nativeMethodA.invoke(object);
      Object resultB = nativeMethodB.invoke(resultA);
      Object resultC = nativeMethodC.invoke(resultB);
      return ((Number) nativeMethodD.invoke(resultC)).intValue();
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("Unexpected test error", e);
    }
  }

  @Benchmark
  public int usingDiamondReflection() {
    Object resultA = diamondMethodA.invokeOn(object);
    Object resultB = diamondMethodB.invokeOn(resultA);
    Object resultC = diamondMethodC.invokeOn(resultB);
    return ((Number) diamondMethodD.invokeOn(resultC)).intValue();
  }

  //------

  private static Field getField() {
    try {
      return FieldAccessorTestObject.class.getDeclaredField(FIELD_NAME);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException("Unexpected error accessing field: " + e.getMessage(), e);
    }
  }


  private static MethodHandle findGetterHandle() {
    try {
      return MethodHandles.lookup().findGetter(FieldAccessorTestObject.class, FIELD_NAME, int.class);
    } catch (Exception e) {
      throw new RuntimeException("Unexpected error finding getter: " + e.getMessage(), e);
    }
  }

  private static Method getDeclaredMethod(String methodName) {
    try {
      return MethodInvocationTestObject.class.getDeclaredMethod(methodName);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Unexpected error finding method: " + e.getMessage(), e);
    }
  }

  private MethodHandle convertUnreflected(MethodHandle methodhandle) {
    return methodhandle.asType(MethodType.methodType(Object.class, Object.class));
  }

  private MethodHandle unreflect(Method reflectedMethod) {
    try {
      return MethodHandles.lookup().unreflect(reflectedMethod);
    } catch (Exception e) {
      throw new RuntimeException("Unexpected error finding method: " + e.getMessage(), e);
    }
  }

  private MethodHandle getHandle(String methodA, Class<?> returnType)  {
    try {
      return MethodHandles.lookup().findVirtual(MethodInvocationTestObject.class, methodA, MethodType.methodType(returnType));
    } catch (Exception e) {
      throw new RuntimeException("Unexpected error finding method: " + e.getMessage(), e);
    }
  }

  private TypeMethod diamondOf(String methodA) {
    return Diamond.of(MethodInvocationTestObject.class).methods().named(methodA).get();
  }

  //------

  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
      .include(MethodAccessBenchmark.class.getSimpleName())
      .timeUnit(TimeUnit.MILLISECONDS)
      .warmupIterations(5)
      .warmupTime(TimeValue.seconds(1))
      .measurementIterations(5)
      .measurementTime(TimeValue.seconds(3))
      .forks(1)
      .build();
    new Runner(opt).run();
  }


}
