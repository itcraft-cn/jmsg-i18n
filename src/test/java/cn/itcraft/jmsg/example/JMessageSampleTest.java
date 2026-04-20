package demo.i18n;

import cn.itcraft.jmsg.builder.MsgTemplateBuilder;
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.MsgTemplateConfig;
import cn.itcraft.jmsg.loader.FileMsgTemplateLoader;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author Helly Guo
 * <p>
 * Created on 2026-04-20 16:53
 */
public class JMessageSampleTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMessageSampleTest.class);

    @Test
    public void test() {
        // 初始化配置
        MsgTemplateConfig.setDefaultLocale(Locale.CHINA);
        FileMsgTemplateLoader.forSimple("simple.properties").load(MsgTemplate.class, null);
        FileMsgTemplateLoader.forNamed("named.properties").load(MsgTemplate.class, null);

        // Simple风格 - {} 占位符
        String msg = MsgTemplateBuilder.create()
                                       .code("ERR_001")
                                       .simple()
                                       .args("数据库超时")
                                       .render();
        // 结果: "内部错误:数据库超时"
        LOGGER.info("i18n msg: {}", msg);

        // Named风格 - Map参数
        Map<String, Object> params = new HashMap<>();
        params.put("userId", "admin");
        params.put("time", "2024-04-17");

        msg = MsgTemplateBuilder.create()
                                .code("LOG_001")
                                .named()
                                .args(params)
                                .render();
        // 结果: "用户admin于2024-04-17登录"
        LOGGER.info("i18n msg: {}", msg);

        // Named风格 - Bean参数 (使用ReflectCache)
        LoginEvent event = new LoginEvent();
        event.userId = "admin";
        event.time = "2024-04-17";

        msg = MsgTemplateBuilder.create()
                                .code("LOG_001")
                                .named()
                                .bean(event)
                                .render();
        LOGGER.info("i18n msg: {}", msg);

        // 指定Locale
        msg = MsgTemplateBuilder.create()
                                .code("ERR_001")
                                .locale(Locale.US)
                                .simple()
                                .args("timeout")
                                .render();
        // 结果: "Internal error:timeout"
        LOGGER.info("i18n msg: {}", msg);
    }

    private static class LoginEvent {
        private String userId;
        private String time;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }
    }
}
