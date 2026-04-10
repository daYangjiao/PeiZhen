package org.example.service.impl;

import org.example.dao.AttendantMapper;
import org.example.dao.UserMapper;
import org.example.model.Attendant;
import org.example.model.User;
import org.example.service.AttendantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AttendantServiceImpl implements AttendantService {
    private static final Logger logger = LoggerFactory.getLogger(AttendantServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AttendantMapper attendantMapper;

    @Override
    @Transactional
    public int registerAttendant(User user, Attendant attendant) {
        // 1. 校验用户名是否已存在
        if (userMapper.findByUsername(user.getUsername()) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        
        // 2. 创建 User 账号，并设置为陪诊师角色
        user.setUserType(1); 
        userMapper.save(user);
        Integer userId = user.getId();
        logger.info("创建陪诊师基础账号成功, User ID: {}", userId);

        // 3. 创建 Attendant 扩展信息
        attendant.setUserId(userId);
        attendantMapper.insert(attendant);
        logger.info("创建陪诊师扩展信息成功, User ID: {}", userId);

        return userId;
    }

    @Override
    public Attendant findByUserId(Integer userId) {
        return attendantMapper.findByUserId(userId);
    }

    @Override
    public int update(Attendant attendant) {
        return attendantMapper.update(attendant);
    }
    
    @Override
    public List<Attendant> findRecommended() {
        // 简单实现：查找所有状态正常的陪诊师
        // 实际业务中可以根据评分、接单量等更复杂的逻辑来推荐
        return attendantMapper.findRecommended();
    }
}