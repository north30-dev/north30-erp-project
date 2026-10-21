-- =============================================================================
-- 集成测试库初始化 第 1 步：幂等建库（连接 postgres 库执行）
--
-- 执行方式：
--   PGPASSWORD=root psql -h localhost -U postgres -d postgres -f database/test/00_create_database.sql
--
-- 后续步骤（连接 erp_test_db 执行，直接复用 init 基线，勿复制内容）：
--   PGPASSWORD=erp_password psql -h localhost -U erp_user -d erp_test_db -f database/init/00_schema.sql
--   PGPASSWORD=erp_password psql -h localhost -U erp_user -d erp_test_db -f database/init/01_seed.sql
--
-- 注意：菜单权限与 admin 账号由应用启动时 AdminInitializer 幂等种入，无需 SQL
-- =============================================================================

DROP DATABASE IF EXISTS erp_test_db;

CREATE DATABASE erp_test_db OWNER erp_user;
