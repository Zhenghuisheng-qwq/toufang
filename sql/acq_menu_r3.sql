-- R3 report admin menus. R4 verified execution is recorded in docs/codex-r4-review.md.
-- Select and back up the ACTUAL authorized system database first. System and business tables may share the verified schema.
-- Set this session variable to the verified system database name; NULL disables all inserts.
SET @approved_system_database = NULL;
-- Inspect IDs, parent and existing permissions BEFORE execution. No role grants in this file.
SELECT DATABASE() AS target_database;
SELECT menu_id,parent_id,path,component,perms FROM sys_menu
WHERE menu_id IN (2000,2008,2802,2803) OR perms LIKE 'acq:report:%';

START TRANSACTION;
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
SELECT desired.menu_id,desired.menu_name,desired.parent_id,desired.order_num,desired.path,desired.component,
       1,0,desired.menu_type,'0','0',desired.perms,'document','admin',NOW(),'R3 report management'
FROM (
SELECT 2008 menu_id,'付费报告' menu_name,2000 parent_id,8 order_num,'report' path,'acq/report/index' component,'C' menu_type,'acq:report:query' perms
UNION ALL
SELECT 2802 menu_id,'查看正文' menu_name,2008 parent_id,2 order_num,'' path,'' component,'F' menu_type,'acq:report:content' perms
UNION ALL
SELECT 2803 menu_id,'受控补生成' menu_name,2008 parent_id,3 order_num,'' path,'' component,'F' menu_type,'acq:report:retry' perms
) desired
WHERE @approved_system_database IS NOT NULL AND DATABASE()=@approved_system_database
AND EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2000 AND path='acq' AND menu_type='M')
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=desired.menu_id OR perms=desired.perms)
-- Stop the whole increment on any ID/permission mismatch; never repair collisions by overwriting.
AND NOT EXISTS (
  SELECT 1 FROM sys_menu existing JOIN (
SELECT 2008 menu_id,'付费报告' menu_name,2000 parent_id,8 order_num,'report' path,'acq/report/index' component,'C' menu_type,'acq:report:query' perms
UNION ALL
SELECT 2802 menu_id,'查看正文' menu_name,2008 parent_id,2 order_num,'' path,'' component,'F' menu_type,'acq:report:content' perms
UNION ALL
SELECT 2803 menu_id,'受控补生成' menu_name,2008 parent_id,3 order_num,'' path,'' component,'F' menu_type,'acq:report:retry' perms
  ) expected ON existing.menu_id=expected.menu_id OR existing.perms=expected.perms
  WHERE existing.menu_id<>expected.menu_id OR COALESCE(existing.perms,'')<>expected.perms
     OR existing.parent_id<>expected.parent_id OR COALESCE(existing.component,'')<>expected.component
     OR existing.path<>expected.path OR existing.menu_type<>expected.menu_type
);
SELECT ROW_COUNT() AS inserted_menu_rows;
COMMIT;
-- Verify all three entries after execution. Zero rows can mean already installed OR guard refusal.
SELECT menu_id,parent_id,path,component,perms FROM sys_menu WHERE menu_id IN (2008,2802,2803);
