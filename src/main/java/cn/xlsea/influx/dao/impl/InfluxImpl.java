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
    public String createDataBase(String... dataBaseName) {
        if (dataBaseName.length > 0) {
            influxDB.createDatabase(dataBaseName[0]);
            log.info("创建数据库" + dataBaseName[0] + "成功");
            return dataBaseName[0];
        }
        if (influxProperty.getDataBaseName() == null) {
            log.error("如参数不指定数据库名,配置文件 spring.influx.dataBaseName 必须指定");
            return null;
        }
        influxDB.createDatabase(influxProperty.getDataBaseName());
        log.info("创建数据库" + influxProperty.getDataBaseName() + "成功");
        return influxProperty.getDataBaseName();
    }

    @Override
    public String deleteDataBase(String... dataBaseName) {
        if (dataBaseName.length > 0) {
            influxDB.deleteDatabase(dataBaseName[0]);
            log.info("删除数据库 " + dataBaseName[0] + " 成功！");
            return dataBaseName[0];
        }
        if (influxProperty.getDataBaseName() == null) {
            log.error("如参数不指定数据库名,配置文件 spring.influx.dataBaseName 必须指定");
            return null;
        }
        influxDB.deleteDatabase(influxProperty.getDataBaseName());
        log.info("删除数据库 " + influxProperty.getDataBaseName() + " 成功！");
        return influxProperty.getDataBaseName();
    }

    @Override
    public <T> int save(T object, String... arrays) {
        String str = arrays.length == 0 ? "" : Arrays.toString(arrays);
        // 构建一个Entity
        Object first = Lang.first(object);
        Class<?> clazz = first.getClass();
        // 表名
        boolean isAnnotation = clazz.isAnnotationPresent(Measurement.class);
        if (!isAnnotation) {
            log.error("插入的数据对应实体类需要@Measurement注解");
            return 0;
        }
        Measurement annotation = clazz.getAnnotation(Measurement.class);
        // 表名
        String measurement = annotation.name() + str;
        Field[] arrfield = clazz.getDeclaredFields();
        // 数据长度
        int size = Lang.eleSize(object);
        String tagField = ReflectUtils.getField(object, Tag.class);
        if (tagField == null) {
            log.error("插入多条数据需对应实体类字段有@Tag注解");
            return 0;
        }
        BatchPoints batchPoints = BatchPoints
                .database(influxProperty.getDataBaseName())
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
                        if (tagValue != null) {
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
                            if (aliasValue != null) {
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
    public <T> List<T> list(Class<T> clazz, String sql, String... arrays) {
        String str = arrays.length == 0 ? "" : Arrays.toString(arrays);
        if (influxProperty.getDataBaseName() == null) {
            log.error("查询数据时配置文件 spring.influx.dataBaseName 必须指定");
            return null;
        }
        QueryResult results = influxDB.query(new Query(sql, influxProperty.getDataBaseName() + str), TimeUnit.MILLISECONDS);
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
    public <T> T getOne(Class<T> clazz, String sql, String... str) {
        List<T> list = list(clazz, sql, str);
        if (list.size() != 0) {
            return list.get(0);
        } else {
            return null;
        }
    }

    /**
     * 自动转换对应Pojo
     */
    public <T> List<T> getQueryData(Class<T> clazz, List<String> columns, List<List<Object>> values) {
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