package cn.xlsea.influx.dao;

import java.util.List;

public interface InfluxDao {

    /**
     * 测试连接是否正常
     */
    Boolean ping();

    /**
     * 创建数据库
     * 说明：方法参数没有指定时，默认使用配置文件中数据库名
     */
    String createDataBase(String... dataBaseName);

    /**
     * 删除数据库
     * 说明：方法参数没有指定时，默认使用配置文件中数据库名
     */
    String deleteDataBase(String... dataBaseName);

    /**
     * 插入数据
     * 支持：对象,集合(集合时对应实体类必须使用@Tag注解指定一个字段)
     */
    <T> int insert(T object,String... str);

    /**
     * 查询数据
     */
    <T> List<T> query(Class<T> clazz, String sql, String... str);

}