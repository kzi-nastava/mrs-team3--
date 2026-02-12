    package com.st3.uber.e2e.s1.pages;

    import org.openqa.selenium.By;
    import org.openqa.selenium.JavascriptExecutor;
    import org.openqa.selenium.WebDriver;
    import org.openqa.selenium.WebElement;
    import org.openqa.selenium.interactions.Actions;
    import org.openqa.selenium.support.FindBy;
    import org.openqa.selenium.support.PageFactory;
    import org.openqa.selenium.support.ui.ExpectedConditions;
    import org.openqa.selenium.support.ui.WebDriverWait;

    import java.time.Duration;
    import java.util.ArrayList;
    import java.util.List;

    public class HomePage {

        @FindBy(id = "history-btn")
        private WebElement historyButton;

        @FindBy(id = "title-ride")
        private WebElement titleRide;

        @FindBy(className = "favorites-btn")
        private WebElement favoritesButton;

        @FindBy(className = "book-ride-btn")
        private WebElement bookBtn;


        @FindBy(id = "tracking-ride-btn")
        private WebElement rideTracking;


        private WebDriver driver;
        private WebDriverWait wait;
        private Actions actions;


        public HomePage(WebDriver webDriver) {
            this.driver = webDriver;
            PageFactory.initElements(this.driver, this);
            this.actions = new Actions(this.driver);
            this.wait = new WebDriverWait(this.driver, Duration.ofSeconds(10));

        }

        public boolean isLoggedIn() {
            return this.wait.until(ExpectedConditions.textToBePresentInElement(titleRide, "Let's ride!"));
        }


        public void goToDriverHistoryPage() {
            this.wait.until(ExpectedConditions.visibilityOf(historyButton));
            this.actions.moveToElement(historyButton).click().perform();
        }

        public void openFavorites() {
            wait.until(ExpectedConditions.elementToBeClickable(favoritesButton));
            favoritesButton.click();
        }

        public boolean selectFavoriteByExactRoute(String historyRoute) {

            //Pretvaram u oblik za hash
            List<String> historyAddresses = normalizeRoute(historyRoute);

            //Klikcem na omiljene
            wait.until(ExpectedConditions.elementToBeClickable(favoritesButton));
            favoritesButton.click();

            //UCitavam rute iz omiljenih
            List<WebElement> favoriteCards =
                    wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                            By.className("favorite-item")));


            //Prolazim kroz omiljene i trazim poklapanje sa istorijom
            for (WebElement card : favoriteCards) {

                List<String> favoriteAddresses = new ArrayList<>();

                String routeText =
                        card.findElement(By.className("favorite-route"))
                                .getText()
                                .trim();

                String[] mainParts = routeText.split("→");

                favoriteAddresses.add(clean(mainParts[0]));
                favoriteAddresses.add(clean(mainParts[1]));

                List<WebElement> stopsSection =
                        card.findElements(By.className("favorite-stops"));

                if (!stopsSection.isEmpty()) {

                    String stopsText =
                            stopsSection.get(0)
                                    .findElement(By.className("stop"))
                                    .getText();

                    String[] stops = stopsText.split(",");

                    for (String stop : stops) {
                        favoriteAddresses.add(1, clean(stop));
                    }
                }


                if (historyAddresses.equals(favoriteAddresses)) {
                    System.out.println("EXACT MATCH FOUND!");

                    card.findElement(By.className("use-btn")).click();
                    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
                    return true;
                }
            }

            return false;
        }

        private List<String> normalizeRoute(String route) {

            List<String> result = new ArrayList<>();

            String[] parts = route.split("→");

            for (String p : parts) {
                result.add(clean(p));
            }

            return result;
        }

        private String clean(String s) {
            return s.trim().toLowerCase();
        }


        public void bookARide() {
            wait.until(ExpectedConditions.elementToBeClickable(bookBtn));
            actions.moveToElement(bookBtn).click().perform();

            WebElement confirmButton = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("confirm-ride-btn")));
            actions.moveToElement(confirmButton).click().perform();

            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.className("p-toast-message")));

            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.className("p-toast-message")));
        }


        public void goToRideTracking() {
            wait.until(ExpectedConditions.elementToBeClickable(rideTracking));
            actions.moveToElement(rideTracking).click().perform();
        }






    }
