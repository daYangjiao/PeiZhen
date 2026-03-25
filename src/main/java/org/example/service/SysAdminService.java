package org.example.service;

import org.example.entity.SysAdmin;

import java.util.List;

public interface SysAdminService {

    SysAdmin login(String username, String password);

    List<SysAdmin> list();

    boolean updateById(SysAdmin admin);
}
