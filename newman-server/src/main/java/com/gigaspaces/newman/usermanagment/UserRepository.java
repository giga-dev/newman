package com.gigaspaces.newman.usermanagment;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UserRepository extends CrudRepository<User, String> {

    List<User> findByUsernameNot(String username);

    User findByUsername(String username);

    @Transactional
    @Modifying
    void deleteByUsername(String username);

    boolean existsByUsername(String username);
}
