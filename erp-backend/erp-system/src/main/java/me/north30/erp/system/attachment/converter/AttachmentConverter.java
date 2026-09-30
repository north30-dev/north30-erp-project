package me.north30.erp.system.attachment.converter;

import me.north30.erp.system.attachment.entity.SysAttachment;
import me.north30.erp.system.attachment.vo.AttachmentVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 附件域对象转换器（MapStruct 编译期生成，全项目唯一转换方案）。
 * <p>与 MyBatis 的 {@code org.apache.ibatis.annotations.Mapper} 无关，勿混淆；
 * MapperScan 只扫描 {@code **.mapper} 包，不会加载本接口。</p>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AttachmentConverter {

    /** 下载地址模板（与接口文档 5.9 一致） */
    String DOWNLOAD_URL_TEMPLATE = "/api/system/attachments/%d/download";

    /**
     * 附件实体 → VO（downloadUrl 由 id 派生，走 {@link #map(Long)}）。
     */
    @Mapping(target = "downloadUrl", source = "id")
    AttachmentVO toVO(SysAttachment attachment);

    /**
     * Long ID → 下载地址（MapStruct 自动应用于 Long→String 的属性映射）。
     */
    default String map(Long id) {
        return id == null ? null : String.format(DOWNLOAD_URL_TEMPLATE, id);
    }
}
