# 项目目录结构设计

```
north30-erp-project/                                    # 项目根目录
├── README.md                                   # 项目总体说明
├── .gitignore
├── .editorconfig
│
├── erp-backend/                                # 📂 后端根目录（Spring Boot 模块化单体，单一 JAR 部署）
│   ├── pom.xml                                 # 后端父POM（统一版本管理）
│   ├── erp-common/                             # 公共模块（Result统一响应/异常处理/JWT工具/公共Util）
│   ├── erp-system/                             # 系统管理模块（用户/角色/权限/菜单/审计日志）
│   ├── erp-base/                               # 基础数据模块（物料/BOM/客商/仓库）
│   ├── erp-business/                           # 业务模块（依赖 erp-common + erp-system + erp-base）
│   │   └── src/main/java/me/north30/erp/business/
│   │       ├── purchase/                       # 采购管理（申请→订单→收货→入库）
│   │       ├── sales/                          # 销售管理（订单→发货→出库→开票）
│   │       ├── inventory/                      # 库存管理（入库/出库/调拨/盘点/流水）
│   │       ├── manufacturing/                  # 生产制造（MRP→工单→领料→报工→入库）
│   │       └── finance/                        # 财务管理（应收应付/凭证/成本核算）
│   └── erp-bootstrap/                          # 启动模块（唯一入口，不写业务代码，唯一含 spring-boot-maven-plugin）
│       └── src/main/resources/
│           ├── application.yml                 # 数据源/Redis/JWT 配置
│           └── mapper/                         # MyBatis XML（按模块分文件夹：system/base/business/purchase…）
│
├── erp-frontend/                               # 📂 前端根目录（Vue3 + TS）
│   ├── package.json
│   ├── vite.config.ts
│   ├── index.html
│   └── src/
│       ├── api/
│       ├── views/
│       ├── router/
│       ├── stores/
│       └── ...
│
├── docs/                                        # 📂 项目文档
│   ├── 需求文档/
│   ├── 数据库设计/
│   └── API文档/
│
├── database/                                         # 📂 数据库脚本
│   ├── init/
│   └── migration/
│
├── deploy/                                     # 📂 部署配置
│   ├── docker/
│   └── k8s/                                    # （微服务阶段引入）
│
└── scripts/                                    # 📂 辅助脚本
    ├── create-module.sh                        # 快速创建 erp-business 业务子包骨架
    └── db-migrate.sh
```