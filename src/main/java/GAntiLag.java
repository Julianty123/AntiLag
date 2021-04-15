import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.extra.harble.HashSupport;
import gearth.extensions.parsers.HEntity;
import gearth.extensions.parsers.HEntityType;
import gearth.extensions.parsers.HUserProfile;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@ExtensionInfo(
        Title = "GAntiLag",
        Description = "Allows you funny options",
        Version = "1",
        Author = "Julianty"
)

public class GAntiLag extends ExtensionForm {
    private HashSupport mHashSupport;

    public CheckBox checkHideSpeech, checkHideShoutOut, checkClickThrough,
    checkHideDance, checkFreezeUsers, checkIgnoreWhispers, checkHideFloorItems,
    checkHideWallItems, checkHideBubbles, checkClickHide, checkDisableDouble, checkUsersToRemove;

    Map<Integer,Integer> IdAndIndex = new HashMap<Integer,Integer>();

    public Text textRequests; // Es utlizado "como un label" porque asi funciona... wtf

    public static void main(String[] args) {
        runExtensionForm(args, GAntiLag.class);
    }

    @Override
    public ExtensionForm launchForm(Stage primaryStage) throws Exception {
        primaryStage.close();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GAntiLag.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("GAntiLag");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.setAlwaysOnTop(true);

        return loader.getController();
    }

    @Override
    protected void initExtension() {
        mHashSupport = new HashSupport(this);

        // Detecta cuando el usuario cierra la ventana (En este proyecto solo funciona aqui)
        primaryStage.setOnCloseRequest(e -> {
            checkClickThrough.setSelected(false);   sendToClient(new HPacket(1573, false));
            checkClickHide.setSelected(false);  checkClickHide.setDisable(false);   checkDisableDouble.setSelected(false);
            checkUsersToRemove.setSelected(false);  IdAndIndex.clear(); checkHideFloorItems.setSelected(false);
            checkHideWallItems.setSelected(false);  checkHideBubbles.setSelected(false);    checkHideSpeech.setSelected(false);
            checkHideShoutOut.setSelected(false);   checkHideDance.setSelected(false);  checkFreezeUsers.setSelected(false);
            checkIgnoreWhispers.setSelected(false);
            //Platform.exit();
        });

        // Cuando clickea un usuario se ejecuta esto
        mHashSupport.intercept(HMessage.Direction.TOSERVER, "GetSelectedBadges", hMessage -> {
            if(checkUsersToRemove.isSelected()){ // Elimina el usuario de la sala en el cliente
                mHashSupport.sendToClient("UserRemove", String.valueOf(IdAndIndex.get(hMessage.getPacket().readInteger())));
            }
        });

        intercept(HMessage.Direction.TOCLIENT, 812, hMessage -> {
            //IdAndIndex.clear(); Al no borrar la lista esos datos se almacenan ojo con eso
            try {
                HPacket hPacket = hMessage.getPacket();
                HEntity[] roomUsersList = HEntity.parse(hPacket);
                for (HEntity hEntity: roomUsersList){
                    // El ID del usuario no esta en el Map (Dictionary en c#)
                    if(!IdAndIndex.containsKey(hEntity.getId())){
                        IdAndIndex.put(hEntity.getId(), hEntity.getIndex());
                    }
                    else { // Se especifica la key, para remplazar el value por uno nuevo
                        IdAndIndex.replace(hEntity.getId(), hEntity.getIndex());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Click para ocultar furnis de piso
        intercept(HMessage.Direction.TOSERVER, 3398, hMessage -> {
            if(checkClickHide.isSelected()){
                sendToClient(new HPacket(834, String.valueOf(hMessage.getPacket().readInteger())
                        , false, 77483470/* Asi es la estructura del paquete */, 0));
            }
        });

        // Click para ocultar furnis de de pared

        // Bloquea el doble click en el servidor
        intercept(HMessage.Direction.TOSERVER, 3398, hMessage -> {
            if(checkDisableDouble.isSelected()){
                hMessage.setBlocked(true);
                System.out.println("prueba");
            }
        });

        // Cada vez que un usuario llega a sala hace esto...
        intercept(HMessage.Direction.TOCLIENT, 812, hMessage -> {
            if(checkClickThrough.isSelected()){
                sendToClient(new HPacket(1573, true));
            }
        });

        // Bloquea cuando un usuario habla en sala
        mHashSupport.intercept(HMessage.Direction.TOCLIENT, "Chat", hMessage -> {
            if(checkHideSpeech.isSelected()){
                hMessage.setBlocked(true);
            }
        });

        // Ignora cuando un usuario escribe en negrilla (gritar)
        mHashSupport.intercept(HMessage.Direction.TOCLIENT, "Shout", hMessage -> {
            if(checkHideShoutOut.isSelected()){
                hMessage.setBlocked(true);
            }
        });

        // Ignora la gente que te susurra
        mHashSupport.intercept(HMessage.Direction.TOCLIENT, "Whisper", hMessage -> {
            if(checkIgnoreWhispers.isSelected()){
                hMessage.setBlocked(true);
            }
        });

        // Esconde las burbujas de los usuarios (si no estan digitando en ese momento)
        mHashSupport.intercept(HMessage.Direction.TOCLIENT, "UserTyping", hMessage -> {
            if(checkHideBubbles.isSelected()){
                hMessage.setBlocked(true);
            }
        });

        // Esconde TU burbuja (Lado del servidor)
        mHashSupport.intercept(HMessage.Direction.TOCLIENT, "UnknownChatMessage_1", hMessage -> {
            if(checkHideBubbles.isSelected()){
                hMessage.setBlocked(true);
            }
        });

        // Bloquea el baile (si no esta bailando en ese momento)
        mHashSupport.intercept(HMessage.Direction.TOCLIENT, "Dance", hMessage -> {
            if(checkHideDance.isSelected()){
                hMessage.setBlocked(true);
            }
        });

        // Congela a los usuarios (si no se estan moviendo en ese momento)
        mHashSupport.intercept(HMessage.Direction.TOCLIENT, "UserUpdate", hMessage -> {
            if(checkFreezeUsers.isSelected()){
                hMessage.setBlocked(true);
            }
        });

        // Oculta los furnis de suelo en la sala
        mHashSupport.intercept(HMessage.Direction.TOCLIENT, "Objects", hMessage -> {
            if(checkHideFloorItems.isSelected()){
                hMessage.setBlocked(true);
            }
        });

        // Oculta los furnis de pared en la sala
        mHashSupport.intercept(HMessage.Direction.TOCLIENT, "Items", hMessage -> {
            if(checkHideWallItems.isSelected()){
                hMessage.setBlocked(true);
            }
        });
    }

    public void Click_through(ActionEvent actionEvent){
        if(checkClickThrough.isSelected()){
            sendToClient(new HPacket(1573, true));   // Habilita el "Click through"
        }
        else {
            sendToClient(new HPacket(1573, false));
        }
    }

    public void handleDoubleClick(ActionEvent actionEvent) {
        if(checkDisableDouble.isSelected()){
            checkClickHide.setSelected(false);  checkClickHide.setDisable(true);
        }
        else {
            checkClickHide.setDisable(false);
        }
    }
}