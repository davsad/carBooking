package state.street.car.booking.carBooking.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import state.street.car.booking.carBooking.dto.CarDTO;
import state.street.car.booking.carBooking.enums.CarType;
import state.street.car.booking.carBooking.service.CarService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
public class CarController {
    
    private final CarService carService;
    
    @GetMapping
    public ResponseEntity<List<CarDTO>> getAllCars(
            @RequestParam(required = false) CarType type,
            @RequestParam(required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = true) Integer duration) {

        List<CarDTO> cars;

        // Return all cars with booking info based on the search date range
        // Cars will be marked as booked if they have conflicting bookings in the requested period
        if (type != null) {
            cars = carService.getCarsByTypeForPeriod(type, startDate, duration);
        } else {
            cars = carService.getAllCarsForPeriod(startDate, duration);
        }

        return ResponseEntity.ok(cars);
    }

    @GetMapping("/simple")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<CarDTO>> getAllCarsSimple(@RequestParam(required = false) CarType type) {
        List<CarDTO> cars;

        if (type != null) {
            cars = carService.getCarsByTypeSimple(type);
        } else {
            cars = carService.getAllCarsSimple();
        }

        return ResponseEntity.ok(cars);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<CarDTO> getCarById(@PathVariable Long id) {
        CarDTO car = carService.getCarById(id);
        return ResponseEntity.ok(car);
    }
    
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<CarDTO> createCar(@RequestBody CarDTO carDTO) {
        CarDTO createdCar = carService.createCar(carDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCar);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<CarDTO> updateCar(@PathVariable Long id, @RequestBody CarDTO carDTO) {
        CarDTO updatedCar = carService.updateCar(id, carDTO);
        return ResponseEntity.ok(updatedCar);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteCar(@PathVariable Long id) {
        carService.deleteCar(id);
        return ResponseEntity.noContent().build();
    }
}

