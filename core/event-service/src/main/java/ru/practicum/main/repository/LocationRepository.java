package ru.practicum.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.main.model.LocationEntity;

public interface LocationRepository extends JpaRepository<LocationEntity, Long> {
}
