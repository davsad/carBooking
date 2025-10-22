package state.street.car.booking.carBooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import state.street.car.booking.carBooking.entity.Booking;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);

    List<Booking> findByCarId(Long carId);

    @Query("SELECT b FROM Booking b WHERE b.car.id = :carId AND " +
           "(b.bookingDate <= :endDate AND " +
           "FUNCTION('DATEADD', DAY, b.duration, b.bookingDate) >= :startDate)")
    List<Booking> findConflictingBookings(@Param("carId") Long carId,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT b FROM Booking b WHERE b.car.id = :carId AND " +
           "FUNCTION('DATEADD', DAY, b.duration, b.bookingDate) >= :currentTime " +
           "ORDER BY b.bookingDate ASC")
    List<Booking> findActiveBookingsForCar(@Param("carId") Long carId,
                                          @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT b FROM Booking b WHERE " +
           "(b.bookingDate <= :endDate AND " +
           "FUNCTION('DATEADD', DAY, b.duration, b.bookingDate) >= :startDate) " +
           "ORDER BY b.bookingDate ASC")
    List<Booking> findBookingsByDateRange(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);
}

