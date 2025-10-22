package state.street.car.booking.carBooking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private LocalDateTime bookingDate;
    
    @Column(nullable = false)
    private Integer duration; // in days
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    public Booking(Car car, User user, LocalDateTime bookingDate, Integer duration) {
        this.car = car;
        this.user = user;
        this.bookingDate = bookingDate;
        this.duration = duration;
        this.createdAt = LocalDateTime.now();
    }
    
    public LocalDateTime getBookingEndDate() {
        return bookingDate.plusDays(duration);
    }
}

