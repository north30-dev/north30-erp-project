package me.north30.erp.system.support;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

/**
 * 单元测试支撑：手动初始化 MyBatis-Plus 实体 TableInfo 缓存。
 * <p>纯 Mockito 单元测试不经过 Spring/MyBatis 启动流程，Mapper 被 mock 后
 * 实体列缓存（LambdaUtils.COLUMN_CACHE_MAP）不会自动注册；
 * 被测服务内部构建 LambdaQueryWrapper 时需要实体 TableInfo，
 * 否则抛出 "can not find lambda cache for this entity"。</p>
 */
public final class MpTableInfoInit {

    private MpTableInfoInit() {
    }

    /**
     * 初始化指定实体的 TableInfo（幂等：已初始化的实体直接复用全局缓存）。
     *
     * @param entities 需要参与 Lambda 条件构造的实体类型
     */
    public static void init(Class<?>... entities) {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        for (Class<?> entity : entities) {
            TableInfoHelper.initTableInfo(assistant, entity);
        }
    }
}
