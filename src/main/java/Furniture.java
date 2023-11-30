import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

public class Furniture
{
    // Esto es para ordenar los elementos de esta clase segun el parametro getClassName
    public static final Comparator<? super Furniture> CASE_INSENSITIVE_ORDER = Comparator.comparing(Furniture::getClassName);

    // Fields to show in the TableView
    private int indexItem;
    private AtomicBoolean active;  // Permite saber si el control esta chequeado o no
    private StringProperty furniId;
    private StringProperty className;
    private IntegerProperty count;

    /*          or s:
    {in:ObjectAdd}{i:1640379877}{i:233}{i:10}{i:3}{i:0}{s:"0.0"}{s:"1.5625"}{i:0}{i:0}{s:"1"}{i:-1}{i:1}{i:87447635}{s:"pablito"}
    1640379877 = furniId
    233 = furniTypeId
    10 = furniCoordX
    3 = furniCoordY
    0 = furniDirection
    "0.0" = furniElevation
    "1.5625" = I dont know
    0 = I dont know, but dosent matter
    0 = I dont know, but dosent matter
    "1" = furniState
    -1 = I dont know
    1 = I dont know, but dosent matter
    87447635 = ownerId
    "pablito" = ownerName
    */

    private int furniTypeId;
    private int furniCoordX;
    private int furniCoordY;
    private int furniDirection;
    private String furniElevation;
    private String furniState;
    private int ownerId;
    private String ownerName;

    // Constructors
    public Furniture(Integer indexItem, String furniId, String className, int count, int furniTypeId, int furniCoordX,
                     int furniCoordY, int furniDirection, String furniElevation, String furniState, int ownerId, String ownerName)
    {
        this.indexItem = indexItem;
        this.active = new AtomicBoolean(false); // Initiate unchecked!
        this.furniId = new SimpleStringProperty(furniId);
        this.className = new SimpleStringProperty(className);
        this.count = new SimpleIntegerProperty(count);
        this.furniTypeId = furniTypeId;
        this.furniCoordX = furniCoordX;
        this.furniCoordY = furniCoordY;
        this.furniDirection = furniDirection;
        this.furniElevation = furniElevation;
        this.furniState = furniState;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
    }

    // Properties

    public int getIndexItem() {
        return indexItem;
    }

    public void setIndexItem(int indexItem) {
        this.indexItem = indexItem;
    }

    public AtomicBoolean getActive() {
        return active;
    }

    public void setActive(AtomicBoolean active) {
        this.active = active;
    }

    public String getFurniId() {
        return furniId.get();
    }

    public StringProperty furniIdProperty() {
        return furniId;
    }

    public void setFurniId(String furniId) {
        this.furniId.set(furniId);
    }

    public String getClassName() {
        return className.get();
    }

    public StringProperty classNameProperty() {
        return className;
    }

    public void setClassName(String className) {
        this.className.set(className);
    }

    public int getCount() {
        return count.get();
    }

    public IntegerProperty countProperty() {
        return count;
    }

    public void setCount(int count) {
        this.count.set(count);
    }

    public int getFurniTypeId() {
        return furniTypeId;
    }

    public void setFurniTypeId(int furniTypeId) {
        this.furniTypeId = furniTypeId;
    }

    public int getFurniCoordX() {
        return furniCoordX;
    }

    public void setFurniCoordX(int furniCoordX) {
        this.furniCoordX = furniCoordX;
    }

    public int getFurniCoordY() {
        return furniCoordY;
    }

    public void setFurniCoordY(int furniCoordY) {
        this.furniCoordY = furniCoordY;
    }

    public int getFurniDirection() {
        return furniDirection;
    }

    public void setFurniDirection(int furniDirection) {
        this.furniDirection = furniDirection;
    }

    public String getFurniElevation() {
        return furniElevation;
    }

    public void setFurniElevation(String furniElevation) {
        this.furniElevation = furniElevation;
    }

    public String getFurniState() {
        return furniState;
    }

    public void setFurniState(String furniState) {
        this.furniState = furniState;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
}