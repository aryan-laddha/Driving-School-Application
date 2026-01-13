package com.example.drivingschool;

import com.example.drivingschool.model.Role;
import com.example.drivingschool.model.User;
import com.example.drivingschool.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class DrivingSchoolApplication {

    public static void main(String[] args) {
        SpringApplication.run(DrivingSchoolApplication.class, args);
    }

    @Bean
    CommandLineRunner init(UserRepository userRepository) {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = User.builder()
                        .username("admin")
                        .password(new BCryptPasswordEncoder().encode("admin123"))
                        .role(Role.ADMIN)
                        .deleted(false)
                        .access(true)           // approved by default
                        .name("Default Admin")
                        .contact("0000000000")
                        .licenseNumber("N/A")
                        .build();
                userRepository.save(admin);
                System.out.println("Default admin created: username=admin, password=admin123");
            }
        };
    }


}
