package org.example.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {
}