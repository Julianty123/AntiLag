import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.parsers.*;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.Timer;
import org.apache.commons.io.IOUtils; // Important library for openConnection (See json or something like that!)


@ExtensionInfo(
        Title = "GAntiLag",
        Description = "Allows you useful and funny options",
        Version = "1.3.4",
        Author = "Julianty"
)

public class GAntiLag extends ExtensionForm implements Initializable {
    public static GAntiLag RUNNING_INSTANCE; // For use this class in other, its useful (Defines it for the class, so in the instance this attribute doesnt exist).

    // private static volatile Instrumentation globalInstrumentation;

    public CheckBox checkHideSpeech, checkHideShoutOut, checkClickThrough, checkOneClickHide,
            checkHideDance, checkHideEffect, checkIgnoreWhispers, checkHideFloorItems, checkHideWallItems,
            checkHideBubbles, checkHideSign, checkDisableDouble, checkUsersToRemove, checkWalkFast;
    public TextField textSteps, txtFilter;
    public TableView<Furniture> tableView;
    public ListView<String> listNotePad;
    public Text txtPath;
    TreeMap<String, Integer> nameToTypeidFloor = new TreeMap<>();
    TreeMap<Integer, String> typeIdToNameFloor = new TreeMap<>();
    TreeMap<Integer,Integer> IdAndIndex = new TreeMap<>();
    LinkedList<Integer> hiddenFloorList = new LinkedList<>();
    LinkedList<Integer> hiddenWallList = new LinkedList<>();
    LinkedList<Furniture> flagListforTableView = new LinkedList<>();
    // LinkedList<Integer> wallListNotePad = new LinkedList<>(); coming soon!

    public int counterFloorItems;
    double xFrame, yFrame;


    public TableColumn<Furniture, AtomicBoolean> columnCheck;
    public TableColumn<Furniture, String> columnFurniId;
    public TableColumn<Furniture, String> columnClassName;
    public TableColumn<Furniture, Integer> columnCount;
    ObservableList <Furniture> dataObservableList = FXCollections.observableArrayList();

    FilteredList<Furniture> listaFiltrada = new FilteredList<>(dataObservableList, p -> true);    // Filtro para la lista observable
    //FilteredList<Furniture> listaFiltrada = new FilteredList<>(FXCollections.observableArrayList(dataObservableList), p -> true);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        columnCheck.setCellValueFactory(new PropertyValueFactory<>("active"));
        columnCheck.setCellFactory(tc -> new AtomicCheckBoxTableCell()); // Make the cell appear as a clickable checkbox
        columnFurniId.setCellValueFactory(new PropertyValueFactory<>("furniId"));
        columnClassName.setCellValueFactory(new PropertyValueFactory<>("className"));
        columnCount.setCellValueFactory(new PropertyValueFactory<>("count"));

        columnFurniId.setPrefWidth(125);
        columnClassName.setPrefWidth(150);

        tableView.setItems(listaFiltrada);   // Asigna la lista filtrada al TableView
        tableView.setEditable(true);
    }

    /*
    "doormat_plain*2", "doormat_plain*3", "doormat_love"
            ,"suncity_c19_floor", "xmas_c17_pavement", "xmas_c17_smallpavement", "xmas_c15_stone", "xmas11_woodfloor" */

    private static final TreeMap<String, String> mapHostToDomain = new TreeMap<>();
    static {
        mapHostToDomain.put("game-es.habbo.com", "https://www.habbo.es/gamedata/furnidata_json/1");
        mapHostToDomain.put("game-br.habbo.com", "https://www.habbo.com.br/gamedata/furnidata_json/1");
        mapHostToDomain.put("game-tr.habbo.com", "https://www.habbo.com.tr/gamedata/furnidata_json/1");
        mapHostToDomain.put("game-us.habbo.com", "https://www.habbo.com/gamedata/furnidata_json/1");
        mapHostToDomain.put("game-de.habbo.com", "https://www.habbo.de/gamedata/furnidata_json/1");
        mapHostToDomain.put("game-fi.habbo.com", "https://www.habbo.fi/gamedata/furnidata_json/1");
        mapHostToDomain.put("game-fr.habbo.com", "https://www.habbo.fr/gamedata/furnidata_json/1");
        mapHostToDomain.put("game-it.habbo.com", "https://www.habbo.it/gamedata/furnidata_json/1");
        mapHostToDomain.put("game-nl.habbo.com", "https://www.habbo.nl/gamedata/furnidata_json/1");
        mapHostToDomain.put("game-s2.habbo.com", "https://sandbox.habbo.com/gamedata/furnidata_json/1");
    }

    private static final TreeMap<String, Integer> directionToCode = new TreeMap<>();
    static {
        directionToCode.put("North", 0);
        directionToCode.put("East", 2);
        directionToCode.put("South", 4);
        directionToCode.put("West", 6);
    }

    public int walkToX;
    public int walkToY;
    public int yourUserId;
    public String YourUserName;
    public int yourIndex = -1;

    // To fix the bug... bruh :(
    public int countTimerFixBug;
    Timer timer1 = new Timer(1, e -> {
        for (Integer furniId : hiddenFloorList) {
            sendToClient(new HPacket("ObjectRemove", HMessage.Direction.TOCLIENT, String.valueOf(furniId), false, yourUserId, 0));
        }
        for (Integer furniId : hiddenWallList) {
            sendToClient(new HPacket("ItemRemove", HMessage.Direction.TOCLIENT, String.valueOf(furniId), yourUserId));
        }
        countTimerFixBug++;
        System.out.println("Timer Fix Bug: " + countTimerFixBug);
        if(countTimerFixBug >= 100){
            stopTimer();    countTimerFixBug = 0;
        }
    });

    // Necessary to solve a "bug", i think you have to click at the correct time, so with this you will give many clicks..
    Timer timerFixBug = new Timer(1, e -> sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, walkToX, walkToY)));

    // This method is called when the extension is opened
    @Override
    protected void onShow() {
        System.out.println("GAntiLag Started!");
        try {
            // C:\Users\Administrador\Downloads\G-Earth\Extensions\GAntiLag\config.txt
            readFile();
        } catch (FileNotFoundException e) {
            System.out.println("File not found!: " + e.getMessage());
        }
    }

    // This method is called when the extension is closed
    @Override
    protected void onHide() {
        sendToClient(new HPacket("YouArePlayingGame", HMessage.Direction.TOCLIENT, false));
        checkClickThrough.setSelected(false);
        checkOneClickHide.setSelected(false);  checkOneClickHide.setDisable(false);   checkDisableDouble.setSelected(false);
        checkUsersToRemove.setSelected(false);  IdAndIndex.clear(); checkHideFloorItems.setSelected(false);
        checkHideWallItems.setSelected(false);  checkHideBubbles.setSelected(false);    checkHideSpeech.setSelected(false);
        checkHideShoutOut.setSelected(false);   checkHideDance.setSelected(false);  checkHideSign.setSelected(false);
        checkIgnoreWhispers.setSelected(false);
        sendToServer(new HPacket("{out:GetHeightMap}")); yourIndex = -1;
    }

    @Override
    protected void initExtension() {

        System.out.println("    /$$$$$           /$$ /$$                       /$$              ");
        System.out.println("   |__  $$          | $$|__/                      | $$              ");
        System.out.println("      | $$ /$$   /$$| $$ /$$  /$$$$$$  / $$$$$$$  /$$$$$$  /$$   /$$");
        System.out.println("      | $$| $$  | $$| $$| $$ |____  $$| $$__   $$ |_$$_/  | $$  | $$");
        System.out.println(" /$$  | $$| $$  | $$| $$| $$  /$$$$$$$| $$  \\  $$ | $$    | $$  | $$");
        System.out.println("| $$  | $$| $$  | $$| $$| $$ /$$__  $$| $$  |  $$ | $$ /$$| $$  | $$");
        System.out.println("| $$$$$$/ |  $$$$$$/| $$| $$|  $$$$$$$| $$  |  $$ |  $$$$/|  $$$$$$$");
        System.out.println(" \\______/  \\______/ |__/|__/ \\_______/|__/  |__/   \\___/   \\____  $$");
        System.out.println("                                                           /$$  | $$");
        System.out.println("                                                          |  $$$$$$/");
        System.out.println("                                                           \\______/ ");

        // Detects when the user close the window (not the best implementation)
        // primaryStage.setOnCloseRequest(e -> {});

        RUNNING_INSTANCE = this;

        try { createFolder(); }
        catch (IOException e) { throw new RuntimeException(e); }

        onConnect((host, port, APIVersion, versionClient, client) -> gameFurnitureData(host));

        // Cuando el usuario empieza a digitar...
        txtFilter.textProperty().addListener((observable, oldValue, newValue) -> {
            // Se filtra lo que se digita...
            listaFiltrada.setPredicate(furniture -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();
                if (furniture.getClassName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (String.valueOf(furniture.getFurniId()).contains(lowerCaseFilter)) {
                    return true;
                } else if (furniture.getOwnerName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }

                return false;
            });
            tableView.refresh();    // Actualiza la tabla, para mostrar lo digitado...
        });

        // Happens when you check or uncheck the checkBox control!
        checkHideFloorItems.setOnAction(e-> {
            sendToServer(new HPacket("{out:GetHeightMap}"));
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


        checkHideWallItems.setOnAction(e -> sendToServer(new HPacket("{out:GetHeightMap}")));

        checkUsersToRemove.setOnAction(e -> {
            // Coming soon i need to fix this
            if(!checkUsersToRemove.isSelected()) sendToServer(new HPacket("{out:GetHeightMap}"));
        });

        // Intercepts the client's response and does something ...
        intercept(HMessage.Direction.TOCLIENT, "UserObject", hMessage -> {
            // Be careful, the data must be obtained in the order of the packet
            yourUserId = hMessage.getPacket().readInteger();
            YourUserName = hMessage.getPacket().readString();
        });

        // Response of packet AvatarExpression
        intercept(HMessage.Direction.TOCLIENT, "Expression", hMessage -> {
            // First integer is index in room, second is animation id, i think
            // this could avoid any bug
            if(primaryStage.isShowing() && yourIndex == -1) yourIndex = hMessage.getPacket().readInteger();
        });

        // Cuando clickea a un usuario se ejecuta esto
        intercept(HMessage.Direction.TOSERVER, "GetSelectedBadges", hMessage -> {
            if(checkUsersToRemove.isSelected()){ // Elimina el usuario de la sala en el cliente
                sendToClient(new HPacket("UserRemove", HMessage.Direction.TOCLIENT,
                        String.valueOf(IdAndIndex.get(hMessage.getPacket().readInteger()))));
            }
        });

        intercept(HMessage.Direction.TOCLIENT, "Users", this::interceptUsers);

        // Intercepts double click to floor item
        intercept(HMessage.Direction.TOSERVER, "UseFurniture", hMessage -> {
            if(checkDisableDouble.isSelected()) hMessage.setBlocked(true);  // Blocks double click
        });

        // Intercepts one click to furniture
        intercept(HMessage.Direction.TOSERVER, "ClickFurni", hMessage -> {
            if(checkOneClickHide.isSelected()){
                int furnitureId = hMessage.getPacket().readInteger();   hiddenFloorList.add(furnitureId);
                sendToClient(new HPacket("ObjectRemove", HMessage.Direction.TOCLIENT,
                        String.valueOf(furnitureId), false, yourUserId, 0)); // Hide Floor Item
                int count = hiddenFloorList.size() + hiddenWallList.size();
                Platform.runLater(()-> {
                    listNotePad.getItems().add("FloorItemId: " + furnitureId);
                    checkOneClickHide.setText("One click to hide (" + count + ")");
                });
            }
        });

        // Intercepts double click to wall item
        intercept(HMessage.Direction.TOSERVER, "UseWallItem", hMessage -> {
            if(checkOneClickHide.isSelected()){
                int furniId = hMessage.getPacket().readInteger();   hiddenWallList.add(furniId);
                sendToClient(new HPacket("ItemRemove", HMessage.Direction.TOCLIENT,
                        String.valueOf(furniId), yourUserId)); // Hide Wall Item
                int count = hiddenFloorList.size() + hiddenWallList.size();
                Platform.runLater(()-> {
                    listNotePad.getItems().add("WallItemId: " + furniId);
                    checkOneClickHide.setText("One click to hide (" + count + ")");
                });
            }
        });
        intercept(HMessage.Direction.TOSERVER, "GetItemData", hMessage -> { // When you give click in a stickie (notes)
            if(checkOneClickHide.isSelected()){
                int furniId = hMessage.getPacket().readInteger();   hiddenWallList.add(furniId);
                sendToClient(new HPacket("ItemRemove", HMessage.Direction.TOCLIENT,
                        String.valueOf(furniId), yourUserId)); // Hide Wall Item
                int count = hiddenFloorList.size() + hiddenWallList.size();
                Platform.runLater(()-> checkOneClickHide.setText("One click to hide (" + count + ")"));
            }
        });

        // Intercepts the floor items in the room
        intercept(HMessage.Direction.TOCLIENT, "Objects", this::interceptObjects);

        // Intercept all wall items in the room
        intercept(HMessage.Direction.TOCLIENT, "Items", hMessage -> {
            try{
                for (HWallItem hWallItem: HWallItem.parse(hMessage.getPacket())){
                    if(checkHideWallItems.isSelected()){
                        sendToClient(new HPacket("ItemRemove", HMessage.Direction.TOCLIENT,
                                String.valueOf(hWallItem.getId()), yourUserId)); // "Hide" wall items
                        hMessage.setBlocked(true);  // Fix the bug
                    }
                    if(hiddenWallList.contains(hWallItem.getId()) && primaryStage.isShowing()){
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
            if(checkHideShoutOut.isSelected()) hMessage.setBlocked(true);
        });

        intercept(HMessage.Direction.TOCLIENT, "AvatarEffect", hMessage -> {
            if(checkHideEffect.isSelected()) hMessage.setBlocked(true);
        });

        // Ignora la gente que te susurra (Me ignoro pero los demas NO a mi)
        intercept(HMessage.Direction.TOCLIENT, "Whisper", hMessage -> {
            if(checkIgnoreWhispers.isSelected()) hMessage.setBlocked(true);
        });

        // Esconde las burbujas de los usuarios (si no estan digitando en ese momento)
        intercept(HMessage.Direction.TOCLIENT, "UserTyping", hMessage -> {
            if(checkHideBubbles.isSelected()) hMessage.setBlocked(true);
        });

        // Esconde TU burbuja (Lado del servidor, la gente no vera cuando susurres!)
        intercept(HMessage.Direction.TOSERVER, "StartTyping", hMessage -> {
            if(checkHideBubbles.isSelected()) hMessage.setBlocked(true);
        });

        // Bloquea el baile (si no esta bailando en ese momento)
        intercept(HMessage.Direction.TOCLIENT, "Dance", hMessage -> {
            if(checkHideDance.isSelected()) hMessage.setBlocked(true);
        });

        intercept(HMessage.Direction.TOCLIENT, "UserUpdate", this::interceptUserUpdate);

        // Thanks to Achantur for the special character, you can see it here: https://github.com/achantur/AntiBobba
        intercept(HMessage.Direction.TOSERVER, "Chat", hMessage -> {
            String message = hMessage.getPacket().readString();
            int color = hMessage.getPacket().readInteger();
            int index = hMessage.getPacket().readInteger();
        });
    }

    public void interceptUsers(HMessage hMessage){
        try {
            HPacket hPacket = hMessage.getPacket();
            HEntity[] roomUsersList = HEntity.parse(hPacket);
            for (HEntity hEntity: roomUsersList){
                if(hEntity.getName().equals(YourUserName)){    // In another room, the index changes
                    yourIndex = hEntity.getIndex();
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
    }

    public void interceptObjects(HMessage hMessage){
        dataObservableList.clear(); // Elimina los datos de la tabla (Evita UnsupportedOperationException)
        flagListforTableView.clear();

        for (HFloorItem hFloorItem: HFloorItem.parse(hMessage.getPacket())){
            try{
                // System.out.println(hFloorItem.getStuff()[0]);
                    /*stateFurni (Obtiene el primer elemento del array, creo)
                    Necesito encontrar la forma de detectar si el furni es background (no tiene direction, creo)
                    quizas usando un if y un else if con hFloorItem.getFacing() podria ser util! */

                List<?> list = Arrays.asList(hFloorItem.getStuff()); // To parse!
                System.out.println(list.get(0) + " " + list);
                if(list.get(0).equals("")){
                    flagListforTableView.add(new Furniture(
                            counterFloorItems,
                            String.valueOf(hFloorItem.getId()),
                            typeIdToNameFloor.get(hFloorItem.getTypeId()),
                            0,
                            hFloorItem.getTypeId(),
                            hFloorItem.getTile().getX(),
                            hFloorItem.getTile().getY(),
                            directionToCode.get(hFloorItem.getFacing().toString()),
                            String.valueOf(hFloorItem.getTile().getZ()),
                            "0",
                            hFloorItem.getOwnerId(),
                            hFloorItem.getOwnerName()));
                }
                else{
                    int size = list.size();
                    flagListforTableView.add(new Furniture(
                            counterFloorItems,
                            String.valueOf(hFloorItem.getId()),
                            typeIdToNameFloor.get(hFloorItem.getTypeId()),
                            0,
                            hFloorItem.getTypeId(),
                            hFloorItem.getTile().getX(),
                            hFloorItem.getTile().getY(),
                            directionToCode.get(hFloorItem.getFacing().toString()),
                            String.valueOf(hFloorItem.getTile().getZ()),
                            (String) list.get(0),hFloorItem.getOwnerId(),
                            hFloorItem.getOwnerName()));
                }

                counterFloorItems++;


                if(checkHideFloorItems.isSelected()){
                    // Many packets can be send to client, dosent matter the delay!
                    sendToClient(new HPacket("ObjectRemove", HMessage.Direction.TOCLIENT, String.valueOf(hFloorItem.getId()), false, yourUserId, 0));
                    hMessage.setBlocked(true); // Solve bug wtf
                }
                if(hiddenFloorList.contains(hFloorItem.getId()) && primaryStage.isShowing()){
                    timer1.start();
                }
            }catch (Exception e){
                // Excepcion porque este tipo de furnis no tiene direccion (hFloorItem.getFacing())
                System.out.println("Exception in Objects: " + e.getMessage());
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
            System.out.println(ids.toString());
            dataObservableList.add(new Furniture(flagIndex, ids.toString(), "", ids.size(), 0,0, 0, 0,
                    "0.0", "0", 1, "a"));
            flagIndex++;
        }

        flagIndex = 0;
        for(String className: overview.keySet()){
            dataObservableList.set(flagIndex, new Furniture(flagIndex, dataObservableList.get(flagIndex).getFurniId(), className,
                    dataObservableList.get(flagIndex).getCount(), 0,0, 0, 0, "0.0", "0", 0, "a"));
            flagIndex++;
        }
    }

    public void interceptUserUpdate(HMessage hMessage){
        for (HEntityUpdate hEntityUpdate: HEntityUpdate.parse(hMessage.getPacket())){
            int CurrentIndex = hEntityUpdate.getIndex();  // HEntityUpdate class allows get UserIndex
            if(yourIndex == CurrentIndex){
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

            if(checkHideSign.isSelected() && hEntityUpdate.getSign() != null){
                hMessage.setBlocked(true);
            }
        }
    }

    public void sendPackets(){
        // The packet is sent to the server and a response is obtained from the CLIENT !!
        sendToServer(new HPacket("{out:InfoRetrieve}"));
        // When its sent, get UserIndex without restart room
        sendToServer(new HPacket("{out:AvatarExpression}{i:0}"));
        // When its sent, get wallitems, flooritems and other things without restart room
        sendToServer(new HPacket("GetHeightMap", HMessage.Direction.TOSERVER));
    }

    private void gameFurnitureData(String host){
        new Thread(()->{
            try{
                System.out.println("Getting GameData...");
                String url = mapHostToDomain.get(host); // Throws exception if you are connecting to holos
                URLConnection connection = (new URL(url)).openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
                connection.connect();

                JSONObject jsonObj = new JSONObject(IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8));

                JSONArray floorJson = jsonObj.getJSONObject("roomitemtypes").getJSONArray("furnitype");
                floorJson.forEach(o -> {
                    JSONObject item = (JSONObject)o;
                    nameToTypeidFloor.put(item.getString("classname"), item.getInt("id"));
                    typeIdToNameFloor.put(item.getInt("id"), item.getString("classname"));
                });
                System.out.println("Gamedata Retrieved!");

                sendPackets(); // Once the API is loaded, the packets are sent to get the room data
            }catch (IOException e){
                System.out.println("MalformedURLException: " + e.getMessage());
            }
        }).start();
    }

    public void stopTimer(){
        timer1.stop();
    }

    public void Click_through(){
        if(checkClickThrough.isSelected()){
            sendToClient(new HPacket("YouArePlayingGame", HMessage.Direction.TOCLIENT, true));   // Habilita "Click Through"
        }
        else {
            sendToClient(new HPacket("YouArePlayingGame", HMessage.Direction.TOCLIENT, false));
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
            sendToServer(new HPacket("{out:GetHeightMap}"));
        }
        hiddenFloorList.clear(); hiddenWallList.clear();    listNotePad.getItems().clear();
        checkOneClickHide.setText("One click to hide (0)");
    }

    public void saveConfig() throws RuntimeException, IOException {
        // Configuracion de la ventana para guardar
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Configuration");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Text File", "*.txt"));
        File file = fileChooser.showSaveDialog(primaryStage);

        try {
            FileWriter fileWriter = new FileWriter(file);
            for(String line: listNotePad.getItems()){
                fileWriter.write(line);
                fileWriter.write("\n");
            }
            fileWriter.flush();
            fileWriter.close();
        }catch (RuntimeException ignored){} // Evita una excepcion cuando se cierra la ventana sin guardar
    }

    public void loadConfig() throws IOException, RuntimeException{
        // Configuracion de la ventana para cargar
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Configuration");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Text File", "*.txt"));
        File file = fileChooser.showOpenDialog(primaryStage);
        System.out.println("File: " + file);

        try{
            BufferedReader bReader = new BufferedReader(new FileReader(file));   // lee el archivo .txt seleccionado
            readData(bReader);
        }catch (RuntimeException ignored){}
    }

    public void readData(BufferedReader bufferedReader) {
        try{
            AtomicInteger counterLine = new AtomicInteger();
            bufferedReader.lines().forEach(line->{
                String[] split = line.split(" ");
                String tagName = split[0];  // FloorItemId: or WallItemId:
                String id = split[1];
                listNotePad.getItems().add(tagName + " " + id);
                if(tagName.equals("FloorItemId:")){
                    hiddenFloorList.add(Integer.parseInt(id));
                }
                else if(tagName.equals("WallItemId:")){
                    hiddenWallList.add(Integer.parseInt(id));
                }
                counterLine.getAndIncrement();
            });
            timer1.start();
            Platform.runLater(()-> checkOneClickHide.setText("One click to hide (" + counterLine + ")"));
        }catch (RuntimeException runtimeException){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(primaryStage);
            alert.setHeaderText("We are sorry :(");
            alert.setOnShowing(event -> alert.setContentText("Error loading the configuration, the format could be wrong."));
            alert.show();
        }
    }

    public void createFolder() throws IOException {
        try {
            // String path = System.getProperty("user.home") + File.separator;

            // https://stackoverflow.com/questions/320542/how-to-get-the-path-of-a-running-jar-file
            String pathJar = GAntiLag.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String decodedPathJar = URLDecoder.decode(pathJar, "UTF-8");
            System.out.println("decodedPathJar: " + decodedPathJar);

            // remove jar name
            // String path = decodedPathJar.substring(0, decodedPathJar.lastIndexOf(File.separator) + 1);
            System.out.println("pathSeparator: " + File.pathSeparator);
            String matriz[] = decodedPathJar.split(File.separatorChar + "");
            System.out.println("matriz: " + matriz[0]);
            // char ch: a.toCharArray()
            for(int i = 0; i < matriz.length; i++){
                System.out.println("matriz: " + matriz[i]);
            }

            File directory = new File("Extensions/" + this.getClass().getName());  // ??
            if (!directory.exists()) {
                if (directory.mkdirs()) {
                    FileWriter fileWriter = new FileWriter(directory + "/config.txt");
                    fileWriter.flush(); // Vacía el contenido búfer del destino
                    fileWriter.close(); // Vacía el contenido del destino y cierra la secuencia
                }
                else System.out.println("Error when creating the directory");
            }
        } catch (Exception e) {
            System.out.println("Exception to create the directory: " + e.getMessage());
        }
    }

    public void readFile() throws FileNotFoundException {
        BufferedReader bReader = new BufferedReader(new FileReader("config.txt"));   // lee el archivo .txt seleccionado
        readData(bReader);
    }

    public void onMouseDragged(MouseEvent mouseEvent) {
        Stage stage = (Stage) ((Node)mouseEvent.getSource()).getScene().getWindow();
        stage.setX(mouseEvent.getScreenX() - xFrame);
        stage.setY(mouseEvent.getScreenY() - yFrame);
    }

    public void onMousePressed(MouseEvent mouseEvent) {
        xFrame = mouseEvent.getSceneX();
        yFrame = mouseEvent.getSceneY();
    }

    public void onMinimize(ActionEvent event) {
        Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        stage.setIconified(true);
    }

    public void onClose() {
        primaryStage.close();
    }
}