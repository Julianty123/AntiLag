import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.parsers.*;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.Timer;
import org.apache.commons.io.IOUtils; // Important library for openConnection (See json or something like that!)


@ExtensionInfo(
        Title = "GAntiLag",
        Description = "Allows you useful and funny options",
        Version = "1.0.2",
        Author = "Julianty"
)


public class GAntiLag extends ExtensionForm implements Initializable{
    public static GAntiLag RUNNING_INSTANCE; // For use this class in other, its useful

    public CheckBox checkHideSpeech, checkHideShoutOut, checkClickThrough,
            checkHideDance, checkIgnoreWhispers, checkHideFloorItems, checkHideWallItems,
            checkHideBubbles, checkClickHide, checkDisableDouble, checkUsersToRemove, checkAntiBobba, checkFastRoom, checkWalkFast;
    public TextField textSteps;
    public TableView<Furniture> tableView;

    TreeMap<String, Integer> nameToTypeidFloor = new TreeMap<>();
    TreeMap<Integer, String> typeIdToNameFloor = new TreeMap<>();
    TreeMap<Integer,Integer> IdAndIndex = new TreeMap<>();
    LinkedList<Integer> hiddenFloorList = new LinkedList<>();
    LinkedList<Integer> hiddenWallList = new LinkedList<>();
    LinkedList<Furniture> flagListforTableView = new LinkedList<>();

    public int indexCounter;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TableColumn<Furniture, AtomicBoolean> checkColumn = new TableColumn<>("Hide");  // Make the column contain the value returned .isActive() or .getActive()
        TableColumn<Furniture, String> idColumn =  new TableColumn<>("Furni Id Set");
        TableColumn<Furniture, String> classNameColumn =  new TableColumn<>("Class Name");

        checkColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
        checkColumn.setCellFactory(tc -> new AtomicCheckBoxTableCell()); // Make the cell appear as a clickable checkbox
        idColumn.setCellValueFactory(new PropertyValueFactory<>("furniId"));
        classNameColumn.setCellValueFactory(new PropertyValueFactory<>("className"));

        idColumn.setPrefWidth(125);
        classNameColumn.setPrefWidth(150);

        tableView.getColumns().add(checkColumn);  // Add the column to the tableView
        tableView.getColumns().add(idColumn);
        tableView.getColumns().add(classNameColumn);
    }

    /*
    "doormat_plain*2", "doormat_plain*3", "doormat_love"
            ,"suncity_c19_floor", "xmas_c17_pavement", "xmas_c17_smallpavement", "xmas_c15_stone", "xmas11_woodfloor" */

    private static TreeMap<String, String> codeToDomainMap = new TreeMap<>();
    static {
        codeToDomainMap.put("br", ".com.br");
        codeToDomainMap.put("de", ".de");
        codeToDomainMap.put("es", ".es");
        codeToDomainMap.put("fi", ".fi");
        codeToDomainMap.put("fr", ".fr");
        codeToDomainMap.put("it", ".it");
        codeToDomainMap.put("nl", ".nl");
        codeToDomainMap.put("tr", ".com.tr");
        codeToDomainMap.put("us", ".com");
    }

    private static TreeMap<String, Integer> directionToCode = new TreeMap<>();
    static {
        directionToCode.put("North", 0);
        directionToCode.put("East", 2);
        directionToCode.put("South", 4);
        directionToCode.put("West", 6);
    }

    public String host;
    public int walkToX;
    public int walkToY;
    public int YourUserID;
    public String YourUserName;
    public int YourIndex = -1;

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

    // Necessary to solve a "bug", you must give two clicks or more when you get outs the teleport...
    Timer timerFixBug = new Timer(1, e -> sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, walkToX, walkToY)));

    @Override
    protected void onShow() {
        // The packet is sent to the server and a response is obtained from the CLIENT !!
        sendToServer(new HPacket("InfoRetrieve", HMessage.Direction.TOSERVER));
        // When its sent, get UserIndex without restart room
        sendToServer(new HPacket("AvatarExpression", HMessage.Direction.TOSERVER, 0));
        // When its sent, get wallitems, flooritems and other things without restart room
        sendToServer(new HPacket("GetHeightMap", HMessage.Direction.TOSERVER));
    }

    @Override
    protected void initExtension() {
        RUNNING_INSTANCE = this;

        onConnect((host, port, APIVersion, versionClient, client) -> {
            this.host = host.substring(5, 7);   // Example: Of "game-es.habbo.com" only takes "es"
            System.out.println("Getting GameData...");
            try { getGameFurniData(); } catch (Exception ignored) { }
            System.out.println("Gamedata Retrieved!");
        });

        // Detecta cuando el usuario cierra la ventana (En este proyecto solo funciona aqui)
        primaryStage.setOnCloseRequest(e -> {
            sendToClient(new HPacket("YouArePlayingGame", HMessage.Direction.TOCLIENT, false));
            checkClickThrough.setSelected(false);
            checkClickHide.setSelected(false);  checkClickHide.setDisable(false);   checkDisableDouble.setSelected(false);
            checkUsersToRemove.setSelected(false);  IdAndIndex.clear(); checkHideFloorItems.setSelected(false);
            checkHideWallItems.setSelected(false);  checkHideBubbles.setSelected(false);    checkHideSpeech.setSelected(false);
            checkHideShoutOut.setSelected(false);   checkHideDance.setSelected(false);
            checkIgnoreWhispers.setSelected(false); checkAntiBobba.setSelected(false);
            sendToServer(new HPacket("GetHeightMap", HMessage.Direction.TOSERVER));     YourIndex = -1;
        });

        // Happens when you check or uncheck the checkBox control!
        checkHideFloorItems.setOnAction(e-> {
            sendToServer(new HPacket("GetHeightMap", HMessage.Direction.TOSERVER));

            // Coming soon! ...
            /* for(Furniture person: flagListforTableView){
                if(checkHideFloorItems.isSelected()){
                    System.out.println("selected!");
                    sendToClient(new HPacket("ObjectRemove", HMessage.Direction.TOCLIENT, String.valueOf(person.getFurniId()), false, person.getOwnerId(), 0));
                }
                else if(!checkHideFloorItems.isSelected()){
                    System.out.println("no selected!");
                    // {in:ObjectAdd}{s:438444797}{i:3631}{i:9}{i:2}{i:0}{s:"1.0"}{s:"1.3"}{i:0}{i:0}{s:""}{i:-1}{i:0}{i:31657128}{s:"pablito"}
                    if(!hiddenFloorList.contains(Integer.parseInt(person.getFurniId()))){
                        sendToClient(new HPacket("ObjectAdd", HMessage.Direction.TOCLIENT, person.getFurniId(), person.getFurniTypeId(), person.getFurniCoordX(),
                                person.getFurniCoordY(), person.getFurniDirection(), person.getFurniElevation(), "1.3", 0, 0, "", -1, 0, person.getOwnerId(), person.getOwnerName()));
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

        // Intercepts the client's response and does something ...
        intercept(HMessage.Direction.TOCLIENT, "UserObject", hMessage -> {
            // Be careful, the data must be obtained in the order of the packet
            YourUserID = hMessage.getPacket().readInteger();
            YourUserName = hMessage.getPacket().readString();
        });

        // Response of packet AvatarExpression
        intercept(HMessage.Direction.TOCLIENT, "Expression", hMessage -> {
            // First integer is index in room, second is animation id, i think
            if(primaryStage.isShowing() && YourIndex == -1){ // this could avoid any bug
                YourIndex = hMessage.getPacket().readInteger();
            }
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
                    if(hEntity.getName().equals(YourUserName)){    // In another room, the index changes
                        YourIndex = hEntity.getIndex();
                    }
                    // El ID del usuario no esta en el Map (Dictionary en c#)
                    if(!IdAndIndex.containsKey(hEntity.getId())){
                        IdAndIndex.put(hEntity.getId(), hEntity.getIndex());
                    }
                    // Se especifica la key, para remplazar el value por uno nuevo
                    if(IdAndIndex.containsKey(hEntity.getId())) {
                        IdAndIndex.replace(hEntity.getId(), hEntity.getIndex());
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
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

        // Intercepts the floor items in the room
        intercept(HMessage.Direction.TOCLIENT, "Objects", hMessage -> {
            tableView.getItems().clear();   flagListforTableView.clear();
            for (HFloorItem hFloorItem: HFloorItem.parse(hMessage.getPacket())){
                try{
                    // System.out.println(hFloorItem.getStuff()[0]);
                    /*stateFurni (Obtiene el primer elemento del array, creo)
                    Necesito encontrar la forma de detectar si el furni es background (no tiene direction, creo)
                    quizas usando un if y un else if con hFloorItem.getFacing() podria ser util! */

                    // System.out.println("ID: " + hFloorItem.getId() + " ; police: " + hFloorItem.getUsagePolicy() + " ;list: " + Arrays.toString(hFloorItem.getStuff()));

                    flagListforTableView.add(new Furniture(indexCounter, String.valueOf(hFloorItem.getId()), typeIdToNameFloor.get(hFloorItem.getTypeId()), hFloorItem.getTypeId(), hFloorItem.getTile().getX(),
                            hFloorItem.getTile().getY(), directionToCode.get(hFloorItem.getFacing().toString()), String.valueOf(hFloorItem.getTile().getZ()), hFloorItem.getOwnerId(), hFloorItem.getOwnerName()));
                    indexCounter++;


                    if(checkHideFloorItems.isSelected()){
                        // Many packets can be send to client, dosent matter the delay!
                        sendToClient(new HPacket("ObjectRemove", HMessage.Direction.TOCLIENT, String.valueOf(hFloorItem.getId()), false, YourUserID, 0));
                        hMessage.setBlocked(true); // Solve bug wtf
                    }
                    if(hiddenFloorList.contains(hFloorItem.getId())){
                        timer1.start();
                    }
                }catch (Exception ignored){
                    System.out.println("Excepcion");
                }
            }

            if(checkClickThrough.isSelected()){ // Allows again "Click Through"
                sendToClient(new HPacket("YouArePlayingGame", HMessage.Direction.TOCLIENT, true));
            }

            // flagListforTableView.sort(Person.CASE_INSENSITIVE_ORDER);    // Ordena los items de la tabla,segun algun parametro

            Map<String, List<Integer>> overview = new HashMap<>();
            for (Furniture entry : flagListforTableView) {
                overview.putIfAbsent(entry.getClassName(), new ArrayList<>());
                overview.get(entry.getClassName()).add(Integer.parseInt(entry.getFurniId()));
            }

            int flagIndex = 0;
            for(List<Integer> ids: overview.values()){
                tableView.getItems().add(new Furniture(flagIndex, ids.toString(), "", 0, 0, 0, 0,
                        "0.0", 1, "a"));
                flagIndex++;
            }
            flagIndex = 0;
            for(String className: overview.keySet()){
                tableView.getItems().set(flagIndex, new Furniture(flagIndex, tableView.getItems().get(flagIndex).getFurniId(), className,
                        0, 0, 0, 0, "0.0", 0, "a"));
                flagIndex++;
            }
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

        intercept(HMessage.Direction.TOSERVER, "OpenFlatConnection", hMessage -> {
            if(checkFastRoom.isSelected()){
                int roomID = hMessage.getPacket().readInteger();
                // I'm not sure if this option is useful or real
                sendToServer(new HPacket("OpenFlatConnection", HMessage.Direction.TOSERVER, roomID, "", -1));
            }
        });

        // MichelC1
        intercept(HMessage.Direction.TOCLIENT, "UserUpdate", hMessage -> {
            for (HEntityUpdate hEntityUpdate: HEntityUpdate.parse(hMessage.getPacket())){
                int CurrentIndex = hEntityUpdate.getIndex();  // HEntityUpdate class allows get UserIndex
                if(YourIndex == CurrentIndex){

                    int currentCoordX, currentCoordY;
                    String currentDirection = hEntityUpdate.getBodyFacing().toString();
                    try {
                        currentCoordX = hEntityUpdate.getMovingTo().getX(); // "Updates coords quickly"
                        currentCoordY = hEntityUpdate.getMovingTo().getY();
                    }
                    catch (NullPointerException nullPointerException) {
                        // Gets coords when you arrives the room. Its possible get coords by intercepting "RoomEntryTile" packet! but its not the same
                        currentCoordX = hEntityUpdate.getTile().getX();
                        currentCoordY = hEntityUpdate.getTile().getY();
                    }
                    // Necesita ser mejorado creo!
                    if(checkWalkFast.isSelected()){
                        System.out.println(currentDirection);
                        System.out.println("x: " + currentCoordX + " y: " + currentCoordY);
                        if(currentDirection.equals("North")){
                            walkToX = currentCoordX;    walkToY = currentCoordY - Integer.parseInt(textSteps.getText());
                            timerFixBug.start();    checkWalkFast.setSelected(false);
                        }
                        else if(currentDirection.equals("South")){
                            walkToX = currentCoordX;    walkToY = currentCoordY + Integer.parseInt(textSteps.getText());
                            timerFixBug.start();    checkWalkFast.setSelected(false);
                        }
                        else if(currentDirection.equals("East")){
                            walkToX = currentCoordX + Integer.parseInt(textSteps.getText());    walkToY = currentCoordY;
                            timerFixBug.start();    checkWalkFast.setSelected(false);
                        }
                        else if(currentDirection.equals("West")){
                            walkToX = currentCoordX - Integer.parseInt(textSteps.getText());    walkToY = currentCoordY;
                            timerFixBug.start();    checkWalkFast.setSelected(false);
                        }
                    }
                    if(currentCoordX == walkToX && currentCoordY == walkToY) { timerFixBug.stop(); }    // Detiene el timer cuando alcanza la posicion
                }
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

    // Interesting you can see the furni is walkable or no with canstandon
    private void getGameFurniData() throws Exception{
        String str = "https://www.habbo%s/gamedata/furnidata_json/1";
        JSONObject jsonObj = new JSONObject(IOUtils.toString(new URL(String.format(str, codeToDomainMap.get(host))).openStream(), StandardCharsets.UTF_8));
        JSONArray floorJson = jsonObj.getJSONObject("roomitemtypes").getJSONArray("furnitype");
        floorJson.forEach(o -> {
            JSONObject item = (JSONObject)o;
            nameToTypeidFloor.put(item.getString("classname"), item.getInt("id"));
            typeIdToNameFloor.put(item.getInt("id"), item.getString("classname"));
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
            /*
            String[] overview = tableView.getItems()
                    .stream()
                    .map(Person::getClassName)
                    .distinct()
                    .map(className -> {
                        String[] ids = tableView.getItems()
                                .stream()
                                .filter(item -> item.getClassName().equals(className))
                                .map(item -> Integer.toString(Integer.parseInt(item.getFurniId())))
                                .toArray(String[]::new);
                        return String.join(", ", ids) + "\t" + className;
                    })
                    .toArray(String[]::new);
            */