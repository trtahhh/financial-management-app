package com.example.finance;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.Connection;

@SpringBootApplication
public class FinancialManagementApplication {
  public static void main(String[] args) {
    SpringApplication.run(FinancialManagementApplication.class, args);
  }

  @Autowired
  private DataSource dataSource;

  @EventListener(ApplicationReadyEvent.class)
  public void printConnectionSuccess() {
    try (Connection conn = dataSource.getConnection()) {
      System.out.println("\u001B[32mDa ket noi thanh cong đen server SQL Server!\u001B[0m");
    } catch (Exception e) {
      System.out.println("\u001B[31mKet noi den server that bai!\u001B[0m");
    }
  }
}