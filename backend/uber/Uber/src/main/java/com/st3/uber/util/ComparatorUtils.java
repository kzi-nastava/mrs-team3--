package com.st3.uber.util;

import com.st3.uber.dto.ride.AdminRideDetailsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;

public class ComparatorUtils {
  public static Comparator<AdminRideDetailsResponse> buildComparator(
      String sortBy,
      String direction
  ) {
    Comparator<AdminRideDetailsResponse> comparator = (r1, r2) -> {
      try {
        Field field = AdminRideDetailsResponse.class.getDeclaredField(sortBy);
        field.setAccessible(true);

        Object v1 = field.get(r1);
        Object v2 = field.get(r2);

        if (v1 == null && v2 == null) return 0;
        if (v1 == null) return 1;
        if (v2 == null) return -1;

        if (v1 instanceof Comparable && v2 instanceof Comparable) {
          @SuppressWarnings("unchecked")
          Comparable<Object> c1 = (Comparable<Object>) v1;
          return c1.compareTo(v2);
        }

        // fallback ako polje nije Comparable
        return v1.toString().compareTo(v2.toString());

      } catch (Exception e) {
        // fallback ako ne postoji polje → sort po datumu početka
        return r1.getRideStartTime().compareTo(r2.getRideStartTime());
      }
    };

    return "DESC".equalsIgnoreCase(direction)
        ? comparator.reversed()
        : comparator;
  }
}
