package com.example.repository;

import com.example.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByVerificationCode(String verificationCode);

    List<User> findByUsernameContainingIgnoreCaseOrHeadlineContainingIgnoreCase(
            String nameQuery,
            String headlineQuery
    );

    List<User> findByRole(com.example.model.Role role);

    List<User> findByRoleAndEnabledTrue(com.example.model.Role role);

    @Query("""
        SELECT u FROM User u
        WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))
        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
    """)
    List<User> searchByUsernameOrEmail(@Param("q") String q);
}
