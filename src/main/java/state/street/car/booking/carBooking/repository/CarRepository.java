package state.street.car.booking.carBooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import state.street.car.booking.carBooking.entity.Car;
import state.street.car.booking.carBooking.enums.CarType;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
    List<Car> findByType(CarType type);
    
    @Query("SELECT c FROM Car c WHERE c.id NOT IN " +
           "(SELECT b.car.id FROM Booking b WHERE " +
           "(b.bookingDate <= :endDate AND " +
           "FUNCTION('DATEADD', DAY, b.duration, b.bookingDate) >= :startDate))")
    List<Car> findAvailableCars(@Param("startDate") LocalDateTime startDate, 
                                @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT c FROM Car c WHERE c.type = :type AND c.id NOT IN " +
           "(SELECT b.car.id FROM Booking b WHERE " +
           "(b.bookingDate <= :endDate AND " +
           "FUNCTION('DATEADD', DAY, b.duration, b.bookingDate) >= :startDate))")
    List<Car> findAvailableCarsByType(@Param("type") CarType type,
                                      @Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
}

