package cn.xlsea.influx.utils;

import cn.xlsea.influx.annotation.Alias;
import org.nutz.lang.Lang;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 反射工具类
 */
public class ReflectUtils {

    /**
     * 类中获取使用了某个注解的字段
     */
    public static String getField(Object object, Class<? extends Annotation> annotation) {
        Object first = Lang.first(object);
        Class<?> clazz = first.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            boolean isAnon = field.isAnnotationPresent(annotation);
            if (isAnon) {
                return field.getName();
            }
        }
        return null;
    }

    public static List<String> getFields(Object object, Class<? extends Annotation> annotation) {
        Object first = Lang.first(object);
        Class<?> clazz = first.getClass();
        Field[] fields = clazz.getDeclaredFields();
        List<String> names = new ArrayList<>();
        for (Field field : fields) {
            field.setAccessible(true);
            boolean isAnon = field.isAnnotationPresent(annotation);
            if (isAnon) {
                names.add(field.getName());
            }
        }
        return names;
    }

    /**
     * 获取注解的value
     */
    public static String getAliasValue(Object obj, String str) {
        Object first = Lang.first(obj);
        Field[] list = first.getClass().getDeclaredFields();
        String value = null;
        for (Field field : list) {
            if (field.isAnnotationPresent(Alias.class)) {
                for (Annotation annotation : field.getDeclaredAnnotations()) {
                    if (annotation.annotationType().equals(Alias.class)) {
                        if (field.getName().equals(str)) {
                            Alias alias = (Alias) annotation;
                            value = alias.value();
                        }
                    }
                }
            }
        }
        return value;
    }

}