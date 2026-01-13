package ru.practicum.user.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.user.service.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
