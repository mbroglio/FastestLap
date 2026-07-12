package com.the_coffe_coders.fastestlap.repository.mapper;

import com.the_coffe_coders.fastestlap.domain.f1.track.Location;
import com.the_coffe_coders.fastestlap.dto.LocationDTO;

public class LocationMapper {
    public static Location toLocation(LocationDTO locationDTO) {
        Location location = new Location();
        location.setLatitude(locationDTO.getLat());
        location.setLongitude(locationDTO.get_long());
        location.setLocality(locationDTO.getLocality());
        location.setCountry(locationDTO.getCountry());
        return location;
    }
}
