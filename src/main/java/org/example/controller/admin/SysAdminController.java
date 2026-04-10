package org.example.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.common.ResponseResult;
import org.example.entity.SysAdmin;
import org.example.service.SysAdminService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/sysAdmin")
@RequiredArgsConstructor
class SysAdminAdminController {

    private final SysAdminService sysAdminService;

    @PostMapping("/login")
    public ResponseResult<SysAdmin> login(@RequestParam String username, @RequestParam String password) {
        return ResponseResult.success(sysAdminService.login(username, password));
    }

    @GetMapping("/list")
    public ResponseResult<List<SysAdmin>> list() {
        return ResponseResult.success(sysAdminService.list());
    }

    @PostMapping("/status/{id}/{status}")
    public ResponseResult<Boolean> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        SysAdmin admin = new SysAdmin();
        admin.setId(id);
        admin.setStatus(status);
        return ResponseResult.success(sysAdminService.updateById(admin));
    }
}
