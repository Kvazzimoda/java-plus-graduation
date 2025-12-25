package ru.practicum.ewm.location.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.location.dto.LocationDto;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.location.repository.LocationRepository;
import ru.practicum.ewm.mapper.EwmMapper;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final LocationRepository locationRepository;
    private final EwmMapper mapper;

    @Transactional
    public Location getOrCreateLocation(LocationDto locationDto) {
        log.info("Получение или создание локации: lat={}, lon={}", locationDto.getLat(), locationDto.getLon());

        Optional<Location> existingLocation = locationRepository
                .findByLatAndLon(locationDto.getLat(), locationDto.getLon());

        if (existingLocation.isPresent()) {
            log.info("Локация уже существует с ID: {}", existingLocation.get().getId());
            return existingLocation.get();
        }

        Location newLocation = mapper.toLocation(locationDto);
        Location savedLocation = locationRepository.save(newLocation);
        log.info("Создана новая локация с ID: {}", savedLocation.getId());

        return savedLocation;
    }
}
