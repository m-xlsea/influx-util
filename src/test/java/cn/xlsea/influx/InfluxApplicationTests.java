package cn.xlsea.influx;

import cn.xlsea.influx.annotation.TableField;
import cn.xlsea.influx.annotation.Tag;
import cn.xlsea.influx.dao.Influx;
import org.influxdb.annotation.Measurement;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SpringBootTest
class InfluxApplicationTests {

    Logger log = LoggerFactory.getLogger(InfluxApplicationTests.class);

    @Resource
    private Influx influx;

    /**
     * 测试连接
     */
    @Test
    void testPing() {
        // 连接成功返回true，失败返回false
        Boolean ping = influx.ping();
        if (ping) {
            log.info("连接成功");
        } else {
            log.info("连接失败");
        }
    }

    /**
     * 判断数据库是否存在
     */
    @Test
    void testExists() {
        // 无参数则创建配置类里的数据库
        // Boolean exists = influx.exists();
        Boolean exists = influx.exists("utilTest");
        if (exists) {
            log.info("数据库存在");
        } else {
            log.info("数据库不存在");
        }
    }

    /**
     * 创建数据库
     */
    @Test
    void testCreateDataBase() {
        // 无参数则创建配置类里的数据库
        // String dataBase = influxDao.createDataBase();
        String dataBase = influx.createDataBase("utilTest");
        log.info(dataBase);
    }

    /**
     * 删除数据库
     */
    @Test
    void testDeleteDataBase() {
        // 无参数则删除配置类里的数据库
        // String dataBase = influxDao.deleteDataBase();
        String dataBase = influx.deleteDataBase("utilTest");
        log.info(dataBase);
    }

    /**
     * 插入数据
     */
    @Test
    void testSave() {
        List<User> users = new ArrayList<>();
        Date date = new Date();
        User user = new User("1", "张三", "男", 18, date, date);
        User user2 = new User("2", "李四", "女", 16, date, date);
        User user3 = new User("3", "王五", "男", 20, date, date);
        users.add(user);
        users.add(user2);
        users.add(user3);
        // 参数可以是实体对象，也可以是对象集合
        int insertOne = influx.save(user);
        int insertAll = influx.save(users);
        // 可以在表名后添加额外文本
        influx.save(user);
        log.info("影响行数：" + insertOne);
        log.info("影响行数：" + insertAll);
    }

    /**
     * 查询多条数据
     */
    @Test
    void testList() {
        // 查询语句
        String sql = "select * from userInfo";
        // 参数为需要返回的对象类型和语句
        List<User> query = influx.list(User.class, sql);
        log.info(query.toString());
    }

    /**
     * 查询单条数据
     */
    @Test
    void testGetOne() {
        // 查询语句
        String sql = "select * from userInfo limit 1";
        // 参数为需要返回的对象类型和语句
        User query = influx.getOne(User.class, sql);
        log.info(query.toString());
    }

    /**
     * 查询记录数
     */
    @Test
    void testCount() {
        // 查询语句
        String sql = "select count(age) from userInfo";
        // 参数为需要返回的对象类型和语句
        int query = influx.count(User.class, sql);
        log.info("共查到" + query + "条数据");
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
