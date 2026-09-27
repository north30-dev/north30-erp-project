package me.north30.erp.system.core.service;

import me.north30.erp.system.core.vo.AttachmentDeleteVO;
import me.north30.erp.system.core.vo.AttachmentDownloadVO;
import me.north30.erp.system.core.vo.AttachmentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 附件服务（SYS-10：PDF/JPG/PNG/XLSX/DOCX，单文件 ≤20MB、单单据 ≤20 个，逻辑删除后不可下载）。
 */
public interface ISysAttachmentService {

    /**
     * 附件上传（接口文档 5.9.1，权限点 system:attachment:upload）：写盘 + 落库。
     */
    AttachmentVO upload(MultipartFile file, String bizType, Long bizId, String bizCode);

    /**
     * 附件列表查询（接口文档 5.9.3，权限点 system:attachment:list）：按业务类型 + 业务主键，id 倒序，不分页。
     */
    List<AttachmentVO> listByBiz(String bizType, Long bizId);

    /**
     * 附件下载（接口文档 5.9.2，权限点 system:attachment:download）：流式返回。
     */
    AttachmentDownloadVO download(Long id);

    /**
     * 附件删除（接口文档 5.9.4，权限点 system:attachment:delete）：逻辑删除，仅上传人可删。
     */
    AttachmentDeleteVO delete(Long id);
}
