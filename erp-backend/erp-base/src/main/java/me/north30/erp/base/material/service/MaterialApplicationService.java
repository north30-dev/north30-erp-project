package me.north30.erp.base.material.service;

import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import me.north30.erp.base.material.dto.MaterialAuditDTO;
import me.north30.erp.base.material.dto.MaterialBatchDeleteDTO;
import me.north30.erp.base.material.dto.MaterialCodeMapCreateDTO;
import me.north30.erp.base.material.dto.MaterialCodeMapQueryDTO;
import me.north30.erp.base.material.dto.MaterialCreateDTO;
import me.north30.erp.base.material.dto.MaterialDisableDTO;
import me.north30.erp.base.material.dto.MaterialQueryDTO;
import me.north30.erp.base.material.dto.MaterialUpdateDTO;
import me.north30.erp.base.material.vo.MaterialBatchDeleteVO;
import me.north30.erp.base.material.vo.MaterialCodeMapCreateVO;
import me.north30.erp.base.material.vo.MaterialCodeMapVO;
import me.north30.erp.base.material.vo.MaterialCreateVO;
import me.north30.erp.base.material.vo.MaterialDeleteVO;
import me.north30.erp.base.material.vo.MaterialDetailVO;
import me.north30.erp.base.material.vo.MaterialImportResultVO;
import me.north30.erp.base.material.vo.MaterialStatusVO;
import me.north30.erp.base.material.vo.MaterialVO;
import me.north30.erp.common.result.PageResult;

/**
 * 物料应用服务接口：事务编排层（写操作事务边界 + 跨层/跨域调用编排），
 * 不写业务规则、不注入 Mapper。方法与 Controller 接口一一对应。
 */
public interface MaterialApplicationService {

    /**
     * 物料分页查询（categoryName 经分类域批量名称接口回填，一次 IN 查询禁 N+1）。
     *
     * @param query 查询参数
     * @return 物料分页结果
     */
    PageResult<MaterialVO> page(MaterialQueryDTO query);

    /**
     * 物料详情。
     *
     * @param id 物料 ID
     * @return 物料详情 VO
     */
    MaterialDetailVO getDetail(Long id);

    /**
     * 按编码查询物料（扫码/编码录入即时回显）。
     *
     * @param materialCode 物料编码
     * @return 物料详情 VO
     */
    MaterialDetailVO getByCode(String materialCode);

    /**
     * 新增物料：编码服务端生成（分类编码前 4 位 + 6 位流水），状态 0-新建。
     *
     * @param dto 创建参数
     * @return 新物料 ID、编码与初始状态
     */
    MaterialCreateVO create(MaterialCreateDTO dto);

    /**
     * 修改物料（部分更新 + 乐观锁，编码不可修改）。
     *
     * @param id  物料 ID
     * @param dto 修改参数
     * @return 更新时间字符串
     */
    String update(Long id, MaterialUpdateDTO dto);

    /**
     * 删除物料（仅 0-新建可删除，逻辑删除）。
     *
     * @param id 物料 ID
     * @return 删除响应
     */
    MaterialDeleteVO delete(Long id);

    /**
     * 批量删除物料（部分失败语义：逐行采集失败原因，成功行照常删除）。
     *
     * @param dto 物料 ID 集合（≤200）
     * @return 批量删除结果
     */
    MaterialBatchDeleteVO batchDelete(MaterialBatchDeleteDTO dto);

    /**
     * 物料审核（0-新建 → 1-审核/驳回，非法前置 11010）。
     *
     * @param id  物料 ID
     * @param dto 审核参数
     * @return 变更后状态
     */
    MaterialStatusVO audit(Long id, MaterialAuditDTO dto);

    /**
     * 物料启用（1-审核 → 2-启用，非法前置 11010/11003）。
     *
     * @param id 物料 ID
     * @return 变更后状态
     */
    MaterialStatusVO enable(Long id);

    /**
     * 物料停用（1-审核/2-启用 → 3-停用，停用原因落 remark）。
     *
     * @param id  物料 ID
     * @param dto 停用参数
     * @return 变更后状态与受影响 BOM 数（BOM 域未落地恒为 0）
     */
    MaterialStatusVO disable(Long id, MaterialDisableDTO dto);

    /**
     * 物料批量导入（CSV 简化口径，ALL_OR_NOTHING 整批事务 / SKIP_ERROR 跳过错误行）。
     *
     * @param file 导入文件
     * @param mode 导入模式（缺省 ALL_OR_NOTHING）
     * @return 导入结果（批次号与逐行错误明细）
     */
    MaterialImportResultVO importMaterials(MultipartFile file, String mode);

    /**
     * 物料列表导出 CSV（查询条件复用分页接口）。
     *
     * @param query 查询参数
     * @return CSV 文件内容字节
     */
    byte[] exportCsv(MaterialQueryDTO query);

    /**
     * 物料导入模板下载（CSV，首行表头）。
     *
     * @return 模板文件内容字节
     */
    byte[] downloadImportTemplate();

    /**
     * 旧码映射分页查询。
     *
     * @param query 查询参数
     * @return 旧码映射分页结果
     */
    PageResult<MaterialCodeMapVO> pageCodeMaps(MaterialCodeMapQueryDTO query);

    /**
     * 新增旧码映射（old_code + source_dept 唯一 11015，新物料必须存在 11002）。
     *
     * @param dto 映射参数
     * @return 映射 ID 与回填的新物料编码
     */
    MaterialCodeMapCreateVO createCodeMap(MaterialCodeMapCreateDTO dto);
}
