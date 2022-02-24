package cn.xlsea.influx.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 数据库
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