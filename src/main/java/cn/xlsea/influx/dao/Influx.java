package cn.xlsea.influx.dao;

import java.util.List;

/**
 * influx工具 dao层
 *
 * @author 花花
 * @since 2022-02-23
 */
public interface Influx {

    /**
     * 测试连接
     *
     * @return Boolean 成功返回true，失败返回false
     */
    Boolean ping();

    /**
     * 创建数据库
     *
     * @param dataBaseName 方法参数为空时，将使用配置文件中数据库名
     * @return String 返回创建后的数据库名称
     * @deprecated influx官方已经弃用
     */
    String createDataBase(String... dataBaseName);

    /**
     * 删除数据库
     *
     * @param dataBaseName 方法参数为空时，将使用配置文件中数据库名
     * @return String 返回创建后的数据库名称
     * @deprecated influx官方已经弃用
     */
    String deleteDataBase(String... dataBaseName);

    /**
     * 插入数据
     *
     * @param object 实体对象,对象集合(对应实体类必须使用@Tag注解指定一个字段)
     * @param str    表名后需要添加的文本
     * @return int 插入数据条数
     */
    <T> int save(T object, String... str);

    /**
     * 查询多条数据
     *
     * @param clazz 实体对象,对象集合(对应实体类必须使用@Tag注解指定一个字段)
     * @param sql   查询语句
     * @return List 返回对象集合
     */
    <T> List<T> list(Class<T> clazz, String sql);

    /**
     * 查询一条数据（如果集，默认返回第一条）
     *
     * @param clazz 实体对象,对象集合(对应实体类必须使用@Tag注解指定一个字段)
     * @param sql   查询语句
     * @return T 返回对象实体
     */
    <T> T getOne(Class<T> clazz, String sql);

}
