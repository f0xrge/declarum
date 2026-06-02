package com.f0xrge.declarum.dfc.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public final class DfcReflection {

    private DfcReflection() {
    }

    public static Object newInstance(String className) {
        try {
            Class<?> type = Class.forName(className);
            Constructor<?> constructor = type.getDeclaredConstructor();
            return constructor.newInstance();
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("DFC class is not available on the classpath: " + className, exception);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to create DFC object: " + className, exception);
        }
    }

    public static Object newInstance(String className, Class<?> parameterType, Object parameterValue) {
        try {
            Class<?> type = Class.forName(className);
            Constructor<?> constructor = type.getConstructor(parameterType);
            return constructor.newInstance(parameterValue);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("DFC class is not available on the classpath: " + className, exception);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to create DFC object: " + className, exception);
        }
    }

    public static Object invoke(Object target, String methodName, Object... arguments) {
        Objects.requireNonNull(target, "target is required");
        try {
            Method method = findCompatibleMethod(target.getClass(), methodName, arguments);
            method.setAccessible(true);
            return method.invoke(target, arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("DFC method failed: " + methodName, cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to invoke DFC method: " + methodName, exception);
        }
    }

    public static Object staticField(String className, String fieldName) {
        try {
            Class<?> type = Class.forName(className);
            Field field = type.getField(fieldName);
            return field.get(null);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("DFC class is not available on the classpath: " + className, exception);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to read DFC field: " + className + "." + fieldName, exception);
        }
    }

    private static Method findCompatibleMethod(Class<?> type, String methodName, Object[] arguments) throws NoSuchMethodException {
        Class<?> currentType = type;
        while (currentType != null) {
            for (Method method : currentType.getMethods()) {
                if (method.getName().equals(methodName) && hasCompatibleParameters(method.getParameterTypes(), arguments)) {
                    return method;
                }
            }
            currentType = currentType.getSuperclass();
        }
        throw new NoSuchMethodException(type.getName() + "." + methodName);
    }

    private static boolean hasCompatibleParameters(Class<?>[] parameterTypes, Object[] arguments) {
        if (parameterTypes.length != arguments.length) {
            return false;
        }

        for (int index = 0; index < parameterTypes.length; index++) {
            Object argument = arguments[index];
            if (argument != null && !wrapPrimitive(parameterTypes[index]).isAssignableFrom(argument.getClass())) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> wrapPrimitive(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (boolean.class.equals(type)) {
            return Boolean.class;
        }
        if (int.class.equals(type)) {
            return Integer.class;
        }
        if (long.class.equals(type)) {
            return Long.class;
        }
        if (double.class.equals(type)) {
            return Double.class;
        }
        if (float.class.equals(type)) {
            return Float.class;
        }
        if (short.class.equals(type)) {
            return Short.class;
        }
        if (byte.class.equals(type)) {
            return Byte.class;
        }
        if (char.class.equals(type)) {
            return Character.class;
        }
        return type;
    }
}
