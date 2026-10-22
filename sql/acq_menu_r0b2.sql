-- R0-B2 menus: system database only. Review ID collisions before execution.
-- Does not alter existing R0-B1 menus or grant roles automatically.
-- Caller must SET @approved_system_database to the verified system schema; unset refuses.
DELIMITER $$
CREATE PROCEDURE acq_menu_r0b2()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  IF @approved_system_database IS NULL OR DATABASE() IS NULL OR DATABASE() <> @approved_system_database THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Set approved_system_database to the verified system database';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2000 AND path='acq' AND menu_type='M') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'R0-B1 acquisition menu missing or ID occupied';
  END IF;
  START TRANSACTION;
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2004 AND (COALESCE(perms,'') <> 'acq:merchant:list' OR parent_id <> 2000 OR COALESCE(component,'') <> 'acq/merchant/index')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:merchant:list' AND menu_id <> 2004) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2004';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2004,'支付商户',2000,4,'merchant','acq/merchant/index',1,0,'C','0','0','acq:merchant:list','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2004);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2401 AND (COALESCE(perms,'') <> 'acq:merchant:query' OR parent_id <> 2004 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:merchant:query' AND menu_id <> 2401) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2401';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2401,'查询',2004,1,'','',1,0,'F','0','0','acq:merchant:query','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2401);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2402 AND (COALESCE(perms,'') <> 'acq:merchant:add' OR parent_id <> 2004 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:merchant:add' AND menu_id <> 2402) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2402';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2402,'新增',2004,2,'','',1,0,'F','0','0','acq:merchant:add','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2402);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2403 AND (COALESCE(perms,'') <> 'acq:merchant:edit' OR parent_id <> 2004 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:merchant:edit' AND menu_id <> 2403) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2403';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2403,'编辑',2004,3,'','',1,0,'F','0','0','acq:merchant:edit','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2403);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2404 AND (COALESCE(perms,'') <> 'acq:merchant:remove' OR parent_id <> 2004 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:merchant:remove' AND menu_id <> 2404) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2404';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2404,'删除',2004,4,'','',1,0,'F','0','0','acq:merchant:remove','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2404);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2405 AND (COALESCE(perms,'') <> 'acq:merchant:status' OR parent_id <> 2004 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:merchant:status' AND menu_id <> 2405) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2405';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2405,'启停',2004,5,'','',1,0,'F','0','0','acq:merchant:status','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2405);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2406 AND (COALESCE(perms,'') <> 'acq:merchant:version' OR parent_id <> 2004 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:merchant:version' AND menu_id <> 2406) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2406';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2406,'版本管理',2004,6,'','',1,0,'F','0','0','acq:merchant:version','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2406);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2407 AND (COALESCE(perms,'') <> 'acq:merchant:secret' OR parent_id <> 2004 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:merchant:secret' AND menu_id <> 2407) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2407';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2407,'敏感引用',2004,7,'','',1,0,'F','0','0','acq:merchant:secret','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2407);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2005 AND (COALESCE(perms,'') <> 'acq:wecom:list' OR parent_id <> 2000 OR COALESCE(component,'') <> 'acq/wecom/index')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:wecom:list' AND menu_id <> 2005) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2005';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2005,'企业微信配置',2000,5,'wecom','acq/wecom/index',1,0,'C','0','0','acq:wecom:list','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2005);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2501 AND (COALESCE(perms,'') <> 'acq:wecom:query' OR parent_id <> 2005 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:wecom:query' AND menu_id <> 2501) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2501';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2501,'查询',2005,1,'','',1,0,'F','0','0','acq:wecom:query','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2501);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2502 AND (COALESCE(perms,'') <> 'acq:wecom:add' OR parent_id <> 2005 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:wecom:add' AND menu_id <> 2502) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2502';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2502,'新增',2005,2,'','',1,0,'F','0','0','acq:wecom:add','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2502);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2503 AND (COALESCE(perms,'') <> 'acq:wecom:edit' OR parent_id <> 2005 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:wecom:edit' AND menu_id <> 2503) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2503';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2503,'编辑',2005,3,'','',1,0,'F','0','0','acq:wecom:edit','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2503);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2504 AND (COALESCE(perms,'') <> 'acq:wecom:remove' OR parent_id <> 2005 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:wecom:remove' AND menu_id <> 2504) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2504';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2504,'删除',2005,4,'','',1,0,'F','0','0','acq:wecom:remove','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2504);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2505 AND (COALESCE(perms,'') <> 'acq:wecom:status' OR parent_id <> 2005 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:wecom:status' AND menu_id <> 2505) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2505';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2505,'启停',2005,5,'','',1,0,'F','0','0','acq:wecom:status','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2505);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2506 AND (COALESCE(perms,'') <> 'acq:wecom:secret' OR parent_id <> 2005 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:wecom:secret' AND menu_id <> 2506) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2506';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2506,'敏感引用',2005,6,'','',1,0,'F','0','0','acq:wecom:secret','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2506);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2006 AND (COALESCE(perms,'') <> 'acq:route:list' OR parent_id <> 2000 OR COALESCE(component,'') <> 'acq/route/index')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:route:list' AND menu_id <> 2006) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2006';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2006,'承接路由',2000,6,'route','acq/route/index',1,0,'C','0','0','acq:route:list','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2006);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2601 AND (COALESCE(perms,'') <> 'acq:route:query' OR parent_id <> 2006 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:route:query' AND menu_id <> 2601) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2601';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2601,'查询',2006,1,'','',1,0,'F','0','0','acq:route:query','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2601);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2602 AND (COALESCE(perms,'') <> 'acq:route:add' OR parent_id <> 2006 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:route:add' AND menu_id <> 2602) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2602';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2602,'新增',2006,2,'','',1,0,'F','0','0','acq:route:add','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2602);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2603 AND (COALESCE(perms,'') <> 'acq:route:edit' OR parent_id <> 2006 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:route:edit' AND menu_id <> 2603) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2603';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2603,'编辑',2006,3,'','',1,0,'F','0','0','acq:route:edit','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2603);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2604 AND (COALESCE(perms,'') <> 'acq:route:remove' OR parent_id <> 2006 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:route:remove' AND menu_id <> 2604) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2604';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2604,'删除',2006,4,'','',1,0,'F','0','0','acq:route:remove','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2604);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2605 AND (COALESCE(perms,'') <> 'acq:route:switch' OR parent_id <> 2006 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:route:switch' AND menu_id <> 2605) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2605';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2605,'切换',2006,5,'','',1,0,'F','0','0','acq:route:switch','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2605);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2606 AND (COALESCE(perms,'') <> 'acq:route:preview' OR parent_id <> 2006 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:route:preview' AND menu_id <> 2606) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2606';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2606,'解析预览',2006,6,'','',1,0,'F','0','0','acq:route:preview','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2606);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2007 AND (COALESCE(perms,'') <> 'acq:ad:list' OR parent_id <> 2000 OR COALESCE(component,'') <> 'acq/ad/index')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:ad:list' AND menu_id <> 2007) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2007';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2007,'广告账户',2000,7,'ad','acq/ad/index',1,0,'C','0','0','acq:ad:list','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2007);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2701 AND (COALESCE(perms,'') <> 'acq:ad:query' OR parent_id <> 2007 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:ad:query' AND menu_id <> 2701) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2701';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2701,'查询',2007,1,'','',1,0,'F','0','0','acq:ad:query','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2701);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2702 AND (COALESCE(perms,'') <> 'acq:ad:add' OR parent_id <> 2007 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:ad:add' AND menu_id <> 2702) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2702';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2702,'新增',2007,2,'','',1,0,'F','0','0','acq:ad:add','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2702);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2703 AND (COALESCE(perms,'') <> 'acq:ad:edit' OR parent_id <> 2007 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:ad:edit' AND menu_id <> 2703) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2703';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2703,'编辑',2007,3,'','',1,0,'F','0','0','acq:ad:edit','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2703);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2704 AND (COALESCE(perms,'') <> 'acq:ad:remove' OR parent_id <> 2007 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:ad:remove' AND menu_id <> 2704) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2704';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2704,'删除',2007,4,'','',1,0,'F','0','0','acq:ad:remove','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2704);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2705 AND (COALESCE(perms,'') <> 'acq:ad:status' OR parent_id <> 2007 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:ad:status' AND menu_id <> 2705) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2705';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2705,'启停',2007,5,'','',1,0,'F','0','0','acq:ad:status','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2705);
  IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2706 AND (COALESCE(perms,'') <> 'acq:ad:secret' OR parent_id <> 2007 OR COALESCE(component,'') <> '')) OR EXISTS (SELECT 1 FROM sys_menu WHERE perms='acq:ad:secret' AND menu_id <> 2706) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Menu ID or permission collision: 2706';
  END IF;
  INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
  SELECT 2706,'敏感引用',2007,6,'','',1,0,'F','0','0','acq:ad:secret','#','admin',NOW(),'R0-B2 configuration only'
  WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2706);
  COMMIT;
END$$
DELIMITER ;
CALL acq_menu_r0b2();
DROP PROCEDURE acq_menu_r0b2;
