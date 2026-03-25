package org.example.service.impl;

import org.example.entity.SysAdmin;
import org.example.service.SysAdminService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class SysAdminServiceImpl implements SysAdminService {

    @Override
    public SysAdmin login(String username, String password) {
        return null;
    }

    @Override
    public List<SysAdmin> list() {
        return Collections.emptyList();
    }

    @Override
    public boolean updateById(SysAdmin admin) {
        return false;
    }
}
