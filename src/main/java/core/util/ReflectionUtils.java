package core.util;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReflectionUtils {

    private static final Logger logger = LoggerFactory.getLogger(ReflectionUtils.class);

    public static Constructor<?> getNoArgsConstructor(Class<?> clazz) {
        return Arrays.stream(clazz.getConstructors())
                .filter(constructor -> constructor.getParameterTypes().length == 0)
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    public static <T> T newInstance(Class<T> clazz, Object... args) {

        Constructor constructor = getConstructorByArgs(clazz, args);

        if (constructor == null) {
            throw new IllegalArgumentException(clazz.getSimpleName() + " doesn't have args size constructor");
        }

        try {
            return clazz.cast(constructor.newInstance(args));
        } catch (IllegalAccessException e) {
            logger.warn("{} constructor access failed", constructor.getName());
        } catch (InvocationTargetException e) {
            logger.warn("{} target invalid", clazz.getSimpleName());
        } catch (InstantiationException e) {
            logger.warn("{} instantiation failed", clazz.getSimpleName());
        }

        throw new RuntimeException(clazz.getSimpleName() + " instantiation failed");
    }

    public static Constructor getConstructorByArgs(Class clazz, Object... args) {
        for (Constructor candidate : clazz.getConstructors()) {
            if (isMatched(candidate, args)) {
                return candidate;
            }
        }

        return null;
    }

    public static boolean isMatched(Constructor constructor, Object... args) {

        if (constructor.getParameterCount() != args.length) {
            return false;
        }

        final Class[] parameterTypes = constructor.getParameterTypes();
        for (int i = 0; i < args.length; i++) {
            Class<?> parameterClass = parameterTypes[i];
            Class<?> argClass = args[i].getClass();

            if (parameterClass.equals(argClass) || Arrays.asList(argClass.getInterfaces()).contains(parameterClass)) {
                return true;
            }
        }

        return false;
    }

    public static Object convertStringValue(String value, Class<?> clazz) {
        if (clazz == String.class) {
            return clazz.cast(value);
        } else if (clazz == int.class || clazz == Integer.class) {
            return Integer.valueOf(value);
        } else if (clazz == long.class || clazz == Long.class) {
            return Long.valueOf(value);
        } else if (clazz == float.class || clazz == Float.class) {
            return Float.valueOf(value);
        } else if (clazz == double.class || clazz == Double.class) {
            return Double.valueOf(value);
        }

        throw new IllegalArgumentException(clazz.getTypeName() + " is not supported");
    }

    public static boolean hasFieldMethod(Class<?> clazz, String methodName, Class<?> type) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterTypes()[0] == type) {
                return true;
            }
        }
        return false;
    }

    public static Set<Class<?>> getPreInstantiatedBeansWithAnnotations(List<Class<? extends Annotation>> targetAnnotations, String... basePackage) {
        Reflections reflections = new Reflections(basePackage, Scanners.TypesAnnotated, Scanners.SubTypes, Scanners.MethodsAnnotated);

        Set<Class<?>> preInstantiatedBeans = new HashSet<>();
        for (Class<? extends Annotation> targetAnnotation : targetAnnotations) {
            Set<Class<?>> targetBeanClasses = reflections.getTypesAnnotatedWith(targetAnnotation);
            preInstantiatedBeans.addAll(targetBeanClasses);
        }

        return preInstantiatedBeans;
    }

}
