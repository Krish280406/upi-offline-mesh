package com.demo.upimesh.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.demo.upimesh.model.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
}