package me.north30.erp.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import me.north30.erp.common.security.CurrentUserProvider;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计字段自动填充：为 BaseEntity 的 createBy/createTime/updateBy/updateTime 填充值。
 * <p>通过 CurrentUserProvider 获取当前操作人；该接口实现类位于 erp-system 模块，
 * 未就绪前用 ObjectProvider 优雅降级（容器中无实现 Bean 也能启动），取不到人时填充 "system"。</p>
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /** 无登录上下文时的兜底操作人标识 */
    private static final String SYSTEM_USER = "system";

    /** ObjectProvider 延迟解析：system 模块实现类就绪前容器中无 CurrentUserProvider Bean，避免启动失败 */
    private final ObjectProvider<CurrentUserProvider> currentUserProvider;

    public MyMetaObjectHandler(ObjectProvider<CurrentUserProvider> currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        String operator = resolveOperator();
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createBy", String.class, operator);
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateBy", String.class, operator);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateBy", String.class, resolveOperator());
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 解析当前操作人：CurrentUserProvider 返回用户名；无实现 Bean 或无登录上下文时降级为 "system"。
     */
    private String resolveOperator() {
        CurrentUserProvider provider = currentUserProvider.getIfAvailable();
        if (provider == null) {
            return SYSTEM_USER;
        }
        String username = provider.getCurrentUsername();
        return username != null ? username : SYSTEM_USER;
    }
}
