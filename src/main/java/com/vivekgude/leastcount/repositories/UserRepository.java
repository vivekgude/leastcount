package com.vivekgude.leastcount.repositories;

import com.vivekgude.leastcount.model.dao.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
}
