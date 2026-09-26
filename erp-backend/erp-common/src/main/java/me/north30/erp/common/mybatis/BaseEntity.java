package me.north30.erp.common.mybatis;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 实体基类：所有业务实体继承该类，审计字段由 MyMetaObjectHandler 自动填充，勿在子类重复定义。
 * <p>使用 @Getter/@Setter 而非 @Data，避免继承场景下 equals/hashCode 的生成陷阱；
 * 子类实体应搭配 @EqualsAndHashCode(callSuper = true) 使用。</p>
 */
@Getter
@Setter
public abstract class BaseEntity {

    /** 主键：雪花算法（MP 内置 ASSIGN_ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 创建人：插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间：插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新人：插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /** 更新时间：插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 乐观锁版本号：配合 OptimisticLockerInnerInterceptor 使用 */
    @Version
    private Integer version;

    /** 逻辑删除标记（SMALLINT）：0 未删除 / 1 已删除，禁止物理 DELETE */
    @TableLogic
    private Integer isDeleted;

    /** 备注 */
    private String remark;
}
