package state.street.car.booking.carBooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDTO {
    private Long id;
    private Long carId;
    private Long userId;
    private String carRegistrationNumber;
    private String username;
    private LocalDateTime bookingDate;
    private Integer duration;
    private LocalDateTime createdAt;
}

