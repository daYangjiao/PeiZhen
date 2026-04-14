package org.example.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.model.Attendant;
import java.util.List;

@Mapper
public interface AttendantMapper {

    int insert(Attendant attendant);

    int update(Attendant attendant);

    Attendant findByUserId(Integer userId);

    List<Attendant> findRecommended();

    List<Attendant> findAiCandidates(@Param("limit") int limit);

    int countAdminAttendants(@Param("keyword") String keyword, @Param("auditStatus") Integer auditStatus);

    List<Attendant> findAdminAttendants(@Param("keyword") String keyword,
                                        @Param("auditStatus") Integer auditStatus,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);
}
