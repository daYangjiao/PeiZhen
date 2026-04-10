package org.example.dao;

import org.apache.ibatis.annotations.Mapper;
import org.example.model.Attendant;
import java.util.List;

@Mapper
public interface AttendantMapper {

    int insert(Attendant attendant);

    int update(Attendant attendant);

    Attendant findByUserId(Integer userId);

    List<Attendant> findRecommended();
}