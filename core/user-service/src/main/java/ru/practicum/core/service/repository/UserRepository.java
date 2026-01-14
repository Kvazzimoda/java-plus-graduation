package ru.practicum.core.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.core.service.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
