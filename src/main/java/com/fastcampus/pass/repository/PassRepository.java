package com.fastcampus.pass.repository;

import com.fastcampus.pass.repository.pass.PassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassRepository extends JpaRepository<PassEntity, Integer> {
}
