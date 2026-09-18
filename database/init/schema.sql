-- =============================================================================
-- north30 ERP（MTO 离散制造）—— 全量建表脚本 schema.sql
-- =============================================================================
-- 文件用途    ：阶段二《数据库设计文档》的配套全量 DDL 脚本，创建 7 个业务域共 76 张表、
--               索引与初始化种子数据，是阶段三 D1 建库与全部模块编码的唯一依据。
-- 数据库版本  ：PostgreSQL 16+
-- 字符集      ：UTF-8（库级 UTF8，脚本无 BOM，LF 换行）
-- 关联文档    ：docs/phase2-system-design/03-数据库设计文档.md（表名、字段、类型、索引一一对应）
--               docs/ERP系统数据库表结构设计文档.md（只读基线）
-- 执行方式    ：psql -h 127.0.0.1 -p 5432 -U erp -d erp -f schema.sql
--               或以 erp 用户在执行客户端中整文件执行；脚本可重复执行（DROP + CREATE）。
-- 生成日期    ：2026-09-16
-- -----------------------------------------------------------------------------
-- 脚本约定（与《ERP系统开发编码规范文档》《AGENTS.md》一致，不可违反）：
--   1) 严禁物理外键（FOREIGN KEY / REFERENCES），引用一致性由应用层 Service 保证；
--   2) 每张表均显式包含全局必备字段：id / create_by / create_time / update_by /
--      update_time / version / is_deleted / remark；
--   3) 金额、单价、数量统一 DECIMAL(20,6)；税率、比率、百分比统一 DECIMAL(5,2)；
--   4) 状态、类型、枚举统一 SMALLINT；布尔统一 BOOLEAN；时间统一 TIMESTAMP（不带时区）；
--      变更前后 JSON 统一 TEXT；主键统一 BIGSERIAL；
--   5) 逻辑删除 is_deleted SMALLINT（0-未删 1-已删），业务唯一键统一使用
--      "部分唯一索引 ... WHERE is_deleted = 0"；
--   6) 表名小写 + 下划线、单数形式，前缀 sys_/base_/inv_/pur_/sal_/mf_/fin_；
--   7) 索引命名：uk_表名_字段（唯一）、idx_表名_字段（普通）；
--   8) 流水类表（inv_transaction）只增不改不删，不提供修改/删除接口。
-- =============================================================================

BEGIN;

-- =============================================================================
-- 一、DROP TABLE 段（按依赖倒序：fin_ → mf_ → sal_ → pur_ → inv_ → base_ → sys_）
--     无物理外键，按业务依赖倒序清理，保证脚本可重复执行。
-- =============================================================================
DROP TABLE IF EXISTS fin_asset_depreciation;
DROP TABLE IF EXISTS fin_asset;
DROP TABLE IF EXISTS fin_opening_balance;
DROP TABLE IF EXISTS fin_period;
DROP TABLE IF EXISTS fin_cost_ledger;
DROP TABLE IF EXISTS fin_settlement_entry;
DROP TABLE IF EXISTS fin_settlement;
DROP TABLE IF EXISTS fin_payable;
DROP TABLE IF EXISTS fin_receivable;
DROP TABLE IF EXISTS fin_voucher_template;
DROP TABLE IF EXISTS fin_voucher_entry;
DROP TABLE IF EXISTS fin_voucher;
DROP TABLE IF EXISTS fin_subject;

DROP TABLE IF EXISTS mf_order_outsource;
DROP TABLE IF EXISTS mf_order_cost;
DROP TABLE IF EXISTS mf_order_report;
DROP TABLE IF EXISTS mf_order_issue_line;
DROP TABLE IF EXISTS mf_order_issue;
DROP TABLE IF EXISTS mf_order_bom;
DROP TABLE IF EXISTS mf_order;
DROP TABLE IF EXISTS mf_mrp_plan_item;
DROP TABLE IF EXISTS mf_mrp_plan;

DROP TABLE IF EXISTS sal_price_policy;
DROP TABLE IF EXISTS sal_return_line;
DROP TABLE IF EXISTS sal_return;
DROP TABLE IF EXISTS sal_outbound_line;
DROP TABLE IF EXISTS sal_outbound;
DROP TABLE IF EXISTS sal_delivery_note_line;
DROP TABLE IF EXISTS sal_delivery_note;
DROP TABLE IF EXISTS sal_delivery_plan;
DROP TABLE IF EXISTS sal_order_line;
DROP TABLE IF EXISTS sal_order;

DROP TABLE IF EXISTS pur_supplier_score;
DROP TABLE IF EXISTS pur_price;
DROP TABLE IF EXISTS pur_invoice_match;
DROP TABLE IF EXISTS pur_return_line;
DROP TABLE IF EXISTS pur_return;
DROP TABLE IF EXISTS pur_receipt_line;
DROP TABLE IF EXISTS pur_receipt;
DROP TABLE IF EXISTS pur_order_line;
DROP TABLE IF EXISTS pur_order;
DROP TABLE IF EXISTS pur_requisition_line;
DROP TABLE IF EXISTS pur_requisition;

DROP TABLE IF EXISTS inv_alert;
DROP TABLE IF EXISTS inv_transfer_line;
DROP TABLE IF EXISTS inv_transfer;
DROP TABLE IF EXISTS inv_stocktake_line;
DROP TABLE IF EXISTS inv_stocktake;
DROP TABLE IF EXISTS inv_batch;
DROP TABLE IF EXISTS inv_transaction;
DROP TABLE IF EXISTS inv_stock;

DROP TABLE IF EXISTS base_location;
DROP TABLE IF EXISTS base_warehouse;
DROP TABLE IF EXISTS base_supplier;
DROP TABLE IF EXISTS base_customer;
DROP TABLE IF EXISTS base_bom_item;
DROP TABLE IF EXISTS base_bom;
DROP TABLE IF EXISTS base_material_code_map;
DROP TABLE IF EXISTS base_material_category;
DROP TABLE IF EXISTS base_material;

DROP TABLE IF EXISTS sys_job_log;
DROP TABLE IF EXISTS sys_export_task;
DROP TABLE IF EXISTS sys_attachment;
DROP TABLE IF EXISTS sys_config;
DROP TABLE IF EXISTS sys_code_sequence;
DROP TABLE IF EXISTS sys_dict_item;
DROP TABLE IF EXISTS sys_dict_type;
DROP TABLE IF EXISTS sys_login_log;
DROP TABLE IF EXISTS sys_audit_log;
DROP TABLE IF EXISTS sys_role_data_scope;
DROP TABLE IF EXISTS sys_dept;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

-- =============================================================================
-- 二、CREATE TABLE 段（按模块顺序：sys_ → base_ → inv_ → pur_ → sal_ → mf_ → fin_）
-- =============================================================================

-- -----------------------------------------------------------------------------
-- sys_ 系统管理域（16 张，SYS-01 ~ SYS-11 / RPT-07 异步导出）
-- -----------------------------------------------------------------------------

-- 1. sys_user 用户表
CREATE TABLE sys_user (
    id                  BIGSERIAL       PRIMARY KEY,
    user_code           VARCHAR(50)     NOT NULL,               -- 用户编号（HR 工号，全局唯一）
    username            VARCHAR(50)     NOT NULL,               -- 登录用户名（全局唯一，禁用作保留字 user）
    password            VARCHAR(100)    NOT NULL,               -- 口令（BCrypt 强度 10 密文，禁止明文）
    real_name           VARCHAR(50)     NOT NULL,               -- 姓名
    dept_id             BIGINT,                                 -- 所属组织/部门（sys_dept.id，逻辑关联）
    warehouse_ids       VARCHAR(500),                           -- 可访问仓库ID集合（逗号分隔，数据权限用）
    phone               VARCHAR(30),                            -- 联系电话
    email               VARCHAR(100),                           -- 邮箱
    gender              SMALLINT        NOT NULL DEFAULT 0,     -- 性别 0-未知 1-男 2-女
    status              SMALLINT        NOT NULL DEFAULT 1,     -- 状态 0-停用 1-启用
    is_admin            SMALLINT        NOT NULL DEFAULT 0,     -- 是否超级管理员 0-否 1-是
    password_update_time TIMESTAMP,                             -- 口令最后修改时间（90 天有效期基准）
    last_login_time     TIMESTAMP,                              -- 最后登录时间
    last_login_ip       VARCHAR(50),                            -- 最后登录IP
    login_fail_count    INT             NOT NULL DEFAULT 0,     -- 连续登录失败次数（阈值 5 次）
    lock_until          TIMESTAMP,                              -- 账号锁定到期时间（锁定 30 分钟）
    create_by           VARCHAR(50)     NOT NULL,               -- 创建人
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(), -- 创建时间
    update_by           VARCHAR(50)     NOT NULL,               -- 修改人
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(), -- 修改时间
    version             INT             NOT NULL DEFAULT 0,     -- 乐观锁版本号
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,     -- 逻辑删除 0-未删 1-已删
    remark              VARCHAR(500)                            -- 备注
);
COMMENT ON TABLE sys_user IS '用户表（SYS-02 用户管理，口令 BCrypt 密文）';
COMMENT ON COLUMN sys_user.status IS '状态 0-停用 1-启用';

-- 2. sys_role 角色表
CREATE TABLE sys_role (
    id                  BIGSERIAL       PRIMARY KEY,
    role_code           VARCHAR(50)     NOT NULL,               -- 角色编码（如 admin、sales_manager）
    role_name           VARCHAR(100)    NOT NULL,               -- 角色名称
    role_sort           INT             NOT NULL DEFAULT 0,     -- 显示顺序
    data_scope          SMALLINT        NOT NULL DEFAULT 1,     -- 数据范围 1-全部 2-本组织及下级 3-本组织 4-本部门及下级 5-本部门 6-仅本人 9-自定义
    is_builtin          SMALLINT        NOT NULL DEFAULT 0,     -- 是否内置角色 0-否 1-是（内置不可删）
    status              SMALLINT        NOT NULL DEFAULT 1,     -- 状态 0-停用 1-启用
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sys_role IS '角色表（SYS-03 角色与菜单权限、SYS-04 数据范围）';

-- 3. sys_menu 菜单与权限点表
CREATE TABLE sys_menu (
    id                  BIGSERIAL       PRIMARY KEY,
    menu_name           VARCHAR(100)    NOT NULL,               -- 菜单/权限点名称
    parent_id           BIGINT          NOT NULL DEFAULT 0,     -- 父级菜单ID（0 为顶级）
    menu_type           SMALLINT        NOT NULL,               -- 类型 1-目录 2-菜单 3-按钮
    path                VARCHAR(200),                           -- 路由地址
    component           VARCHAR(200),                           -- 前端组件路径
    perms               VARCHAR(100),                           -- 权限点标识（如 sales:order:approve）
    icon                VARCHAR(100),                           -- 图标
    menu_sort           INT             NOT NULL DEFAULT 0,     -- 显示顺序
    visible             SMALLINT        NOT NULL DEFAULT 1,     -- 是否显示 0-隐藏 1-显示
    status              SMALLINT        NOT NULL DEFAULT 1,     -- 状态 0-停用 1-启用
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sys_menu IS '菜单与按钮权限点表（SYS-03，三级：目录/菜单/按钮）';

-- 4. sys_user_role 用户-角色关联表
CREATE TABLE sys_user_role (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL,               -- 用户ID（sys_user.id）
    role_id             BIGINT          NOT NULL,               -- 角色ID（sys_role.id）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sys_user_role IS '用户-角色关联表（SYS-03，多角色取权限并集）';

-- 5. sys_role_menu 角色-菜单关联表
CREATE TABLE sys_role_menu (
    id                  BIGSERIAL       PRIMARY KEY,
    role_id             BIGINT          NOT NULL,               -- 角色ID（sys_role.id）
    menu_id             BIGINT          NOT NULL,               -- 菜单ID（sys_menu.id）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sys_role_menu IS '角色-菜单关联表（SYS-03）';

-- 6. sys_dept 组织/部门表
CREATE TABLE sys_dept (
    id                  BIGSERIAL       PRIMARY KEY,
    dept_code           VARCHAR(50)     NOT NULL,               -- 组织编码（如 ORG001）
    dept_name           VARCHAR(100)    NOT NULL,               -- 组织名称
    parent_id           BIGINT          NOT NULL DEFAULT 0,     -- 上级组织ID（0 为顶级）
    dept_type           SMALLINT        NOT NULL,               -- 类型 1-集团 2-生产基地 3-销售分公司 4-部门 5-车间
    dept_level          SMALLINT        NOT NULL DEFAULT 1,     -- 层级（1 为顶级）
    ancestors           VARCHAR(500),                           -- 祖级路径（如 0,1,5，用于"及下级"数据权限）
    leader              VARCHAR(50),                            -- 负责人
    phone               VARCHAR(30),                            -- 联系电话
    dept_sort           INT             NOT NULL DEFAULT 0,     -- 显示顺序
    status              SMALLINT        NOT NULL DEFAULT 1,     -- 状态 0-停用 1-启用
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sys_dept IS '组织/部门表（SYS-02/SYS-04、E-01 多组织维度）';

-- 7. sys_role_data_scope 数据权限范围配置表
CREATE TABLE sys_role_data_scope (
    id                  BIGSERIAL       PRIMARY KEY,
    role_id             BIGINT          NOT NULL,               -- 角色ID（sys_role.id）
    biz_object          VARCHAR(50)     NOT NULL,               -- 业务对象 SALES_ORDER/PURCHASE_ORDER/WORK_ORDER/INVENTORY
    filter_dimension    VARCHAR(50)     NOT NULL,               -- 过滤维度 CREATOR/CUSTOMER_OWNER/BUYER/PROD_ORG/WAREHOUSE
    scope_type          SMALLINT        NOT NULL,               -- 数据范围 1-全部 2-本组织及下级 3-本组织 4-本部门及下级 5-本部门 6-仅本人 9-自定义
    dept_ids            VARCHAR(500),                           -- 自定义勾选组织ID集合（逗号分隔）
    user_ids            VARCHAR(500),                           -- 自定义勾选人员ID集合（逗号分隔）
    field_mask          SMALLINT        NOT NULL DEFAULT 0,     -- 字段级掩码 0-不掩码 1-金额掩码 2-银行账号掩码
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sys_role_data_scope IS '数据权限范围配置表（SYS-04：业务对象 + 过滤维度 + 数据范围 + 字段掩码）';

-- 8. sys_audit_log 审计日志表（只增不改不删，在线保存 ≥180 天）
CREATE TABLE sys_audit_log (
    id                  BIGSERIAL       PRIMARY KEY,
    module              VARCHAR(50)     NOT NULL,               -- 业务模块 PURCHASE/SALES/INVENTORY/MANUFACTURING/FINANCE/SYSTEM
    biz_type            VARCHAR(50),                            -- 业务类型 SALES_ORDER/PURCHASE_ORDER/WORK_ORDER/STOCK_TRANSACTION...
    biz_code            VARCHAR(50),                            -- 单据号（如 SO202608001）
    operate_type        VARCHAR(30)     NOT NULL,               -- 操作类型 CREATE/UPDATE/APPROVE/REJECT/DELETE/POST/UNAPPROVE/CLOSE/REOPEN
    operate_desc        VARCHAR(200),                           -- 操作描述
    operate_by          VARCHAR(50)     NOT NULL,               -- 操作人（用户名）
    operate_time        TIMESTAMP       NOT NULL DEFAULT NOW(), -- 操作时间
    operate_ip          VARCHAR(50),                            -- 操作IP（含 X-Forwarded-For 解析结果）
    request_uri         VARCHAR(200),                           -- 请求地址
    request_method      VARCHAR(10),                            -- 请求方法
    before_json         TEXT,                                   -- 变更前 JSON（白名单字段，超长截断标记）
    after_json          TEXT,                                   -- 变更后 JSON（白名单字段，超长截断标记）
    result_status       SMALLINT        NOT NULL DEFAULT 1,     -- 结果 0-失败 1-成功
    error_code          INT,                                    -- 失败时的错误码
    error_message       VARCHAR(500),                           -- 失败原因
    cost_time           BIGINT,                                 -- 方法耗时（毫秒）
    trace_id            VARCHAR(64),                            -- 链路追踪ID（X-Request-Id）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sys_audit_log IS '审计日志表（SYS-05：@AuditLog + AOP 落库，只增不改不删，在线保存 ≥180 天）';

-- 9. sys_login_log 登录日志表
CREATE TABLE sys_login_log (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT,                                 -- 用户ID（失败且用户不存在时为空）
    username            VARCHAR(50)     NOT NULL,               -- 用户名
    login_type          SMALLINT        NOT NULL,               -- 事件类型 1-登录 2-登出 3-令牌刷新 4-登录失败
    login_time          TIMESTAMP       NOT NULL DEFAULT NOW(), -- 事件时间
    login_ip            VARCHAR(50),                            -- 来源IP
    user_agent          VARCHAR(500),                           -- 客户端 UA
    result_status       SMALLINT        NOT NULL DEFAULT 1,     -- 结果 0-失败 1-成功
    fail_reason         VARCHAR(200),                           -- 失败原因（账号或密码错误/账号锁定/账号停用）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sys_login_log IS '登录日志表（SYS-01：登录/登出/刷新/失败四类事件）';

-- 10. sys_dict_type 字典类型表
CREATE TABLE sys_dict_type (
    id                  BIGSERIAL       PRIMARY KEY,
    dict_type           VARCHAR(100)    NOT NULL,               -- 字典类型编码（如 settlement_method）
    dict_name           VARCHAR(100)    NOT NULL,               -- 字典类型名称
    status              SMALLINT        NOT NULL DEFAULT 1,     -- 状态 0-停用 1-启用
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sys_dict_type IS '字典类型表（SYS-06）';

-- 11. sys_dict_item 字典项表（预留多语言：同一 dict_type + item_value 可按 lang 多行）
CREATE TABLE sys_dict_item (
    id                  BIGSERIAL       PRIMARY KEY,
    dict_type           VARCHAR(100)    NOT NULL,               -- 字典类型编码（sys_dict_type.dict_type）
    item_label          VARCHAR(200)    NOT NULL,               -- 字典项标签（按 lang 存储对应语言文案）
    item_value          VARCHAR(100)    NOT NULL,               -- 字典项值（编码值，不随语言变化）
    lang                VARCHAR(10)     NOT NULL DEFAULT 'zh-CN', -- 语言 E-03 国际化预留：zh-CN/en-US
    item_sort           INT             NOT NULL DEFAULT 0,     -- 显示顺序
    css_class           VARCHAR(100),                           -- 前端标签样式
    is_default          SMALLINT        NOT NULL DEFAULT 0,     -- 是否默认选中 0-否 1-是
    ext_json            TEXT,                                   -- 扩展属性 JSON
    status              SMALLINT        NOT NULL DEFAULT 1,     -- 状态 0-停用 1-启用
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sys_dict_item IS '字典项表（SYS-06；lang 字段为 E-03 国际化预留，本期仅 zh-CN）';

-- 12. sys_code_sequence 单据编号序列表
CREATE TABLE sys_code_sequence (
    id                  BIGSERIAL       PRIMARY KEY,
    biz_type            VARCHAR(30)     NOT NULL,               -- 单据类型 SALES_ORDER/PURCHASE_ORDER/MO/FZ/STOCK_TRANSACTION...
    prefix              VARCHAR(10)     NOT NULL,               -- 编号前缀（SO/PO/GR/MO/FZ/T...）
    period              VARCHAR(6)      NOT NULL,               -- 期间 YYYYMM（按月重置）
    current_no          INT             NOT NULL DEFAULT 0,     -- 当前已用流水号（原子递增）
    seq_length          SMALLINT        NOT NULL DEFAULT 3,     -- 流水位数（期初建账扩展为 5）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sys_code_sequence IS '单据编号序列表（SYS-07：前缀 + YYYYMM + 3 位流水，唯一键 biz_type + period）';

-- 13. sys_config 系统参数表
CREATE TABLE sys_config (
    id                  BIGSERIAL       PRIMARY KEY,
    config_key          VARCHAR(100)    NOT NULL,               -- 参数键（如 inventory.stagnant.days）
    config_name         VARCHAR(200)    NOT NULL,               -- 参数名称
    config_value        VARCHAR(500)    NOT NULL,               -- 参数值
    value_type          SMALLINT        NOT NULL DEFAULT 1,     -- 值类型 1-字符串 2-数字 3-布尔 4-JSON
    config_group        VARCHAR(50)     NOT NULL,               -- 分组 SYSTEM/INVENTORY/PURCHASE/SALES/FINANCE
    is_system           SMALLINT        NOT NULL DEFAULT 0,     -- 是否系统内置 0-否 1-是（内置不可删）
    status              SMALLINT        NOT NULL DEFAULT 1,     -- 状态 0-停用 1-启用
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sys_config IS '系统参数表（SYS-06：阈值/容差/天数集中维护，修改后 ≤1 分钟生效）';

-- 14. sys_attachment 附件表
CREATE TABLE sys_attachment (
    id                  BIGSERIAL       PRIMARY KEY,
    biz_type            VARCHAR(50)     NOT NULL,               -- 关联业务类型 PURCHASE_RECEIPT/SALES_RETURN/BOM...
    biz_id              BIGINT          NOT NULL,               -- 关联业务主键
    biz_code            VARCHAR(50),                            -- 关联单据号
    file_name           VARCHAR(200)    NOT NULL,               -- 原始文件名（服务器端安全处理，防路径穿越）
    file_path           VARCHAR(500)    NOT NULL,               -- 存储路径（相对根目录，不含用户可控路径）
    file_type           VARCHAR(20),                            -- 文件类型 PDF/JPG/PNG/XLSX/DOCX
    file_size           BIGINT          NOT NULL DEFAULT 0,      -- 文件大小（字节，单文件 ≤20MB）
    storage_type        SMALLINT        NOT NULL DEFAULT 1,     -- 存储类型 1-本地目录
    upload_by           VARCHAR(50)     NOT NULL,               -- 上传人
    upload_time         TIMESTAMP       NOT NULL DEFAULT NOW(), -- 上传时间
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sys_attachment IS '附件表（SYS-10：PDF/JPG/PNG/XLSX/DOCX，单文件 ≤20MB、单单据 ≤20 个）';

-- 15. sys_export_task 导出任务表（RPT-07 异步导出，文件保留期默认 7 天）
CREATE TABLE sys_export_task (
    id                  BIGSERIAL       PRIMARY KEY,
    task_code           VARCHAR(50)     NOT NULL,               -- 任务号（EX + YYYYMM + 3 位流水，全局唯一）
    report_name         VARCHAR(100)    NOT NULL,               -- 报表名称（如销售分析报表）
    module              VARCHAR(30)     NOT NULL,               -- 所属模块 FINANCE/INVENTORY/PURCHASE/SALES/MANUFACTURING/REPORT
    query_params        TEXT,                                   -- 筛选条件 JSON（与列表接口同一参数结构，用于复用数据权限）
    export_format       VARCHAR(10)     NOT NULL DEFAULT 'XLSX', -- 导出格式 XLSX/PDF
    total_rows          INT             NOT NULL DEFAULT 0,     -- 预计/实际导出行数（单次 ≤50,000）
    task_status         SMALLINT        NOT NULL DEFAULT 0,     -- 任务状态 0-待执行 1-执行中 2-已完成 3-已失败 4-已过期
    file_path           VARCHAR(500),                           -- 文件存储路径（相对根目录，不含用户可控路径）
    file_size           BIGINT,                                 -- 文件大小（字节）
    error_msg           VARCHAR(500),                           -- 失败原因
    start_time          TIMESTAMP,                              -- 开始时间
    finish_time         TIMESTAMP,                              -- 完成时间
    expire_time         TIMESTAMP,                              -- 文件过期时间（创建时间 + 保留期 rpt.export.retain-days，默认 7 天）
    create_by           VARCHAR(50)     NOT NULL,               -- 创建人（导出发起人）
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(), -- 创建时间
    update_by           VARCHAR(50)     NOT NULL,               -- 修改人
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(), -- 修改时间
    version             INT             NOT NULL DEFAULT 0,     -- 乐观锁版本号
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,     -- 逻辑删除 0-未删 1-已删
    remark              VARCHAR(500)                            -- 备注
);
COMMENT ON TABLE sys_export_task IS '导出任务表（RPT-07：异步导出任务与导出文件生命周期，任务与文件仅创建人可查，保留期默认 7 天）';
COMMENT ON COLUMN sys_export_task.task_status IS '任务状态 0-待执行 1-执行中 2-已完成 3-已失败 4-已过期';
COMMENT ON COLUMN sys_export_task.export_format IS '导出格式 XLSX/PDF';
COMMENT ON COLUMN sys_export_task.create_by IS '创建人（导出发起人，任务仅创建人可查、文件仅创建人可下载）';

-- 16. sys_job_log 定时任务执行日志表（Spring @Scheduled 任务执行留痕，只增不改不删）
CREATE TABLE sys_job_log (
    id                  BIGSERIAL       PRIMARY KEY,
    job_name            VARCHAR(100)    NOT NULL,               -- 任务名（safetyStockAlert/slowMovingDetect/shelfLifeAlert/receivableOverdue/stockReconcile...）
    job_group           VARCHAR(50),                            -- 任务分组（inventory/finance/purchase/sales/manufacturing/system）
    run_no              VARCHAR(50),                            -- 执行批次号（区分同任务同日重跑）
    trigger_type        SMALLINT        NOT NULL DEFAULT 1,     -- 触发方式 1-定时 2-手工 3-重跑
    run_status          SMALLINT        NOT NULL DEFAULT 0,     -- 执行状态 0-执行中 1-成功 2-失败 3-部分成功
    start_time          TIMESTAMP       NOT NULL,               -- 开始时间
    end_time            TIMESTAMP,                              -- 结束时间
    cost_ms             BIGINT,                                 -- 耗时（毫秒）
    affected_rows       INT             NOT NULL DEFAULT 0,     -- 影响行数（处理条数）
    error_msg           VARCHAR(500),                           -- 异常信息（失败或部分成功时填写）
    create_by           VARCHAR(50)     NOT NULL,               -- 创建人（定时任务为 system）
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(), -- 创建时间
    update_by           VARCHAR(50)     NOT NULL,               -- 修改人
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(), -- 修改时间
    version             INT             NOT NULL DEFAULT 0,     -- 乐观锁版本号
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,     -- 逻辑删除 0-未删 1-已删
    remark              VARCHAR(500)                            -- 备注
);
COMMENT ON TABLE sys_job_log IS '定时任务执行日志表（SYS-06 / A-01 / E-05：@Scheduled 任务执行记录，只增不改不删，在线保留 ≥180 天）';
COMMENT ON COLUMN sys_job_log.trigger_type IS '触发方式 1-定时 2-手工 3-重跑';
COMMENT ON COLUMN sys_job_log.run_status IS '执行状态 0-执行中 1-成功 2-失败 3-部分成功';
COMMENT ON COLUMN sys_job_log.job_name IS '任务名（如 safetyStockAlert 安全库存预警、slowMovingDetect 呆滞识别、shelfLifeAlert 保质期预警、receivableOverdue 应收逾期、stockReconcile 库存对账）';

-- -----------------------------------------------------------------------------
-- base_ 基础数据域（9 张，MD-01 ~ MD-06 / BOM-01 ~ BOM-07 / CS-01 ~ CS-03 / INV-01 主数据）
-- -----------------------------------------------------------------------------

-- 17. base_material 物料主数据表
CREATE TABLE base_material (
    id                  BIGSERIAL       PRIMARY KEY,
    material_code       VARCHAR(50)     NOT NULL,               -- 物料编码（2位一级分类+2位二级分类+6位流水，如 01-03-000128）
    material_name       VARCHAR(200)    NOT NULL,               -- 物料名称
    spec                VARCHAR(200),                           -- 规格型号
    brand               VARCHAR(100),                           -- 品牌（MD-02 重复判定四要素之一）
    category_id         BIGINT          NOT NULL,               -- 物料分类ID（base_material_category.id，须为叶子节点）
    material_type       SMALLINT        NOT NULL,               -- 物料类型 1-原材料 2-半成品 3-成品 4-辅料
    source_type         SMALLINT        NOT NULL,               -- 来源类型 1-自制 2-外购 3-外协
    unit                VARCHAR(10)     NOT NULL,               -- 计量单位（个/套/台/米/公斤/片/卷/包）
    drawing_no          VARCHAR(100),                           -- 图号（MD-02，【迁移方案补充项 1】）
    drawing_version     VARCHAR(20),                            -- 图号版本（MD-02，【迁移方案补充项 1】）
    standard_cost       DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 标准成本（财务核算与报价参考）
    safety_stock        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 安全库存量（INV-07 预警基准）
    max_stock           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 最大库存量（MD-03，【迁移方案补充项 2】）
    min_order_qty       DECIMAL(20,6)   NOT NULL DEFAULT 1,     -- 最小采购批量（≥1）
    abc_class           CHAR(1)         NOT NULL DEFAULT 'C',   -- ABC 分类 A/B/C（A 每月盘点）
    lead_time           INT             NOT NULL DEFAULT 0,     -- 采购提前期（天）
    production_lead_time INT            NOT NULL DEFAULT 0,     -- 生产提前期（天，【迁移方案补充项 5】）
    cost_method         SMALLINT        NOT NULL DEFAULT 0,     -- 计价方法 0-移动加权平均 1-先进先出（本期仅实现 0，【补充项 3】）
    inventory_subject_id BIGINT,                                -- 默认存货科目（fin_subject.id 叶子科目，【补充项 4】）
    cost_subject_id     BIGINT,                                 -- 默认成本科目（fin_subject.id 叶子科目，【补充项 4】）
    revenue_subject_id  BIGINT,                                 -- 默认收入科目（fin_subject.id 叶子科目，【补充项 4】）
    default_supplier_id BIGINT,                                 -- 默认供应商（MD-03）
    default_warehouse_id BIGINT,                                -- 默认仓库（MD-03 / INV-01）
    default_location_id BIGINT,                                 -- 默认库位（MD-03 / INV-01）
    is_batch_managed    BOOLEAN         NOT NULL DEFAULT FALSE, -- 是否批次管理（关键料约占 15%）
    is_serial_managed   BOOLEAN         NOT NULL DEFAULT FALSE, -- 是否序列号管理（成品启用）
    is_shelf_life       BOOLEAN         NOT NULL DEFAULT FALSE, -- 是否保质期管理
    shelf_life_days     INT,                                    -- 保质期天数（电解电容 1095 天）
    is_mrp_active       SMALLINT        NOT NULL DEFAULT 1,     -- 是否参与 MRP 运算 0-否 1-是（活跃物料约 5,000 种）
    status              SMALLINT        NOT NULL DEFAULT 0,     -- 状态 0-新建 1-审核 2-启用 3-停用
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE base_material IS '物料主数据表（MD-01~MD-06，编码规则 2+2+6 位）';
COMMENT ON COLUMN base_material.material_type IS '物料类型 1-原材料 2-半成品 3-成品 4-辅料';
COMMENT ON COLUMN base_material.source_type IS '来源类型 1-自制 2-外购 3-外协';
COMMENT ON COLUMN base_material.status IS '状态 0-新建 1-审核 2-启用 3-停用';
COMMENT ON COLUMN base_material.cost_method IS '计价方法 0-移动加权平均（本期唯一实现）1-先进先出（字段保留，本期不启用）';

-- 18. base_material_category 物料分类表
CREATE TABLE base_material_category (
    id                  BIGSERIAL       PRIMARY KEY,
    category_code       VARCHAR(50)     NOT NULL,               -- 分类编码（一级 2 位如 01；二级 4 位如 0103，与物料编码前 4 位对应）
    category_name       VARCHAR(100)    NOT NULL,               -- 分类名称
    parent_id           BIGINT          NOT NULL DEFAULT 0,     -- 父级分类ID（0 为一级）
    category_level      SMALLINT        NOT NULL,               -- 层级 1-一级 2-二级（最多 2 级）
    is_leaf             BOOLEAN         NOT NULL DEFAULT FALSE, -- 是否叶子节点（物料只能挂在叶子节点）
    category_sort       INT             NOT NULL DEFAULT 0,     -- 显示顺序
    status              SMALLINT        NOT NULL DEFAULT 1,     -- 状态 0-停用 1-启用
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE base_material_category IS '物料分类表（MD-06，两级分类树，【迁移方案补充项 6】）';

-- 19. base_material_code_map 物料旧码→新码映射表（迁移追溯）
CREATE TABLE base_material_code_map (
    id                  BIGSERIAL       PRIMARY KEY,
    old_code            VARCHAR(50)     NOT NULL,               -- 旧编码（IC-001 / WZ-089 / PL-023 等）
    old_name            VARCHAR(200),                           -- 旧名称
    source_dept         VARCHAR(50)     NOT NULL,               -- 来源部门 采购部/仓库/生产部
    merge_group         VARCHAR(50),                            -- 合并组编号（如 M-0001）
    new_material_id     BIGINT          NOT NULL,               -- 新物料ID（base_material.id）
    new_material_code   VARCHAR(50)     NOT NULL,               -- 新物料编码（如 01-03-000128）
    migrate_batch       VARCHAR(50),                            -- 迁移批次号
    migrated_at         TIMESTAMP       NOT NULL DEFAULT NOW(), -- 迁移时间
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE base_material_code_map IS '物料旧码→新码映射表（一物多码合并追溯，【迁移方案补充项 7】，建议永久保留）';

-- 20. base_bom BOM 主表
CREATE TABLE base_bom (
    id                  BIGSERIAL       PRIMARY KEY,
    bom_code            VARCHAR(50)     NOT NULL,               -- BOM 编号（BM+父件编码去连字符+2位版本序号，如 BM010300012801）
    parent_material_id  BIGINT          NOT NULL,               -- 父件物料ID（material_type ∈ {2,3}）
    version             VARCHAR(20)     NOT NULL,               -- BOM 版本号（V1.0 / V1.1 / V2.0）
    quantity            DECIMAL(20,6)   NOT NULL DEFAULT 1,     -- 父件基准数量（通常为 1）
    bom_level           INT             NOT NULL DEFAULT 1,     -- BOM 层级（自顶层成品算起，最大 10 层）
    status              SMALLINT        NOT NULL DEFAULT 0,     -- 状态 0-草稿 1-审核 2-生效 3-历史
    effective_date      DATE,                                   -- 生效日期
    expiry_date         DATE,                                   -- 失效日期
    approve_by          VARCHAR(50),                            -- 审核人
    approve_time        TIMESTAMP,                              -- 审核时间
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE base_bom IS 'BOM 主表（BOM-01/BOM-03/BOM-05，同一父件仅一个生效版本）';
COMMENT ON COLUMN base_bom.status IS '状态 0-草稿 1-审核 2-生效 3-历史（仅生效版本参与 MRP 与工单锁版）';

-- 21. base_bom_item BOM 明细表
CREATE TABLE base_bom_item (
    id                  BIGSERIAL       PRIMARY KEY,
    bom_id              BIGINT          NOT NULL,               -- 所属 BOM 头（base_bom.id）
    child_material_id   BIGINT          NOT NULL,               -- 子件物料ID（须为启用状态）
    child_qty           DECIMAL(20,6)   NOT NULL,               -- 子件标准用量（按 1 个父件折算，>0）
    loss_rate           DECIMAL(5,2)    NOT NULL DEFAULT 0,     -- 损耗率（%，0~100）
    scrap_rate          DECIMAL(5,2)    NOT NULL DEFAULT 0,     -- 报废率（%，0~100）
    seq_no              INT             NOT NULL DEFAULT 1,     -- 工序/行序
    is_alternative      BOOLEAN         NOT NULL DEFAULT FALSE, -- 是否替代料
    alternative_priority INT,                                   -- 替代优先级（1 最高）
    replace_rule        SMALLINT        NOT NULL DEFAULT 1,     -- 替代规则 1-完全替代(1:1) 2-部分替代(按比例)
    replace_ratio       DECIMAL(20,6)   NOT NULL DEFAULT 1,     -- 替代比例（部分替代时有效）
    effective_date      DATE,                                   -- 替代关系生效日期
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE base_bom_item IS 'BOM 明细表（BOM-02/BOM-04，同 BOM 内同一子件唯一）';

-- 22. base_customer 客户主数据表
CREATE TABLE base_customer (
    id                  BIGSERIAL       PRIMARY KEY,
    customer_code       VARCHAR(50)     NOT NULL,               -- 客户编码（CUS + 4 位流水，如 CUS0001）
    customer_name       VARCHAR(200)    NOT NULL,               -- 客户名称（工商全称）
    unified_social_credit_code VARCHAR(18),                     -- 统一社会信用代码（18 位，GB 32100 校验）
    contact             VARCHAR(50),                            -- 主联系人
    phone               VARCHAR(30),                            -- 联系电话
    delivery_address    VARCHAR(300),                           -- 默认收货地址（多地址记入 remark）
    settlement_method   VARCHAR(50),                            -- 结算方式（款到发货/月结30天/月结60天/月结90天）
    credit_limit        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 信用额度（≥0）
    credit_days         INT             NOT NULL DEFAULT 60,    -- 信用期限（天）
    region              VARCHAR(50),                            -- 地区维度（华东/华南/华北，CS-03）
    industry            VARCHAR(50),                            -- 行业维度（CS-03）
    scale               VARCHAR(50),                            -- 规模维度（CS-03）
    customer_level      VARCHAR(50),                            -- 客户等级（战略/重点/普通，CS-03 与 SAL-07 价格策略）
    tags                VARCHAR(500),                           -- 多维分类标签集合（逗号分隔，CS-03）
    dept_id             BIGINT,                                 -- 归属销售分公司（sys_dept.id，数据权限）
    owner_id            BIGINT,                                 -- 客户负责人/业务员（sys_user.id，数据权限 SALES_ORDER.CUSTOMER_OWNER）
    bank_name           VARCHAR(100),                           -- 开户行（脱敏展示）
    bank_account        VARCHAR(50),                            -- 银行账号（非财务角色掩码展示 6222****1234）
    status              SMALLINT        NOT NULL DEFAULT 1,     -- 状态 0-停用 1-启用
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE base_customer IS '客户主数据表（CS-01/CS-03，【迁移方案补充项 8】）';

-- 23. base_supplier 供应商主数据表
CREATE TABLE base_supplier (
    id                  BIGSERIAL       PRIMARY KEY,
    supplier_code       VARCHAR(50)     NOT NULL,               -- 供应商编码（SUP + 4 位流水，如 SUP0001）
    supplier_name       VARCHAR(200)    NOT NULL,               -- 供应商名称（工商全称）
    unified_social_credit_code VARCHAR(18),                     -- 统一社会信用代码（18 位）
    contact             VARCHAR(50),                            -- 主联系人
    phone               VARCHAR(30),                            -- 联系电话
    supply_category     VARCHAR(500),                           -- 供货品类（物料分类编码集合，逗号分隔，CS-02/SC-03）
    settlement_method   VARCHAR(50),                            -- 结算方式
    payment_terms       VARCHAR(100),                           -- 付款条件（如月结 30 天）
    bank_name           VARCHAR(100),                           -- 开户行
    bank_account        VARCHAR(50),                            -- 银行账号（非财务角色掩码）
    rating              CHAR(1)         NOT NULL DEFAULT 'C',   -- 评级 A/B/C（PUR-08）
    lead_time           INT             NOT NULL DEFAULT 15,    -- 供应商交货提前期（天）
    latest_score        DECIMAL(5,2)    NOT NULL DEFAULT 0,     -- 最近月度绩效总分（PUR-08，下单展示排序）
    status              SMALLINT        NOT NULL DEFAULT 1,     -- 状态 0-停用 1-启用
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE base_supplier IS '供应商主数据表（CS-02/CS-03、PUR-08，【迁移方案补充项 9】）';

-- 24. base_warehouse 仓库主数据表
CREATE TABLE base_warehouse (
    id                  BIGSERIAL       PRIMARY KEY,
    warehouse_code      VARCHAR(50)     NOT NULL,               -- 仓库编码（如 WH001）
    warehouse_name      VARCHAR(100)    NOT NULL,               -- 仓库名称（原材料仓/半成品仓/苏州成品仓/东莞成品仓）
    warehouse_type      SMALLINT        NOT NULL,               -- 仓库类型 1-原材料仓 2-半成品仓 3-成品仓 4-退货/隔离仓
    dept_id             BIGINT,                                 -- 所属组织（sys_dept.id，E-01）
    address             VARCHAR(200),                           -- 仓库地址
    keeper              VARCHAR(50),                            -- 仓管员
    is_default          SMALLINT        NOT NULL DEFAULT 0,     -- 是否默认仓 0-否 1-是
    status              SMALLINT        NOT NULL DEFAULT 1,     -- 状态 0-停用 1-启用
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE base_warehouse IS '仓库主数据表（INV-01，4 个中心仓，【迁移方案补充项 15】）';

-- 25. base_location 库位主数据表
CREATE TABLE base_location (
    id                  BIGSERIAL       PRIMARY KEY,
    location_code       VARCHAR(50)     NOT NULL,               -- 库位编码（仓库内唯一，如 YL-A-01）
    location_name       VARCHAR(100)    NOT NULL,               -- 库位名称
    warehouse_id        BIGINT          NOT NULL,               -- 所属仓库（base_warehouse.id）
    area                VARCHAR(50),                            -- 库区（如电子料区/结构件区）
    capacity            DECIMAL(20,6),                          -- 库位容量标记
    is_default          SMALLINT        NOT NULL DEFAULT 0,     -- 是否仓库默认库位 0-否 1-是
    status              SMALLINT        NOT NULL DEFAULT 1,     -- 状态 0-停用 1-启用
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE base_location IS '库位主数据表（INV-01，约 260 个库位，【迁移方案补充项 16】）';

-- -----------------------------------------------------------------------------
-- inv_ 库存域（8 张，INV-01 ~ INV-11）
-- -----------------------------------------------------------------------------

-- 26. inv_stock 即时库存快照表
CREATE TABLE inv_stock (
    id                  BIGSERIAL       PRIMARY KEY,
    material_id         BIGINT          NOT NULL,               -- 物料ID（base_material.id）
    warehouse_id        BIGINT          NOT NULL,               -- 仓库ID（base_warehouse.id）
    location_id         BIGINT,                                 -- 库位ID（base_location.id，空值表示未细分库位）
    batch_no            VARCHAR(50),                            -- 批次号（批次管理物料非空）
    qty                 DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 实际库存数量
    frozen_qty          DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 冻结数量（收货待检/预留占用）
    unit_cost           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 结存单位成本（移动加权平均，【迁移方案补充项 13】）
    amount              DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 结存金额 = qty × unit_cost
    production_date     DATE,                                   -- 批次生产日期（【迁移方案补充项 12】）
    expiry_date         DATE,                                   -- 有效期（生产日期 + 保质期天数，【补充项 12】）
    last_transaction_time TIMESTAMP,                            -- 最后变动时间（呆滞判定基准）
    last_in_time        TIMESTAMP,                              -- 最后入库时间
    last_out_time       TIMESTAMP,                              -- 最后出库时间
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,     -- 乐观锁版本号（并发更新必须校验）
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE inv_stock IS '即时库存快照表（INV-01/INV-09，唯一维度：物料+仓库+库位+批次，可用量 = qty - frozen_qty）';

-- 27. inv_transaction 库存流水表（只增不改不删）
CREATE TABLE inv_transaction (
    id                  BIGSERIAL       PRIMARY KEY,
    trans_code          VARCHAR(50)     NOT NULL,               -- 流水号（IV + YYYYMM + 3 位流水；期初建账扩展为 5 位）
    material_id         BIGINT          NOT NULL,               -- 物料ID（base_material.id）
    warehouse_id        BIGINT          NOT NULL,               -- 仓库ID（base_warehouse.id）
    location_id         BIGINT,                                 -- 库位ID（base_location.id）
    batch_no            VARCHAR(50),                            -- 批次号
    trans_type          SMALLINT        NOT NULL,               -- 流水类型 10-采购入库 20-生产入库 30-销售出库 40-生产领料 50-调拨入库 60-调拨出库 70-盘盈 80-盘亏 90-期初建账
    source_biz_type     VARCHAR(30),                            -- 来源单据类型 PURCHASE_RECEIPT/MFG_COMPLETE/SALES_OUTBOUND/MFG_ISSUE/INV_TRANSFER/INV_STOCKTAKE/OPENING
    source_biz_id       BIGINT,                                 -- 来源单据主键ID
    source_biz_code     VARCHAR(50),                            -- 来源单据号（如 GR202610001）
    in_qty              DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 入库数量（增）
    out_qty             DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 出库数量（减）
    before_qty          DECIMAL(20,6)   NOT NULL,               -- 变动前库存
    after_qty           DECIMAL(20,6)   NOT NULL,               -- 变动后库存（after_qty = before_qty + in_qty - out_qty）
    unit_cost           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 成本单价（移动加权平均，FIN-04）
    amount              DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 本次发生金额
    serial_no           VARCHAR(100),                           -- 序列号（成品启用序列号管理时登记，INV-04）
    production_date     DATE,                                   -- 生产日期（批次/保质期物料）
    expiry_date         DATE,                                   -- 有效期（批次/保质期物料）
    trans_time          TIMESTAMP       NOT NULL DEFAULT NOW(), -- 业务发生时间
    is_reversal         SMALLINT        NOT NULL DEFAULT 0,     -- 是否冲销流水 0-否 1-是
    origin_trans_code   VARCHAR(50),                            -- 被冲销的原流水号（冲销时填写）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE inv_transaction IS '库存流水表（INV-02/INV-03/INV-09，只增不改不删，错误靠反向冲销流水更正）';
COMMENT ON COLUMN inv_transaction.trans_type IS '流水类型 10-采购入库 20-生产入库 30-销售出库 40-生产领料 50-调拨入库 60-调拨出库 70-盘盈 80-盘亏 90-期初建账';

-- 28. inv_batch 批次台账表
CREATE TABLE inv_batch (
    id                  BIGSERIAL       PRIMARY KEY,
    material_id         BIGINT          NOT NULL,               -- 物料ID（base_material.id）
    batch_no            VARCHAR(50)     NOT NULL,               -- 批次号（供应商批次号或系统编码）
    supplier_id         BIGINT,                                 -- 供应商ID（来源供应商批次）
    source_biz_type     VARCHAR(30),                            -- 来源类型 PURCHASE_RECEIPT/MFG_COMPLETE/OPENING
    source_biz_id       BIGINT,                                 -- 来源单据ID
    source_biz_code     VARCHAR(50),                            -- 来源单据号
    in_time             TIMESTAMP,                              -- 首次入库时间
    in_qty              DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 入库数量
    qc_report_no        VARCHAR(50),                            -- 质检报告号（INV-04 追溯）
    production_date     DATE,                                   -- 生产日期
    expiry_date         DATE,                                   -- 有效期
    status              SMALLINT        NOT NULL DEFAULT 1,     -- 状态 1-正常 2-冻结 3-超期 4-隔离
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE inv_batch IS '批次台账表（INV-04/INV-05，正向与反向追溯）';

-- 29. inv_stocktake 盘点单主表
CREATE TABLE inv_stocktake (
    id                  BIGSERIAL       PRIMARY KEY,
    stocktake_code      VARCHAR(50)     NOT NULL,               -- 盘点单号（ST + YYYYMM + 3 位流水）
    warehouse_id        BIGINT          NOT NULL,               -- 盘点仓库（base_warehouse.id）
    stocktake_type      SMALLINT        NOT NULL,               -- 盘点类型 1-周期盘点 2-全面盘点 3-期初盘点
    abc_class           CHAR(1),                                -- 盘点范围 ABC 分类（A 每月/B 每季/C 每半年）
    stocktake_date      DATE            NOT NULL,               -- 盘点基准日（账面快照时点）
    status              SMALLINT        NOT NULL DEFAULT 0,     -- 状态 0-待盘点 1-盘点中 2-待审批 3-已审批 4-已关闭
    total_line_count    INT             NOT NULL DEFAULT 0,     -- 盘点行数
    diff_line_count     INT             NOT NULL DEFAULT 0,     -- 差异行数
    gain_qty            DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 盘盈数量合计
    loss_qty            DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 盘亏数量合计
    gain_amount         DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 盘盈金额合计
    loss_amount         DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 盘亏金额合计
    accuracy_rate       DECIMAL(5,2)    NOT NULL DEFAULT 0,     -- 盘点准确率（%）= 1 - 差异行数/总行数
    is_opening          SMALLINT        NOT NULL DEFAULT 0,     -- 是否期初盘点 0-否 1-是（INV-11 期初建账归集）
    approve_by          VARCHAR(50),                            -- 审批人
    approve_time        TIMESTAMP,                              -- 审批时间
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE inv_stocktake IS '盘点单主表（INV-06/INV-11，差异审批通过后才生成 70/80 流水，【补充项 17】）';
COMMENT ON COLUMN inv_stocktake.status IS '状态 0-待盘点 1-盘点中 2-待审批 3-已审批 4-已关闭';

-- 30. inv_stocktake_line 盘点单明细表
CREATE TABLE inv_stocktake_line (
    id                  BIGSERIAL       PRIMARY KEY,
    stocktake_id        BIGINT          NOT NULL,               -- 盘点单ID（inv_stocktake.id）
    line_no             INT             NOT NULL,               -- 行号
    material_id         BIGINT          NOT NULL,               -- 物料ID（base_material.id）
    location_id         BIGINT,                                 -- 库位ID（base_location.id）
    batch_no            VARCHAR(50),                            -- 批次号
    book_qty            DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 账面数量（生成盘点单时冻结的快照）
    actual_qty          DECIMAL(20,6),                          -- 实盘数量
    diff_qty            DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 差异数量（实盘 - 账面）
    unit_cost           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 成本单价
    diff_amount         DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 差异金额
    diff_type           SMALLINT        NOT NULL DEFAULT 0,     -- 差异类型 0-一致 1-盘盈 2-盘亏
    trans_code          VARCHAR(50),                            -- 审批后生成的流水号（70/80）
    line_status         SMALLINT        NOT NULL DEFAULT 0,     -- 行状态 0-待盘 1-已盘
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE inv_stocktake_line IS '盘点单明细表（INV-06，含账面快照与差异）';

-- 31. inv_transfer 调拨单主表
CREATE TABLE inv_transfer (
    id                  BIGSERIAL       PRIMARY KEY,
    transfer_code       VARCHAR(50)     NOT NULL,               -- 调拨单号（TR + YYYYMM + 3 位流水）
    transfer_type       SMALLINT        NOT NULL,               -- 调拨类型 1-库位间调拨 2-仓库间调拨
    source_warehouse_id BIGINT          NOT NULL,               -- 源仓库（base_warehouse.id）
    source_location_id  BIGINT,                                 -- 源库位（base_location.id）
    target_warehouse_id BIGINT          NOT NULL,               -- 目标仓库（base_warehouse.id）
    target_location_id  BIGINT,                                 -- 目标库位（base_location.id）
    transfer_date       DATE            NOT NULL,               -- 调拨日期
    total_qty           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 调拨数量合计
    total_amount        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 调拨金额合计（按成本价结转，不产生损益）
    original_transfer_id BIGINT,                                -- 反向调拨时指向原调拨单ID
    status              SMALLINT        NOT NULL DEFAULT 0,     -- 状态 0-草稿 1-已审核 2-已关闭 3-已撤销
    approve_by          VARCHAR(50),                            -- 审核人
    approve_time        TIMESTAMP,                              -- 审核时间
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE inv_transfer IS '调拨单主表（INV-10，审核后成对生成 60 出 + 50 入流水）';
COMMENT ON COLUMN inv_transfer.status IS '状态 0-草稿 1-已审核 2-已关闭 3-已撤销';

-- 32. inv_transfer_line 调拨单明细表
CREATE TABLE inv_transfer_line (
    id                  BIGSERIAL       PRIMARY KEY,
    transfer_id         BIGINT          NOT NULL,               -- 调拨单ID（inv_transfer.id）
    line_no             INT             NOT NULL,               -- 行号
    material_id         BIGINT          NOT NULL,               -- 物料ID（base_material.id）
    batch_no            VARCHAR(50),                            -- 批次号
    qty                 DECIMAL(20,6)   NOT NULL,               -- 调拨数量（>0）
    unit_cost           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 成本单价
    amount              DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 调拨金额
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE inv_transfer_line IS '调拨单明细表（INV-10）';

-- 33. inv_alert 库存预警表（安全库存/保质期/呆滞三类）
CREATE TABLE inv_alert (
    id                  BIGSERIAL       PRIMARY KEY,
    alert_type          SMALLINT        NOT NULL,               -- 预警类型 1-安全库存 2-保质期 3-呆滞
    alert_date          DATE            NOT NULL,               -- 预警日期（与物料/仓库/批次构成幂等键）
    material_id         BIGINT          NOT NULL,               -- 物料ID（base_material.id）
    warehouse_id        BIGINT,                                 -- 仓库ID（base_warehouse.id）
    location_id         BIGINT,                                 -- 库位ID（base_location.id）
    batch_no            VARCHAR(50),                            -- 批次号
    current_qty         DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 当前数量/可用量
    compare_qty         DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 比较基准（安全库存量）
    suggest_qty         DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 建议补货数量（按最小采购批量取整）
    suggest_date        DATE,                                   -- 建议到货日期（按提前期倒推）
    expiry_date         DATE,                                   -- 有效期（保质期预警）
    days_value          INT             NOT NULL DEFAULT 0,     -- 天数（剩余保质期天数 / 呆滞天数）
    amount              DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 涉及金额（呆滞金额 = qty × unit_cost）
    last_transaction_time TIMESTAMP,                            -- 最后变动时间
    handle_status       SMALLINT        NOT NULL DEFAULT 0,     -- 处理状态 0-未处理 1-已转申请 2-已忽略
    handle_by           VARCHAR(50),                            -- 处理人
    handle_time         TIMESTAMP,                              -- 处理时间
    ignore_reason       VARCHAR(500),                           -- 忽略原因（忽略必填）
    dispose_flag        SMALLINT        NOT NULL DEFAULT 0,     -- 呆滞处置标记 0-未标记 1-继续持有 2-降价处理 3-报废
    source_biz_id       BIGINT,                                 -- 转采购申请后的单据ID（pur_requisition.id）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE inv_alert IS '库存预警表（INV-07 安全库存 / INV-05 保质期 / INV-08 呆滞，每日定时刷新且可重跑）';
COMMENT ON COLUMN inv_alert.alert_type IS '预警类型 1-安全库存 2-保质期 3-呆滞';
COMMENT ON COLUMN inv_alert.handle_status IS '处理状态 0-未处理 1-已转申请 2-已忽略';

-- -----------------------------------------------------------------------------
-- pur_ 采购域（11 张，PUR-01 ~ PUR-08）
-- -----------------------------------------------------------------------------

-- 34. pur_requisition 采购申请主表
CREATE TABLE pur_requisition (
    id                  BIGSERIAL       PRIMARY KEY,
    requisition_code    VARCHAR(50)     NOT NULL,               -- 采购申请号（PA + YYYYMM + 3 位流水）
    source_type         SMALLINT        NOT NULL,               -- 申请来源 1-MRP采购建议 2-手工临时 3-安全库存预警
    mrp_plan_id         BIGINT,                                 -- MRP 计划批次ID（mf_mrp_plan.id，来源 1 时非空）
    apply_by            VARCHAR(50)     NOT NULL,               -- 申请人（用户名）
    dept_id             BIGINT,                                 -- 申请组织（sys_dept.id，数据权限）
    apply_date          DATE            NOT NULL,               -- 申请日期
    require_date        DATE            NOT NULL,               -- 需求日期
    require_warehouse_id BIGINT,                                -- 需求仓库（base_warehouse.id）
    total_amount        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 申请金额合计（不含税）
    apply_reason        VARCHAR(500),                           -- 申请原因（手工临时申请必填）
    approval_level      SMALLINT        NOT NULL DEFAULT 1,     -- 审批层级 1-采购经理 2-上级（金额超阈值 5 万元转上级）
    approval_status     SMALLINT        NOT NULL DEFAULT 0,     -- 审批状态 0-待审 1-通过 2-驳回
    approve_by          VARCHAR(50),                            -- 审批人
    approve_time        TIMESTAMP,                              -- 审批时间
    reject_reason       VARCHAR(500),                           -- 驳回原因
    convert_status      SMALLINT        NOT NULL DEFAULT 0,     -- 转单状态 0-未转单 1-部分转单 2-已转单
    status              SMALLINT        NOT NULL DEFAULT 0,     -- 单据状态 0-草稿 1-已提交 2-已审批 3-已转单 4-已关闭
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE pur_requisition IS '采购申请主表（PUR-01，来源 MRP/手工/安全库存预警）';
COMMENT ON COLUMN pur_requisition.source_type IS '申请来源 1-MRP采购建议 2-手工临时 3-安全库存预警';
COMMENT ON COLUMN pur_requisition.approval_status IS '审批状态 0-待审 1-通过 2-驳回';

-- 35. pur_requisition_line 采购申请明细表
CREATE TABLE pur_requisition_line (
    id                  BIGSERIAL       PRIMARY KEY,
    requisition_id      BIGINT          NOT NULL,               -- 申请单ID（pur_requisition.id）
    line_no             INT             NOT NULL,               -- 行号
    material_id         BIGINT          NOT NULL,               -- 物料ID（base_material.id）
    qty                 DECIMAL(20,6)   NOT NULL,               -- 申请数量（>0）
    unit_price          DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 参考单价（不含税）
    line_amount         DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 行金额（不含税）
    require_date        DATE            NOT NULL,               -- 需求日期
    suggest_supplier_id BIGINT,                                 -- 建议供应商（base_supplier.id）
    mrp_plan_item_id    BIGINT,                                 -- MRP 建议行ID（mf_mrp_plan_item.id）
    converted_status    SMALLINT        NOT NULL DEFAULT 0,     -- 转单状态 0-未转 1-已转采购单（幂等）
    converted_order_id  BIGINT,                                 -- 已转采购订单ID（pur_order.id）
    converted_time      TIMESTAMP,                              -- 转单时间
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE pur_requisition_line IS '采购申请明细表（PUR-01，已转单行不可重复转采购订单）';

-- 36. pur_order 采购订单主表
CREATE TABLE pur_order (
    id                  BIGSERIAL       PRIMARY KEY,
    order_code          VARCHAR(50)     NOT NULL,               -- 采购订单号（PO + YYYYMM + 3 位流水）
    supplier_id         BIGINT          NOT NULL,               -- 供应商ID（base_supplier.id）
    requisition_id      BIGINT,                                 -- 关联采购申请ID（pur_requisition.id）
    buyer_id            BIGINT,                                 -- 采购员（sys_user.id，数据权限 PURCHASE_ORDER.BUYER）
    dept_id             BIGINT,                                 -- 采购组织（sys_dept.id，E-01）
    order_date          DATE            NOT NULL,               -- 下单日期
    required_date       DATE            NOT NULL,               -- 要求到货日期
    delivery_method     VARCHAR(50),                            -- 交货方式
    receipt_warehouse_id BIGINT,                                -- 收货仓库（base_warehouse.id）
    total_amount        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 订单总金额（含税）
    tax_amount          DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 税额合计
    tax_rate            DECIMAL(5,2)    NOT NULL DEFAULT 13.00, -- 默认税率（%）
    currency            VARCHAR(3)      NOT NULL DEFAULT 'CNY', -- 币别（E-02 多币别）
    exchange_rate       DECIMAL(20,6)   NOT NULL DEFAULT 1,     -- 汇率（本位币 = 原币 × 汇率）
    payment_terms       VARCHAR(100),                           -- 付款条件（默认取供应商结算方式）
    source_type         SMALLINT        NOT NULL DEFAULT 1,     -- 来源 1-采购申请转单 2-手工创建
    order_status        SMALLINT        NOT NULL DEFAULT 0,     -- 执行状态 0-草稿 1-审核 2-部分收货 3-已收货 4-已关闭
    settlement_status   SMALLINT        NOT NULL DEFAULT 0,     -- 结算状态 0-未结算 1-部分结算 2-已结算
    approve_by          VARCHAR(50),                            -- 审核人
    approve_time        TIMESTAMP,                              -- 审核时间
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE pur_order IS '采购订单主表（PUR-02，审核后关键字段变更须走变更流程并留痕）';
COMMENT ON COLUMN pur_order.order_status IS '执行状态 0-草稿 1-审核 2-部分收货 3-已收货 4-已关闭';
COMMENT ON COLUMN pur_order.settlement_status IS '结算状态 0-未结算 1-部分结算 2-已结算（不改变执行状态）';

-- 37. pur_order_line 采购订单明细表
CREATE TABLE pur_order_line (
    id                  BIGSERIAL       PRIMARY KEY,
    order_id            BIGINT          NOT NULL,               -- 采购订单ID（pur_order.id）
    line_no             INT             NOT NULL,               -- 行号（1,2,3…单据内唯一，【迁移方案补充项 10】）
    material_id         BIGINT          NOT NULL,               -- 物料ID（base_material.id）
    qty                 DECIMAL(20,6)   NOT NULL,               -- 订购数量（>0）
    unit_price          DECIMAL(20,6)   NOT NULL,               -- 单价（不含税，6 位小数）
    tax_rate            DECIMAL(5,2)    NOT NULL DEFAULT 13.00, -- 税率（%）
    tax_amount          DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 税额（【迁移方案补充项 10】）
    line_amount         DECIMAL(20,6)   NOT NULL,               -- 行金额（含税）
    received_qty        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 已收货数量
    returned_qty        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 已退货数量
    invoiced_qty        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 已开票数量
    matched_qty         DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 三单匹配通过数量
    required_date       DATE            NOT NULL,               -- 要求到货日期
    requisition_line_id BIGINT,                                 -- 关联采购申请行ID（pur_requisition_line.id）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE pur_order_line IS '采购订单明细表（PUR-02，结构与 sal_order_line 对齐）';

-- 38. pur_receipt 采购收货单主表（质检结论落此表，不单独建质检表）
CREATE TABLE pur_receipt (
    id                  BIGSERIAL       PRIMARY KEY,
    receipt_code        VARCHAR(50)     NOT NULL,               -- 收货单号（GR + YYYYMM + 3 位流水）
    pur_order_id        BIGINT          NOT NULL,               -- 关联采购订单ID（pur_order.id）
    supplier_id         BIGINT          NOT NULL,               -- 供应商ID（base_supplier.id）
    receipt_date        TIMESTAMP       NOT NULL DEFAULT NOW(), -- 收货时间
    warehouse_id        BIGINT          NOT NULL,               -- 目标仓库（base_warehouse.id）
    reject_warehouse_id BIGINT,                                 -- 不合格品隔离仓/退货仓（base_warehouse.id）
    total_receipt_qty   DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 收货数量合计
    total_qualified_qty DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 合格数量合计
    total_reject_qty    DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 不合格数量合计
    status              SMALLINT        NOT NULL DEFAULT 0,     -- 状态 0-待检 1-合格入库 2-部分合格 3-退货（整单取明细汇总）
    inspect_by          VARCHAR(50),                            -- 检验人
    inspect_time        TIMESTAMP,                              -- 检验时间
    is_estimated        SMALLINT        NOT NULL DEFAULT 0,     -- 是否已生成暂估应付 0-否 1-是（PUR-06 100% 覆盖）
    estimated_amount    DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 暂估金额
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE pur_receipt IS '采购收货单主表（PUR-03/PUR-06，质检结论落本表，不单独建质检单据与编号序列）';
COMMENT ON COLUMN pur_receipt.status IS '状态 0-待检 1-合格入库 2-部分合格 3-退货';

-- 39. pur_receipt_line 采购收货单明细表（粒度：物料 + 收货批次）
CREATE TABLE pur_receipt_line (
    id                  BIGSERIAL       PRIMARY KEY,
    receipt_id          BIGINT          NOT NULL,               -- 收货单ID（pur_receipt.id）
    line_no             INT             NOT NULL,               -- 行号
    pur_order_line_id   BIGINT,                                 -- 关联采购订单行ID（pur_order_line.id）
    material_id         BIGINT          NOT NULL,               -- 物料ID（base_material.id）
    receipt_qty         DECIMAL(20,6)   NOT NULL,               -- 收货数量
    qualified_qty       DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 合格数量
    reject_qty          DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 不合格数量
    batch_no            VARCHAR(50),                            -- 供应商批次号（批次管理物料必填）
    production_date     DATE,                                   -- 生产日期（保质期物料必填）
    expiry_date         DATE,                                   -- 有效期（生产日期 + 保质期天数）
    unit_price          DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 不含税单价（暂估与匹配基准）
    tax_rate            DECIMAL(5,2)    NOT NULL DEFAULT 13.00, -- 税率（%）
    amount              DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 暂估金额（不含税）
    location_id         BIGINT,                                 -- 入库库位（base_location.id）
    trans_code          VARCHAR(50),                            -- 生成的入库流水号（trans_type=10）
    status              SMALLINT        NOT NULL DEFAULT 0,     -- 行状态 0-待检 1-合格入库 2-部分合格 3-退货
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE pur_receipt_line IS '采购收货单明细表（PUR-03，qualified_qty + reject_qty = receipt_qty）';

-- 40. pur_return 采购退货单主表
CREATE TABLE pur_return (
    id                  BIGSERIAL       PRIMARY KEY,
    return_code         VARCHAR(50)     NOT NULL,               -- 采购退货单号（PR + YYYYMM + 3 位流水）
    pur_order_id        BIGINT,                                 -- 关联采购订单ID（pur_order.id）
    receipt_id          BIGINT,                                 -- 关联收货单ID（pur_receipt.id）
    supplier_id         BIGINT          NOT NULL,               -- 供应商ID（base_supplier.id）
    warehouse_id        BIGINT          NOT NULL,               -- 出库仓库（base_warehouse.id）
    return_date         DATE            NOT NULL,               -- 退货日期
    return_type         SMALLINT        NOT NULL,               -- 退货类型 1-不合格品退货 2-多余物料退货
    total_qty           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 退货数量合计
    total_amount        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 退货金额合计（不含税）
    status              SMALLINT        NOT NULL DEFAULT 0,     -- 状态 0-草稿 1-已审核 2-在途 3-供应商确认 4-已完成 5-已关闭
    confirm_date        DATE,                                   -- 供应商确认日期（超期 30 天未完成退货清单依据）
    approve_by          VARCHAR(50),                            -- 审核人
    approve_time        TIMESTAMP,                              -- 审核时间
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE pur_return IS '采购退货单主表（PUR-04，退货数量 ≤ 收货单不合格数量）';
COMMENT ON COLUMN pur_return.status IS '状态 0-草稿 1-已审核 2-在途 3-供应商确认 4-已完成 5-已关闭';

-- 41. pur_return_line 采购退货单明细表
CREATE TABLE pur_return_line (
    id                  BIGSERIAL       PRIMARY KEY,
    return_id           BIGINT          NOT NULL,               -- 退货单ID（pur_return.id）
    line_no             INT             NOT NULL,               -- 行号
    material_id         BIGINT          NOT NULL,               -- 物料ID（base_material.id）
    receipt_line_id     BIGINT,                                 -- 关联收货明细行ID（pur_receipt_line.id）
    batch_no            VARCHAR(50),                            -- 批次号
    qty                 DECIMAL(20,6)   NOT NULL,               -- 退货数量（>0）
    unit_price          DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 不含税单价
    line_amount         DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 行金额（不含税，冲减应付基准）
    location_id         BIGINT,                                 -- 出库库位（base_location.id）
    trans_code          VARCHAR(50),                            -- 生成的出库流水号（冲减方向）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE pur_return_line IS '采购退货单明细表（PUR-04）';

-- 42. pur_invoice_match 三单匹配表（采购订单 + 收货单 + 供应商发票）
CREATE TABLE pur_invoice_match (
    id                  BIGSERIAL       PRIMARY KEY,
    match_code          VARCHAR(50)     NOT NULL,               -- 匹配单号（IM + YYYYMM + 3 位流水）
    supplier_id         BIGINT          NOT NULL,               -- 供应商ID（base_supplier.id）
    pur_order_id        BIGINT,                                 -- 采购订单ID（pur_order.id）
    receipt_id          BIGINT,                                 -- 收货单ID（pur_receipt.id）
    invoice_no          VARCHAR(50)     NOT NULL,               -- 发票号码（税控/电子发票平台同步）
    invoice_date        DATE,                                   -- 开票日期
    invoice_amount      DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 发票含税金额
    invoice_tax_amount  DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 发票税额
    period              VARCHAR(6),                             -- 会计期间 YYYYMM
    match_date          DATE            NOT NULL,               -- 匹配日期
    qty_tolerance       DECIMAL(5,2)    NOT NULL DEFAULT 2.00,  -- 数量容差（%，SYS-06 默认 ±2%）
    price_tolerance     DECIMAL(5,2)    NOT NULL DEFAULT 0.50,  -- 单价容差（%，SYS-06 默认 ±0.5%）
    match_result        SMALLINT        NOT NULL DEFAULT 0,     -- 匹配结果 0-待匹配 1-通过 2-有差异
    diff_type           SMALLINT        NOT NULL DEFAULT 0,     -- 差异类型 0-无差异 1-数量差异 2-单价差异 3-无订单 4-无收货
    diff_amount         DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 差异金额
    diff_desc           VARCHAR(500),                           -- 差异说明
    handle_status       SMALLINT        NOT NULL DEFAULT 0,     -- 处理状态 0-未处理 1-已处理
    reverse_voucher_id  BIGINT,                                 -- 红冲暂估凭证ID（fin_voucher.id）
    payable_id          BIGINT,                                 -- 生成的正式应付ID（fin_payable.id）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE pur_invoice_match IS '三单匹配表（PUR-05，支持一单多票/一票多单，容差超限生成差异记录）';
COMMENT ON COLUMN pur_invoice_match.match_result IS '匹配结果 0-待匹配 1-通过 2-有差异';
COMMENT ON COLUMN pur_invoice_match.diff_type IS '差异类型 0-无差异 1-数量差异 2-单价差异 3-无订单 4-无收货';

-- 43. pur_price 采购价格历史表
CREATE TABLE pur_price (
    id                  BIGSERIAL       PRIMARY KEY,
    supplier_id         BIGINT          NOT NULL,               -- 供应商ID（base_supplier.id）
    material_id         BIGINT          NOT NULL,               -- 物料ID（base_material.id）
    pur_order_id        BIGINT,                                 -- 来源采购订单ID（pur_order.id）
    order_code          VARCHAR(50),                            -- 来源采购订单号
    order_date          DATE            NOT NULL,               -- 下单日期（价格走势排序基准）
    unit_price          DECIMAL(20,6)   NOT NULL,               -- 不含税单价
    tax_rate            DECIMAL(5,2)    NOT NULL DEFAULT 13.00, -- 税率（%）
    currency            VARCHAR(3)      NOT NULL DEFAULT 'CNY', -- 币别
    price_type          SMALLINT        NOT NULL DEFAULT 1,     -- 价格类型 1-实际成交价 2-供应商报价
    price_note          VARCHAR(500),                           -- 价格说明（高于历史最高价或偏离阈值必填）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE pur_price IS '采购价格历史表（PUR-07，供应商 + 物料维度的价格走势与预警）';

-- 44. pur_supplier_score 供应商绩效评分表
CREATE TABLE pur_supplier_score (
    id                  BIGSERIAL       PRIMARY KEY,
    supplier_id         BIGINT          NOT NULL,               -- 供应商ID（base_supplier.id）
    score_period        VARCHAR(6)      NOT NULL,               -- 评分周期 YYYYMM（自然月）
    delivery_score      DECIMAL(5,2)    NOT NULL DEFAULT 0,     -- 交货准时率得分（满分 40）
    quality_score       DECIMAL(5,2)    NOT NULL DEFAULT 0,     -- 质量合格率得分（满分 30）
    price_score         DECIMAL(5,2)    NOT NULL DEFAULT 0,     -- 价格水平得分（满分 20）
    service_score       DECIMAL(5,2)    NOT NULL DEFAULT 0,     -- 服务响应得分（满分 10）
    total_score         DECIMAL(5,2)    NOT NULL DEFAULT 0,     -- 总得分（满分 100）
    rating              CHAR(1)         NOT NULL DEFAULT 'C',   -- 评级 A ≥90 / B 75~89 / C <75
    delivery_total_batch INT            NOT NULL DEFAULT 0,     -- 收货批次总数
    delivery_on_time_batch INT          NOT NULL DEFAULT 0,     -- 准时交货批次数
    receipt_total_qty   DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 收货数量合计
    receipt_qualified_qty DECIMAL(20,6) NOT NULL DEFAULT 0,     -- 合格数量合计
    price_deviation     DECIMAL(5,2)    NOT NULL DEFAULT 0,     -- 价格偏离度（%）
    service_hours       DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 异常/退货平均响应时长（小时，人工录入）
    calc_time           TIMESTAMP       NOT NULL DEFAULT NOW(), -- 计算时间
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE pur_supplier_score IS '供应商绩效评分表（PUR-08，自然月评分，同月重跑覆盖更新）';

-- -----------------------------------------------------------------------------
-- sal_ 销售域（10 张，SAL-01 ~ SAL-08）
-- -----------------------------------------------------------------------------

-- 45. sal_order 销售订单主表
CREATE TABLE sal_order (
    id                  BIGSERIAL       PRIMARY KEY,
    order_code          VARCHAR(50)     NOT NULL,               -- 销售订单号（SO + YYYYMM + 3 位流水）
    customer_id         BIGINT          NOT NULL,               -- 客户ID（base_customer.id）
    sales_user_id       BIGINT,                                 -- 业务员（sys_user.id，数据权限 SALES_ORDER.CUSTOMER_OWNER）
    dept_id             BIGINT,                                 -- 销售组织/分公司（sys_dept.id，E-01）
    order_date          DATE            NOT NULL,               -- 下单日期
    required_date       DATE            NOT NULL,               -- 要求交货日期（取分批计划最晚批次日期）
    promised_date       DATE,                                   -- 承诺交货日期
    total_amount        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 订单总金额（含税，= Σ line_amount）
    tax_amount          DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 税额合计
    tax_rate            DECIMAL(5,2)    NOT NULL DEFAULT 13.00, -- 默认税率（%）
    currency            VARCHAR(3)      NOT NULL DEFAULT 'CNY', -- 币别（E-02）
    exchange_rate       DECIMAL(20,6)   NOT NULL DEFAULT 1,     -- 汇率
    price_include_tax   SMALLINT        NOT NULL DEFAULT 0,     -- 录入价格口径 0-不含税录入 1-含税录入（SAL-01）
    payment_terms       VARCHAR(100),                           -- 付款条件（默认取客户结算方式）
    contract_no         VARCHAR(50),                            -- 合同号
    credit_occupy_amount DECIMAL(20,6)  NOT NULL DEFAULT 0,     -- 审核时占用信用额度（SAL-02）
    approval_status     SMALLINT        NOT NULL DEFAULT 0,     -- 审批状态 0-待审 1-通过 2-驳回
    special_approval    SMALLINT        NOT NULL DEFAULT 0,     -- 是否特批 0-否 1-是（超额度/超授权价）
    approval_reason     VARCHAR(500),                           -- 特批原因（特批必填）
    approve_by          VARCHAR(50),                            -- 审批人
    approve_time        TIMESTAMP,                              -- 审批时间
    reject_reason       VARCHAR(500),                           -- 驳回原因
    order_status        SMALLINT        NOT NULL DEFAULT 0,     -- 执行状态 0-草稿 1-审核 2-部分发货 3-已发货 4-已关闭
    source_type         SMALLINT        NOT NULL DEFAULT 1,     -- 来源 1-在线录入 2-迁移导入
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sal_order IS '销售订单主表（SAL-01/SAL-02/SAL-04，审批状态与执行状态分离）';
COMMENT ON COLUMN sal_order.order_status IS '执行状态 0-草稿 1-审核 2-部分发货 3-已发货 4-已关闭';
COMMENT ON COLUMN sal_order.approval_status IS '审批状态 0-待审 1-通过 2-驳回';

-- 46. sal_order_line 销售订单明细表
CREATE TABLE sal_order_line (
    id                  BIGSERIAL       PRIMARY KEY,
    order_id            BIGINT          NOT NULL,               -- 销售订单ID（sal_order.id）
    line_no             INT             NOT NULL,               -- 行号（单据内唯一）
    material_id         BIGINT          NOT NULL,               -- 物料ID（base_material.id，仅启用物料）
    qty                 DECIMAL(20,6)   NOT NULL,               -- 销售数量（>0）
    unit_price          DECIMAL(20,6)   NOT NULL,               -- 单价（不含税）
    tax_rate            DECIMAL(5,2)    NOT NULL DEFAULT 13.00, -- 税率（%）
    tax_amount          DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 税额
    line_amount         DECIMAL(20,6)   NOT NULL,               -- 行金额（含税）
    delivered_qty       DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 已发货数量
    outbound_qty        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 已出库数量
    invoiced_qty        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 已开票数量
    returned_qty        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 已退货数量
    required_date       DATE            NOT NULL,               -- 行要求交货日期
    price_policy_id     BIGINT,                                 -- 适用价格策略ID（sal_price_policy.id，SAL-07）
    is_authorized_price SMALLINT        NOT NULL DEFAULT 1,     -- 是否在授权价区间内 0-否（触发特批）1-是
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sal_order_line IS '销售订单明细表（SAL-01，单价不含税、行金额含税）';

-- 47. sal_delivery_plan 分批交货计划表（结构化，不接受文字备注）
CREATE TABLE sal_delivery_plan (
    id                  BIGSERIAL       PRIMARY KEY,
    order_id            BIGINT          NOT NULL,               -- 销售订单ID（sal_order.id）
    order_line_id       BIGINT          NOT NULL,               -- 销售订单行ID（sal_order_line.id）
    line_no             INT             NOT NULL,               -- 订单行号（冗余，便于查询）
    plan_no             INT             NOT NULL,               -- 计划批次序号（行内从 1 起）
    plan_date           DATE            NOT NULL,               -- 计划交货日期
    plan_qty            DECIMAL(20,6)   NOT NULL,               -- 计划交货数量（Σ plan_qty = qty - delivered_qty）
    actual_qty          DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 已发货数量（按发货通知单回写）
    delivery_note_id    BIGINT,                                 -- 已生成的发货通知单ID（sal_delivery_note.id）
    status              SMALLINT        NOT NULL DEFAULT 0,     -- 计划状态 0-未发货 1-部分发货 2-已发货 3-已取消
    source_type         SMALLINT        NOT NULL DEFAULT 1,     -- 来源 1-订单录入 2-迁移导入（备注文字拆解）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sal_delivery_plan IS '分批交货计划表（SAL-01/SAL-05，【迁移方案补充项 11】结构化计划落地）';
COMMENT ON COLUMN sal_delivery_plan.status IS '计划状态 0-未发货 1-部分发货 2-已发货 3-已取消';

-- 48. sal_delivery_note 发货通知单主表
CREATE TABLE sal_delivery_note (
    id                  BIGSERIAL       PRIMARY KEY,
    delivery_code       VARCHAR(50)     NOT NULL,               -- 发货通知单号（DN + YYYYMM + 3 位流水）
    order_id            BIGINT          NOT NULL,               -- 销售订单ID（sal_order.id）
    customer_id         BIGINT          NOT NULL,               -- 客户ID（base_customer.id）
    plan_id             BIGINT,                                 -- 关联分批交货计划ID（sal_delivery_plan.id，一批次一张）
    sales_user_id       BIGINT,                                 -- 业务员（sys_user.id，数据权限）
    dept_id             BIGINT,                                 -- 销售组织（sys_dept.id）
    warehouse_id        BIGINT          NOT NULL,               -- 发货仓库（base_warehouse.id）
    delivery_date       DATE            NOT NULL,               -- 计划交货日期
    receiver            VARCHAR(50),                            -- 收货人
    receiver_phone      VARCHAR(30),                            -- 收货电话
    receive_address     VARCHAR(300),                           -- 收货地址
    total_qty           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 发货数量合计（≤ 未发货余额）
    total_amount        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 发货金额合计（含税）
    status              SMALLINT        NOT NULL DEFAULT 0,     -- 状态 0-草稿 1-已审核 2-已拣货 3-已出库 4-已关闭
    outbound_id         BIGINT,                                 -- 生成的销售出库单ID（sal_outbound.id）
    approve_by          VARCHAR(50),                            -- 审核人
    approve_time        TIMESTAMP,                              -- 审核时间
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sal_delivery_note IS '发货通知单主表（SAL-05，按分批交货计划生成，一批次一张）';
COMMENT ON COLUMN sal_delivery_note.status IS '状态 0-草稿 1-已审核 2-已拣货 3-已出库 4-已关闭';

-- 49. sal_delivery_note_line 发货通知单明细表
CREATE TABLE sal_delivery_note_line (
    id                  BIGSERIAL       PRIMARY KEY,
    delivery_id         BIGINT          NOT NULL,               -- 发货通知单ID（sal_delivery_note.id）
    line_no             INT             NOT NULL,               -- 行号
    order_line_id       BIGINT          NOT NULL,               -- 销售订单行ID（sal_order_line.id）
    material_id         BIGINT          NOT NULL,               -- 物料ID（base_material.id）
    plan_qty            DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 计划交货数量（取自分批计划）
    delivery_qty        DECIMAL(20,6)   NOT NULL,               -- 本次发货数量（≤ 未发货余额）
    batch_no            VARCHAR(50),                            -- 批次号
    location_id         BIGINT,                                 -- 出库库位（base_location.id）
    unit_price          DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 单价（不含税）
    line_amount         DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 行金额（含税）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sal_delivery_note_line IS '发货通知单明细表（SAL-05）';

-- 50. sal_outbound 销售出库单主表
CREATE TABLE sal_outbound (
    id                  BIGSERIAL       PRIMARY KEY,
    outbound_code       VARCHAR(50)     NOT NULL,               -- 销售出库单号（OD + YYYYMM + 3 位流水）
    delivery_id         BIGINT,                                 -- 来源发货通知单ID（sal_delivery_note.id）
    order_id            BIGINT,                                 -- 来源销售订单ID（sal_order.id）
    customer_id         BIGINT          NOT NULL,               -- 客户ID（base_customer.id）
    warehouse_id        BIGINT          NOT NULL,               -- 出库仓库（base_warehouse.id）
    outbound_date       TIMESTAMP       NOT NULL DEFAULT NOW(), -- 出库时间
    total_qty           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 出库数量合计
    total_amount        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 出库收入金额合计（含税）
    total_cost_amount   DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 出库成本金额合计（移动加权平均，FIN-04）
    status              SMALLINT        NOT NULL DEFAULT 0,     -- 状态 0-草稿 1-已出库 2-已红冲
    voucher_id          BIGINT,                                 -- 结转成本凭证ID（fin_voucher.id）
    sales_user_id       BIGINT,                                 -- 业务员（sys_user.id，数据权限）
    dept_id             BIGINT,                                 -- 销售组织（sys_dept.id）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sal_outbound IS '销售出库单主表（SAL-05，审核通过生成 trans_type=30 流水并结转成本）';
COMMENT ON COLUMN sal_outbound.status IS '状态 0-草稿 1-已出库 2-已红冲';

-- 51. sal_outbound_line 销售出库单明细表
CREATE TABLE sal_outbound_line (
    id                  BIGSERIAL       PRIMARY KEY,
    outbound_id         BIGINT          NOT NULL,               -- 销售出库单ID（sal_outbound.id）
    line_no             INT             NOT NULL,               -- 行号
    order_line_id       BIGINT,                                 -- 销售订单行ID（sal_order_line.id）
    material_id         BIGINT          NOT NULL,               -- 物料ID（base_material.id）
    batch_no            VARCHAR(50),                            -- 批次号
    location_id         BIGINT,                                 -- 出库库位（base_location.id）
    qty                 DECIMAL(20,6)   NOT NULL,               -- 出库数量（>0）
    unit_cost           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 出库成本单价（移动加权平均）
    amount              DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 出库成本金额
    serial_no           VARCHAR(100),                           -- 序列号（成品序列号管理）
    trans_code          VARCHAR(50),                            -- 生成的出库流水号（trans_type=30）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sal_outbound_line IS '销售出库单明细表（SAL-05，成本单价写入流水 unit_cost）';

-- 52. sal_return 销售退货单主表（RMA）
CREATE TABLE sal_return (
    id                  BIGSERIAL       PRIMARY KEY,
    return_code         VARCHAR(50)     NOT NULL,               -- 销售退货单号（SR + YYYYMM + 3 位流水）
    order_id            BIGINT,                                 -- 原销售订单ID（sal_order.id）
    outbound_id         BIGINT,                                 -- 原销售出库单ID（sal_outbound.id）
    customer_id         BIGINT          NOT NULL,               -- 客户ID（base_customer.id）
    warehouse_id        BIGINT          NOT NULL,               -- 退货入库仓库（base_warehouse.id）
    return_date         DATE            NOT NULL,               -- 退货申请日期
    return_reason       VARCHAR(500),                           -- 退货原因
    total_qty           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 退货数量合计（≤ 已发货 - 已退货）
    total_amount        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 退货金额合计（含税）
    handle_type         SMALLINT        NOT NULL DEFAULT 1,     -- 处理方式 1-退款 2-换货
    qc_by               VARCHAR(50),                            -- 质检人
    qc_time             TIMESTAMP,                              -- 质检时间
    qc_remark           VARCHAR(500),                           -- 质检说明
    new_delivery_id     BIGINT,                                 -- 换货生成的新发货通知单ID（sal_delivery_note.id）
    status              SMALLINT        NOT NULL DEFAULT 0,     -- 状态 0-已申请 1-待质检 2-质检通过待入库 3-已入库 4-已退款 5-已换货 6-已关闭
    approve_by          VARCHAR(50),                            -- 审批人
    approve_time        TIMESTAMP,                              -- 审批时间
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sal_return IS '销售退货单主表（SAL-06，退货入库成本按原出库单价回冲）';
COMMENT ON COLUMN sal_return.status IS '状态 0-已申请 1-待质检 2-质检通过待入库 3-已入库 4-已退款 5-已换货 6-已关闭';

-- 53. sal_return_line 销售退货单明细表
CREATE TABLE sal_return_line (
    id                  BIGSERIAL       PRIMARY KEY,
    return_id           BIGINT          NOT NULL,               -- 退货单ID（sal_return.id）
    line_no             INT             NOT NULL,               -- 行号
    order_line_id       BIGINT,                                 -- 原销售订单行ID（sal_order_line.id）
    material_id         BIGINT          NOT NULL,               -- 物料ID（base_material.id）
    batch_no            VARCHAR(50),                            -- 批次号
    return_qty          DECIMAL(20,6)   NOT NULL,               -- 退货申请数量（>0）
    qualified_qty       DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 质检合格数量（入库）
    reject_qty          DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 质检不合格数量（不入库）
    unit_price          DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 原销售单价（不含税，冲减应收基准）
    unit_cost           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 原出库成本单价（回冲库存成本）
    line_amount         DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 行金额（含税）
    location_id         BIGINT,                                 -- 入库库位（base_location.id）
    trans_code          VARCHAR(50),                            -- 生成的入库流水号（退货入库方向）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sal_return_line IS '销售退货单明细表（SAL-06）';

-- 54. sal_price_policy 销售价格策略表
CREATE TABLE sal_price_policy (
    id                  BIGSERIAL       PRIMARY KEY,
    policy_code         VARCHAR(50)     NOT NULL,               -- 策略编号（PP + YYYYMM + 3 位流水）
    policy_name         VARCHAR(100)    NOT NULL,               -- 策略名称
    policy_type         SMALLINT        NOT NULL,               -- 策略类型 1-客户+物料 2-客户等级+品类 3-品类 4-标准售价
    customer_id         BIGINT,                                 -- 指定客户（base_customer.id，类型 1）
    customer_level      VARCHAR(50),                            -- 客户等级（类型 2）
    category_id         BIGINT,                                 -- 物料品类（base_material_category.id，类型 2/3）
    material_id         BIGINT,                                 -- 指定物料（base_material.id，类型 1）
    min_qty             DECIMAL(20,6),                          -- 订货数量区间下限
    max_qty             DECIMAL(20,6),                          -- 订货数量区间上限
    price               DECIMAL(20,6)   NOT NULL,               -- 策略单价（不含税）
    discount_amount     DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 折扣金额（如 50 元/台）
    discount_rate       DECIMAL(5,2)    NOT NULL DEFAULT 0,     -- 折扣率（%）
    priority            INT             NOT NULL DEFAULT 1,      -- 优先级（1 最高：客户+物料 > 客户等级+品类 > 品类 > 标准售价）
    effective_date      DATE            NOT NULL,               -- 生效日期
    expiry_date         DATE,                                   -- 失效日期
    status              SMALLINT        NOT NULL DEFAULT 0,     -- 状态 0-草稿 1-审核 2-生效 3-失效
    approve_by          VARCHAR(50),                            -- 审核人
    approve_time        TIMESTAMP,                              -- 审核时间
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE sal_price_policy IS '销售价格策略表（SAL-07，同条件取生效日期最新，人工调价受授权区间约束）';
COMMENT ON COLUMN sal_price_policy.policy_type IS '策略类型 1-客户+物料 2-客户等级+品类 3-品类 4-标准售价';

-- -----------------------------------------------------------------------------
-- mf_ 生产域（9 张，MFG-01 ~ MFG-10）
-- -----------------------------------------------------------------------------

-- 55. mf_mrp_plan MRP 计划主表
CREATE TABLE mf_mrp_plan (
    id                  BIGSERIAL       PRIMARY KEY,
    plan_code           VARCHAR(50)     NOT NULL,               -- MRP 计划批次号（MP + YYYYMM + 3 位流水）
    plan_name           VARCHAR(200),                           -- 计划名称
    run_type            SMALLINT        NOT NULL,               -- 运算范围 1-全量 2-指定销售订单 3-指定物料范围
    sal_order_id        BIGINT,                                 -- 触发的销售订单ID（sal_order.id，范围 2 时非空）
    material_scope      VARCHAR(500),                           -- 指定物料范围（物料ID集合，范围 3 时非空）
    param_json          TEXT,                                   -- 运算参数快照 JSON（MFG-02）
    run_by              VARCHAR(50)     NOT NULL,               -- 运算人
    run_time            TIMESTAMP       NOT NULL DEFAULT NOW(), -- 运算时间
    run_cost_time       BIGINT          NOT NULL DEFAULT 0,     -- 运算耗时（毫秒）
    item_count          INT             NOT NULL DEFAULT 0,     -- 建议行数
    plan_status         SMALLINT        NOT NULL DEFAULT 0,     -- 计划状态 0-新生成 1-已转 2-已关闭
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE mf_mrp_plan IS 'MRP 计划主表（MFG-01/MFG-02，保留历史运算批次与参数快照）';
COMMENT ON COLUMN mf_mrp_plan.plan_status IS '计划状态 0-新生成 1-已转 2-已关闭';

-- 56. mf_mrp_plan_item MRP 计划建议明细表
CREATE TABLE mf_mrp_plan_item (
    id                  BIGSERIAL       PRIMARY KEY,
    plan_id             BIGINT          NOT NULL,               -- MRP 计划ID（mf_mrp_plan.id）
    line_no             INT             NOT NULL DEFAULT 1,     -- 行号
    material_id         BIGINT          NOT NULL,               -- 物料ID（base_material.id）
    level_no            INT             NOT NULL DEFAULT 1,     -- BOM 展开层级
    gross_qty           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 毛需求
    stock_qty           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 现有可用库存
    on_order_qty        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 在途采购量（已审核未入库）
    wip_qty             DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 在制预计产出量（已下达未完工）
    safety_stock        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 安全库存
    net_qty             DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 净需求/建议数量（毛需求-库存-在途-在制+安全库存）
    suggest_type        SMALLINT        NOT NULL,               -- 建议类型 1-采购 2-生产
    suggest_supplier_id BIGINT,                                 -- 建议供应商（base_supplier.id）
    suggest_start_date  DATE,                                   -- 建议开始日期（按提前期倒推）
    suggest_end_date    DATE,                                   -- 建议到货/完工日期
    lead_time           INT             NOT NULL DEFAULT 0,     -- 提前期（天，快照）
    param_missing       SMALLINT        NOT NULL DEFAULT 0,     -- 计划参数缺失标识 0-完整 1-缺失（输出缺失清单）
    converted_status    SMALLINT        NOT NULL DEFAULT 0,     -- 转单状态 0-未转 1-已转采购单 2-已转工单
    converted_biz_id    BIGINT,                                 -- 已转单据ID（pur_requisition.id / mf_order.id）
    converted_time      TIMESTAMP,                              -- 转单时间
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE mf_mrp_plan_item IS 'MRP 计划建议明细表（MFG-03，建议行不可重复转单）';
COMMENT ON COLUMN mf_mrp_plan_item.suggest_type IS '建议类型 1-采购 2-生产';
COMMENT ON COLUMN mf_mrp_plan_item.converted_status IS '转单状态 0-未转 1-已转采购单 2-已转工单';

-- 57. mf_order 生产工单主表（6 态）
CREATE TABLE mf_order (
    id                  BIGSERIAL       PRIMARY KEY,
    work_order_code     VARCHAR(50)     NOT NULL,               -- 工单号（MO + YYYYMM + 3 位流水）
    order_type          SMALLINT        NOT NULL DEFAULT 1,     -- 工单类型 1-自制 2-委外
    sal_order_id        BIGINT,                                 -- 关联销售订单ID（sal_order.id，MTO）
    sal_order_code      VARCHAR(50),                            -- 关联销售订单号（冗余，便于跟踪）
    material_id         BIGINT          NOT NULL,               -- 产出物料ID（base_material.id）
    bom_id              BIGINT          NOT NULL,               -- 下达时锁定的 BOM 版本ID（base_bom.id）
    bom_version         VARCHAR(20)     NOT NULL,               -- 锁定的 BOM 版本号快照
    plan_id             BIGINT,                                 -- 来源 MRP 建议行ID（mf_mrp_plan_item.id）
    planned_qty         DECIMAL(20,6)   NOT NULL,               -- 计划生产数量
    completed_qty       DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 累计完工数量
    qualified_qty       DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 累计合格数量
    scrapped_qty        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 累计报废数量
    planned_start_date  DATE            NOT NULL,               -- 计划开工日期
    planned_end_date    DATE            NOT NULL,               -- 计划完工日期
    actual_start_date   TIMESTAMP,                              -- 实际开工时间（首次领料/报工）
    actual_end_date     TIMESTAMP,                              -- 实际完工时间
    warehouse_id        BIGINT,                                 -- 完工入库仓库（base_warehouse.id）
    dept_id             BIGINT,                                 -- 生产组织/车间（sys_dept.id，数据权限）
    priority            SMALLINT        NOT NULL DEFAULT 3,     -- 优先级 1-紧急 2-高 3-普通
    progress_rate       DECIMAL(5,2)    NOT NULL DEFAULT 0,     -- 工序完成率（%，按报工实时计算）
    qualified_rate      DECIMAL(5,2)    NOT NULL DEFAULT 0,     -- 累计合格率（%）
    order_status        SMALLINT        NOT NULL DEFAULT 0,     -- 工单状态 0-计划 1-已下达 2-领料中 3-加工中 4-已完工 5-已关闭
    release_by          VARCHAR(50),                            -- 下达人
    release_time        TIMESTAMP,                              -- 下达时间
    close_by            VARCHAR(50),                            -- 关闭人
    close_time          TIMESTAMP,                              -- 关闭时间
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE mf_order IS '生产工单主表（MFG-04，工单状态 6 态：0-计划 1-已下达 2-领料中 3-加工中 4-已完工 5-已关闭）';
COMMENT ON COLUMN mf_order.order_status IS '工单状态 0-计划 1-已下达 2-领料中 3-加工中 4-已完工 5-已关闭';
COMMENT ON COLUMN mf_order.order_type IS '工单类型 1-自制 2-委外（MFG-10）';

-- 58. mf_order_bom 工单锁定 BOM 快照表
CREATE TABLE mf_order_bom (
    id                  BIGSERIAL       PRIMARY KEY,
    work_order_id       BIGINT          NOT NULL,               -- 工单ID（mf_order.id）
    parent_material_id  BIGINT          NOT NULL,               -- 父件物料ID（base_material.id）
    bom_id              BIGINT          NOT NULL,               -- 来源 BOM 版本ID（base_bom.id，快照来源）
    bom_version         VARCHAR(20)     NOT NULL,               -- BOM 版本号快照
    line_no             INT             NOT NULL,               -- 行号
    child_material_id   BIGINT          NOT NULL,               -- 子件物料ID（base_material.id）
    child_qty           DECIMAL(20,6)   NOT NULL,               -- 子件标准用量（快照）
    loss_rate           DECIMAL(5,2)    NOT NULL DEFAULT 0,     -- 损耗率（%，快照）
    scrap_rate          DECIMAL(5,2)    NOT NULL DEFAULT 0,     -- 报废率（%，快照）
    planned_qty         DECIMAL(20,6)   NOT NULL,               -- 定额应领数量 = planned_qty × child_qty × (1 + loss_rate/100)
    issued_qty          DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 累计已领数量
    seq_no              INT             NOT NULL DEFAULT 1,     -- 工序/行序
    is_alternative      SMALLINT        NOT NULL DEFAULT 0,     -- 是否替代料领用 0-否 1-是
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE mf_order_bom IS '工单锁定 BOM 快照表（BOM-03/MFG-05，工单下达即冻结用量，后续 BOM 变更不影响已下达工单）';

-- 59. mf_order_issue 生产领料单主表
CREATE TABLE mf_order_issue (
    id                  BIGSERIAL       PRIMARY KEY,
    issue_code          VARCHAR(50)     NOT NULL,               -- 领料单号（MI + YYYYMM + 3 位流水）
    work_order_id       BIGINT          NOT NULL,               -- 关联工单ID（mf_order.id）
    warehouse_id        BIGINT          NOT NULL,               -- 出库仓库（base_warehouse.id）
    issue_date          DATE            NOT NULL,               -- 领料日期
    issue_type          SMALLINT        NOT NULL DEFAULT 1,     -- 领料类型 1-定额领料 2-超定额领料 3-补料
    total_qty           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 领料数量合计
    total_amount        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 领料成本金额合计
    over_rate           DECIMAL(5,2)    NOT NULL DEFAULT 0,     -- 超定额比例（%）
    apply_reason        VARCHAR(500),                           -- 超定额/补料原因（超领必填）
    approval_status     SMALLINT        NOT NULL DEFAULT 0,     -- 审批状态 0-无需审批 1-待审批 2-通过 3-驳回
    approve_by          VARCHAR(50),                            -- 审批人
    approve_time        TIMESTAMP,                              -- 审批时间
    status              SMALLINT        NOT NULL DEFAULT 0,     -- 状态 0-草稿 1-待审批 2-已审核 3-已出库 4-已驳回 5-已关闭
    dept_id             BIGINT,                                 -- 生产组织（sys_dept.id）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE mf_order_issue IS '生产领料单主表（MFG-05，审核通过生成 trans_type=40 流水）';
COMMENT ON COLUMN mf_order_issue.status IS '状态 0-草稿 1-待审批 2-已审核 3-已出库 4-已驳回 5-已关闭';

-- 60. mf_order_issue_line 生产领料单明细表
CREATE TABLE mf_order_issue_line (
    id                  BIGSERIAL       PRIMARY KEY,
    issue_id            BIGINT          NOT NULL,               -- 领料单ID（mf_order_issue.id）
    line_no             INT             NOT NULL,               -- 行号
    bom_item_id         BIGINT,                                 -- 工单 BOM 快照行ID（mf_order_bom.id）
    material_id         BIGINT          NOT NULL,               -- 实际领用物料ID（base_material.id，替代料时为替代料ID）
    original_material_id BIGINT,                                -- 被替代的主料ID（使用替代料时填写，BOM-04）
    planned_qty         DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 定额应领数量（快照）
    actual_qty          DECIMAL(20,6)   NOT NULL,               -- 实际领料数量
    batch_no            VARCHAR(50),                            -- 批次号（批次管理物料必填）
    location_id         BIGINT,                                 -- 出库库位（base_location.id）
    unit_cost           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 出库成本单价（移动加权平均）
    amount              DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 出库成本金额
    trans_code          VARCHAR(50),                            -- 生成的出库流水号（trans_type=40）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE mf_order_issue_line IS '生产领料单明细表（MFG-05，累计领料不得超过定额 ×(1+超领上限 10%)）';

-- 61. mf_order_report 生产报工单
CREATE TABLE mf_order_report (
    id                  BIGSERIAL       PRIMARY KEY,
    report_code         VARCHAR(50)     NOT NULL,               -- 报工单号（MW + YYYYMM + 3 位流水）
    work_order_id       BIGINT          NOT NULL,               -- 关联工单ID（mf_order.id）
    report_date         DATE            NOT NULL,               -- 报工日期
    process_name        VARCHAR(100)    NOT NULL,               -- 工序名称
    process_seq         INT             NOT NULL DEFAULT 1,     -- 工序顺序
    shift               VARCHAR(20),                            -- 班次（白班/夜班）
    report_qty          DECIMAL(20,6)   NOT NULL,               -- 完工数量
    qualified_qty       DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 合格数量
    unqualified_qty     DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 不合格数量
    scrapped_qty        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 报废数量
    work_hours          DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 工时（小时，6 位小数）
    operator            VARCHAR(50),                            -- 操作人
    dept_id             BIGINT,                                 -- 生产组织/车间（sys_dept.id）
    status              SMALLINT        NOT NULL DEFAULT 0,     -- 状态 0-草稿 1-已提交 2-已撤销 3-已归集成本
    revoke_by           VARCHAR(50),                            -- 撤销人
    revoke_time         TIMESTAMP,                              -- 撤销时间
    revoke_reason       VARCHAR(500),                           -- 撤销原因
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE mf_order_report IS '生产报工单（MFG-06，合格数量 + 不合格数量 ≤ 完工数量）';
COMMENT ON COLUMN mf_order_report.status IS '状态 0-草稿 1-已提交 2-已撤销 3-已归集成本';

-- 62. mf_order_cost 工单成本归集表
CREATE TABLE mf_order_cost (
    id                  BIGSERIAL       PRIMARY KEY,
    work_order_id       BIGINT          NOT NULL,               -- 工单ID（mf_order.id）
    score_period        VARCHAR(6)      NOT NULL,               -- 会计期间 YYYYMM
    material_cost       DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 直接材料成本（实际领料金额）
    labor_cost          DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 直接人工成本（报工工时 × 工时单价）
    overhead_cost       DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 制造费用（按工时分摊）
    labor_hours         DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 报工工时合计
    overhead_rate       DECIMAL(5,2)    NOT NULL DEFAULT 0,     -- 制造费用分摊率（%）
    total_cost          DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 成本合计
    completed_qty       DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 本期完工数量
    unit_cost           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 实际单位成本 = total_cost / completed_qty
    wip_cost            DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 未完工在制成本（按月结转）
    standard_cost       DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 标准成本（对比基准）
    cost_diff           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 标准-实际差异
    cost_status         SMALLINT        NOT NULL DEFAULT 1,     -- 状态 1-已归集 2-已结转
    calc_time           TIMESTAMP       NOT NULL DEFAULT NOW(), -- 归集时间
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE mf_order_cost IS '工单成本归集表（MFG-08/FIN-05，材料+人工+制造费用三段可下钻）';

-- 63. mf_order_outsource 委外加工记录表
CREATE TABLE mf_order_outsource (
    id                  BIGSERIAL       PRIMARY KEY,
    outsource_code      VARCHAR(50)     NOT NULL,               -- 委外单号（OS + YYYYMM + 3 位流水）
    work_order_id       BIGINT          NOT NULL,               -- 关联工单ID（mf_order.id）
    supplier_id         BIGINT          NOT NULL,               -- 外协厂商（base_supplier.id）
    process_name        VARCHAR(100),                           -- 外协工序（PCB 贴片/表面处理等）
    material_id         BIGINT          NOT NULL,               -- 外协物料ID（base_material.id）
    out_qty             DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 发出数量
    in_qty              DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 回收数量
    diff_qty            DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 短少数量（发出 - 回收）
    work_price          DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 加工单价
    work_amount         DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 加工费（计入工单制造费用）
    receive_warehouse_id BIGINT,                                -- 回收入库仓库（base_warehouse.id）
    out_date            DATE,                                   -- 发料日期
    expect_return_date  DATE,                                   -- 预计回收日期
    actual_return_date  DATE,                                   -- 实际回收日期
    diff_approval_status SMALLINT       NOT NULL DEFAULT 0,     -- 短少差异审批 0-无需 1-待审 2-通过 3-驳回
    status              SMALLINT        NOT NULL DEFAULT 0,     -- 状态 0-草稿 1-已发料 2-部分回收 3-已回收 4-已结算 5-已关闭
    dept_id             BIGINT,                                 -- 生产组织（sys_dept.id）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE mf_order_outsource IS '委外加工记录表（MFG-10，外协期间物料按委外在途统计，不计入本厂可用库存）';
COMMENT ON COLUMN mf_order_outsource.status IS '状态 0-草稿 1-已发料 2-部分回收 3-已回收 4-已结算 5-已关闭';

-- -----------------------------------------------------------------------------
-- fin_ 财务域（13 张，FIN-01 ~ FIN-09 / FR-09 科目映射）
-- -----------------------------------------------------------------------------

-- 64. fin_subject 会计科目表
CREATE TABLE fin_subject (
    id                  BIGSERIAL       PRIMARY KEY,
    subject_code        VARCHAR(20)     NOT NULL,               -- 科目编码（如 1403、220201）
    subject_name        VARCHAR(100)    NOT NULL,               -- 科目名称
    parent_id           BIGINT          NOT NULL DEFAULT 0,     -- 父级科目ID（0 为一级）
    subject_level       SMALLINT        NOT NULL DEFAULT 1,     -- 层级（1~4 级）
    is_leaf             BOOLEAN         NOT NULL DEFAULT TRUE,  -- 是否叶子科目（仅叶子可记账）
    subject_direction   SMALLINT        NOT NULL,               -- 科目方向 1-借方 2-贷方
    subject_category    SMALLINT        NOT NULL,               -- 科目类别 1-资产 2-负债 3-权益 4-成本 5-损益
    is_inventory_subject SMALLINT       NOT NULL DEFAULT 0,     -- 是否存货类科目 0-否 1-是（与 INV-11/FIN-09 对账）
    status              SMALLINT        NOT NULL DEFAULT 1,     -- 状态 0-停用 1-启用
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE fin_subject IS '会计科目表（FIN-01，1~4 级，叶子科目可记账）';
COMMENT ON COLUMN fin_subject.subject_direction IS '科目方向 1-借方 2-贷方';
COMMENT ON COLUMN fin_subject.subject_category IS '科目类别 1-资产 2-负债 3-权益 4-成本 5-损益';

-- 65. fin_voucher 会计凭证主表
CREATE TABLE fin_voucher (
    id                  BIGSERIAL       PRIMARY KEY,
    voucher_code        VARCHAR(50)     NOT NULL,               -- 凭证号（FZ + YYYYMM + 3 位流水）
    voucher_type        SMALLINT        NOT NULL DEFAULT 1,     -- 凭证类型 1-记账凭证 2-红字凭证 3-期初调账凭证
    voucher_date        DATE            NOT NULL,               -- 记账日期
    period              VARCHAR(6)      NOT NULL,               -- 会计期间 YYYYMM
    source_biz_type     VARCHAR(30),                            -- 来源业务类型 PURCHASE_RECEIPT/PURCHASE_INVOICE/MFG_ISSUE/MFG_COMPLETE/SALES_OUTBOUND/INV_STOCKTAKE/OPENING
    source_biz_id       BIGINT,                                 -- 来源单据ID
    source_biz_code     VARCHAR(50),                            -- 来源单据号（如 GR202610001）
    total_debit         DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 借方合计
    total_credit        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 贷方合计（过账前校验 total_debit = total_credit）
    currency            VARCHAR(3)      NOT NULL DEFAULT 'CNY', -- 币别（E-02）
    exchange_rate       DECIMAL(20,6)   NOT NULL DEFAULT 1,     -- 汇率
    is_reversal         SMALLINT        NOT NULL DEFAULT 0,     -- 是否红冲凭证 0-否 1-是
    origin_voucher_id   BIGINT,                                 -- 被冲销的原凭证ID（fin_voucher.id）
    origin_voucher_code VARCHAR(50),                            -- 被冲销的原凭证号
    attachment_count    INT             NOT NULL DEFAULT 0,     -- 附件张数
    voucher_status      SMALLINT        NOT NULL DEFAULT 0,     -- 凭证状态 0-草稿 1-已过账（过账后不可修改，更正走红冲）
    post_by             VARCHAR(50),                            -- 过账人
    post_time           TIMESTAMP,                              -- 过账时间
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE fin_voucher IS '会计凭证主表（FIN-01，过账后不可修改，更正一律红冲）';
COMMENT ON COLUMN fin_voucher.voucher_status IS '凭证状态 0-草稿 1-已过账（不可修改）';
COMMENT ON COLUMN fin_voucher.voucher_type IS '凭证类型 1-记账凭证 2-红字凭证 3-期初调账凭证';

-- 66. fin_voucher_entry 会计凭证分录表
CREATE TABLE fin_voucher_entry (
    id                  BIGSERIAL       PRIMARY KEY,
    voucher_id          BIGINT          NOT NULL,               -- 凭证ID（fin_voucher.id）
    line_no             INT             NOT NULL,               -- 行号
    subject_id          BIGINT          NOT NULL,               -- 科目ID（fin_subject.id，须为叶子科目）
    subject_code        VARCHAR(20)     NOT NULL,               -- 科目编码（快照，便于报表查询）
    subject_name        VARCHAR(100)    NOT NULL,               -- 科目名称（快照）
    direction           SMALLINT        NOT NULL,               -- 借贷方向 1-借 2-贷
    amount              DECIMAL(20,6)   NOT NULL,               -- 金额（本位币）
    foreign_amount      DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 原币金额（外币业务）
    exchange_rate       DECIMAL(20,6)   NOT NULL DEFAULT 1,     -- 汇率
    summary             VARCHAR(200),                           -- 摘要
    material_id         BIGINT,                                 -- 物料ID（存货类分录辅助核算维度）
    dept_id             BIGINT,                                 -- 组织/部门（辅助核算维度，E-01）
    warehouse_id        BIGINT,                                 -- 仓库（辅助核算维度）
    source_biz_type     VARCHAR(30),                            -- 来源业务类型（冗余，便于下钻）
    source_biz_id       BIGINT,                                 -- 来源单据ID（冗余，便于下钻）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE fin_voucher_entry IS '会计凭证分录表（FIN-01，借/贷方向 + 科目 + 金额，支持辅助核算维度下钻）';

-- 67. fin_voucher_template 凭证模板与科目映射表（FR-09 科目映射可配置化）
CREATE TABLE fin_voucher_template (
    id                  BIGSERIAL       PRIMARY KEY,
    template_code       VARCHAR(50)     NOT NULL,               -- 模板编码（如 PUR_RECEIPT_ESTIMATE）
    template_name       VARCHAR(100)    NOT NULL,               -- 模板名称
    biz_type            VARCHAR(30)     NOT NULL,               -- 业务类型 PURCHASE_RECEIPT/PURCHASE_INVOICE/MFG_ISSUE/MFG_COMPLETE/SALES_OUTBOUND/INV_STOCKTAKE
    entry_no            INT             NOT NULL,               -- 分录行序
    entry_direction     SMALLINT        NOT NULL,               -- 分录方向 1-借 2-贷
    subject_source      SMALLINT        NOT NULL,               -- 科目来源 1-固定科目 2-物料存货科目 3-物料成本科目 4-物料收入科目 5-客户应收科目 6-供应商应付科目 7-固定资产科目 8-累计折旧科目
    subject_id          BIGINT,                                 -- 固定科目ID（subject_source=1 时非空）
    amount_source       SMALLINT        NOT NULL,               -- 金额来源 1-流水金额 2-含税金额 3-不含税金额 4-税额 5-成本金额
    summary_template    VARCHAR(200),                           -- 摘要模板（支持 {orderCode}/{materialCode} 占位符）
    effective_date      DATE,                                   -- 生效日期
    status              SMALLINT        NOT NULL DEFAULT 1,     -- 状态 0-停用 1-启用
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE fin_voucher_template IS '凭证模板与科目映射表（FR-09，科目映射可配置化：新增物料类型/业务类型无需改代码）';
COMMENT ON COLUMN fin_voucher_template.subject_source IS '科目来源 1-固定科目 2-物料存货科目 3-物料成本科目 4-物料收入科目 5-客户应收科目 6-供应商应付科目 7-固定资产科目 8-累计折旧科目';
COMMENT ON COLUMN fin_voucher_template.amount_source IS '金额来源 1-流水金额 2-含税金额 3-不含税金额 4-税额 5-成本金额';

-- 68. fin_receivable 应收账款表
CREATE TABLE fin_receivable (
    id                  BIGSERIAL       PRIMARY KEY,
    receivable_code     VARCHAR(50)     NOT NULL,               -- 应收单号（AR + YYYYMM + 3 位流水）
    customer_id         BIGINT          NOT NULL,               -- 客户ID（base_customer.id）
    sal_order_id        BIGINT,                                 -- 销售订单ID（sal_order.id）
    sal_order_code      VARCHAR(50),                            -- 销售订单号（冗余）
    outbound_id         BIGINT,                                 -- 销售出库单ID（sal_outbound.id）
    invoice_no          VARCHAR(50),                            -- 发票号码（税控/电子发票平台同步）
    invoice_date        DATE,                                   -- 开票日期
    biz_date            DATE            NOT NULL,               -- 立账日期（出库日/开票日）
    due_date            DATE            NOT NULL,               -- 到期日（立账日 + 客户信用期限，月结 60 天）
    amount              DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 应收金额（含税）
    tax_amount          DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 税额
    received_amount     DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 已收金额
    status              SMALLINT        NOT NULL DEFAULT 0,     -- 状态 0-未收款 1-部分收款 2-已收款 3-已关闭
    period              VARCHAR(6),                             -- 会计期间 YYYYMM
    voucher_id          BIGINT,                                 -- 立账凭证ID（fin_voucher.id）
    aging_days          INT             NOT NULL DEFAULT 0,     -- 账龄天数（定时任务刷新）
    overdue_flag        SMALLINT        NOT NULL DEFAULT 0,     -- 是否逾期 0-否 1-是
    sales_user_id       BIGINT,                                 -- 业务员（sys_user.id，数据权限）
    dept_id             BIGINT,                                 -- 销售组织（sys_dept.id）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE fin_receivable IS '应收账款表（FIN-02，账龄 30/60/90/超 90 天，支持一单多收/一收多单核销）';
COMMENT ON COLUMN fin_receivable.status IS '状态 0-未收款 1-部分收款 2-已收款 3-已关闭';

-- 69. fin_payable 应付账款表
CREATE TABLE fin_payable (
    id                  BIGSERIAL       PRIMARY KEY,
    payable_code        VARCHAR(50)     NOT NULL,               -- 应付单号（AP + YYYYMM + 3 位流水）
    supplier_id         BIGINT          NOT NULL,               -- 供应商ID（base_supplier.id）
    pur_order_id        BIGINT,                                 -- 采购订单ID（pur_order.id）
    pur_order_code      VARCHAR(50),                            -- 采购订单号（冗余）
    receipt_id          BIGINT,                                 -- 收货单ID（pur_receipt.id）
    payable_type        SMALLINT        NOT NULL DEFAULT 2,     -- 应付类型 1-暂估应付 2-正式应付
    invoice_no          VARCHAR(50),                            -- 发票号码
    invoice_date        DATE,                                   -- 开票日期
    biz_date            DATE            NOT NULL,               -- 立账日期（收货日/发票匹配日）
    due_date            DATE            NOT NULL,               -- 到期日（收货日 + 付款条件，如月结 30 天）
    amount              DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 应付金额（不含税）
    tax_amount          DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 税额
    paid_amount         DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 已付金额
    status              SMALLINT        NOT NULL DEFAULT 0,     -- 状态 0-未付款 1-部分付款 2-已付款 3-已红冲
    period              VARCHAR(6),                             -- 会计期间 YYYYMM
    voucher_id          BIGINT,                                 -- 立账凭证ID（fin_voucher.id）
    reverse_voucher_id  BIGINT,                                 -- 红冲凭证ID（暂估红冲后暂估余额归零）
    aging_days          INT             NOT NULL DEFAULT 0,     -- 账龄天数（定时任务刷新）
    overdue_flag        SMALLINT        NOT NULL DEFAULT 0,     -- 是否逾期 0-否 1-是
    buyer_id            BIGINT,                                 -- 采购员（sys_user.id，数据权限）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE fin_payable IS '应付账款表（FIN-03，暂估应付与正式应付分列展示）';
COMMENT ON COLUMN fin_payable.payable_type IS '应付类型 1-暂估应付 2-正式应付';
COMMENT ON COLUMN fin_payable.status IS '状态 0-未付款 1-部分付款 2-已付款 3-已红冲';

-- 70. fin_settlement 收款/付款单主表（含付款申请审批与银行回单导入）
CREATE TABLE fin_settlement (
    id                  BIGSERIAL       PRIMARY KEY,
    settlement_code     VARCHAR(50)     NOT NULL,               -- 收款单号 RC+YYYYMM+3 位流水 / 付款单号 PY+YYYYMM+3 位流水
    settlement_type     SMALLINT        NOT NULL,               -- 单据类型 1-收款单 2-付款单
    partner_type        SMALLINT        NOT NULL,               -- 往来单位类型 1-客户 2-供应商
    partner_id          BIGINT          NOT NULL,               -- 往来单位ID（base_customer.id / base_supplier.id）
    dept_id             BIGINT,                                 -- 业务组织（sys_dept.id）
    apply_by            VARCHAR(50),                            -- 申请人（付款申请）
    settlement_date     DATE            NOT NULL,               -- 收付款日期
    amount              DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 收付款金额
    writeoff_amount     DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 已核销金额
    settle_method       SMALLINT        NOT NULL DEFAULT 1,     -- 结算方式 1-电汇 2-银行承兑 3-现金 4-支票 5-其他
    bank_name           VARCHAR(100),                           -- 银行名称
    bank_account        VARCHAR(50),                            -- 银行账号（脱敏展示）
    bank_receipt_no     VARCHAR(50),                            -- 银行回单号（人工导入，银企直连本期不实现）
    approval_status     SMALLINT        NOT NULL DEFAULT 0,     -- 审批状态 0-无需审批 1-待审 2-通过 3-驳回
    approve_by          VARCHAR(50),                            -- 审批人
    approve_time        TIMESTAMP,                              -- 审批时间
    status              SMALLINT        NOT NULL DEFAULT 0,     -- 状态 0-草稿 1-待审批 2-审批通过 3-已核销 4-已驳回 5-已作废
    voucher_id          BIGINT,                                 -- 生成凭证ID（fin_voucher.id）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE fin_settlement IS '收款/付款单主表（FIN-02/FIN-03，付款申请审批 + 银行回单人工导入核销）';
COMMENT ON COLUMN fin_settlement.status IS '状态 0-草稿 1-待审批 2-审批通过 3-已核销 4-已驳回 5-已作废';

-- 71. fin_settlement_entry 收付款核销明细表
CREATE TABLE fin_settlement_entry (
    id                  BIGSERIAL       PRIMARY KEY,
    settlement_id       BIGINT          NOT NULL,               -- 收付款单ID（fin_settlement.id）
    line_no             INT             NOT NULL,               -- 行号
    biz_type            SMALLINT        NOT NULL,               -- 核销对象 1-应收单 2-应付单
    biz_id              BIGINT          NOT NULL,               -- 应收单ID（fin_receivable.id）/ 应付单ID（fin_payable.id）
    biz_code            VARCHAR(50),                            -- 应收/应付单号
    writeoff_amount     DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 本次核销金额
    writeoff_time       TIMESTAMP       NOT NULL DEFAULT NOW(), -- 核销时间
    is_reverse          SMALLINT        NOT NULL DEFAULT 0,     -- 是否反核销 0-否 1-是
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE fin_settlement_entry IS '收付款核销明细表（FIN-02/FIN-03，支持一单多收与一收多单）';

-- 72. fin_cost_ledger 存货核算成本流水表
CREATE TABLE fin_cost_ledger (
    id                  BIGSERIAL       PRIMARY KEY,
    period              VARCHAR(6)      NOT NULL,               -- 会计期间 YYYYMM
    material_id         BIGINT          NOT NULL,               -- 物料ID（base_material.id）
    warehouse_id        BIGINT,                                 -- 仓库ID（base_warehouse.id）
    batch_no            VARCHAR(50),                            -- 批次号
    trans_code          VARCHAR(50),                            -- 对应库存流水号（inv_transaction.trans_code）
    trans_time          TIMESTAMP,                              -- 业务发生时间
    biz_type            SMALLINT        NOT NULL,               -- 业务类型 1-入库核算 2-出库核算
    qty                 DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 本次数量
    unit_cost           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 本次单位成本（移动加权平均）
    amount              DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 本次金额
    begin_qty           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 滚动期初数量
    begin_amount        DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 滚动期初金额
    end_qty             DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 滚动期末数量
    end_amount          DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 滚动期末金额
    cost_method         SMALLINT        NOT NULL DEFAULT 0,     -- 计价方法 0-移动加权平均（本期唯一实现）
    calc_time           TIMESTAMP       NOT NULL DEFAULT NOW(), -- 核算时间
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE fin_cost_ledger IS '存货核算成本流水表（FIN-04，逐笔成本与滚动结存；收发存汇总按本表实时聚合，本期不建独立汇总表）';
COMMENT ON COLUMN fin_cost_ledger.biz_type IS '业务类型 1-入库核算 2-出库核算';

-- 73. fin_period 会计期间表
CREATE TABLE fin_period (
    id                  BIGSERIAL       PRIMARY KEY,
    period              VARCHAR(6)      NOT NULL,               -- 会计期间 YYYYMM
    period_year         INT             NOT NULL,               -- 年度
    period_month        INT             NOT NULL,               -- 月份（1~12）
    start_date          DATE            NOT NULL,               -- 期间开始日期
    end_date            DATE            NOT NULL,               -- 期间结束日期
    status              SMALLINT        NOT NULL DEFAULT 0,     -- 期间状态 0-未启用 1-开放 2-已关闭
    is_current          SMALLINT        NOT NULL DEFAULT 0,     -- 是否当前期间 0-否 1-是（全局唯一）
    unposted_voucher_count INT          NOT NULL DEFAULT 0,     -- 月结检查①未过账凭证数（须为 0）
    unmatched_estimated_count INT       NOT NULL DEFAULT 0,     -- 月结检查②上月暂估未匹配条数（须为 0）
    stock_check_diff    INT             NOT NULL DEFAULT 0,     -- 月结检查③库存流水与快照对账差异条数（须为 0）
    wip_order_amount    DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 月结检查④未完工工单在制余额
    unsettled_doc_count INT             NOT NULL DEFAULT 0,     -- 月结检查⑤未结算单据数
    check_json          TEXT,                                   -- 月结检查清单结果快照 JSON
    close_by            VARCHAR(50),                            -- 结账人
    close_time          TIMESTAMP,                              -- 结账时间
    reopen_count        INT             NOT NULL DEFAULT 0,     -- 期间重开次数（仅财务经理权限）
    last_reopen_by      VARCHAR(50),                            -- 最近重开人
    last_reopen_time    TIMESTAMP,                              -- 最近重开时间
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE fin_period IS '会计期间表（FIN-08，月结检查清单 5 项全部通过方可关闭；关闭后禁止新增/修改/过账）';
COMMENT ON COLUMN fin_period.status IS '期间状态 0-未启用 1-开放 2-已关闭';

-- 74. fin_opening_balance 期初余额建账表
CREATE TABLE fin_opening_balance (
    id                  BIGSERIAL       PRIMARY KEY,
    balance_type        SMALLINT        NOT NULL,               -- 建账类型 1-科目期初余额 2-存货期初（与 INV-11 联动）
    period              VARCHAR(6)      NOT NULL,               -- 首个会计期间 YYYYMM
    subject_id          BIGINT,                                 -- 科目ID（fin_subject.id，类型 1）
    subject_code        VARCHAR(20),                            -- 科目编码（快照）
    dept_id             BIGINT,                                 -- 组织维度（sys_dept.id）
    direction           SMALLINT,                               -- 余额方向 1-借 2-贷
    amount              DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 期初金额（本位币）
    material_id         BIGINT,                                 -- 物料ID（类型 2）
    warehouse_id        BIGINT,                                 -- 仓库ID（类型 2）
    batch_no            VARCHAR(50),                            -- 批次号（类型 2）
    qty                 DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 期初数量（类型 2）
    unit_cost           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 期初单价（类型 2）
    trial_balance_diff  DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 试算平衡差额（贷方合计 - 借方合计，须为 0）
    voucher_id          BIGINT,                                 -- 期初调账凭证ID（fin_voucher.id）
    import_batch        VARCHAR(50),                            -- 导入批次号
    status              SMALLINT        NOT NULL DEFAULT 0,     -- 状态 0-草稿 1-已提交 2-已生成凭证 3-已锁定（首期月结后不可修改）
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE fin_opening_balance IS '期初余额建账表（FIN-09，试算平衡差额必须为 0；存货期初金额须与 INV-11 一致）';
COMMENT ON COLUMN fin_opening_balance.balance_type IS '建账类型 1-科目期初余额 2-存货期初';
COMMENT ON COLUMN fin_opening_balance.status IS '状态 0-草稿 1-已提交 2-已生成凭证 3-已锁定';

-- 75. fin_asset 固定资产卡片表
CREATE TABLE fin_asset (
    id                  BIGSERIAL       PRIMARY KEY,
    asset_code          VARCHAR(50)     NOT NULL,               -- 资产编号（FA + YYYYMM + 3 位流水）
    asset_name          VARCHAR(200)    NOT NULL,               -- 资产名称
    asset_category      VARCHAR(50),                            -- 资产类别（字典维护）
    original_value      DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 资产原值
    residual_rate       DECIMAL(5,2)    NOT NULL DEFAULT 5.00,  -- 残值率（%）
    useful_life_months  INT             NOT NULL,               -- 使用年限（月）
    depreciation_method SMALLINT        NOT NULL DEFAULT 1,     -- 折旧方法 1-年限平均法 2-双倍余额递减法
    monthly_depreciation DECIMAL(20,6)  NOT NULL DEFAULT 0,     -- 月折旧额
    accumulated_depreciation DECIMAL(20,6) NOT NULL DEFAULT 0,  -- 累计折旧
    net_value           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 净值 = 原值 - 累计折旧
    depreciation_subject_id BIGINT,                             -- 累计折旧科目（fin_subject.id）
    expense_subject_id  BIGINT,                                 -- 折旧费用科目（fin_subject.id）
    used_dept_id        BIGINT,                                 -- 使用部门（sys_dept.id）
    location            VARCHAR(200),                           -- 存放地点
    start_date          DATE            NOT NULL,               -- 启用日期
    last_depreciation_period VARCHAR(6),                        -- 最后计提期间 YYYYMM
    asset_status        SMALLINT        NOT NULL DEFAULT 1,     -- 资产状态 1-在用 2-闲置 3-报废 4-已处置
    scrap_date          DATE,                                   -- 报废/处置日期
    scrap_voucher_id    BIGINT,                                 -- 处置损益凭证ID（fin_voucher.id）
    scrap_gain_loss     DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 处置损益金额
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE fin_asset IS '固定资产卡片表（FIN-06，年限平均法/双倍余额递减法按月计提，本期在用资产卡片在线重建）';
COMMENT ON COLUMN fin_asset.asset_status IS '资产状态 1-在用 2-闲置 3-报废 4-已处置';
COMMENT ON COLUMN fin_asset.depreciation_method IS '折旧方法 1-年限平均法 2-双倍余额递减法';

-- 76. fin_asset_depreciation 固定资产折旧明细表
CREATE TABLE fin_asset_depreciation (
    id                  BIGSERIAL       PRIMARY KEY,
    asset_id            BIGINT          NOT NULL,               -- 资产ID（fin_asset.id）
    period              VARCHAR(6)      NOT NULL,               -- 会计期间 YYYYMM
    depreciation_amount DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 本期折旧额
    accumulated_depreciation DECIMAL(20,6) NOT NULL DEFAULT 0,  -- 累计折旧（含本期）
    net_value           DECIMAL(20,6)   NOT NULL DEFAULT 0,     -- 期末净值
    dept_id             BIGINT,                                 -- 使用部门（sys_dept.id，辅助核算）
    voucher_id          BIGINT,                                 -- 折旧凭证ID（fin_voucher.id）
    depreciation_status SMALLINT        NOT NULL DEFAULT 0,     -- 计提状态 0-未计提 1-已计提 2-已生成凭证
    calc_time           TIMESTAMP       NOT NULL DEFAULT NOW(), -- 计算时间
    create_by           VARCHAR(50)     NOT NULL,
    create_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_by           VARCHAR(50)     NOT NULL,
    update_time         TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             INT             NOT NULL DEFAULT 0,
    is_deleted          SMALLINT        NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);
COMMENT ON TABLE fin_asset_depreciation IS '固定资产折旧明细表（FIN-06，同资产同期间唯一，凭证借贷平衡）';
COMMENT ON COLUMN fin_asset_depreciation.depreciation_status IS '计提状态 0-未计提 1-已计提 2-已生成凭证';

-- =============================================================================
-- 三、索引段
--   命名规范：uk_表名_字段（唯一）、idx_表名_字段（普通）；
--   逻辑删除表的业务唯一键一律使用部分唯一索引 WHERE is_deleted = 0；
--   空值可空维度（location_id / batch_no）使用 COALESCE 表达式索引，避免 PostgreSQL
--   将多个 NULL 视为互不相等而导致重复数据；单表索引数控制在 5~6 个以内。
-- =============================================================================

-- sys_ 系统管理域
CREATE UNIQUE INDEX uk_sys_user_username ON sys_user (username) WHERE is_deleted = 0;
CREATE UNIQUE INDEX uk_sys_user_code ON sys_user (user_code) WHERE is_deleted = 0;
CREATE INDEX idx_sys_user_dept ON sys_user (dept_id);
CREATE INDEX idx_sys_user_status ON sys_user (status);
CREATE UNIQUE INDEX uk_sys_role_code ON sys_role (role_code) WHERE is_deleted = 0;
CREATE INDEX idx_sys_role_status ON sys_role (status);
CREATE INDEX idx_sys_menu_parent ON sys_menu (parent_id);
CREATE INDEX idx_sys_menu_perms ON sys_menu (perms);
CREATE UNIQUE INDEX uk_sys_user_role ON sys_user_role (user_id, role_id) WHERE is_deleted = 0;
CREATE INDEX idx_sys_user_role_role ON sys_user_role (role_id);
CREATE UNIQUE INDEX uk_sys_role_menu ON sys_role_menu (role_id, menu_id) WHERE is_deleted = 0;
CREATE INDEX idx_sys_role_menu_menu ON sys_role_menu (menu_id);
CREATE UNIQUE INDEX uk_sys_dept_code ON sys_dept (dept_code) WHERE is_deleted = 0;
CREATE INDEX idx_sys_dept_parent ON sys_dept (parent_id);
CREATE INDEX idx_sys_role_data_scope_role ON sys_role_data_scope (role_id, biz_object);
CREATE INDEX idx_sys_audit_log_biz_code ON sys_audit_log (biz_code);
CREATE INDEX idx_sys_audit_log_operate_time ON sys_audit_log (operate_time);
CREATE INDEX idx_sys_audit_log_operate_by ON sys_audit_log (operate_by);
CREATE INDEX idx_sys_login_log_username ON sys_login_log (username, login_time);
CREATE INDEX idx_sys_login_log_login_time ON sys_login_log (login_time);
CREATE UNIQUE INDEX uk_sys_dict_type ON sys_dict_type (dict_type) WHERE is_deleted = 0;
CREATE UNIQUE INDEX uk_sys_dict_item ON sys_dict_item (dict_type, item_value, lang) WHERE is_deleted = 0;
CREATE INDEX idx_sys_dict_item_type ON sys_dict_item (dict_type);
CREATE UNIQUE INDEX uk_sys_code_sequence ON sys_code_sequence (biz_type, period) WHERE is_deleted = 0;
CREATE UNIQUE INDEX uk_sys_config_key ON sys_config (config_key) WHERE is_deleted = 0;
CREATE INDEX idx_sys_config_group ON sys_config (config_group);
CREATE INDEX idx_sys_attachment_biz ON sys_attachment (biz_type, biz_id);
CREATE UNIQUE INDEX uk_sys_export_task_code ON sys_export_task (task_code) WHERE is_deleted = 0;
CREATE INDEX idx_sys_export_task_create_by ON sys_export_task (create_by);
CREATE INDEX idx_sys_export_task_status ON sys_export_task (task_status);
CREATE INDEX idx_sys_export_task_create_time ON sys_export_task (create_time);
CREATE INDEX idx_sys_job_log_name_time ON sys_job_log (job_name, start_time);
CREATE INDEX idx_sys_job_log_run_status ON sys_job_log (run_status);

-- base_ 基础数据域
CREATE UNIQUE INDEX uk_base_material_code ON base_material (material_code) WHERE is_deleted = 0;
CREATE INDEX idx_base_material_category ON base_material (category_id);
CREATE INDEX idx_base_material_name ON base_material (material_name);
CREATE INDEX idx_base_material_status ON base_material (status, material_type);
CREATE INDEX idx_base_material_mrp ON base_material (is_mrp_active);
CREATE UNIQUE INDEX uk_base_material_category_code ON base_material_category (category_code) WHERE is_deleted = 0;
CREATE INDEX idx_base_material_category_parent ON base_material_category (parent_id);
CREATE UNIQUE INDEX uk_base_material_code_map ON base_material_code_map (old_code, source_dept) WHERE is_deleted = 0;
CREATE INDEX idx_base_material_code_map_new ON base_material_code_map (new_material_id);
CREATE UNIQUE INDEX uk_base_bom_code ON base_bom (bom_code) WHERE is_deleted = 0;
CREATE INDEX idx_base_bom_parent ON base_bom (parent_material_id, status);
CREATE UNIQUE INDEX uk_base_bom_version ON base_bom (parent_material_id, version) WHERE is_deleted = 0;
CREATE UNIQUE INDEX uk_base_bom_item ON base_bom_item (bom_id, child_material_id) WHERE is_deleted = 0;
CREATE INDEX idx_base_bom_item_child ON base_bom_item (child_material_id);
CREATE UNIQUE INDEX uk_base_customer_code ON base_customer (customer_code) WHERE is_deleted = 0;
CREATE INDEX idx_base_customer_name ON base_customer (customer_name);
CREATE INDEX idx_base_customer_owner ON base_customer (owner_id);
CREATE INDEX idx_base_customer_status ON base_customer (status);
CREATE UNIQUE INDEX uk_base_supplier_code ON base_supplier (supplier_code) WHERE is_deleted = 0;
CREATE INDEX idx_base_supplier_name ON base_supplier (supplier_name);
CREATE INDEX idx_base_supplier_status ON base_supplier (status);
CREATE UNIQUE INDEX uk_base_warehouse_code ON base_warehouse (warehouse_code) WHERE is_deleted = 0;
CREATE INDEX idx_base_warehouse_type ON base_warehouse (warehouse_type);
CREATE UNIQUE INDEX uk_base_location ON base_location (warehouse_id, location_code) WHERE is_deleted = 0;
CREATE INDEX idx_base_location_warehouse ON base_location (warehouse_id, status);

-- inv_ 库存域
CREATE UNIQUE INDEX uk_inv_stock_dim ON inv_stock (material_id, warehouse_id, COALESCE(location_id, 0), COALESCE(batch_no, '')) WHERE is_deleted = 0;
CREATE INDEX idx_inv_stock_warehouse ON inv_stock (warehouse_id, material_id);
CREATE INDEX idx_inv_stock_expiry ON inv_stock (expiry_date);
CREATE UNIQUE INDEX uk_inv_transaction_code ON inv_transaction (trans_code) WHERE is_deleted = 0;
CREATE UNIQUE INDEX uk_inv_transaction_source ON inv_transaction (source_biz_type, source_biz_id, material_id, trans_type, COALESCE(location_id, 0), COALESCE(batch_no, '')) WHERE source_biz_type IS NOT NULL AND is_deleted = 0;
CREATE INDEX idx_inv_transaction_material_time ON inv_transaction (material_id, trans_time);
CREATE INDEX idx_inv_transaction_source ON inv_transaction (source_biz_type, source_biz_id);
CREATE INDEX idx_inv_transaction_time ON inv_transaction (trans_time);
CREATE INDEX idx_inv_transaction_type ON inv_transaction (trans_type);
CREATE UNIQUE INDEX uk_inv_batch ON inv_batch (material_id, batch_no) WHERE is_deleted = 0;
CREATE INDEX idx_inv_batch_expiry ON inv_batch (expiry_date);
CREATE UNIQUE INDEX uk_inv_stocktake_code ON inv_stocktake (stocktake_code) WHERE is_deleted = 0;
CREATE INDEX idx_inv_stocktake_warehouse ON inv_stocktake (warehouse_id, stocktake_date);
CREATE INDEX idx_inv_stocktake_status ON inv_stocktake (status);
CREATE INDEX idx_inv_stocktake_line_stocktake ON inv_stocktake_line (stocktake_id, line_no);
CREATE INDEX idx_inv_stocktake_line_material ON inv_stocktake_line (material_id);
CREATE UNIQUE INDEX uk_inv_transfer_code ON inv_transfer (transfer_code) WHERE is_deleted = 0;
CREATE INDEX idx_inv_transfer_status ON inv_transfer (status);
CREATE INDEX idx_inv_transfer_date ON inv_transfer (transfer_date);
CREATE INDEX idx_inv_transfer_line_transfer ON inv_transfer_line (transfer_id);
CREATE INDEX idx_inv_transfer_line_material ON inv_transfer_line (material_id);
CREATE UNIQUE INDEX uk_inv_alert ON inv_alert (alert_type, material_id, warehouse_id, COALESCE(batch_no, ''), alert_date) WHERE is_deleted = 0;
CREATE INDEX idx_inv_alert_handle ON inv_alert (handle_status, alert_date);
CREATE INDEX idx_inv_alert_material ON inv_alert (material_id);

-- pur_ 采购域
CREATE UNIQUE INDEX uk_pur_requisition_code ON pur_requisition (requisition_code) WHERE is_deleted = 0;
CREATE INDEX idx_pur_requisition_status ON pur_requisition (status, approval_status);
CREATE INDEX idx_pur_requisition_dept ON pur_requisition (dept_id);
CREATE INDEX idx_pur_requisition_line_req ON pur_requisition_line (requisition_id, line_no);
CREATE INDEX idx_pur_requisition_line_material ON pur_requisition_line (material_id);
CREATE UNIQUE INDEX uk_pur_order_code ON pur_order (order_code) WHERE is_deleted = 0;
CREATE INDEX idx_pur_order_supplier ON pur_order (supplier_id);
CREATE INDEX idx_pur_order_status ON pur_order (order_status, settlement_status);
CREATE INDEX idx_pur_order_date ON pur_order (order_date);
CREATE INDEX idx_pur_order_buyer ON pur_order (buyer_id);
CREATE INDEX idx_pur_order_line_order ON pur_order_line (order_id, line_no);
CREATE INDEX idx_pur_order_line_material ON pur_order_line (material_id);
CREATE UNIQUE INDEX uk_pur_receipt_code ON pur_receipt (receipt_code) WHERE is_deleted = 0;
CREATE INDEX idx_pur_receipt_order ON pur_receipt (pur_order_id);
CREATE INDEX idx_pur_receipt_status ON pur_receipt (status);
CREATE INDEX idx_pur_receipt_date ON pur_receipt (receipt_date);
CREATE INDEX idx_pur_receipt_line_receipt ON pur_receipt_line (receipt_id, line_no);
CREATE INDEX idx_pur_receipt_line_material ON pur_receipt_line (material_id);
CREATE UNIQUE INDEX uk_pur_return_code ON pur_return (return_code) WHERE is_deleted = 0;
CREATE INDEX idx_pur_return_status ON pur_return (status, return_date);
CREATE INDEX idx_pur_return_line_return ON pur_return_line (return_id);
CREATE UNIQUE INDEX uk_pur_invoice_match_code ON pur_invoice_match (match_code) WHERE is_deleted = 0;
CREATE INDEX idx_pur_invoice_match_invoice ON pur_invoice_match (invoice_no);
CREATE INDEX idx_pur_invoice_match_order ON pur_invoice_match (pur_order_id);
CREATE INDEX idx_pur_invoice_match_result ON pur_invoice_match (match_result, period);
CREATE INDEX idx_pur_price_supplier_material ON pur_price (supplier_id, material_id, order_date);
CREATE UNIQUE INDEX uk_pur_supplier_score ON pur_supplier_score (supplier_id, score_period) WHERE is_deleted = 0;
CREATE INDEX idx_pur_supplier_score_rating ON pur_supplier_score (rating, score_period);

-- sal_ 销售域
CREATE UNIQUE INDEX uk_sal_order_code ON sal_order (order_code) WHERE is_deleted = 0;
CREATE INDEX idx_sal_order_customer ON sal_order (customer_id);
CREATE INDEX idx_sal_order_status ON sal_order (order_status, approval_status);
CREATE INDEX idx_sal_order_date ON sal_order (order_date);
CREATE INDEX idx_sal_order_sales_user ON sal_order (sales_user_id);
CREATE INDEX idx_sal_order_line_order ON sal_order_line (order_id, line_no);
CREATE INDEX idx_sal_order_line_material ON sal_order_line (material_id);
CREATE UNIQUE INDEX uk_sal_delivery_plan ON sal_delivery_plan (order_line_id, plan_no) WHERE is_deleted = 0;
CREATE INDEX idx_sal_delivery_plan_order ON sal_delivery_plan (order_id);
CREATE INDEX idx_sal_delivery_plan_status ON sal_delivery_plan (status, plan_date);
CREATE UNIQUE INDEX uk_sal_delivery_note_code ON sal_delivery_note (delivery_code) WHERE is_deleted = 0;
CREATE INDEX idx_sal_delivery_note_order ON sal_delivery_note (order_id);
CREATE INDEX idx_sal_delivery_note_status ON sal_delivery_note (status);
CREATE INDEX idx_sal_delivery_note_line_delivery ON sal_delivery_note_line (delivery_id, line_no);
CREATE INDEX idx_sal_delivery_note_line_material ON sal_delivery_note_line (material_id);
CREATE UNIQUE INDEX uk_sal_outbound_code ON sal_outbound (outbound_code) WHERE is_deleted = 0;
CREATE INDEX idx_sal_outbound_order ON sal_outbound (order_id);
CREATE INDEX idx_sal_outbound_date ON sal_outbound (outbound_date);
CREATE INDEX idx_sal_outbound_customer ON sal_outbound (customer_id);
CREATE INDEX idx_sal_outbound_line_outbound ON sal_outbound_line (outbound_id, line_no);
CREATE INDEX idx_sal_outbound_line_material ON sal_outbound_line (material_id);
CREATE UNIQUE INDEX uk_sal_return_code ON sal_return (return_code) WHERE is_deleted = 0;
CREATE INDEX idx_sal_return_customer ON sal_return (customer_id);
CREATE INDEX idx_sal_return_status ON sal_return (status);
CREATE INDEX idx_sal_return_line_return ON sal_return_line (return_id);
CREATE UNIQUE INDEX uk_sal_price_policy_code ON sal_price_policy (policy_code) WHERE is_deleted = 0;
CREATE INDEX idx_sal_price_policy_match ON sal_price_policy (policy_type, customer_id, material_id, category_id);
CREATE INDEX idx_sal_price_policy_status ON sal_price_policy (status, effective_date);

-- mf_ 生产域
CREATE UNIQUE INDEX uk_mf_mrp_plan_code ON mf_mrp_plan (plan_code) WHERE is_deleted = 0;
CREATE INDEX idx_mf_mrp_plan_status ON mf_mrp_plan (plan_status, run_time);
CREATE INDEX idx_mf_mrp_plan_item_plan ON mf_mrp_plan_item (plan_id, line_no);
CREATE INDEX idx_mf_mrp_plan_item_material ON mf_mrp_plan_item (material_id);
CREATE INDEX idx_mf_mrp_plan_item_converted ON mf_mrp_plan_item (converted_status);
CREATE UNIQUE INDEX uk_mf_order_code ON mf_order (work_order_code) WHERE is_deleted = 0;
CREATE INDEX idx_mf_order_status ON mf_order (order_status);
CREATE INDEX idx_mf_order_material ON mf_order (material_id);
CREATE INDEX idx_mf_order_sal_order ON mf_order (sal_order_id);
CREATE INDEX idx_mf_order_end_date ON mf_order (planned_end_date);
CREATE INDEX idx_mf_order_bom_order ON mf_order_bom (work_order_id, line_no);
CREATE INDEX idx_mf_order_bom_child ON mf_order_bom (child_material_id);
CREATE UNIQUE INDEX uk_mf_order_issue_code ON mf_order_issue (issue_code) WHERE is_deleted = 0;
CREATE INDEX idx_mf_order_issue_order ON mf_order_issue (work_order_id);
CREATE INDEX idx_mf_order_issue_status ON mf_order_issue (status);
CREATE INDEX idx_mf_order_issue_line_issue ON mf_order_issue_line (issue_id, line_no);
CREATE INDEX idx_mf_order_issue_line_material ON mf_order_issue_line (material_id);
CREATE UNIQUE INDEX uk_mf_order_report_code ON mf_order_report (report_code) WHERE is_deleted = 0;
CREATE INDEX idx_mf_order_report_order ON mf_order_report (work_order_id, report_date);
CREATE INDEX idx_mf_order_report_process ON mf_order_report (process_name);
CREATE UNIQUE INDEX uk_mf_order_cost ON mf_order_cost (work_order_id, score_period) WHERE is_deleted = 0;
CREATE INDEX idx_mf_order_cost_period ON mf_order_cost (score_period);
CREATE UNIQUE INDEX uk_mf_order_outsource_code ON mf_order_outsource (outsource_code) WHERE is_deleted = 0;
CREATE INDEX idx_mf_order_outsource_order ON mf_order_outsource (work_order_id);
CREATE INDEX idx_mf_order_outsource_supplier ON mf_order_outsource (supplier_id);

-- fin_ 财务域
CREATE UNIQUE INDEX uk_fin_subject_code ON fin_subject (subject_code) WHERE is_deleted = 0;
CREATE INDEX idx_fin_subject_parent ON fin_subject (parent_id);
CREATE INDEX idx_fin_subject_leaf ON fin_subject (is_leaf, subject_category);
CREATE UNIQUE INDEX uk_fin_voucher_code ON fin_voucher (voucher_code) WHERE is_deleted = 0;
CREATE UNIQUE INDEX uk_fin_voucher_source ON fin_voucher (source_biz_type, source_biz_id, voucher_type) WHERE source_biz_type IS NOT NULL AND is_deleted = 0;
CREATE INDEX idx_fin_voucher_period ON fin_voucher (period, voucher_status);
CREATE INDEX idx_fin_voucher_date ON fin_voucher (voucher_date);
CREATE INDEX idx_fin_voucher_source ON fin_voucher (source_biz_type, source_biz_id);
CREATE INDEX idx_fin_voucher_entry_voucher ON fin_voucher_entry (voucher_id, line_no);
CREATE INDEX idx_fin_voucher_entry_subject ON fin_voucher_entry (subject_id);
CREATE INDEX idx_fin_voucher_entry_material ON fin_voucher_entry (material_id);
CREATE UNIQUE INDEX uk_fin_voucher_template ON fin_voucher_template (template_code, entry_no) WHERE is_deleted = 0;
CREATE INDEX idx_fin_voucher_template_biz ON fin_voucher_template (biz_type);
CREATE UNIQUE INDEX uk_fin_receivable_code ON fin_receivable (receivable_code) WHERE is_deleted = 0;
CREATE INDEX idx_fin_receivable_customer ON fin_receivable (customer_id, status);
CREATE INDEX idx_fin_receivable_due ON fin_receivable (due_date, status);
CREATE INDEX idx_fin_receivable_period ON fin_receivable (period);
CREATE UNIQUE INDEX uk_fin_payable_code ON fin_payable (payable_code) WHERE is_deleted = 0;
CREATE INDEX idx_fin_payable_supplier ON fin_payable (supplier_id, status);
CREATE INDEX idx_fin_payable_due ON fin_payable (due_date, status);
CREATE INDEX idx_fin_payable_type ON fin_payable (payable_type);
CREATE UNIQUE INDEX uk_fin_settlement_code ON fin_settlement (settlement_code) WHERE is_deleted = 0;
CREATE INDEX idx_fin_settlement_partner ON fin_settlement (partner_type, partner_id);
CREATE INDEX idx_fin_settlement_status ON fin_settlement (status, settlement_date);
CREATE INDEX idx_fin_settlement_entry_settlement ON fin_settlement_entry (settlement_id);
CREATE INDEX idx_fin_settlement_entry_biz ON fin_settlement_entry (biz_type, biz_id);
CREATE UNIQUE INDEX uk_fin_cost_ledger_trans ON fin_cost_ledger (trans_code) WHERE trans_code IS NOT NULL AND is_deleted = 0;
CREATE INDEX idx_fin_cost_ledger_period ON fin_cost_ledger (period, material_id);
CREATE INDEX idx_fin_cost_ledger_material ON fin_cost_ledger (material_id, trans_time);
CREATE UNIQUE INDEX uk_fin_period ON fin_period (period) WHERE is_deleted = 0;
CREATE UNIQUE INDEX uk_fin_period_current ON fin_period (is_current) WHERE is_current = 1 AND is_deleted = 0;
CREATE UNIQUE INDEX uk_fin_opening_balance ON fin_opening_balance (period, balance_type, COALESCE(subject_id, 0), COALESCE(material_id, 0), COALESCE(warehouse_id, 0)) WHERE is_deleted = 0;
CREATE INDEX idx_fin_opening_balance_type ON fin_opening_balance (balance_type);
CREATE UNIQUE INDEX uk_fin_asset_code ON fin_asset (asset_code) WHERE is_deleted = 0;
CREATE INDEX idx_fin_asset_status ON fin_asset (asset_status);
CREATE INDEX idx_fin_asset_dept ON fin_asset (used_dept_id);
CREATE UNIQUE INDEX uk_fin_asset_depreciation ON fin_asset_depreciation (asset_id, period) WHERE is_deleted = 0;
CREATE INDEX idx_fin_asset_depreciation_period ON fin_asset_depreciation (period);

-- =============================================================================
-- 四、初始化种子数据段（可选执行，建议在新建库时执行一次）
--   内容：组织、角色、4 个中心仓与示例库位、会计科目、数据字典、系统参数、
--         编号序列初始行、当前会计期间。
--   说明：①本段使用显式 id 写入以保证层级引用确定性，执行后需 setval 校准序列；
--         ②admin 用户由应用启动时的 AdminInitializer 幂等创建（BCrypt 强度 10，
--           初始口令 Admin@123456 首次登录强制修改），脚本不写入任何明文口令与口令哈希；
--         ③物料/客户/供应商/BOM/订单等业务数据由阶段六数据迁移导入，脚本内不预置。
-- =============================================================================

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
INSERT INTO sys_role (id, role_code, role_name, role_sort, data_scope, is_builtin, status, create_by, update_by) VALUES
(1, 'ADMIN', '超级管理员', 1, 1, 1, 1, 'admin', 'admin'),
(2, 'SALES_DIRECTOR', '销售总监', 2, 1, 1, 1, 'admin', 'admin'),
(3, 'SALES_USER', '销售业务员', 3, 6, 1, 1, 'admin', 'admin'),
(4, 'BUYER', '采购员', 4, 6, 1, 1, 'admin', 'admin'),
(5, 'WAREHOUSE_KEEPER', '仓管员', 5, 3, 1, 1, 'admin', 'admin'),
(6, 'PRODUCTION_PLANNER', '生产计划员', 6, 4, 1, 1, 'admin', 'admin'),
(7, 'FINANCE_MANAGER', '财务经理', 7, 1, 1, 1, 'admin', 'admin');
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
(18, 'system.page.max.size', '列表单页上限条数', '200', 2, 'SYSTEM', 1, 1, 'admin', 'admin');
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

-- 4.11 admin 账号与 admin-角色绑定（由应用 AdminInitializer 幂等创建，脚本不写入明文口令）
--   创建规则：username='admin'，password=BCrypt(强度 10, 明文遵循 SYS-09 口令策略)，
--   is_admin=1、status=1、首次登录强制修改口令；随后写入 sys_user_role 绑定 ADMIN 角色。
--   本脚本不提供该 INSERT，以避免在脚本中固化口令与口令哈希（安全要求 S-04/S-06）。

COMMIT;

-- =============================================================================
-- 脚本结束。执行后自检建议：
--   1) SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';  -- 期望 76
--   2) 业务侧自检：SELECT COUNT(*) FROM base_warehouse;                                -- 期望 4
--   3) 对账口径：库存流水汇总（Σ in_qty - Σ out_qty）必须等于 inv_stock.qty 汇总（日终差异 0）
-- =============================================================================