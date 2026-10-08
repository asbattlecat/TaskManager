package org.example.taskmanager.taskmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableResilientMethods
@SpringBootApplication
public class TaskManagerApplication {

  public static void main(String[] args) {
    SpringApplication.run(TaskManagerApplication.class, args);
  }

}
