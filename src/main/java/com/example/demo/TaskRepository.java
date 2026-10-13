package com.example.demo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
    
    // ★【追加】タイトルにキーワードが含まれているタスクを検索するおまじない（大文字小文字を区別しません）
    List<Task> findByTitleContainingIgnoreCase(String keyword);
}
