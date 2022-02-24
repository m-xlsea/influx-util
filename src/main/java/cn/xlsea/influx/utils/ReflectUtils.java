package cn.xlsea.influx.utils;

import cn.xlsea.influx.annotation.TableField;
import cn.xlsea.influx.annotation.Tag;
import org.nutz.lang.Lang;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 反射工具类
 *
 * @author 花花
 * @since 2022-02-23
 */
public class ReflectUtils {

    /**
     * 获取类中使用了某个注解的变量
     *
     * @param object     对象实体类
     * @param annotation 注解
     * @return String 变量名
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

    /**
     * 类中获取使用了某个注解的多个变量
     *
     * @param object     对象实体类
     * @param annotation 注解
     * @return List<String> 变量名集合
     */
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
     * 获取@TableField注解的value值
     *
     * @param obj 对象实体类
     * @param str 变量名
     * @return String @TableField注解的value值
     */
    public static String getTableFieldValue(Object obj, String str) {
        Object first = Lang.first(obj);
        Field[] list = first.getClass().getDeclaredFields();
        String value = null;
        for (Field field : list) {
            if (field.isAnnotationPresent(TableField.class)) {
                for (Annotation annotation : field.getDeclaredAnnotations()) {
                    if (annotation.annotationType().equals(TableField.class)) {
                        if (field.getName().equals(str)) {
                            TableField alias = (TableField) annotation;
                            value = alias.value();
                        }
                    }
                }
            }
        }
        return value;
    }

    /**
     * 获取@TableField注解的exist值
     *
     * @param obj 对象实体类
     * @param str 变量名
     * @return boolean @TableField注解的exist值
     */
    public static boolean getTableFieldExist(Object obj, String str) {
        Object first = Lang.first(obj);
        Field[] list = first.getClass().getDeclaredFields();
        boolean exist = true;
        for (Field field : list) {
            if (field.isAnnotationPresent(TableField.class)) {
                for (Annotation annotation : field.getDeclaredAnnotations()) {
                    if (annotation.annotationType().equals(TableField.class)) {
                        if (field.getName().equals(str)) {
                            TableField alias = (TableField) annotation;
                            exist = alias.exist();
                        }
                    }
                }
            }
        }
        return exist;
    }

    /**
     * 获取@Tag注解的value值
     *
     * @param obj 对象实体类
     * @param str 变量名
     * @return String @Tag注解的value值
     */
    public static String getTagValue(Object obj, String str) {
        Object first = Lang.first(obj);
        Field[] list = first.getClass().getDeclaredFields();
        String value = null;
        for (Field field : list) {
            if (field.isAnnotationPresent(Tag.class)) {
                for (Annotation annotation : field.getDeclaredAnnotations()) {
                    if (annotation.annotationType().equals(Tag.class)) {
                        if (field.getName().equals(str)) {
                            Tag alias = (Tag) annotation;
                            value = alias.value();
                        }
                    }
                }
            }
        }
        return value;
    }

}