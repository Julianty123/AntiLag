import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.parsers.HEntity;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.event.ActionEvent;
import javafx.scene.control.CheckBox;
import java.util.TreeMap;

@ExtensionInfo(
        Title = "GAntiLag",
        Description = "Allows you funny options",
        Version = "1.0",
        Author = "Julianty"
)

public class GAntiLag extends ExtensionForm {
    public CheckBox checkHideSpeech, checkHideShoutOut, checkClickThrough,
            checkHideDance, checkFreezeUsers, checkIgnoreWhispers, checkHideFloorItems,
            checkHideWallItems, checkHideBubbles, checkClickHide, checkDisableDouble, checkUsersToRemove;

    TreeMap<Integer,Integer> IdAndIndex = new TreeMap<>();

    // public Text textRequests; //Se podria usar como un "label" porque asi funciona... wtf
    public int UserID;

    @Override
    protected void onShow() {
        sendToServer(new HPacket("GetHeightMap", HMessage.Direction.TOSERVER));
    }

    @Override
    protected void initExtension() {
        // Detecta cuando el usuario cierra la ventana (En este proyecto solo funciona aqui)
        primaryStage.setOnCloseRequest(e -> {
            sendToClient(new HPacket("YouArePlayingGame", HMessage.Direction.TOCLIENT, false));
            checkClickThrough.setSelected(false);
            checkClickHide.setSelected(false);  checkClickHide.setDisable(false);   checkDisableDouble.setSelected(false);
            checkUsersToRemove.setSelected(false);  IdAndIndex.clear(); checkHideFloorItems.setSelected(false);
            checkHideWallItems.setSelected(false);  checkHideBubbles.setSelected(false);    checkHideSpeech.setSelected(false);
            checkHideShoutOut.setSelected(false);   checkHideDance.setSelected(false);  checkFreezeUsers.setSelected(false);
            checkIgnoreWhispers.setSelected(false);
        });

        // The packet is sent to the server and a response is obtained from the CLIENT !!
        sendToServer(new HPacket("InfoRetrieve", HMessage.Direction.TOSERVER));

        // Intercepts the client's response and does something ...
        intercept(HMessage.Direction.TOCLIENT, "UserObject", hMessage -> {
            // Be careful, the data must be obtained in the order of the packet
            UserID = hMessage.getPacket().readInteger();
        });

        // Cuando clickea a un usuario se ejecuta esto
        intercept(HMessage.Direction.TOSERVER, "GetSelectedBadges", hMessage -> {
            if(checkUsersToRemove.isSelected()){ // Elimina el usuario de la sala en el cliente
                sendToClient(new HPacket("UserRemove", HMessage.Direction.TOCLIENT,
                        String.valueOf(IdAndIndex.get(hMessage.getPacket().readInteger()))));
            }
        });

        intercept(HMessage.Direction.TOCLIENT, "Users", hMessage -> {
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

        // Intercepts doble click to floor item
        intercept(HMessage.Direction.TOSERVER, "UseFurniture", hMessage -> {
            if(checkDisableDouble.isSelected()){ // Bloquea el doble click
                hMessage.setBlocked(true);
            }
            if(checkClickHide.isSelected()){ // Oculta furnis de piso
                sendToClient(new HPacket("ObjectRemove", HMessage.Direction.TOCLIENT,
                                String.valueOf(hMessage.getPacket().readInteger()), false, UserID, 0));
            }
        });

        // Intercepts doble click to wall item
        intercept(HMessage.Direction.TOSERVER, "UseWallItem", hMessage -> {
            if(checkClickHide.isSelected()){ // Hide wall item
                sendToClient(new HPacket("ItemRemove", HMessage.Direction.TOCLIENT,
                        String.valueOf(hMessage.getPacket().readInteger()), UserID));
            }
        });

        // When a user arrives to room
        intercept(HMessage.Direction.TOCLIENT, "Users", hMessage -> {
            if(checkClickThrough.isSelected()){ // Habilita de nuevo "Click Through"
                sendToClient(new HPacket("YouArePlayingGame", HMessage.Direction.TOCLIENT, true));
            }
        });

        // Ignora cuando un usuario habla en sala (Me ignoro pero los demas NO a mi)
        intercept(HMessage.Direction.TOCLIENT, "Chat", hMessage -> {
            if(checkHideSpeech.isSelected()){
                hMessage.setBlocked(true);
            }
        });

        // Ignora cuando un usuario escribe en negrilla (Me ignoro pero los demas NO a mi)
        intercept(HMessage.Direction.TOCLIENT, "Shout", hMessage -> {
            if(checkHideShoutOut.isSelected()){
                hMessage.setBlocked(true);
            }
        });

        // Ignora la gente que te susurra (Me ignoro pero los demas NO a mi)
        intercept(HMessage.Direction.TOCLIENT, "Whisper", hMessage -> {
            if(checkIgnoreWhispers.isSelected()){
                hMessage.setBlocked(true);
            }
        });

        // Esconde las burbujas de los usuarios (si no estan digitando en ese momento)
        intercept(HMessage.Direction.TOCLIENT, "UserTyping", hMessage -> {
            if(checkHideBubbles.isSelected()){
                hMessage.setBlocked(true);
            }
        });

        // Esconde TU burbuja (Lado del servidor, la gente no vera cuando susurres!)
        intercept(HMessage.Direction.TOSERVER, "StartTyping", hMessage -> {
            if(checkHideBubbles.isSelected()){
                hMessage.setBlocked(true);
            }
        });

        // Bloquea el baile (si no esta bailando en ese momento)
        intercept(HMessage.Direction.TOCLIENT, "Dance", hMessage -> {
            if(checkHideDance.isSelected()){
                hMessage.setBlocked(true);
            }
        });

        // Congela a los usuarios (si no se estan moviendo en ese momento)
        intercept(HMessage.Direction.TOCLIENT, "UserUpdate", hMessage -> {
            if(checkFreezeUsers.isSelected()){
                hMessage.setBlocked(true);
            }
        });

        // Oculta los furnis de suelo en la sala
        intercept(HMessage.Direction.TOCLIENT, "Objects", hMessage -> {
            if(checkHideFloorItems.isSelected()){
                hMessage.setBlocked(true);
            }
        });

        // Oculta los furnis de pared en la sala
        intercept(HMessage.Direction.TOCLIENT, "Items", hMessage -> {
            if(checkHideWallItems.isSelected()){
                hMessage.setBlocked(true);
            }
        });
    }

    public void Click_through(ActionEvent actionEvent){
        if(checkClickThrough.isSelected()){
            sendToClient(new HPacket("YouArePlayingGame", HMessage.Direction.TOCLIENT, true));   // Habilita "Click Through"
        }
        else {
            sendToClient(new HPacket("YouArePlayingGame", HMessage.Direction.TOCLIENT, false));
        }
    }

    public void handleDoubleClick() {
        if(checkDisableDouble.isSelected()){
            checkClickHide.setSelected(false);  checkClickHide.setDisable(true);
        }
        else {
            checkClickHide.setDisable(false);
        }
    }
}