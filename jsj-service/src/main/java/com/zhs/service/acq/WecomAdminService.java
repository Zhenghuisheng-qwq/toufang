package com.zhs.service.acq;

import com.zhs.service.acq.dto.AcqPage;
import com.zhs.service.acq.dto.ConfigCommands;

import java.util.Map;

public interface WecomAdminService {

    Map<String, Object> routeOptions(Long corpId);

    AcqPage<Map<String, Object>> corps(ConfigCommands.Query query);

    Map<String, Object> corp(Long id, boolean secrets);

    Long saveCorp(Long id, ConfigCommands.Corp command);

    void secrets(Long id, ConfigCommands.CorpSecrets command);

    void corpStatus(Long id, Integer status);

    void deleteCorp(Long id);

    Long saveTeacher(Long id, ConfigCommands.Teacher command);

    void teacherStatus(Long id, Integer status);

    void deleteTeacher(Long id);

    Long saveTag(Long id, ConfigCommands.Tag command);

    void tagStatus(Long id, Integer status);

    void deleteTag(Long id);

    AcqPage<Map<String, Object>> routes(ConfigCommands.Query query);

    Map<String, Object> route(Long id);

    Long saveRoute(Long id, ConfigCommands.Route command);

    void routeStatus(Long id, Integer status);

    void deleteRoute(Long id);

    Map<String, Object> resolve(Long landingPageId);
}
