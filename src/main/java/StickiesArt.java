import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.parsers.HWallItem;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

@ExtensionInfo(
        Title = "StickiesArt",
        Description = "Art with stickies created with love",
        Version = "1.7",
        Author = "DanielaNaomi"
)

public class StickiesArt extends ExtensionForm {
    public TextField text;
    public TextField basew1;
    public TextField basew2;
    public TextField basel1;
    public TextField basel2;
    public TextField increment;
    public Button buttongoart;
    public LinkedList<Integer> items = new LinkedList<>();
    public LinkedList<Integer> usedItems = new LinkedList<>();
    public Set<Integer> usedItemsSet = new HashSet<>();
    public Button buttondisorganizeall;
    public CheckBox always_on_top_cbx;
    public int uniqueId = -1;
    private static final HashMap<String, Integer> host_postit = new HashMap<>();

    static {
        host_postit.put("game-es.habbo.com", 4238);
        host_postit.put("game-br.habbo.com", 4214);
        host_postit.put("game-tr.habbo.com", 4216);
        host_postit.put("game-us.habbo.com", 4221);
        host_postit.put("game-de.habbo.com", 4236);
        host_postit.put("game-fi.habbo.com", 4220);
        host_postit.put("game-fr.habbo.com", 4213);
        host_postit.put("game-it.habbo.com", 4236);
        host_postit.put("game-nl.habbo.com", 4219);
        host_postit.put("game-s2.habbo.com", 4236);
    }

    public CheckBox draw_cbx;
    public Set<Integer> selectedCells = new HashSet<>();
    private final Map<String, Color> colors = new HashMap<>();
    public Button buttonopenblackboard;
    public CheckBox blueletters_cbx;
    public CheckBox pinkletters_cbx;
    public CheckBox greenletters_cbx;
    public CheckBox yellowletters_cbx;
    private Stage blackboardStage;
    private boolean isBlackboardOpen = false;
    private Color selectedColor = Color.web("FFFF33");
    private final Map<Integer, Color> cellColorMap = new HashMap<>();

    @Override
    protected void initExtension() {
        onConnect((host, port, APIVersion, versionClient, client) -> {
            if (host_postit.containsKey(host))
                uniqueId = host_postit.get(host);
        });
        sendToServer(new HPacket("GetHeightMap", HMessage.Direction.TOSERVER));
        intercept(HMessage.Direction.TOCLIENT, "Items", this::Items);
        intercept(HMessage.Direction.TOSERVER, "OpenFlatConnection", this::OpenFlatConnection);

        setTextFieldNumeric(basew1);
        setTextFieldNumeric(basew2);
        setTextFieldNumeric(basel1);
        setTextFieldNumeric(basel2);
        setTextFieldNumeric(increment);

        setTextFieldDefault(basew1, "0");
        setTextFieldDefault(basew2, "0");
        setTextFieldDefault(basel1, "0");
        setTextFieldDefault(basel2, "0");
        setTextFieldDefault(increment, "2");
    }

    @Override
    protected void onHide() {
        if (blackboardStage != null) {
            blackboardStage.close();
            isBlackboardOpen = false;
            selectedColor = Color.web("FFFF33");
            selectedCells.clear();
        }
    }

    public void toggleAlwaysOnTop() {
        primaryStage.setAlwaysOnTop(always_on_top_cbx.isSelected());
    }

    private void setTextFieldNumeric(TextField textField) {
        textField.addEventFilter(KeyEvent.KEY_TYPED, keyEvent -> {
            String character = keyEvent.getCharacter();
            if (!character.matches("[0-9-]") || (character.equals("-") && textField.getText().contains("-"))) {
                keyEvent.consume();
            }
        });
    }

    private void setTextFieldDefault(TextField textField, String defaultValue) {
        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && textField.getText().isEmpty()) {
                textField.setText(defaultValue);
            }
        });
    }

    private void OpenFlatConnection(HMessage hMessage) {
        items.clear();
        usedItems.clear();
        usedItemsSet.clear();
    }

    private void Items(HMessage hMessage) {
        HWallItem[] hWallItems = HWallItem.parse(hMessage.getPacket());
        for (HWallItem hWallItem : hWallItems) {
            int noteid = hWallItem.getId();
            int typeid = hWallItem.getTypeId();
            if (typeid == uniqueId) {
                items.add(noteid);
            }
        }
    }

    public void handlegoart() {
        String inputText = text.getText();

        try {
            int baseW1 = Integer.parseInt(basew1.getText());
            int baseW2 = Integer.parseInt(basew2.getText());
            int baseL1 = Integer.parseInt(basel1.getText());
            int baseL2 = Integer.parseInt(basel2.getText());
            int incrementValue = Integer.parseInt(increment.getText());

            new Thread(() -> {
                if (draw_cbx.isSelected()) {
                    handleDrawGrid(baseW1, baseW2, baseL1, baseL2);
                } else {
                    handlegoart(inputText, baseW1, baseW2, baseL1, baseL2, incrementValue);
                }
            }).start();
        } catch (NumberFormatException e) {
            System.err.println("Error parsing base dimensions or increment: " + e.getMessage());
        }
    }

    private void handleDrawGrid(int baseW1, int baseW2, int baseL1, int baseL2) {
        disableGridPane();

        int offsetW2 = 0;
        int itemIndex = 0;

        for (int index : selectedCells) {
            while (itemIndex < items.size() && usedItemsSet.contains(items.get(itemIndex))) {
                itemIndex++;
            }

            if (itemIndex >= items.size()) break;

            String position = DrawMapper.getCoordinate(index);
            String[] posParts = position.split(" ");
            if (posParts.length == 3) {
                try {
                    String wPart = posParts[0].split("=")[1];
                    int originalW1 = Integer.parseInt(wPart.split(",")[0]);
                    int originalW2 = Integer.parseInt(wPart.split(",")[1]);
                    int newW1 = originalW1 + baseW1;
                    int newW2 = originalW2 + baseW2 + offsetW2;

                    String lOrR = posParts[2];
                    String[] coordinates = posParts[1].split("=")[1].split(",");
                    int originalX = Integer.parseInt(coordinates[0]);
                    int originalY = Integer.parseInt(coordinates[1]);
                    int newX = originalX - baseL1;
                    int newY = originalY - baseL2;

                    HPacket movePacket = new HPacket("MoveWallItem", HMessage.Direction.TOSERVER);
                    movePacket.appendInt(items.get(itemIndex));
                    movePacket.appendString(":w=" + newW1 + "," + newW2 + " l=" + newX + "," + newY + " " + lOrR);
                    sendToServer(movePacket);

                    try {
                        Thread.sleep(125);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Color cellColor = getCellColor(index);
                    String colorCode = String.format("%02X%02X%02X",
                            (int) (cellColor.getRed() * 255),
                            (int) (cellColor.getGreen() * 255),
                            (int) (cellColor.getBlue() * 255));

                    HPacket colorPacket = new HPacket("SetItemData", HMessage.Direction.TOSERVER);
                    colorPacket.appendInt(items.get(itemIndex));
                    colorPacket.appendString(colorCode);
                    colorPacket.appendString("");
                    sendToServer(colorPacket);

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    usedItems.add(items.get(itemIndex));
                    usedItemsSet.add(items.get(itemIndex));
                    itemIndex++;
                } catch (NumberFormatException ex) {
                    System.err.println("Error parsing positions: " + ex.getMessage());
                }
            }
        }
        enableGridPane();
    }

    private void disableGridPane() {
        if (blackboardStage != null) {
            blackboardStage.getScene().getRoot().setDisable(true);
        }
    }

    private void enableGridPane() {
        if (blackboardStage != null) {
            blackboardStage.getScene().getRoot().setDisable(false);
        }
    }

    public void handlegoart(String text, int baseW1, int baseW2, int baseL1, int baseL2, int incrementValue) {
        int offsetW2 = 0;
        int itemIndex = 0;

        List<String> selectedColors = new ArrayList<>();
        if (blueletters_cbx.isSelected()) selectedColors.add("9CCEFF");
        if (pinkletters_cbx.isSelected()) selectedColors.add("FF9CFF");
        if (greenletters_cbx.isSelected()) selectedColors.add("9CFF9C");
        if (yellowletters_cbx.isSelected()) selectedColors.add("FFFF33");

        int colorIndex = 0;

        for (char letter : text.toCharArray()) {
            String[] positions = LetterMapper.getLetterMapping(letter);
            for (String position : positions) {
                while (itemIndex < items.size() && usedItemsSet.contains(items.get(itemIndex))) {
                    itemIndex++;
                }

                if (itemIndex >= items.size()) break;

                String[] posParts = position.split(" ");
                if (posParts.length == 3) {
                    try {
                        String wPart = posParts[0].split("=")[1];
                        int originalW1 = Integer.parseInt(wPart.split(",")[0]);
                        int originalW2 = Integer.parseInt(wPart.split(",")[1]);
                        int newW1 = originalW1 + baseW1;
                        int newW2 = originalW2 + baseW2 + offsetW2;

                        String lOrR = posParts[2];
                        String[] coordinates = posParts[1].split("=")[1].split(",");
                        int originalX = Integer.parseInt(coordinates[0]);
                        int originalY = Integer.parseInt(coordinates[1]);
                        int newX = originalX - baseL1;
                        int newY = originalY - baseL2;

                        HPacket movePacket = new HPacket("MoveWallItem", HMessage.Direction.TOSERVER);
                        movePacket.appendInt(items.get(itemIndex));
                        movePacket.appendString(":w=" + newW1 + "," + newW2 + " l=" + newX + "," + newY + " " + lOrR);
                        sendToServer(movePacket);
                        try {
                            Thread.sleep(125);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (!selectedColors.isEmpty()) {
                            HPacket colorPacket = new HPacket("SetItemData", HMessage.Direction.TOSERVER);
                            colorPacket.appendInt(items.get(itemIndex));
                            colorPacket.appendString(selectedColors.get(colorIndex));
                            colorPacket.appendString("");
                            sendToServer(colorPacket);

                            colorIndex = (colorIndex + 1) % selectedColors.size();

                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        usedItems.add(items.get(itemIndex));
                        usedItemsSet.add(items.get(itemIndex));
                        itemIndex++;
                    } catch (NumberFormatException ex) {
                        System.err.println("Error parsing positions: " + ex.getMessage());
                    }
                }
            }
            offsetW2 -= incrementValue;
        }
    }

    public void handledisorganizeall() {
        LinkedList<Integer> itemsToDisorganize = usedItems.isEmpty() ? items : new LinkedList<>(usedItems);

        new Thread(() -> {
            Random random = new Random();

            for (int itemId : itemsToDisorganize) {
                int newW1 = random.nextInt(10);
                int newW2 = random.nextInt(32);
                int newX = random.nextInt(10);
                int newY = random.nextInt(32);

                HPacket movePacket = new HPacket("MoveWallItem", HMessage.Direction.TOSERVER);
                movePacket.appendInt(itemId);
                movePacket.appendString(":w=" + newW1 + "," + newW2 + " l=" + newX + "," + newY + " l");
                sendToServer(movePacket);

                try {
                    Thread.sleep(125);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            usedItems.clear();
            usedItemsSet.clear();
        }).start();
    }

    public void handleopenblackboard() {
        if (isBlackboardOpen) {
            return;
        }

        GridPane gridPane = new GridPane();

        colors.put("Blue", Color.web("#9CCEFF"));
        colors.put("Pink", Color.web("#FF9CFF"));
        colors.put("Green", Color.web("#9CFF9C"));
        colors.put("Yellow", Color.web("#FFFF33"));

        List<String> colorOrder = Arrays.asList("Blue", "Pink", "Green", "Yellow");

        int rowIndex = 0;
        for (String colorName : colorOrder) {
            Button colorButton = new Button();
            colorButton.setStyle("-fx-background-color: #" + colors.get(colorName).toString().substring(2, 8) + ";");
            colorButton.setMinSize(80, 40);
            colorButton.setMaxSize(80, 40);

            colorButton.setOnAction(event -> selectedColor = colors.get(colorName));

            gridPane.add(colorButton, 0, rowIndex);
            rowIndex++;
        }

        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 12; j++) {
                Rectangle cell = new Rectangle(40, 40);
                cell.setFill(Color.BLACK);
                cell.setStroke(Color.DARKGRAY);
                int index = i * 12 + j;

                cell.setOnMousePressed(event -> {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        selectedCells.add(index);
                        cell.setFill(selectedColor);
                        cellColorMap.put(index, selectedColor);
                    } else if (event.getButton() == MouseButton.SECONDARY) {
                        selectedCells.remove(index);
                        cell.setFill(Color.BLACK);
                        cellColorMap.remove(index);
                    }
                });

                cell.setOnMouseDragged(event -> {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        selectedCells.add(index);
                        cell.setFill(selectedColor);
                        cellColorMap.put(index, selectedColor);
                    } else if (event.getButton() == MouseButton.SECONDARY) {
                        selectedCells.remove(index);
                        cell.setFill(Color.BLACK);
                        cellColorMap.remove(index);
                    }
                });

                gridPane.add(cell, j + 1, i);
            }
        }

        gridPane.setOnMouseDragged(event -> {
            Node source = event.getPickResult().getIntersectedNode();
            if (source instanceof Rectangle) {
                Rectangle cell = (Rectangle) source;
                int index = GridPane.getRowIndex(cell) * 12 + GridPane.getColumnIndex(cell) - 1;
                if (event.isPrimaryButtonDown()) {
                    selectedCells.add(index);
                    cell.setFill(selectedColor);
                    cellColorMap.put(index, selectedColor);
                } else if (event.isSecondaryButtonDown()) {
                    selectedCells.remove(index);
                    cell.setFill(Color.BLACK);
                    cellColorMap.remove(index);
                }
            }
        });

        blackboardStage = new Stage();
        blackboardStage.setTitle("Blackboard");

        VBox vbox = new VBox();
        Scene scene = new Scene(vbox);

        vbox.getChildren().add(gridPane);

        blackboardStage.setScene(scene);
        blackboardStage.setResizable(false);

        always_on_top_cbx.selectedProperty().addListener((observable, oldValue, newValue) -> {
            blackboardStage.setAlwaysOnTop(newValue);
        });

        blackboardStage.setAlwaysOnTop(always_on_top_cbx.isSelected());

        blackboardStage.setOnCloseRequest(event -> {
            selectedColor = Color.web("FFFF33");
            selectedCells.clear();
            isBlackboardOpen = false;
        });

        blackboardStage.show();
        isBlackboardOpen = true;
    }

    private Color getCellColor(int index) {
        return cellColorMap.getOrDefault(index, Color.web("FFFF33"));
    }
}