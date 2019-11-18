package info.kfgodel.diamond;

import ar.com.kfgodel.diamond.api.Diamond;
import ar.com.kfgodel.diamond.api.fields.TypeField;
import info.kfgodel.diamond.objects.FieldAccessorTestObject;
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
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class FieldAccessBenchmark {

  private static final String FIELD_NAME = "publicField";

  private final FieldAccessorTestObject object = new FieldAccessorTestObject();
  private final Field field = getField();

  private final MethodHandle getterHandle = findGetterHandle();
  private final MethodHandle setterHandle = findSetterHandle();

  private final MethodHandle unreflectedGetter = findUnreflectedGetterHandle();
  private final MethodHandle unreflectedSetter = findUnreflectedSetterHandle();

  private final MethodHandle convertedGetter = unreflectedGetter.asType(MethodType.methodType(Integer.class, Object.class));
  private final MethodHandle convertedSetter = unreflectedSetter.asType(MethodType.methodType(void.class, Object.class, Object.class));

  private final TypeField diamondField = Diamond.of(FieldAccessorTestObject.class).fields().named(FIELD_NAME).get();


  @Benchmark
  public void noOpZero() {
    // This is a reference test so we know what the best case is on this machine
  }

  @Benchmark
  public void publicFieldDirectAccess() {
    object.publicField = object.publicField + 2;
  }

  @Benchmark
  public void usingGetterAndSetter() {
    object.setPublicField(object.getPublicField() + 2);
  }

  @Benchmark
  public void invokingInexactMethodHandles() {
    try {
      setterHandle.invoke(object, (int) getterHandle.invoke(object) + 2);
    } catch (Throwable e) {
      throw new RuntimeException("Unexpected test error", e);
    }
  }

  @Benchmark
  public void invokingExactMethodHandles() {
    try {
      setterHandle.invokeExact(object, (int) getterHandle.invokeExact(object) + 2);
    } catch (Throwable e) {
      throw new RuntimeException("Unexpected test error", e);
    }
  }

  @Benchmark
  public void invokingExactUnreflectedMethodHandles() {
    try {
      unreflectedSetter.invokeExact(object, (int) unreflectedGetter.invokeExact(object) + 2);
    } catch (Throwable e) {
      throw new RuntimeException("Unexpected test error", e);
    }
  }

  @Benchmark
  public void usingNativeReflection() {
    try {
      field.set(object, (Integer) field.get(object) + 2);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Unexpected test error", e);
    }
  }

  @Benchmark
  public void usingDiamondReflection() {
    diamondField.setValueOn(object, diamondField.<Integer>getValueFrom(object) + 2);
  }

  @Benchmark
  public void usingConvertedUnreflectedMethodHandles() {
    try {
      Object i = (Integer) convertedGetter.invokeExact(object) + 2;
      convertedSetter.invokeExact(object, i);
    } catch (Throwable e) {
      throw new RuntimeException("Unexpected test error", e);
    }
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

  private MethodHandle findUnreflectedGetterHandle() {
    try {
      return MethodHandles.lookup().unreflectGetter(field);
    } catch (Exception e) {
      throw new RuntimeException("Unexpected error finding getter: " + e.getMessage(), e);
    }
  }

  private MethodHandle findSetterHandle() {
    try {
      return MethodHandles.lookup().findSetter(FieldAccessorTestObject.class, FIELD_NAME, int.class);
    } catch (Exception e) {
      throw new RuntimeException("Unexpected error finding setter: " + e.getMessage(), e);
    }
  }

  private MethodHandle findUnreflectedSetterHandle() {
    try {
      return MethodHandles.lookup().unreflectSetter(field);
    } catch (Exception e) {
      throw new RuntimeException("Unexpected error finding getter: " + e.getMessage(), e);
    }
  }

  //------

  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
      .include(FieldAccessBenchmark.class.getSimpleName())
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
