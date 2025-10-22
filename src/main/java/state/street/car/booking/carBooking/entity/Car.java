package state.street.car.booking.carBooking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import state.street.car.booking.carBooking.enums.CarType;

import java.math.BigDecimal;

@Entity
@Table(name = "cars")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Car {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String registrationNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CarType type;
    
    @Column(nullable = false)
    private BigDecimal costPerDay;
    
    @Column(nullable = false)
    private Integer capacity;
    
    public Car(String registrationNumber, CarType type, BigDecimal costPerDay, Integer capacity) {
        this.registrationNumber = registrationNumber;
        this.type = type;
        this.costPerDay = costPerDay;
        this.capacity = capacity;
    }
}

