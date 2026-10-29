-- =============================================================================
-- 四、初始化种子数据段（可选执行，建议在新建库时执行一次）
--   执行顺序：先执行同目录 00_schema.sql（建表与索引），再执行本文件
--   内容：组织、角色、4 个中心仓与示例库位、会计科目、数据字典、系统参数、
--         编号序列初始行、当前会计期间。
--   说明：①本段使用显式 id 写入以保证层级引用确定性，执行后需 setval 校准序列；
--         ②admin 用户由应用启动时的 AdminInitializer 幂等创建（BCrypt 强度 10，
--           初始口令 Admin@123456 首次登录强制修改），脚本不写入任何明文口令与口令哈希；
--         ③物料/客户/供应商/BOM/订单等业务数据由阶段六数据迁移导入，脚本内不预置。
-- =============================================================================

BEGIN;

-- 4.1 组织与部门（E-01 多组织维度；对应 sys_dept）
INSERT INTO sys_dept (id, dept_code, dept_name, parent_id, dept_type, dept_level, ancestors, dept_sort, status, create_by, update_by) VALUES
(1, 'ORG001', '集团总部', 0, 1, 1, '0', 1, 1, 'admin', 'admin'),
(2, 'ORG002', '苏州生产基地', 1, 2, 2, '0,1', 2, 1, 'admin', 'admin'),
(3, 'ORG003', '东莞生产基地', 1, 2, 2, '0,1', 3, 1, 'admin', 'admin'),
(4, 'ORG004', '华东分公司', 1, 3, 2, '0,1', 4, 1, 'admin', 'admin'),
(5, 'ORG005', '华南分公司', 1, 3, 2, '0,1', 5, 1, 'admin', 'admin'),
(6, 'ORG006', '华北分公司', 1, 3, 2, '0,1', 6, 1, 'admin', 'admin');
SELECT setval('sys_dept_id_seq', (SELECT MAX(id) FROM sys_dept));

-- 4.2 角色（SYS-03/SYS-04；数据范围 1-全部 3-本组织 4-本部门及下级 6-仅本人）
--   ADMIN/SYSTEM_ADMIN 为管理内置角色；SYSTEM_ADMIN 预勾选全部 /system 菜单
--   （角色-菜单授权由应用 AdminInitializer 启动时幂等完成）。
INSERT INTO sys_role (id, role_code, role_name, data_scope, is_builtin, status, create_by, update_by) VALUES
(1, 'ADMIN', '超级管理员', 1, 1, 1, 'admin', 'admin'),
(2, 'SYSTEM_ADMIN', '系统管理员', 1, 1, 1, 'admin', 'admin'),
(3, 'SALES_DIRECTOR', '销售总监', 1, 1, 1, 'admin', 'admin'),
(4, 'SALES_USER', '销售业务员', 6, 1, 1, 'admin', 'admin'),
(5, 'BUYER', '采购员', 6, 1, 1, 'admin', 'admin'),
(6, 'WAREHOUSE_KEEPER', '仓管员', 3, 1, 1, 'admin', 'admin'),
(7, 'PRODUCTION_PLANNER', '生产计划员', 4, 1, 1, 'admin', 'admin'),
(8, 'FINANCE_MANAGER', '财务经理', 1, 1, 1, 'admin', 'admin');
SELECT setval('sys_role_id_seq', (SELECT MAX(id) FROM sys_role));

-- 4.3 4 个中心仓库（INV-01 裁定：原材料仓、半成品仓、苏州成品仓、东莞成品仓）
INSERT INTO base_warehouse (id, warehouse_code, warehouse_name, warehouse_type, dept_id, is_default, status, create_by, update_by) VALUES
(1, 'WH001', '原材料仓', 1, 2, 1, 1, 'admin', 'admin'),
(2, 'WH002', '半成品仓', 2, 2, 0, 1, 'admin', 'admin'),
(3, 'WH003', '苏州成品仓', 3, 2, 0, 1, 'admin', 'admin'),
(4, 'WH004', '东莞成品仓', 3, 3, 0, 1, 'admin', 'admin');
SELECT setval('base_warehouse_id_seq', (SELECT MAX(id) FROM base_warehouse));

-- 4.4 示例库位（INV-01，每仓含 1 个默认库位；约 260 个库位由仓储中心按台账补全）
INSERT INTO base_location (id, location_code, location_name, warehouse_id, area, is_default, status, create_by, update_by) VALUES
(1, 'YL-DEFAULT', '原材料仓默认库位', 1, '默认区', 1, 1, 'admin', 'admin'),
(2, 'YL-A-01', '原材料仓电子料区 A 货架 01 位', 1, '电子料区', 0, 1, 'admin', 'admin'),
(3, 'YL-A-02', '原材料仓电子料区 A 货架 02 位', 1, '电子料区', 0, 1, 'admin', 'admin'),
(4, 'YL-B-01', '原材料仓结构件区 B 货架 01 位', 1, '结构件区', 0, 1, 'admin', 'admin'),
(5, 'YL-B-02', '原材料仓结构件区 B 货架 02 位', 1, '结构件区', 0, 1, 'admin', 'admin'),
(6, 'BC-DEFAULT', '半成品仓默认库位', 2, '默认区', 1, 1, 'admin', 'admin'),
(7, 'BC-A-01', '半成品仓主控板组件区 01 位', 2, '组件区', 0, 1, 'admin', 'admin'),
(8, 'BC-A-02', '半成品仓电源模块区 01 位', 2, '组件区', 0, 1, 'admin', 'admin'),
(9, 'CP-SZ-DEFAULT', '苏州成品仓默认库位', 3, '默认区', 1, 1, 'admin', 'admin'),
(10, 'CP-SZ-01', '苏州成品仓 AC 系列成品区 01 位', 3, '成品区', 0, 1, 'admin', 'admin'),
(11, 'CP-SZ-02', '苏州成品仓 AC 系列成品区 02 位', 3, '成品区', 0, 1, 'admin', 'admin'),
(12, 'CP-SZ-03', '苏州成品仓待发区 01 位', 3, '待发区', 0, 1, 'admin', 'admin'),
(13, 'CP-DG-DEFAULT', '东莞成品仓默认库位', 4, '默认区', 1, 1, 'admin', 'admin'),
(14, 'CP-DG-01', '东莞成品仓成品区 01 位', 4, '成品区', 0, 1, 'admin', 'admin'),
(15, 'CP-DG-02', '东莞成品仓成品区 02 位', 4, '成品区', 0, 1, 'admin', 'admin'),
(16, 'CP-DG-ISOLATE', '东莞成品仓隔离区', 4, '隔离区', 0, 1, 'admin', 'admin');
SELECT setval('base_location_id_seq', (SELECT MAX(id) FROM base_location));

-- 4.5 会计科目（FIN-01/FR-09，覆盖存货核算、成本归集、暂估应付、进销项税等映射所需科目）
INSERT INTO fin_subject (id, subject_code, subject_name, parent_id, subject_level, is_leaf, subject_direction, subject_category, is_inventory_subject, status, create_by, update_by) VALUES
(1,  '1001', '库存现金', 0, 1, TRUE, 1, 1, 0, 1, 'admin', 'admin'),
(2,  '1002', '银行存款', 0, 1, TRUE, 1, 1, 0, 1, 'admin', 'admin'),
(3,  '1122', '应收账款', 0, 1, TRUE, 1, 1, 0, 1, 'admin', 'admin'),
(4,  '1123', '预付账款', 0, 1, TRUE, 1, 1, 0, 1, 'admin', 'admin'),
(5,  '1221', '其他应收款', 0, 1, TRUE, 1, 1, 0, 1, 'admin', 'admin'),
(6,  '1403', '原材料', 0, 1, TRUE, 1, 1, 1, 1, 'admin', 'admin'),
(7,  '1405', '库存商品', 0, 1, TRUE, 1, 1, 1, 1, 'admin', 'admin'),
(8,  '1408', '委托加工物资', 0, 1, TRUE, 1, 1, 1, 1, 'admin', 'admin'),
(9,  '1601', '固定资产', 0, 1, TRUE, 1, 1, 0, 1, 'admin', 'admin'),
(10, '1602', '累计折旧', 0, 1, TRUE, 2, 1, 0, 1, 'admin', 'admin'),
(11, '1606', '固定资产清理', 0, 1, TRUE, 1, 1, 0, 1, 'admin', 'admin'),
(12, '1901', '待处理财产损溢', 0, 1, TRUE, 1, 1, 0, 1, 'admin', 'admin'),
(13, '2202', '应付账款', 0, 1, FALSE, 2, 2, 0, 1, 'admin', 'admin'),
(14, '220201', '应付账款-暂估', 13, 2, TRUE, 2, 2, 0, 1, 'admin', 'admin'),
(15, '220202', '应付账款-外部单位', 13, 2, TRUE, 2, 2, 0, 1, 'admin', 'admin'),
(16, '2203', '预收账款', 0, 1, TRUE, 2, 2, 0, 1, 'admin', 'admin'),
(17, '2211', '应付职工薪酬', 0, 1, TRUE, 2, 2, 0, 1, 'admin', 'admin'),
(18, '2221', '应交税费', 0, 1, FALSE, 2, 2, 0, 1, 'admin', 'admin'),
(19, '222101', '应交税费-应交增值税-进项税额', 18, 2, TRUE, 1, 2, 0, 1, 'admin', 'admin'),
(20, '222102', '应交税费-应交增值税-销项税额', 18, 2, TRUE, 2, 2, 0, 1, 'admin', 'admin'),
(21, '4001', '实收资本', 0, 1, TRUE, 2, 3, 0, 1, 'admin', 'admin'),
(22, '4103', '本年利润', 0, 1, TRUE, 2, 3, 0, 1, 'admin', 'admin'),
(23, '4104', '利润分配', 0, 1, TRUE, 2, 3, 0, 1, 'admin', 'admin'),
(24, '5001', '生产成本', 0, 1, FALSE, 1, 4, 0, 1, 'admin', 'admin'),
(25, '500101', '生产成本-直接材料', 24, 2, TRUE, 1, 4, 0, 1, 'admin', 'admin'),
(26, '500102', '生产成本-直接人工', 24, 2, TRUE, 1, 4, 0, 1, 'admin', 'admin'),
(27, '500103', '生产成本-制造费用', 24, 2, TRUE, 1, 4, 0, 1, 'admin', 'admin'),
(28, '5101', '制造费用', 0, 1, TRUE, 1, 4, 0, 1, 'admin', 'admin'),
(29, '6001', '主营业务收入', 0, 1, TRUE, 2, 5, 0, 1, 'admin', 'admin'),
(30, '6401', '主营业务成本', 0, 1, TRUE, 1, 5, 0, 1, 'admin', 'admin'),
(31, '6601', '销售费用', 0, 1, TRUE, 1, 5, 0, 1, 'admin', 'admin'),
(32, '6602', '管理费用', 0, 1, TRUE, 1, 5, 0, 1, 'admin', 'admin'),
(33, '6603', '财务费用', 0, 1, TRUE, 1, 5, 0, 1, 'admin', 'admin'),
(34, '6301', '营业外收入', 0, 1, TRUE, 2, 5, 0, 1, 'admin', 'admin'),
(35, '6711', '营业外支出', 0, 1, TRUE, 1, 5, 0, 1, 'admin', 'admin');
SELECT setval('fin_subject_id_seq', (SELECT MAX(id) FROM fin_subject));

-- 4.6 数据字典类型（SYS-06；lang 字段为 E-03 国际化预留，本期仅 zh-CN）
INSERT INTO sys_dict_type (id, dict_type, dict_name, status, create_by, update_by) VALUES
(1, 'material_type', '物料类型', 1, 'admin', 'admin'),
(2, 'source_type', '物料来源类型', 1, 'admin', 'admin'),
(3, 'material_status', '物料状态', 1, 'admin', 'admin'),
(4, 'bom_status', 'BOM 版本状态', 1, 'admin', 'admin'),
(5, 'sales_order_status', '销售订单执行状态', 1, 'admin', 'admin'),
(6, 'approval_status', '审批状态', 1, 'admin', 'admin'),
(7, 'purchase_order_status', '采购订单执行状态', 1, 'admin', 'admin'),
(8, 'settlement_status', '采购结算状态', 1, 'admin', 'admin'),
(9, 'receipt_status', '采购收货单状态', 1, 'admin', 'admin'),
(10, 'work_order_status', '生产工单状态', 1, 'admin', 'admin'),
(11, 'stock_trans_type', '库存流水类型', 1, 'admin', 'admin'),
(12, 'settlement_method', '结算方式', 1, 'admin', 'admin'),
(13, 'abc_class', 'ABC 分类', 1, 'admin', 'admin'),
(14, 'customer_region', '客户地区', 1, 'admin', 'admin'),
(15, 'customer_level', '客户等级', 1, 'admin', 'admin'),
(16, 'unit', '计量单位', 1, 'admin', 'admin'),
(17, 'voucher_status', '凭证状态', 1, 'admin', 'admin'),
(18, 'asset_status', '固定资产状态', 1, 'admin', 'admin'),
(19, 'period_status', '会计期间状态', 1, 'admin', 'admin'),
(20, 'audit_operate_type', '审计操作类型', 1, 'admin', 'admin'),
(21, 'receivable_status', '应收状态', 1, 'admin', 'admin'),
(22, 'payable_status', '应付状态', 1, 'admin', 'admin'),
(23, 'alert_type', '库存预警类型', 1, 'admin', 'admin'),
(24, 'price_policy_type', '销售价格策略类型', 1, 'admin', 'admin'),
(25, 'mrp_suggest_type', 'MRP 建议类型', 1, 'admin', 'admin'),
(26, 'mfg_issue_status', '生产领料单状态', 1, 'admin', 'admin');
SELECT setval('sys_dict_type_id_seq', (SELECT MAX(id) FROM sys_dict_type));

-- 4.7 数据字典项（SYS-06；item_value 为编码值，不随语言变化）
INSERT INTO sys_dict_item (dict_type, item_label, item_value, item_sort, create_by, update_by) VALUES
('material_type', '原材料', '1', 1, 'admin', 'admin'),
('material_type', '半成品', '2', 2, 'admin', 'admin'),
('material_type', '成品', '3', 3, 'admin', 'admin'),
('material_type', '辅料', '4', 4, 'admin', 'admin'),
('source_type', '自制', '1', 1, 'admin', 'admin'),
('source_type', '外购', '2', 2, 'admin', 'admin'),
('source_type', '外协', '3', 3, 'admin', 'admin'),
('material_status', '新建', '0', 1, 'admin', 'admin'),
('material_status', '审核', '1', 2, 'admin', 'admin'),
('material_status', '启用', '2', 3, 'admin', 'admin'),
('material_status', '停用', '3', 4, 'admin', 'admin'),
('bom_status', '草稿', '0', 1, 'admin', 'admin'),
('bom_status', '审核', '1', 2, 'admin', 'admin'),
('bom_status', '生效', '2', 3, 'admin', 'admin'),
('bom_status', '历史', '3', 4, 'admin', 'admin'),
('sales_order_status', '草稿', '0', 1, 'admin', 'admin'),
('sales_order_status', '审核', '1', 2, 'admin', 'admin'),
('sales_order_status', '部分发货', '2', 3, 'admin', 'admin'),
('sales_order_status', '已发货', '3', 4, 'admin', 'admin'),
('sales_order_status', '已关闭', '4', 5, 'admin', 'admin'),
('approval_status', '待审', '0', 1, 'admin', 'admin'),
('approval_status', '通过', '1', 2, 'admin', 'admin'),
('approval_status', '驳回', '2', 3, 'admin', 'admin'),
('purchase_order_status', '草稿', '0', 1, 'admin', 'admin'),
('purchase_order_status', '审核', '1', 2, 'admin', 'admin'),
('purchase_order_status', '部分收货', '2', 3, 'admin', 'admin'),
('purchase_order_status', '已收货', '3', 4, 'admin', 'admin'),
('purchase_order_status', '已关闭', '4', 5, 'admin', 'admin'),
('settlement_status', '未结算', '0', 1, 'admin', 'admin'),
('settlement_status', '部分结算', '1', 2, 'admin', 'admin'),
('settlement_status', '已结算', '2', 3, 'admin', 'admin'),
('receipt_status', '待检', '0', 1, 'admin', 'admin'),
('receipt_status', '合格入库', '1', 2, 'admin', 'admin'),
('receipt_status', '部分合格', '2', 3, 'admin', 'admin'),
('receipt_status', '退货', '3', 4, 'admin', 'admin'),
('work_order_status', '计划', '0', 1, 'admin', 'admin'),
('work_order_status', '已下达', '1', 2, 'admin', 'admin'),
('work_order_status', '领料中', '2', 3, 'admin', 'admin'),
('work_order_status', '加工中', '3', 4, 'admin', 'admin'),
('work_order_status', '已完工', '4', 5, 'admin', 'admin'),
('work_order_status', '已关闭', '5', 6, 'admin', 'admin'),
('stock_trans_type', '采购入库', '10', 1, 'admin', 'admin'),
('stock_trans_type', '生产入库', '20', 2, 'admin', 'admin'),
('stock_trans_type', '销售出库', '30', 3, 'admin', 'admin'),
('stock_trans_type', '生产领料', '40', 4, 'admin', 'admin'),
('stock_trans_type', '调拨入库', '50', 5, 'admin', 'admin'),
('stock_trans_type', '调拨出库', '60', 6, 'admin', 'admin'),
('stock_trans_type', '盘盈', '70', 7, 'admin', 'admin'),
('stock_trans_type', '盘亏', '80', 8, 'admin', 'admin'),
('stock_trans_type', '期初建账', '90', 9, 'admin', 'admin'),
('settlement_method', '款到发货', 'PREPAY', 1, 'admin', 'admin'),
('settlement_method', '月结30天', 'NET30', 2, 'admin', 'admin'),
('settlement_method', '月结60天', 'NET60', 3, 'admin', 'admin'),
('settlement_method', '月结90天', 'NET90', 4, 'admin', 'admin'),
('abc_class', 'A 类（高价值重点管控）', 'A', 1, 'admin', 'admin'),
('abc_class', 'B 类', 'B', 2, 'admin', 'admin'),
('abc_class', 'C 类', 'C', 3, 'admin', 'admin'),
('customer_region', '华东', 'EAST', 1, 'admin', 'admin'),
('customer_region', '华南', 'SOUTH', 2, 'admin', 'admin'),
('customer_region', '华北', 'NORTH', 3, 'admin', 'admin'),
('customer_level', '战略客户', 'STRATEGIC', 1, 'admin', 'admin'),
('customer_level', '重点客户', 'KEY', 2, 'admin', 'admin'),
('customer_level', '普通客户', 'NORMAL', 3, 'admin', 'admin'),
('unit', '个', 'PCS', 1, 'admin', 'admin'),
('unit', '套', 'SET', 2, 'admin', 'admin'),
('unit', '台', 'UNIT', 3, 'admin', 'admin'),
('unit', '米', 'M', 4, 'admin', 'admin'),
('unit', '公斤', 'KG', 5, 'admin', 'admin'),
('unit', '片', 'SHEET', 6, 'admin', 'admin'),
('unit', '卷', 'ROLL', 7, 'admin', 'admin'),
('unit', '包', 'PACK', 8, 'admin', 'admin'),
('voucher_status', '草稿', '0', 1, 'admin', 'admin'),
('voucher_status', '已过账（不可修改）', '1', 2, 'admin', 'admin'),
('asset_status', '在用', '1', 1, 'admin', 'admin'),
('asset_status', '闲置', '2', 2, 'admin', 'admin'),
('asset_status', '报废', '3', 3, 'admin', 'admin'),
('asset_status', '已处置', '4', 4, 'admin', 'admin'),
('period_status', '未启用', '0', 1, 'admin', 'admin'),
('period_status', '开放', '1', 2, 'admin', 'admin'),
('period_status', '已关闭', '2', 3, 'admin', 'admin'),
('audit_operate_type', '创建', 'CREATE', 1, 'admin', 'admin'),
('audit_operate_type', '修改', 'UPDATE', 2, 'admin', 'admin'),
('audit_operate_type', '审核', 'APPROVE', 3, 'admin', 'admin'),
('audit_operate_type', '驳回', 'REJECT', 4, 'admin', 'admin'),
('audit_operate_type', '删除', 'DELETE', 5, 'admin', 'admin'),
('audit_operate_type', '过账', 'POST', 6, 'admin', 'admin'),
('audit_operate_type', '反审核', 'UNAPPROVE', 7, 'admin', 'admin'),
('audit_operate_type', '关闭', 'CLOSE', 8, 'admin', 'admin'),
('audit_operate_type', '期间重开', 'REOPEN', 9, 'admin', 'admin'),
('receivable_status', '未收款', '0', 1, 'admin', 'admin'),
('receivable_status', '部分收款', '1', 2, 'admin', 'admin'),
('receivable_status', '已收款', '2', 3, 'admin', 'admin'),
('receivable_status', '已关闭', '3', 4, 'admin', 'admin'),
('payable_status', '未付款', '0', 1, 'admin', 'admin'),
('payable_status', '部分付款', '1', 2, 'admin', 'admin'),
('payable_status', '已付款', '2', 3, 'admin', 'admin'),
('payable_status', '已红冲', '3', 4, 'admin', 'admin'),
('alert_type', '安全库存预警', '1', 1, 'admin', 'admin'),
('alert_type', '保质期预警', '2', 2, 'admin', 'admin'),
('alert_type', '呆滞库存', '3', 3, 'admin', 'admin'),
('price_policy_type', '客户 + 物料', '1', 1, 'admin', 'admin'),
('price_policy_type', '客户等级 + 品类', '2', 2, 'admin', 'admin'),
('price_policy_type', '品类', '3', 3, 'admin', 'admin'),
('price_policy_type', '标准售价', '4', 4, 'admin', 'admin'),
('mrp_suggest_type', '采购建议', '1', 1, 'admin', 'admin'),
('mrp_suggest_type', '生产建议', '2', 2, 'admin', 'admin'),
('mfg_issue_status', '草稿', '0', 1, 'admin', 'admin'),
('mfg_issue_status', '待审批', '1', 2, 'admin', 'admin'),
('mfg_issue_status', '已审核', '2', 3, 'admin', 'admin'),
('mfg_issue_status', '已出库', '3', 4, 'admin', 'admin'),
('mfg_issue_status', '已驳回', '4', 5, 'admin', 'admin'),
('mfg_issue_status', '已关闭', '5', 6, 'admin', 'admin');

-- 4.8 系统参数（SYS-06，修改后 ≤1 分钟生效，无需重启）
INSERT INTO sys_config (id, config_key, config_name, config_value, value_type, config_group, is_system, status, create_by, update_by) VALUES
(1, 'inventory.stagnant.days', '呆滞库存判定天数', '180', 2, 'INVENTORY', 1, 1, 'admin', 'admin'),
(2, 'inventory.shelf.life.warn.days', '保质期预警提前天数', '30', 2, 'INVENTORY', 1, 1, 'admin', 'admin'),
(3, 'inventory.stocktake.frequency.a', 'A 类物料盘点频率（月）', '1', 2, 'INVENTORY', 1, 1, 'admin', 'admin'),
(4, 'inventory.stocktake.frequency.b', 'B 类物料盘点频率（月）', '3', 2, 'INVENTORY', 1, 1, 'admin', 'admin'),
(5, 'inventory.stocktake.frequency.c', 'C 类物料盘点频率（月）', '6', 2, 'INVENTORY', 1, 1, 'admin', 'admin'),
(6, 'inventory.expiry.outbound.block', '超期物料出库拦截开关', 'true', 3, 'INVENTORY', 1, 1, 'admin', 'admin'),
(7, 'sales.credit.control.enabled', '信用控制开关', 'true', 3, 'SALES', 1, 1, 'admin', 'admin'),
(8, 'sales.price.float.ratio', '单价授权浮动比例（%）', '5', 2, 'SALES', 1, 1, 'admin', 'admin'),
(9, 'purchase.requisition.approve.threshold', '采购申请上级审批金额阈值（元）', '50000', 2, 'PURCHASE', 1, 1, 'admin', 'admin'),
(10, 'purchase.match.qty.tolerance', '三单匹配数量容差（%）', '2', 2, 'PURCHASE', 1, 1, 'admin', 'admin'),
(11, 'purchase.match.price.tolerance', '三单匹配单价容差（%）', '0.5', 2, 'PURCHASE', 1, 1, 'admin', 'admin'),
(12, 'purchase.price.warn.ratio', '采购价格预警偏离阈值（%）', '10', 2, 'PURCHASE', 1, 1, 'admin', 'admin'),
(13, 'manufacturing.issue.over.ratio', '生产领料超领上限（%）', '10', 2, 'MANUFACTURING', 1, 1, 'admin', 'admin'),
(14, 'manufacturing.overhead.allocate.basis', '制造费用分摊基准（1-工时 2-产量）', '1', 2, 'MANUFACTURING', 1, 1, 'admin', 'admin'),
(15, 'system.import.max.rows', '单批导入上限（行）', '10000', 2, 'SYSTEM', 1, 1, 'admin', 'admin'),
(16, 'system.export.max.rows', '单次导出上限（行）', '50000', 2, 'SYSTEM', 1, 1, 'admin', 'admin'),
(17, 'system.page.default.size', '列表默认每页条数', '20', 2, 'SYSTEM', 1, 1, 'admin', 'admin'),
(18, 'system.page.max.size', '列表单页上限条数', '200', 2, 'SYSTEM', 1, 1, 'admin', 'admin'),
(19, 'attachment.max.file.size.mb', '附件单文件大小上限（MB）', '20', 2, 'ATTACHMENT', 1, 1, 'admin', 'admin'),
(20, 'attachment.max.count.per.biz', '单据附件数量上限（个）', '20', 2, 'ATTACHMENT', 1, 1, 'admin', 'admin');
SELECT setval('sys_config_id_seq', (SELECT MAX(id) FROM sys_config));

-- 4.9 单据编号序列初始行（SYS-07，按当前期间初始化；跨月由 period 变化自动重置）
INSERT INTO sys_code_sequence (biz_type, prefix, period, current_no, seq_length, create_by, update_by) VALUES
('SALES_ORDER', 'SO', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('PURCHASE_REQUISITION', 'PA', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('PURCHASE_ORDER', 'PO', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('PURCHASE_RECEIPT', 'GR', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('PURCHASE_RETURN', 'PR', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('SALES_DELIVERY_NOTE', 'DN', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('SALES_OUTBOUND', 'OD', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('SALES_RETURN', 'SR', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('WORK_ORDER', 'MO', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('MFG_ISSUE', 'MI', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('MFG_REPORT', 'MW', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('MFG_COMPLETE', 'FI', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('MRP_PLAN', 'MP', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('STOCK_TRANSACTION', 'IV', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('OPENING_TRANSACTION', 'IV', to_char(NOW(), 'YYYYMM'), 0, 5, 'admin', 'admin'),
('STOCKTAKE', 'ST', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('TRANSFER', 'TR', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('VOUCHER', 'FZ', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('RECEIVABLE', 'AR', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('PAYABLE', 'AP', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('RECEIPT_SETTLEMENT', 'RC', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('PAYMENT_SETTLEMENT', 'PY', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('FIXED_ASSET', 'FA', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('INVOICE_MATCH', 'IM', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('PRICE_POLICY', 'PP', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin'),
('OUTSOURCE', 'OS', to_char(NOW(), 'YYYYMM'), 0, 3, 'admin', 'admin');

-- 4.10 当前会计期间（FIN-08；后续期间由期间初始化功能在月末自动创建）
INSERT INTO fin_period (period, period_year, period_month, start_date, end_date, status, is_current, create_by, update_by) VALUES
(to_char(NOW(), 'YYYYMM'), CAST(EXTRACT(YEAR FROM NOW()) AS INT), CAST(EXTRACT(MONTH FROM NOW()) AS INT),
 date_trunc('month', NOW())::DATE, (date_trunc('month', NOW()) + INTERVAL '1 month' - INTERVAL '1 day')::DATE,
 1, 1, 'admin', 'admin');

-- 4.11 管理员账号与角色绑定（由应用 AdminInitializer 幂等创建，脚本不写入明文口令）
--   创建规则：超级管理员 username='admin'、userCode='ADMIN'、is_admin=1，绑定 ADMIN 角色；
--             系统管理员 username='sysadmin'、userCode='SYSADMIN'、is_admin=0，绑定 SYSTEM_ADMIN 角色。
--   password=BCrypt(强度 10, 明文由 application.yml 的 erp.admin 配置/环境变量注入，
--   默认 Admin@123456 首次登录强制修改)；本脚本不提供 INSERT（安全要求 S-04/S-06）。

COMMIT;

-- =============================================================================
-- 脚本结束。执行后自检建议：
--   1) SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';  -- 期望 76
--   2) 业务侧自检：SELECT COUNT(*) FROM base_warehouse;                                -- 期望 4
--   3) 对账口径：库存流水汇总（Σ in_qty - Σ out_qty）必须等于 inv_stock.qty 汇总（日终差异 0）
