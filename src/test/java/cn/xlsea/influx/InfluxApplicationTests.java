package cn.xlsea.influx;

import cn.xlsea.influx.annotation.TableField;
import cn.xlsea.influx.annotation.Tag;
import cn.xlsea.influx.dao.Influx;
import org.influxdb.annotation.Measurement;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@SpringBootTest
class InfluxApplicationTests {

    @Resource
    private Influx influx;

    @Test
    void save() {
        User user = new User("111111", "张三", "男", 20, new Date(), new Date());
        int save = influx.save(user);
        System.out.println(save);
    }

    @Test
    void list() {
        String sql = "select * from userInfo";
        List<User> list = influx.list(User.class, sql);
        System.out.println(list);
    }

    @Measurement(name = "userInfo")
    public static class User {
        @Tag()
        private String id;
        @Tag("name")                      // 标识tag字段
        private String names;
        @TableField("gender")       // 字段名称
        private String sex;
        private Integer age;
        @TableField(exist = false)  // 不需要插入的字段
        private Date createTime;
        @TableField("time")         // time字段
        private Date updateTime;

        public User() {
        }

        public User(String id, String names, String sex, Integer age, Date createTime, Date updateTime) {
            this.id = id;
            this.names = names;
            this.sex = sex;
            this.age = age;
            this.createTime = createTime;
            this.updateTime = updateTime;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getNames() {
            return names;
        }

        public void setNames(String name) {
            this.names = name;
        }

        public String getSex() {
            return sex;
        }

        public void setSex(String sex) {
            this.sex = sex;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public Date getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Date createTime) {
            this.createTime = createTime;
        }

        public Date getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(Date updateTime) {
            this.updateTime = updateTime;
        }

        @Override
        public String toString() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String createTimeStr = createTime != null ? sdf.format(createTime) : null;
            String updateTimeStr = updateTime != null ? sdf.format(updateTime) : null;
            return "User{id=" + id + ", names=" + names + ", sex=" + sex + ", age=" + age + ", createTime=" + createTimeStr + ", updateTime=" + updateTimeStr + "}";
        }
    }


}
