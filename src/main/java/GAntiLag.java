import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.parsers.HEntity;
import gearth.extensions.parsers.HFloorItem;
import gearth.extensions.parsers.HWallItem;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javax.swing.*;
import java.util.LinkedList;
import java.util.TreeMap;

@ExtensionInfo(
        Title = "GAntiLag",
        Description = "Allows you funny options",
        Version = "1.0.1",
        Author = "Julianty"
)

public class GAntiLag extends ExtensionForm {
    public CheckBox checkHideSpeech, checkHideShoutOut, checkClickThrough,
            checkHideDance, checkFreezeUsers, checkIgnoreWhispers, checkHideFloorItems,
            checkHideWallItems, checkHideBubbles, checkClickHide, checkDisableDouble, checkUsersToRemove, checkAntiBobba;

    TreeMap<Integer,Integer> IdAndIndex = new TreeMap<>();
    LinkedList<Integer> hiddenFloorList = new LinkedList<>();
    LinkedList<Integer> hiddenWallList = new LinkedList<>();
    // LinkedList<FloorObject> floorObjectsList = new LinkedList<>(); // Coming soon!

    // public Text textRequests; //Se puede usar como un "label" lmao
    public int YourUserID;
    public String YourUserName;

    // To fix the bug... bruh :(
    Timer timer1 = new Timer(1, e -> {
        for (Integer furniId : hiddenFloorList) {
            sendToClient(new HPacket("ObjectRemove", HMessage.Direction.TOCLIENT, String.valueOf(furniId), false, YourUserID, 0));
        }
        for (Integer furniId : hiddenWallList) {
            sendToClient(new HPacket("ItemRemove", HMessage.Direction.TOCLIENT, String.valueOf(furniId), YourUserID));
        }
        stopTimer();
    });


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
            checkIgnoreWhispers.setSelected(false); checkAntiBobba.setSelected(false);
            sendToServer(new HPacket("GetHeightMap", HMessage.Direction.TOSERVER));
        });

        // Happens when you check or uncheck the checkBox control!
        checkHideFloorItems.setOnAction(e-> {
            sendToServer(new HPacket("GetHeightMap", HMessage.Direction.TOSERVER));
            /* Coming soon! ...
            for(FloorObject floorObject: floorObjectsList){
                if(checkHideFloorItems.isSelected()){
                    sendToClient(new HPacket("ObjectRemove", HMessage.Direction.TOCLIENT, String.valueOf(floorObject.getFurniId()), false, YourUserID, 0));
                }
                else if(!checkHideFloorItems.isSelected()){
                    if(!hiddenFloorList.contains(floorObject.getFurniId())){
                        sendToClient(new HPacket("ObjectAdd", HMessage.Direction.TOCLIENT, floorObject.getFurniId(), floorObject.furniTypeId, floorObject.furniCoordX,
                                floorObject.furniCoordY, 0, floorObject.furniElevation, floorObject.idkString1, 0, 0, "1", -1, 1, 51157174, YourUserName));
                    }
                }
            }*/
        });

        checkHideWallItems.setOnAction(e -> sendToServer(new HPacket("GetHeightMap", HMessage.Direction.TOSERVER)));


        checkUsersToRemove.setOnAction(e -> {
            // Coming soon i need to fix this
            if(!checkUsersToRemove.isSelected()){
                sendToServer(new HPacket("GetHeightMap", HMessage.Direction.TOSERVER));
            }
        });

        // The packet is sent to the server and a response is obtained from the CLIENT !!
        sendToServer(new HPacket("InfoRetrieve", HMessage.Direction.TOSERVER));

        // Intercepts the client's response and does something ...
        intercept(HMessage.Direction.TOCLIENT, "UserObject", hMessage -> {
            // Be careful, the data must be obtained in the order of the packet
            YourUserID = hMessage.getPacket().readInteger();
            YourUserName = hMessage.getPacket().readString();
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
            if(checkDisableDouble.isSelected()){
                hMessage.setBlocked(true);  // Blocks double click
            }
            if(checkClickHide.isSelected()){
                int furniId = hMessage.getPacket().readInteger();   hiddenFloorList.add(furniId);
                sendToClient(new HPacket("ObjectRemove", HMessage.Direction.TOCLIENT,
                                String.valueOf(furniId), false, YourUserID, 0)); // Hide Floor Item
                int count = hiddenFloorList.size() + hiddenWallList.size();
                Platform.runLater(()-> checkClickHide.setText("Double click for hide (" + count + ")"));
            }
        });

        // Intercepts doble click to wall item
        intercept(HMessage.Direction.TOSERVER, "UseWallItem", hMessage -> {
            if(checkClickHide.isSelected()){
                int furniId = hMessage.getPacket().readInteger();   hiddenWallList.add(furniId);
                sendToClient(new HPacket("ItemRemove", HMessage.Direction.TOCLIENT,
                        String.valueOf(furniId), YourUserID)); // Hide Wall Item
                int count = hiddenFloorList.size() + hiddenWallList.size();
                Platform.runLater(()-> checkClickHide.setText("Double click for hide (" + count + ")"));
            }
        });
        intercept(HMessage.Direction.TOSERVER, "GetItemData", hMessage -> { // When you give click in a stickie (notes)
            if(checkClickHide.isSelected()){
                int furniId = hMessage.getPacket().readInteger();   hiddenWallList.add(furniId);
                sendToClient(new HPacket("ItemRemove", HMessage.Direction.TOCLIENT,
                        String.valueOf(furniId), YourUserID)); // Hide Wall Item
                int count = hiddenFloorList.size() + hiddenWallList.size();
                Platform.runLater(()-> checkClickHide.setText("Double click for hide (" + count + ")"));
            }
        });

        // When a user arrives to room
        intercept(HMessage.Direction.TOCLIENT, "Users", hMessage -> {
            if(checkClickThrough.isSelected()){ // Allows again "Click Through"
                sendToClient(new HPacket("YouArePlayingGame", HMessage.Direction.TOCLIENT, true));
            }
        });

        // Oculta los furnis de suelo en la sala
        intercept(HMessage.Direction.TOCLIENT, "Objects", hMessage -> {
            // floorObjectsList.clear();   // Limpia la lista
            try{
                for (HFloorItem hFloorItem: HFloorItem.parse(hMessage.getPacket())){
                    // {l}{h:2368}{i:602199816}{i:3378}{i:9}{i:13}{i:0}{s:"0.65"}{s:"0.37"}{i:0}{i:0}{s:"1"}{i:-1}{i:1}{i:51157174}{s:"Julianty"}
                    // hFloorItem.getStuff()[0] // stateFurni (Obtiene el primer elemento del array, creo)
                    // I need to get the furni orientation
                    /*  Coming Soon!
                    floorObjectsList.add(new FloorObject(hFloorItem.getId(), hFloorItem.getTypeId(), hFloorItem.getTile().getX(), hFloorItem.getTile().getY(), 0,
                            hFloorItem.getTile().getZ()+"", "0.37", 0, 0, "1", -1, 1, 51157174, YourUserName));*/
                    if(checkHideFloorItems.isSelected()){
                        // Se pueden enviar muchos paquetes al cliente sin importar su delay
                        sendToClient(new HPacket("ObjectRemove", HMessage.Direction.TOCLIENT, String.valueOf(hFloorItem.getId()), false, YourUserID, 0));
                        hMessage.setBlocked(true); // Solve bug wtf
                    }
                    if(hiddenFloorList.contains(hFloorItem.getId())){
                        timer1.start();
                    }
                }
            }catch (Exception ignored){}
        });

        // Intercept all wall items in the room
        intercept(HMessage.Direction.TOCLIENT, "Items", hMessage -> {
            try{
                for (HWallItem hWallItem: HWallItem.parse(hMessage.getPacket())){
                    if(checkHideWallItems.isSelected()){
                        sendToClient(new HPacket("ItemRemove", HMessage.Direction.TOCLIENT,
                                String.valueOf(hWallItem.getId()), YourUserID)); // "Hide" wall items
                        hMessage.setBlocked(true);  // Fix the bug
                    }
                    if(hiddenWallList.contains(hWallItem.getId())){
                        timer1.start();
                    }
                }
            }catch (Exception ignored){}
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

        // Thanks to Achantur for the special character, you can see it here: https://github.com/achantur/AntiBobba
        intercept(HMessage.Direction.TOSERVER, "Chat", hMessage -> {
            String message = hMessage.getPacket().readString();
            int color = hMessage.getPacket().readInteger();
            int index = hMessage.getPacket().readInteger();
            if (checkAntiBobba.isSelected()) {
                hMessage.setBlocked(true);
                bypass(message, color, index);
            }
        });
    }

    public void stopTimer(){
        timer1.stop();
    }

    private void bypass(String message, int color, int index) {
        StringBuilder stringBuilder = new StringBuilder();
        for (char ch : message.toCharArray()) {
            stringBuilder.append("ӵӵ"); // ӵӵ Its the special character
            stringBuilder.append(ch);
        }
        stringBuilder.append("ӵӵ");
        sendToServer(new HPacket("Chat", HMessage.Direction.TOSERVER, stringBuilder.toString(), color, index));
    }

    public void Click_through(){
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

    public void handleHiddenList() {
        if(!checkHideFloorItems.isSelected()){
            /* Coming soon!
            for(FloorObject floorObject: floorObjectsList){
                if(hiddenFloorList.contains(floorObject.getFurniId())){
                    sendToClient(new HPacket("ObjectAdd", HMessage.Direction.TOCLIENT, floorObject.getFurniId(), floorObject.furniTypeId,
                            floorObject.furniCoordX, floorObject.furniCoordY, 0, floorObject.furniElevation, floorObject.idkString1, 0, 0, "1", -1, 1, 51157174, YourUserName));
                }
            }*/
            sendToServer(new HPacket("GetHeightMap", HMessage.Direction.TOSERVER));
        }
        hiddenFloorList.clear(); hiddenWallList.clear();
        checkClickHide.setText("Double click for hide (0)");
    }
}