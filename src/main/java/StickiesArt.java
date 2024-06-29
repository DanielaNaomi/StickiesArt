import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.parsers.HWallItem;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

@ExtensionInfo(
        Title = "StickiesArt",
        Description = "Art with stickies created with love",
        Version = "1.0",
        Author = "DanielaNaomi"
)

public class StickiesArt extends ExtensionForm {
    public TextField text;
    public TextField wbase;
    public TextField lbase;
    public Button buttongoart;
    public LinkedList<Integer> items = new LinkedList<>();
    public Button buttondisorganizeall;
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

    @Override
    protected void initExtension() {
        onConnect((host, port, APIVersion, versionClient, client) -> {
            if (host_postit.containsKey(host))
                uniqueId = host_postit.get(host);
        });
        intercept(HMessage.Direction.TOCLIENT, "Items", this::Items);
        intercept(HMessage.Direction.TOSERVER, "OpenFlatConnection", this::OpenFlatConnection);
    }

    private void OpenFlatConnection(HMessage hMessage) {
        items.clear();
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
        String[] wbaseParts = wbase.getText().split(",");
        String[] lbaseParts = lbase.getText().split(",");

        if (wbaseParts.length != 2 || lbaseParts.length != 2) {
            System.err.println("Base dimensions are not correctly formatted.");
            return;
        }

        int baseW1 = Integer.parseInt(wbaseParts[0].trim());
        int baseW2 = Integer.parseInt(wbaseParts[1].trim());
        int baseL1 = Integer.parseInt(lbaseParts[0].trim());
        int baseL2 = Integer.parseInt(lbaseParts[1].trim());

        new Thread(() -> {
            handlegoart(inputText, baseW1, baseW2, baseL1, baseL2);
        }).start();
    }

    public void handlegoart(String text, int baseW1, int baseW2, int baseL1, int baseL2) {
        int offsetW2 = 0;
        int itemIndex = 0;

        for (char letter : text.toCharArray()) {
            String[] positions = LetterMapper.getLetterMapping(letter);
            for (String position : positions) {
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
                        movePacket.appendInt(items.get(itemIndex++));
                        movePacket.appendString(":w=" + newW1 + "," + newW2 + " l=" + newX + "," + newY + " " + lOrR);
                        try {
                            Thread.sleep(125);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        sendToServer(movePacket);
                    } catch (NumberFormatException ex) {
                        System.err.println("Error parsing positions: " + ex.getMessage());
                    }
                }
            }
            offsetW2 -= 2;
        }
    }

    public void handledisorganizeall() {
        LinkedList<Integer> itemsCopy = new LinkedList<>(items);

        new Thread(() -> {
            Random random = new Random();

            for (int itemId : itemsCopy) {
                int newW1 = random.nextInt(10);
                int newW2 = random.nextInt(32);
                int newX = random.nextInt(10);
                int newY = random.nextInt(32);

                HPacket movePacket = new HPacket("MoveWallItem", HMessage.Direction.TOSERVER);
                movePacket.appendInt(itemId);
                movePacket.appendString(":w=" + newW1 + "," + newW2 + " l=" + newX + "," + newY + " l");
                try {
                    Thread.sleep(125);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sendToServer(movePacket);
            }
        }).start();
    }
}