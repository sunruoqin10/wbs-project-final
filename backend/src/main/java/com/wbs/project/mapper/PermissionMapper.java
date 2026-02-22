package com.wbs.project.mapper;

import com.wbs.project.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PermissionMapper {

    List<Permission> selectAll();

    List<Permission> selectByRole(@Param("role") String role);

    List<String> selectPermissionCodesByRole(@Param("role") String role);
}
