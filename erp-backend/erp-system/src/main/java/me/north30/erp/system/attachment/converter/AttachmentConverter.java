package me.north30.erp.system.attachment.converter;

import me.north30.erp.system.attachment.dto.AttachmentBizQueryDTO;
import me.north30.erp.system.attachment.entity.SysAttachment;
import me.north30.erp.system.attachment.vo.AttachmentVO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.springframework.web.multipart.MultipartFile;

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
     * 上传入参 → 附件实体（组装逻辑收口，替代 Service 手工 set 链）。
     * <p>fileName/filePath/fileType 由 FileStorageStrategy 产出后传入；
     * bizType 由调用方经 {@code bizTypeNormalized()} 归一化后传入；
     * uploadTime 为组装时刻；storageType 固定 1-本地目录。</p>
     *
     * @param dto          业务归属入参（取 bizId/bizCode）
     * @param bizType      已归一化的业务类型
     * @param file         上传文件（仅取 size）
     * @param fileName     清洗后的展示文件名
     * @param relativePath 存储相对路径
     * @param fileType     文件类型 PDF/JPG/PNG/XLSX/DOCX
     * @param uploadBy     上传人（未登录上下文为 system）
     * @return 未持久化的附件实体
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "bizType", source = "bizType")
    @Mapping(target = "bizId", source = "dto.bizId")
    @Mapping(target = "bizCode", source = "dto.bizCode")
    @Mapping(target = "fileName", source = "fileName")
    @Mapping(target = "filePath", source = "relativePath")
    @Mapping(target = "fileType", source = "fileType")
    @Mapping(target = "fileSize", source = "file.size")
    @Mapping(target = "storageType", constant = "1")
    @Mapping(target = "uploadBy", source = "uploadBy")
    @Mapping(target = "uploadTime", expression = "java(java.time.LocalDateTime.now())")
    SysAttachment createSysAttachment(AttachmentBizQueryDTO dto, String bizType, MultipartFile file,
        String fileName, String relativePath, String fileType, String uploadBy);

    /**
     * 附件实体 → VO（downloadUrl 由 id 派生，走 {@link #map(Long)}）。
     */
    @Mapping(target = "downloadUrl", source = "id")
    AttachmentVO toAttachmentVO(SysAttachment attachment);

    

    /**
     * Long ID → 下载地址（MapStruct 自动应用于 Long→String 的属性映射）。
     */
    default String map(Long id) {
        return id == null ? null : String.format(DOWNLOAD_URL_TEMPLATE, id);
    }
}
