package com.carlosdaza.splitexpense.repository;

import com.carlosdaza.splitexpense.domain.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

    @Query("SELECT g FROM Group g JOIN g.members m WHERE m.id = :userId")
    List<Group> findByMemberId(@Param("userId") UUID userId);
}
