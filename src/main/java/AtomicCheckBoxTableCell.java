import gearth.protocol.HPacket;
import javafx.scene.control.CheckBox;
import javafx.scene.control.cell.CheckBoxTableCell;
import java.util.concurrent.atomic.AtomicBoolean;

// I must admit that adding a Table with CheckBox was possible thanks to WiredSpast
public class AtomicCheckBoxTableCell extends CheckBoxTableCell<Furniture, AtomicBoolean> {
// Could be of help: https://jenkov.com/tutorials/javafx/tableview.html
    @Override   // This is very important!
    public void updateItem(AtomicBoolean item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null){
            setGraphic(null);   // sometimes item is null, so to avoid that!
        }
        else {
            CheckBox cb = new CheckBox();
            setGraphic(cb);
            cb.setSelected(item.get()); // generates bug
            cb.selectedProperty().addListener((observable, oldValue, newValue) -> {
                item.set(newValue);   // so this is for fix it!

                int length1 = getTableView().getItems().get(getIndex()).getFurniId().length();
                String withOutBrackets1 = getTableView().getItems().get(getIndex()).getFurniId().substring(1, length1-1);
                String[] ids = withOutBrackets1.split(", ");  // Could be important, regex for "[": \\[

                if(item.get()){ // checkBox is selected!
                    for(String id: ids){
                        // {in:ObjectRemove}{s:"438444797"}{b:false}{i:31657128}{i:0}
                        String strPacket = "{in:ObjectRemove}{s:\"" + id + "\"}{b:false}{i:" + GAntiLag.RUNNING_INSTANCE.yourUserId + "}{i:0}";
                        GAntiLag.RUNNING_INSTANCE.sendToClient(new HPacket(strPacket));
                    }
                }

                else if(!item.get()){
                    for(String id: ids){
                        GAntiLag.RUNNING_INSTANCE.flagListforTableView.forEach(flagList->{
                            if(id.equals(flagList.getFurniId())){
                                int typeId = flagList.getFurniTypeId();
                                int x = flagList.getFurniCoordX();
                                int y = flagList.getFurniCoordY();
                                int direction = flagList.getFurniDirection();
                                String elevation = flagList.getFurniElevation();
                                String state = flagList.getFurniState();
                                int ownerId = flagList.getOwnerId();
                                String ownerName = flagList.getOwnerName();
                                String strPacket = "{in:ObjectAdd}{s:"+id+"}{i:"+typeId+"}{i:"+x+"}{i:"+y+"}{i:"+direction+"}" +
                                        "{s:\""+elevation+"\"}{s:\"1.3\"}{i:0}{i:0}{s:\""+state+"\"}{i:-1}{i:0}{i:"+ownerId+"}{s:\""+ownerName+"\"}";
                                String intPacket = "{in:ObjectAdd}{i:"+id+"}{i:"+typeId+"}{i:"+x+"}{i:"+y+"}{i:"+direction+"}" +
                                        "{s:\""+elevation+"\"}{s:\"1.3\"}{i:0}{i:0}{s:\""+state+"\"}{i:-1}{i:0}{i:"+ownerId+"}{s:\""+ownerName+"\"}";
                                System.out.println(strPacket);
                                System.out.println(intPacket);
                                GAntiLag.RUNNING_INSTANCE.sendToClient(new HPacket(strPacket));
                                GAntiLag.RUNNING_INSTANCE.sendToClient(new HPacket(intPacket));
                            }
                        });
                    }
                }

            });
        }
    }
}