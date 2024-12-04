package application;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class FarmDashboardController {

    @FXML
    private TreeView<String> itemTree;

    @FXML
    private Canvas farmCanvas;
    
    @FXML
    private ToggleGroup droneActions;
    @FXML
    private RadioButton visitItemRadio;
    
    @FXML
    private RadioButton scanFarmRadio;
    

    private GraphicsContext gc;

    private final ItemContainer root = new ItemContainer("Root", 0, 0, 0, 0, 800, 600);
    private Item selectedItem;

    private final DoubleProperty droneX = new SimpleDoubleProperty(0);
    private final DoubleProperty droneY = new SimpleDoubleProperty(0);
    private boolean isDroneVisible = false; 
    private double commandCenterX = 0; 
    private double commandCenterY = 0;

    private Image droneImage;

    @FXML
    private void initialize() {
        gc = farmCanvas.getGraphicsContext2D();
        droneImage = new Image(getClass().getResourceAsStream("/images/drone.jpeg"));
        
        initializeTreeView();
        drawFarmLayout();

        droneX.addListener((obs, oldVal, newVal) -> drawFarmLayout());
        droneY.addListener((obs, oldVal, newVal) -> drawFarmLayout());
    }
    
    
    @FXML
    private void executeDroneAction() {
        RadioButton selectedAction = (RadioButton) droneActions.getSelectedToggle();
        if (selectedAction != null) {
            if ("Visit Item".equals(selectedAction.getText())) {
                if (selectedItem != null) {
                    animateDroneTo(selectedItem.getLocationX(), selectedItem.getLocationY());
                } else {
                    showError("No Selection", "Please select an item or container to visit.");
                }
            } else if ("Scan Farm".equals(selectedAction.getText())) {
                animateDroneScanFarm();
            }
        } else {
            showError("No Action Selected","");
        }
    }
    private void animateDroneTo(double targetX, double targetY) {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(droneX, droneX.get()), new KeyValue(droneY, droneY.get())),
            new KeyFrame(Duration.seconds(2), new KeyValue(droneX, targetX), new KeyValue(droneY, targetY))
        );
        timeline.play();
    }
   
    private void animateDroneScanFarm() {
       double farmWidth = farmCanvas.getWidth();
       double farmHeight = farmCanvas.getHeight();
       double droneWidth = 70;
       double stepHeight = 80;
 
       Timeline timeline = new Timeline();
       boolean directionRight = true;
 
       for (double y = 0; y < farmHeight; y += stepHeight) {
           double startX = directionRight ? 0 : farmWidth - droneWidth;
           double endX = directionRight ? farmWidth - droneWidth : 0;
 
           KeyFrame horizontalMove = new KeyFrame(
               Duration.seconds(y / stepHeight * 2),
               new KeyValue(droneX, endX),
               new KeyValue(droneY, y)
           );
           timeline.getKeyFrames().add(horizontalMove);
 
           directionRight = !directionRight;
       }
 
       KeyFrame returnToDronePos = new KeyFrame(
               Duration.seconds((farmHeight / stepHeight + 1) * 2),
               new KeyValue(droneX, droneX.get()),
               new KeyValue(droneY, droneY.get())
           );
       timeline.getKeyFrames().add(returnToDronePos);
 
       timeline.setCycleCount(1);
       timeline.play();
   }


    private void initializeTreeView() {
        TreeItem<String> rootNode = new TreeItem<>("Root");
        rootNode.setExpanded(true);
        itemTree.setRoot(rootNode);
        itemTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                selectedItem = findItemByName(root, newValue.getValue());
            }
        });
    }

    private Item findItemByName(ItemContainer container, String name) {
        if (container.getName().equals(name)) {
            return container;
        }
        for (Item item : container.getItems()) {
            if (item instanceof ItemContainer) {
                Item result = findItemByName((ItemContainer) item, name);
                if (result != null) return result;
            } else if (item.getName().equals(name)) {
                return item;
            }
        }
        return null;
    }

    private void drawFarmLayout() {
        gc.clearRect(0, 0, farmCanvas.getWidth(), farmCanvas.getHeight());
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, farmCanvas.getWidth(), farmCanvas.getHeight());
        drawItems(root);
        if (isDroneVisible && droneImage != null) {
            gc.drawImage(droneImage, droneX.get(), droneY.get(), 30, 30);
        }
    }

    private void drawItems(ItemContainer container) {
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(container.getLocationX(), container.getLocationY(), container.getLength(), container.getWidth());
        gc.setStroke(Color.BLACK);
        gc.strokeRect(container.getLocationX(), container.getLocationY(), container.getLength(), container.getWidth());
        gc.strokeText(container.getName(), container.getLocationX() + 5, container.getLocationY() + 15);

        for (Item item : container.getItems()) {
            if (item instanceof ItemContainer) {
                drawItems((ItemContainer) item);
            } else {
                gc.setFill(Color.RED);
                gc.fillRect(item.getLocationX(), item.getLocationY(), item.getLength(), item.getWidth());
                gc.setStroke(Color.BLACK);
                gc.strokeRect(item.getLocationX(), item.getLocationY(), item.getLength(), item.getWidth());
                gc.strokeText(item.getName(), item.getLocationX() + 5, item.getLocationY() + 15);
            }
        }
    }
    
    @FXML
    private void addItem() {
        showInputDialog("Add Item", "Enter item details: Name, Price, X, Y, Length, Width:", input -> {
            if (selectedItem instanceof ItemContainer) {
                try {
                    String[] details = input.split(",");
                    if (details.length == 6) {
                        String name = details[0].trim();
                        double price = Double.parseDouble(details[1].trim());
                        double x = Double.parseDouble(details[2].trim());
                        double y = Double.parseDouble(details[3].trim());
                        double length = Double.parseDouble(details[4].trim());
                        double width = Double.parseDouble(details[5].trim());

                        Item newItem = new Item(name, price, x, y, length, width);
                        ((ItemContainer) selectedItem).addItem(newItem);
                        refreshTreeView();
                        drawFarmLayout();

                        if ("Drone".equalsIgnoreCase(name) && "Command Center".equalsIgnoreCase(selectedItem.getName())) {
                            isDroneVisible = true; 
                            droneX.set(x); 
                            droneY.set(y); 
                        }
                    } else {
                        showError("Invalid Input", "Please enter all the required details.");
                    }
                } catch (NumberFormatException e) {
                    showError("Invalid Input", "Please ensure numeric fields are valid.");
                }
            } else {
                showError("Invalid Selection", "Please select a container to add an item.");
            }
        });
    }


    @FXML
    private void addItemContainer() {
        showInputDialog("Add Item Container", "Enter container details: Name, Price, X, Y, Length, Width:", input -> {
            if (selectedItem instanceof ItemContainer) {
                try {
                    String[] details = input.split(",");
                    if (details.length == 6) {
                        String name = details[0].trim();
                        double price = Double.parseDouble(details[1].trim());
                        double x = Double.parseDouble(details[2].trim());
                        double y = Double.parseDouble(details[3].trim());
                        double length = Double.parseDouble(details[4].trim());
                        double width = Double.parseDouble(details[5].trim());

                        ItemContainer newContainer = new ItemContainer(name, price, x, y, length, width);
                        ((ItemContainer) selectedItem).addItem(newContainer);
                        refreshTreeView();
                        drawFarmLayout();

                        if ("Command Center".equalsIgnoreCase(name)) {
                            commandCenterX = x; 
                            commandCenterY = y;
                        }
                    } else {
                        showError("Invalid Input", "Please enter all the required details.");
                    }
                } catch (NumberFormatException e) {
                    showError("Invalid Input", "Please ensure numeric fields are valid.");
                }
            } else {
                showError("Invalid Selection", "Please select a container to add an item container.");
            }
        });
    }
    private void refreshTreeView() {
        TreeItem<String> rootNode = new TreeItem<>("Root");
        buildTree(root, rootNode);
        rootNode.setExpanded(true);
        itemTree.setRoot(rootNode);
    }

    private void buildTree(ItemContainer container, TreeItem<String> parent) {
        for (Item item : container.getItems()) {
            TreeItem<String> child = new TreeItem<>(item.getName());
            parent.getChildren().add(child);
            if (item instanceof ItemContainer) {
                buildTree((ItemContainer) item, child);
            }
        }
    }
    
    private ItemContainer findParent(ItemContainer container, Item child) {
        if (container.getItems().contains(child)) {
            return container;
        }
        for (Item item : container.getItems()) {
            if (item instanceof ItemContainer) {
                ItemContainer result = findParent((ItemContainer) item, child);
                if (result != null) return result;
            }
        }
        return null;
    }

    private void showInputDialog(String title, String message, java.util.function.Consumer<String> onResult) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message);
        dialog.showAndWait().ifPresent(onResult);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @FXML
    private void renameItem() {
        showInputDialog("Rename Item", "Enter new name for the item:", name -> {
            if (selectedItem != null) {
                selectedItem.setName(name);
                refreshTreeView();
                drawFarmLayout();
            } else {
                showError("No Selection", "Please select an item to rename.");
            }
        });
    }
    
    @FXML
    private void renameItemContainer() {
        renameItem();
    }
    
    @FXML
    private void changeContainerLocation() {
        showInputDialog("Change Container Location", "Enter new X and Y coordinates", input -> {
            if (selectedItem instanceof ItemContainer) {
                try {
                    String[] coords = input.split(",");
                    if (coords.length == 2) {
                        selectedItem.setLocationX(Double.parseDouble(coords[0].trim()));
                        selectedItem.setLocationY(Double.parseDouble(coords[1].trim()));
                        drawFarmLayout();
                    } else {
                        showError("Invalid Input", "Please enter valid X and Y coordinates.");
                    }
                } catch (NumberFormatException e) {
                    showError("Invalid Input", "Coordinates must be numeric.");
                }
            } else {
                showError("Invalid Selection", "Please select a container to change its location.");
            }
        });
    }
    
    @FXML
    private void changeContainerPrice() {
        showInputDialog("Change Container Price", "Enter new price for the container:", price -> {
            if (selectedItem instanceof ItemContainer) {
                try {
                    selectedItem.setPrice(Double.parseDouble(price));
                    drawFarmLayout();
                } catch (NumberFormatException e) {
                    showError("Invalid Input", "Price must be numeric.");
                }
            } else {
                showError("Invalid Selection", "Please select a container to change its price.");
            }
        });
    }

    @FXML
    private void changeContainerDimensions() {
        showInputDialog("Change Container Dimensions", "Enter new length and width", input -> {
            if (selectedItem instanceof ItemContainer) {
                try {
                    String[] dims = input.split(",");
                    if (dims.length == 2) {
                        selectedItem.setLength(Double.parseDouble(dims[0].trim()));
                        selectedItem.setWidth(Double.parseDouble(dims[1].trim()));
                        drawFarmLayout();
                    } else {
                        showError("Invalid Input", "Please enter valid length and width.");
                    }
                } catch (NumberFormatException e) {
                    showError("Invalid Input", "Dimensions must be numeric.");
                }
            } else {
                showError("Invalid Selection", "Please select a container to change its dimensions.");
            }
        });
    }
    
   @FXML
    private void changeItemLocation() {
        showInputDialog("Change Item Location", "Enter new X and Y coordinates", input -> {
            if (selectedItem != null) {
                try {
                    String[] coords = input.split(",");
                    if (coords.length == 2) {
                        selectedItem.setLocationX(Double.parseDouble(coords[0].trim()));
                        selectedItem.setLocationY(Double.parseDouble(coords[1].trim()));
                        drawFarmLayout();
                    } else {
                        showError("Invalid Input", "Please enter valid X and Y coordinates.");
                    }
                } catch (NumberFormatException e) {
                    showError("Invalid Input", "Coordinates must be numeric.");
                }
            } else {
                showError("No Selection", "Please select an item to change its location.");
            }
        });
    }

@FXML
    private void changeItemPrice() {
        showInputDialog("Change Item Price", "Enter new price for the item:", price -> {
            if (selectedItem != null) {
                try {
                    selectedItem.setPrice(Double.parseDouble(price));
                } catch (NumberFormatException e) {
                    showError("Invalid Input", "Price must be numeric.");
                }
            } else {
                showError("No Selection", "Please select an item to change its price.");
            }
        });
    }
    
    @FXML
    private void changeItemDimensions() {
        showInputDialog("Change Item Dimensions", "Enter new length and width", input -> {
            if (selectedItem != null) {
                try {
                    String[] dims = input.split(",");
                    if (dims.length == 2) {
                        selectedItem.setLength(Double.parseDouble(dims[0].trim()));
                        selectedItem.setWidth(Double.parseDouble(dims[1].trim()));
                        drawFarmLayout();
                    } else {
                        showError("Invalid Input", "Please enter valid length and width.");
                    }
                } catch (NumberFormatException e) {
                    showError("Invalid Input", "Dimensions must be numeric.");
                }
            } else {
                showError("No Selection", "Please select an item to change its dimensions.");
            }
        });
    }

    @FXML
    private void deleteItem() {
        if (selectedItem != null) {
            ItemContainer parent = findParent(root, selectedItem);
            if (parent != null) {
                parent.getItems().remove(selectedItem);
                refreshTreeView();
                drawFarmLayout();
            } else {
                showError("Invalid Operation", "Cannot delete the root container.");
            }
        } else {
            showError("No Selection", "Please select an item to delete.");
        }
    }s 
    @FXML
    private void deleteItemContainer() {
        if (selectedItem instanceof ItemContainer) {
            ItemContainer parent = findParent(root, selectedItem);
            if (parent != null) {
                parent.getItems().remove(selectedItem);
                refreshTreeView();
                drawFarmLayout();
            } else {
                showError("Invalid Operation", "Cannot delete the root container.");
            }
        } else {
            showError("Invalid Selection", "Please select a container to delete.");
        }
    }
}
