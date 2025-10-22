package state.street.car.booking.carBooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {
    private Long carId;
    private LocalDateTime bookingDate;
    private Integer duration;
}

