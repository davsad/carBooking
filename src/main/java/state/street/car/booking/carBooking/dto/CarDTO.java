package state.street.car.booking.carBooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import state.street.car.booking.carBooking.enums.CarType;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarDTO {
    private Long id;
    private String registrationNumber;
    private CarType type;
    private BigDecimal costPerDay;
    private Integer capacity;

    private Long currentBookingId;
    private Long bookedByUserId;
    private String bookedByUsername;
    private String bookingStartDate;
    private String bookingEndDate;
    private Boolean isBooked;

    public CarDTO(Long id, String registrationNumber, CarType type, BigDecimal costPerDay, Integer capacity) {
        this.id = id;
        this.registrationNumber = registrationNumber;
        this.type = type;
        this.costPerDay = costPerDay;
        this.capacity = capacity;
        this.isBooked = false;
    }
}

