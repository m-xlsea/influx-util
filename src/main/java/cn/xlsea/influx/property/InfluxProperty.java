package cn.xlsea.influx.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 获取配置文件里数据库名称
 *
 * @author 花花
 * @since 2022-02-23
 */
@Component
@ConfigurationProperties(prefix = "spring.influx")
public class InfluxProperty {

    private String dataBaseName;    // 数据库名

    public InfluxProperty() {
    }

    public InfluxProperty(String dataBaseName) {
        this.dataBaseName = dataBaseName;
    }

    public String getDataBaseName() {
        return dataBaseName;
    }

    public void setDataBaseName(String dataBaseName) {
        this.dataBaseName = dataBaseName;
    }
}