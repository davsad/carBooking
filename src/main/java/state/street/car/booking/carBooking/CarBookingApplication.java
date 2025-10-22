package state.street.car.booking.carBooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CarBookingApplication {

	public static void main(String[] args) {
		SpringApplication.run(CarBookingApplication.class, args);
	}

}
