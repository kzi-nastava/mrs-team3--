package com.st3.uber.e2e.s3.test;

import com.st3.uber.e2e.s3.pages.AdminHistoryPage;
import com.st3.uber.e2e.s3.pages.HomePage;
import com.st3.uber.e2e.s3.pages.HomePageUnregistered;
import com.st3.uber.e2e.s3.pages.LoginPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class FilterAndSortAdminRideHistoryTest {
  WebDriver driver;
  HomePageUnregistered homePageUnregistered;
  LoginPage loginPage;
  HomePage homePage;
  AdminHistoryPage adminHistoryPage;

  private String email = "admin@gmail.com";
  private String password = "aaaaaa";

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

  }

}
