package com.example.repository;

import com.example.model.ActionLog;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionLogRepository extends CrudRepository<ActionLog, Long> {
    List<ActionLog> findTop200ByOrderByCreatedAtDesc();
    List<ActionLog> findTop200ByRoleOrderByCreatedAtDesc(String role);

    default List<ActionLog> findAllRecent() {
        return findTop200ByOrderByCreatedAtDesc();
    }
}
