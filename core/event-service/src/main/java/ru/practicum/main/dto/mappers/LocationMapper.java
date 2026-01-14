package ru.practicum.main.dto.mappers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.main.dto.Location;
import ru.practicum.main.model.LocationEntity;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocationMapper {
    public static LocationEntity toLocation(Location location) {
        return LocationEntity.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .build();
    }

    public static Location toLocationDto(LocationEntity locationEntity) {
        return Location.builder()
                .lat(locationEntity.getLat())
                .lon(locationEntity.getLon())
                .build();
    }
}
