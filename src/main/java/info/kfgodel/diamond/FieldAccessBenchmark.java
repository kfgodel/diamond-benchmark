package info.kfgodel.diamond;

import ar.com.kfgodel.diamond.api.Diamond;
import ar.com.kfgodel.diamond.api.fields.TypeField;
import info.kfgodel.diamond.objects.FieldAccessorTestObject;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
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
  public int publicFieldDirectAccess() {
    return object.publicField = object.publicField + 2;
  }

  @Benchmark
  public int usingGetterAndSetter() {
    return object.setPublicField(object.getPublicField() + 2);
  }

  @Benchmark
  public Object invokingInexactMethodHandles() {
    try {
      return setterHandle.invoke(object, (int) getterHandle.invoke(object) + 2);
    } catch (Throwable e) {
      throw new RuntimeException("Unexpected test error", e);
    }
  }

  @Benchmark
  public Object invokingExactMethodHandles() {
    try {
      final int value = (int) getterHandle.invokeExact(object) + 2;
      setterHandle.invokeExact(object, value);
      return value;
    } catch (Throwable e) {
      throw new RuntimeException("Unexpected test error", e);
    }
  }

  @Benchmark
  public Object invokingExactUnreflectedMethodHandles() {
    try {
      final int value = (int) unreflectedGetter.invokeExact(object) + 2;
      unreflectedSetter.invokeExact(object, value);
      return value;
    } catch (Throwable e) {
      throw new RuntimeException("Unexpected test error", e);
    }
  }

  @Benchmark
  public int usingNativeReflection() {
    try {
      final int value = (Integer) field.get(object) + 2;
      field.set(object, value);
      return value;
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Unexpected test error", e);
    }
  }

  @Benchmark
  public int usingDiamondReflection() {
    final int value = diamondField.<Integer>getValueFrom(object) + 2;
    diamondField.setValueOn(object, value);
    return value;
  }

  @Benchmark
  public Object usingConvertedUnreflectedMethodHandles() {
    try {
      Object i = (Integer) convertedGetter.invokeExact((Object)object) + 2;
      convertedSetter.invokeExact((Object)object, i);
      return i;
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
      .mode(Mode.AverageTime)
      .timeUnit(TimeUnit.MILLISECONDS)
      .warmupIterations(10)
      .warmupTime(TimeValue.seconds(1))
      .warmupBatchSize(1_000_000)
      .measurementIterations(5)
      .measurementBatchSize(1_000_000)
      .measurementTime(TimeValue.seconds(5))
      .forks(1)
      .build();
    new Runner(opt).run();
  }


}
