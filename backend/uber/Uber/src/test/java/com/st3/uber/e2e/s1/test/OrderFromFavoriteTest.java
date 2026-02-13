package com.st3.uber.e2e.s1.test;

import com.st3.uber.e2e.s1.pages.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class OrderFromFavoriteTest {

    WebDriver driver;
    HomePageUnregistered homePageUnregistered;
    LoginPage loginPage;
    HomePage homePage;
    PassengerHistoryPage passengerHistoryPage;
    RideTrackingPage rideTrackingPage;

    @BeforeSuite
    public void initialize() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");

        this.driver = new ChromeDriver(options);
        this.homePageUnregistered = new HomePageUnregistered(this.driver);
        this.loginPage = new LoginPage(this.driver);
        this.homePage = new HomePage(this.driver);
        this.passengerHistoryPage = new PassengerHistoryPage(this.driver);
        this.rideTrackingPage = new RideTrackingPage(this.driver);
    }


    @Test
    public void testGoToLoginPage() {
        this.homePageUnregistered.clickLoginButton();
        assertTrue(this.loginPage.isOnLoginPage());
    }

    @Test(dataProvider = "getLoginData")
    public void testLogin(String email, String password) {
        this.homePageUnregistered.clickLoginButton();
        loginPage.login(email,password);
        assertTrue(homePage.isLoggedIn());
    }


    @Test(dataProvider = "getData")
    public void testAddToFavorite(String email, String password, int index) {
        this.homePageUnregistered.clickLoginButton();
        loginPage.login(email,password);
        assertTrue(homePage.isLoggedIn());
        this.homePage.goToDriverHistoryPage();
        assertTrue(this.passengerHistoryPage.isOnHistoryPage());
        this.passengerHistoryPage.addToFavorite(index);
        assertTrue(this.passengerHistoryPage.isAddedToFavorite(index));
    }

    @Test(dataProvider = "getData")
    public void testOrderFromFavorite(String email, String password, int index) {
        this.homePageUnregistered.clickLoginButton();
        loginPage.login(email,password);
        assertTrue(homePage.isLoggedIn());
        this.homePage.goToDriverHistoryPage();
        assertTrue(this.passengerHistoryPage.isOnHistoryPage());
        this.passengerHistoryPage.addToFavorite(index);
        assertTrue(this.passengerHistoryPage.isAddedToFavorite(index));
        String historyRoute = this.passengerHistoryPage.getFullRouteFromFirstRide(index);
        this.passengerHistoryPage.goToHomePage();
        assertTrue(this.homePage.isLoggedIn());

        assertTrue(homePage.selectFavoriteByExactRoute(historyRoute));

        this.homePage.bookARide();

        this.homePage.goToRideTracking();

        assertTrue(this.rideTrackingPage.isOnRideTrackingPageAndAccepted());

    }


    @DataProvider
    Object[][] getLoginData() {
        return new Object[][] {
                {"prlincevic04@gmail.com", "Lukaprle123"}
        };
    }

    @DataProvider
    Object[][] getData() {
        return new Object[][] {
                {"prlincevic04@gmail.com", "Lukaprle123",0}
        };
    }

    @AfterSuite
    public void deinitialize() {
        this.driver.quit();
    }
}
