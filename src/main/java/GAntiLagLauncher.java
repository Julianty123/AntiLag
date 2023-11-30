import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionFormCreator;
import gearth.ui.themes.Theme;
import gearth.ui.themes.ThemeFactory;
import gearth.ui.titlebar.DefaultTitleBarConfig;
import gearth.ui.titlebar.TitleBarController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GAntiLagLauncher extends ExtensionFormCreator {

    @Override
    public ExtensionForm createForm(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GAntiLag.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("GAntiLag");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.setAlwaysOnTop(true);

        Theme defaultTheme = ThemeFactory.themeForTitle("G-Earth Dark");
        DefaultTitleBarConfig config = new DefaultTitleBarConfig(primaryStage, defaultTheme) {
            public boolean displayThemePicker() {
                return false; // For show bar themes (I like to be redundant)
            }
        };

        TitleBarController.create(primaryStage, config); // Idk implementation, but applies the theme to the bar
        Platform.runLater(() -> {
            primaryStage.getScene().getRoot().getStyleClass().add(defaultTheme.title().replace(" ", "-").toLowerCase());
            primaryStage.getScene().getRoot().getStyleClass().add(defaultTheme.isDark() ? "g-dark" : "g-light");
        });

        return loader.getController();
    }

    public static void main(String[] args) {
        runExtensionForm(args, GAntiLagLauncher.class);
    }
}