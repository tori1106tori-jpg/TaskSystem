package com.example.demo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    // ユーザー名でデータベースを検索するおまじない
    Optional<User> findByUsername(String username);
}
