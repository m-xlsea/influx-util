package cn.xlsea.influx.dao.impl;

import cn.xlsea.influx.dao.Influx;
import cn.xlsea.influx.utils.ReflectUtils;
import cn.xlsea.influx.annotation.TableField;
import cn.xlsea.influx.annotation.Tag;
import cn.xlsea.influx.property.InfluxProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.annotation.Measurement;
import org.influxdb.dto.*;
import org.influxdb.dto.Point.Builder;
import org.influxdb.dto.QueryResult.Result;
import org.influxdb.dto.QueryResult.Series;
import org.nutz.json.Json;
import org.nutz.lang.Lang;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * influx工具 实现层
 *
 * @author 花花
 * @since 2022-02-23
 */
@Component
public class InfluxImpl implements Influx {

    private final Logger log = LoggerFactory.getLogger(Influx.class);

    @Resource
    private InfluxDB influxDB;

    @Resource
    private InfluxProperty influxProperty;

    @Override
    public Boolean ping() {
        boolean isConnected = false;
        Pong pong;
        try {
            pong = influxDB.ping();
            if (pong != null) {
                isConnected = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isConnected;
    }

    @Override
    public Boolean exists(String... arrays) {
        String dataBaseName = getDataBaseName(arrays);
        if (dataBaseName == null) {
            throw new RuntimeException("如参数不指定数据库名,配置文件 spring.influx.dataBaseName 必须指定");
        }
        return influxDB.databaseExists(dataBaseName);
    }

    @Override
    public String createDataBase(String... arrays) {
        String dataBaseName = getDataBaseName(arrays);
        if (dataBaseName == null) {
            throw new RuntimeException("如参数不指定数据库名,配置文件 spring.influx.dataBaseName 必须指定");
        }
        influxDB.createDatabase(dataBaseName);
        log.debug("创建数据库" + dataBaseName + "成功");
        return dataBaseName;
    }

    @Override
    public String deleteDataBase(String... arrays) {
        String dataBaseName = getDataBaseName(arrays);
        isExists(dataBaseName);
        influxDB.deleteDatabase(dataBaseName);
        log.debug("删除数据库 " + dataBaseName + " 成功！");
        return dataBaseName;
    }

    @Override
    public <T> int save(T object, String... arrays) {
        String dataBaseName = influxProperty.getDataBaseName();
        isExists(dataBaseName);
        String str = "";
        if (arrays.length != 0) {
            StringBuilder sb = new StringBuilder();
            for (String string : arrays) {
                sb.append(string);
            }
            str = String.valueOf(sb);
        }
        // 构建一个Entity
        Object first = Lang.first(object);
        Class<?> clazz = first.getClass();
        // 表名
        boolean isAnnotation = clazz.isAnnotationPresent(Measurement.class);
        if (!isAnnotation) {
            throw new RuntimeException("插入的数据对应实体类需要@Measurement注解");
        }
        Measurement annotation = clazz.getAnnotation(Measurement.class);
        // 表名
        String measurement = annotation.name() + str;
        Field[] arrfield = clazz.getDeclaredFields();
        // 数据长度
        int size = Lang.eleSize(object);
        String tagField = ReflectUtils.getField(object, Tag.class);
        if (tagField == null) {
            throw new RuntimeException("插入多条数据需对应实体类字段有@Tag注解");
        }
        BatchPoints batchPoints = BatchPoints
                .database(dataBaseName)
                // 一致性
                .consistency(ConsistencyLevel.ALL)
                .build();
        int count = 0;
        for (int i = 0; i < size; i++) {
            count++;
            Map<String, Object> map = new HashMap<>();
            Builder builder = Point.measurement(measurement);
            for (Field field : arrfield) {
                // 私有属性需要开启
                field.setAccessible(true);
                Object result = first;
                try {
                    if (size > 1) {
                        List<?> objects = (List<?>) (object);
                        result = objects.get(i);
                    }
                    if (field.getName().equals(tagField)) {
                        String tagValue = ReflectUtils.getTagValue(object, field.getName());
                        if (tagValue != null && !tagValue.equals("")) {
                            builder.tag(tagValue, String.valueOf(field.get(result)));
                        } else {
                            builder.tag(tagField, String.valueOf(field.get(result)));
                        }
                    } else if (field.getName().equals("time")) {
                        builder.time(Long.parseLong(String.valueOf(field.get(result))), TimeUnit.MILLISECONDS);
                    } else {
                        boolean tableFieldExist = ReflectUtils.getTableFieldExist(object, field.getName());
                        if (tableFieldExist) {
                            String aliasValue = ReflectUtils.getTableFieldValue(object, field.getName());
                            if (aliasValue != null && !aliasValue.equals("")) {
                                if (aliasValue.equals("time")) {
                                    builder.time(Long.parseLong(String.valueOf(field.get(result))), TimeUnit.MILLISECONDS);
                                } else {
                                    map.put(aliasValue, field.get(result));
                                }
                            } else {
                                map.put(field.getName(), field.get(result));
                            }
                        }
                    }
                } catch (IllegalAccessException e) {
                    log.error("实体转换出错");
                    e.printStackTrace();
                }
            }
            builder.fields(map);
            batchPoints.point(builder.build());
        }
        influxDB.write(batchPoints);
        return count;
    }

    @Override
    public <T> List<T> list(Class<T> clazz, String sql) {
        String dataBaseName = influxProperty.getDataBaseName();
        if (dataBaseName == null) {
            throw new RuntimeException("查询数据时配置文件 spring.influx.dataBaseName 必须指定");
        }
        boolean exists = influxDB.databaseExists(dataBaseName);
        if (!exists) {
            throw new RuntimeException("数据库 " + dataBaseName + " 不存在，请检查此数据库是否存在");
        }
        QueryResult results = influxDB.query(new Query(sql, dataBaseName), TimeUnit.MILLISECONDS);
        if (results != null) {
            if (results.getResults() == null) {
                return null;
            }
            List<Object> list = new ArrayList<>();

            for (Result result : results.getResults()) {
                List<Series> series = result.getSeries();
                if (series == null) {
                    continue;
                }
                for (Series serie : series) {
                    List<List<Object>> values = serie.getValues();
                    for (List<Object> value : values) {
                        Double time = (Double) value.get(0);
                        value.set(0, time.longValue());
                    }
                    List<String> columns = serie.getColumns();
                    // 构建Bean
                    list.addAll(getQueryData(clazz, columns, values));
                }
            }
            log.debug("sql -----> " + sql);
            return Json.fromJsonAsList(clazz, Json.toJson(list));
        }
        return null;
    }

    @Override
    public <T> T getOne(Class<T> clazz, String sql) {
        List<T> list = list(clazz, sql);
        if (list.size() == 1) {
            return list.get(0);
        } else if (list.size() > 1) {
            log.warn("查询结果大于一条");
            return list.get(0);
        } else {
            return null;
        }
    }

    @Override
    public <T> int count(Class<T> clazz, String sql) {
        String dataBaseName = influxProperty.getDataBaseName();
        isExists(dataBaseName);
        QueryResult results = influxDB.query(new Query(sql, dataBaseName), TimeUnit.MILLISECONDS);
        if (results == null) {
            log.debug("没有查询到记录");
            return 0;
        }
        if (results.hasError()) {
            throw new RuntimeException(results.getError());
        }
        int count;
        try {
            Double double1 = (Double) (results.getResults().get(0).getSeries().get(0).getValues().get(0).get(1));
            count = double1.intValue();
            return count;
        } catch (Exception e) {
            // TODO: handle exception
            log.error("请检查您的语句是否正确");
            throw new RuntimeException("Not Found Records.");
        }
    }

    /**
     * 获取数据库名称
     */
    private String getDataBaseName(String... arrays) {
        String dataBaseName = influxProperty.getDataBaseName();
        if (arrays.length != 0) {
            StringBuilder sb = new StringBuilder();
            for (String string : arrays) {
                sb.append(string);
            }
            dataBaseName = String.valueOf(sb);
        }
        return dataBaseName;
    }

    /**
     * 判断是否存在数据库
     */
    private void isExists(String dataBaseName) {
        if (dataBaseName == null) {
            throw new RuntimeException("查询数据时配置文件 spring.influx.dataBaseName 必须指定");
        } else {
            boolean exists = influxDB.databaseExists(dataBaseName);
            if (!exists) {
                throw new RuntimeException("数据库 " + dataBaseName + " 不存在，请检查此数据库是否存在");
            }
        }
    }

    /**
     * 自动转换对应Pojo
     */
    private <T> List<T> getQueryData(Class<T> clazz, List<String> columns, List<List<Object>> values) {
        List<T> results = new ArrayList<>();
        for (List<Object> list : values) {
            BeanWrapperImpl bean = null;
            T result = null;
            try {
                result = clazz.newInstance();
                bean = new BeanWrapperImpl(result);
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < list.size(); i++) {
                // 字段名
                String filedName = columns.get(i);
                if (filedName.equals("Tag")) {
                    continue;
                }
                List<String> fields = ReflectUtils.getFields(result, TableField.class);
                for (String field : fields) {
                    String aliasValue = ReflectUtils.getTableFieldValue(result, field);
                    if (filedName.equals(aliasValue)) {
                        filedName = field;
                    }
                }
                List<String> tags = ReflectUtils.getFields(result, Tag.class);
                for (String tag : tags) {
                    String tagValue = ReflectUtils.getTagValue(result, tag);
                    if (filedName.equals(tagValue)) {
                        filedName = tag;
                    }
                }
                try {
                    clazz.getDeclaredField(filedName);
                } catch (NoSuchFieldException e) {
                    continue;
                }
                // 值
                Object value = list.get(i);
                if (bean != null) {
                    bean.setPropertyValue(filedName, value);
                }
            }
            results.add(result);
        }
        return results;
    }

}