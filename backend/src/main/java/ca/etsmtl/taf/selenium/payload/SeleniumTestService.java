package ca.etsmtl.taf.selenium.payload;

import ca.etsmtl.taf.selenium.payload.requests.SeleniumAction;
import ca.etsmtl.taf.selenium.payload.requests.SeleniumCase;
import ca.etsmtl.taf.selenium.payload.requests.SeleniumResponse;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

@Component
public class SeleniumTestService {

    public SeleniumResponse executeTestCase(SeleniumCase seleniumCase) {
        List<SeleniumAction> seleniumActions = seleniumCase.getActions();

        SeleniumResponse seleniumResponse = new SeleniumResponse();
        seleniumResponse.setCase_id(seleniumCase.getCase_id());
        seleniumResponse.setCaseName(seleniumCase.getCaseName());
        seleniumResponse.setSeleniumActions(seleniumActions);
        long currentTimestamp = (new Timestamp(System.currentTimeMillis())).getTime();
        seleniumResponse.setTimestamp(currentTimestamp / 1000);

        WebDriver driver = new SafariDriver(); // Utilisation de SafariDriver
        // Attente implicite globale
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        long startTime = System.currentTimeMillis();

        try {
            for (SeleniumAction seleniumAction : seleniumActions) {
                switch (seleniumAction.getAction_type_id()) {
                    case 1: // goToUrl
                        driver.get(seleniumAction.getInput());
                        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
                        break;
                
                    case 2: // FillField amélioré
                    WebElement textBox = driver.findElement(By.name(seleniumAction.getObject()));
                    String fieldType = textBox.getAttribute("type");
                    
                    // Vérifier si le champ a une limite de caractères
                    String maxLengthAttr = textBox.getAttribute("maxlength");
                    int maxLength = maxLengthAttr != null ? Integer.parseInt(maxLengthAttr) : Integer.MAX_VALUE;
                    
                    // Limiter l'entrée si nécessaire
                    String inputText = seleniumAction.getInput().length() > maxLength 
                                        ? seleniumAction.getInput().substring(0, maxLength)
                                        : seleniumAction.getInput();
                
                    // Vérifier le type de champ et insérer les données appropriées
                    if ("email".equals(fieldType)) {
                        // Valider l'entrée d'email basique si nécessaire
                        if (inputText.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")) {
                            textBox.sendKeys(inputText);
                        } else {
                            setFailureResponse(seleniumResponse, driver, startTime, "Invalid email format for field " + seleniumAction.getObject());
                            return seleniumResponse;
                        }
                    } else if ("number".equals(fieldType)) {
                        // Assurer que l'entrée est numérique pour un champ de type "number"
                        try {
                            Double.parseDouble(inputText);
                            textBox.sendKeys(inputText);
                        } catch (NumberFormatException e) {
                            setFailureResponse(seleniumResponse, driver, startTime, "Invalid number format for field " + seleniumAction.getObject());
                            return seleniumResponse;
                        }
                    } else {
                        // Pour tout autre type de champ (text, password, etc.), insérer simplement l'entrée
                        textBox.sendKeys(inputText);
                    }
                    break;
                
                    case 3: // GetAttribute
                        WebElement webElement = driver.findElement(By.name(seleniumAction.getTarget()));
                        String pageAttribute = webElement.getAttribute(seleniumAction.getObject());
                        if (!pageAttribute.equals(seleniumAction.getInput())) {
                            setFailureResponse(seleniumResponse, driver, startTime, "Attribute " + seleniumAction.getObject() + " of " + seleniumAction.getTarget() + " is " + pageAttribute + " instead of " + seleniumAction.getInput());
                            return seleniumResponse;
                        }
                        break;
                    case 4: // GetPageTitle
                        String pageTitle = driver.getTitle();
                        if (!pageTitle.equals(seleniumAction.getTarget())) {
                            setFailureResponse(seleniumResponse, driver, startTime, "Page title is " + pageTitle + " instead of " + seleniumAction.getTarget());
                            return seleniumResponse;
                        }
                        break;
                    case 5: // Clear
                        WebElement textBoxToClear = driver.findElement(By.name(seleniumAction.getObject()));
                        textBoxToClear.clear();
                        break;
                    case 6: // Click
                        try {
                            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10)); // 10 secondes d'attente
                            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(seleniumAction.getObject())));
                            element.click();
                        } catch (NoSuchElementException e) {
                            setFailureResponse(seleniumResponse, driver, startTime, "L'élément avec l'ID '" + seleniumAction.getObject() + "' est introuvable.");
                            return seleniumResponse;
                        }
                        break;
                    
                    case 7: // IsDisplayed amélioré pour sites statiques et dynamiques
                        try {
                            // Utilise WebDriverWait pour attendre la visibilité de l'élément
                            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5)); // Délai maximum ajustable
                            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name(seleniumAction.getObject()))); // Remplacer par le sélecteur adapté
                            
                            // Vérifie que l'élément est bien visible (isDisplayed)
                            if (!element.isDisplayed()) {
                                setFailureResponse(seleniumResponse, driver, startTime, "L'élément " + seleniumAction.getObject() + " est présent mais non visible à l'écran.");
                                return seleniumResponse;
                            }
                    
                        } catch (NoSuchElementException | TimeoutException e) {
                            // Définir une réponse d'échec si l'élément est introuvable ou invisible dans le délai imparti
                            setFailureResponse(seleniumResponse, driver, startTime, "L'élément " + seleniumAction.getObject() + " est introuvable ou invisible après le délai imparti.");
                            return seleniumResponse;
                        }
                        break;
                    
                    case 8: // VerifyText
                        WebElement element = driver.findElement(By.id(seleniumAction.getObject()));
                        String actualText = element.getText().trim(); // Trim pour éviter les espaces en trop
                        System.out.println("Texte capturé : " + actualText); // Affiche le texte dans la console
                        if (!actualText.equals(seleniumAction.getTarget().trim())) { // Comparaison avec trim
                            setFailureResponse(seleniumResponse, driver, startTime, "Text of " + seleniumAction.getObject() + " is '" + actualText + "' instead of '" + seleniumAction.getTarget() + "'");
                            return seleniumResponse;
                        }
                        break;
                    
                    case 9: // SelectDropdown
                    WebElement dropdown = driver.findElement(By.id(seleniumAction.getObject()));
                    Select select = new Select(dropdown);
                    select.selectByVisibleText(seleniumAction.getInput()); // sélection par le texte visible
                        break;
                    case 10: // HoverOver
                        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                        WebElement hoverElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(seleniumAction.getObject())));
                        Actions actions = new Actions(driver);
                        actions.moveToElement(hoverElement).perform();
                        // Attendre un peu pour observer le résultat
                        Thread.sleep(2000); 
                        break;
                    case 11: // ToggleCheckbox
                        WebElement checkbox = driver.findElement(By.xpath(seleniumAction.getObject()));
                        boolean isChecked = checkbox.isSelected();
                        if ((seleniumAction.getInput().equalsIgnoreCase("true") && !isChecked) ||
                            (seleniumAction.getInput().equalsIgnoreCase("false") && isChecked)) {
                            checkbox.click(); // Coche ou décoche en fonction de la valeur dans input
                        }
                        break;

                    case 12: // SelectRadio avec JavaScript pour le clic
                        try {
                            WebElement radioButton = driver.findElement(By.id(seleniumAction.getObject()));
                            
                            // Vérifie si le bouton radio est activé avant de tenter de cliquer
                            if (!radioButton.isEnabled()) {
                                setFailureResponse(seleniumResponse, driver, startTime, "L'élément radio " + seleniumAction.getObject() + " est désactivé et ne peut pas être sélectionné.");
                                return seleniumResponse;
                            }
                            
                            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", radioButton);
                        } catch (NoSuchElementException e) {
                            setFailureResponse(seleniumResponse, driver, startTime, "L'élément radio " + seleniumAction.getObject() + " est introuvable.");
                            return seleniumResponse;
                        }
                        break;
                    

                    case 13: // File Upload
                        try {
                            WebElement fileInput = driver.findElement(By.name(seleniumAction.getObject()));
                            fileInput.sendKeys(seleniumAction.getInput()); // Chemin du fichier à télécharger
                        } catch (NoSuchElementException e) {
                            setFailureResponse(seleniumResponse, driver, startTime, "Le champ de téléchargement de fichier " + seleniumAction.getObject() + " est introuvable.");
                            return seleniumResponse;
                        }
                        break;
                    
                    case 14: // JavaScript Alert Handling
                        try {
                            Alert alert = driver.switchTo().alert();
                            
                            // Vérification de l'action à effectuer sur l'alerte
                            if ("accept".equalsIgnoreCase(seleniumAction.getInput())) {
                                alert.accept(); // Accepter l'alerte
                                seleniumResponse.setOutput("Alerte acceptée avec succès.");
                            } else if ("dismiss".equalsIgnoreCase(seleniumAction.getInput())) {
                                alert.dismiss(); // Refuser l'alerte
                                seleniumResponse.setOutput("Alerte refusée avec succès.");
                            } else {
                                setFailureResponse(seleniumResponse, driver, startTime, "Action d'alerte non reconnue : " + seleniumAction.getInput());
                                return seleniumResponse;
                            }
                            
                            seleniumResponse.setSuccess(true); // Marquer l'action comme réussie
                        } catch (NoAlertPresentException e) {
                            setFailureResponse(seleniumResponse, driver, startTime, "Aucune alerte JavaScript présente pour " + seleniumAction.getObject());
                        } catch (Exception e) {
                            setFailureResponse(seleniumResponse, driver, startTime, "Erreur inattendue lors de la gestion de l'alerte : " + e.getMessage());
                        }
                        break;
                    
                    case 15: // Input
                        try {
                            WebElement inputField = driver.findElement(By.cssSelector(seleniumAction.getObject()));
                            inputField.clear();
                            inputField.sendKeys(seleniumAction.getInput()); // Texte à insérer
                        } catch (NoSuchElementException e) {
                            setFailureResponse(seleniumResponse, driver, startTime, "Le champ de saisie " + seleniumAction.getObject() + " est introuvable.");
                            return seleniumResponse;
                        }
                        break;

                    case 16: // Redirect Link
                        try {
                            // Obtenir l'URL actuelle avant le clic
                            String currentUrl = driver.getCurrentUrl();
                            
                            // Cliquer sur l'élément de redirection
                            WebElement link = driver.findElement(By.id(seleniumAction.getObject()));
                            link.click();
                            
                            // Vérifier immédiatement si l'URL a changé
                            String newUrl = driver.getCurrentUrl();
                            
                            if (newUrl.equals(currentUrl)) {
                                setFailureResponse(seleniumResponse, driver, startTime, "La redirection n'a pas eu lieu.");
                                return seleniumResponse;
                            }
                        } catch (NoSuchElementException e) {
                            setFailureResponse(seleniumResponse, driver, startTime, "L'élément de redirection est introuvable.");
                            return seleniumResponse;
                        }
                        break;
                    

                    
                    
                    default:
                        System.out.println("Unsupported action type id: " + seleniumAction.getAction_type_id());
                        break;
                }
            }

            driver.quit();
            seleniumResponse.setDuration(System.currentTimeMillis() - startTime);
            seleniumResponse.setSuccess(true);

        } catch (Exception e) {
            setFailureResponse(seleniumResponse, driver, startTime, e.getMessage());
        }

        return seleniumResponse;
    }

    private void setFailureResponse(SeleniumResponse seleniumResponse, WebDriver driver, long startTime, String message) {
        driver.quit();
        seleniumResponse.setSuccess(false);
        seleniumResponse.setOutput(message);
        seleniumResponse.setDuration(System.currentTimeMillis() - startTime);
    }
}
