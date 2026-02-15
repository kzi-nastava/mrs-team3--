package com.st3.uber.e2e.s3.test;

import com.st3.uber.e2e.s3.pages.AdminHistoryPage;
import com.st3.uber.e2e.s3.pages.HomePage;
import com.st3.uber.e2e.s3.pages.HomePageUnregistered;
import com.st3.uber.e2e.s3.pages.LoginPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertTrue;

public class FilterAndSortAdminRideHistoryTest {
  WebDriver driver;
  HomePageUnregistered homePageUnregistered;
  LoginPage loginPage;
  HomePage homePage;
  AdminHistoryPage adminHistoryPage;

  private String email = "admin@gmail.com";
  private String password = "aaaaaa";
  private String startDate = "2026-02-10";
  private String endDate = "2026-02-15";

  private List<String> sortOptions = List.of(
      "startTime|desc", "startTime|asc",
      "endTime|desc", "endTime|asc",
      "price|desc", "price|asc",
      "status|desc", "status|asc",
      "panic|desc", "panic|asc",
      "route|desc", "route|asc"
  );
  private String sortBy = sortOptions.get(8);

  @BeforeSuite
  public void initialize() {
    ChromeOptions options = new ChromeOptions();
    options.addArguments("--start-maximized");

    this.driver = new ChromeDriver(options);
    this.homePageUnregistered = new HomePageUnregistered(this.driver);
    this.loginPage = new LoginPage(this.driver);
    this.homePage = new HomePage(this.driver);
    this.adminHistoryPage = new AdminHistoryPage(this.driver);
  }

  @Test
  public void testFilterAndSort(){
    this.homePageUnregistered.clickLoginButton();
    assertTrue(this.loginPage.isOnLoginPage());

    this.homePageUnregistered.clickLoginButton();
    loginPage.login(email,password);
    assertTrue(homePage.isLoggedIn());

    this.homePage.goToRideHistoryPage();
    assertTrue(this.homePage.isOnHistoryPage());

    this.adminHistoryPage.selectFromDate(startDate);
    this.adminHistoryPage.selectToDate(endDate);

    this.adminHistoryPage.sortByValue(sortBy);
  }
}
