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
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.InputStream;

public class GAntiLagLauncher extends ExtensionFormCreator {

    @Override
    public ExtensionForm createForm(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GAntiLag.fxml"));
        Parent root = loader.load();

        primaryStage.setScene(new Scene(root));
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.getScene().setFill(Color.TRANSPARENT);
        primaryStage.setResizable(false);
        primaryStage.setAlwaysOnTop(true);

        // Best way to set the icon (Works in both IDE and compiled application)
        String resourceName = "imageJ.jfif";
        InputStream inputStream = GAntiLagLauncher.class.getClassLoader().getResourceAsStream(resourceName);
        if (inputStream != null) {
            Image image = new Image(inputStream);
            primaryStage.getIcons().add(image);
        }

        return loader.getController();
    }

    public static void main(String[] args) {
        runExtensionForm(args, GAntiLagLauncher.class);
    }
}