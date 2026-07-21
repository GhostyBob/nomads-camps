package com.ghosty.nomadscamps;

import com.ghosty.nomadscamps.networking.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Nullable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CampSuppliesGUI extends Screen {

    // region FIELDS
    /// A reference to the last visited screen
    public Screen prevScreen = null;
    /// The name of the current GUI screen.
    public String address;
    ///  A placeholder element to display while waiting for savedStructures.
    private TextWidget structureListLoadingText;
    /// A field to keep track of the structure slot currently being edited
    private int currentSlotIndex = -1;
    // Some constants used for the look of the menu.
    private static final Identifier BACKGROUND_TEXTURE = Identifier.ofVanilla("textures/gui/demo_background.png");
    private final int backgroundWidth = 248;
    private final int backgroundHeight = 166;
    //demo_background.png is 248x166px

    // Flags to stop the screen from blurring and the world from pausing.
    @Override
    public boolean shouldPause() {return false;}
    @Override
    protected void applyBlur(float delta) {}
    // endregion FIELDS

    // region CONSTRUCTORS
    public CampSuppliesGUI(String title) {
        super(Text.of(title));
        address = title;
    }
    public CampSuppliesGUI(String title, Screen prev) {
        super(Text.of(title));
        address = title;
        prevScreen = prev;
    }
    // endregion CONSTRUCTORS

    // region PUBLIC METHODS
    @Override
    public void init() {
        switch(address) {
            case "home":
                openHome();
                break;
            case "structureList":
                openStructureList();
                break;
            case "structureCreator":
                openStructureCreator();
                break;
            case "claim":
                openClaimScreen();
                break;
            case "structurePlacerTemp":
                openStructurePlacerTEMP();
                break;
            case "slotEditor":
                openSlotEditor();
                break;
            default:
                close();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        drawBackground(context, delta, mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {this.client.setScreen(prevScreen);}

    public void forceClose() {this.client.setScreen(null);}
    // endregion PUBLIC METHODS

    // region PAGE LAYOUTS
    private void openHome() {
        ButtonWidget closeButton = ButtonWidget.builder(Text.of("Close"), (btn) -> {
            this.close();
        }).dimensions(super.width / 2 - 116, super.height / 2 + 52 - 24, 232, 48).build();

        ButtonWidget upgradesButton = ButtonWidget.builder(Text.of("Manage Upgrades"), (btn) -> {
            switchScreen("upgrades", this, null);
        }).dimensions(super.width / 2 - 116, super.height / 2 - 24, 232, 48).build();

        ButtonWidget structureMenuButton = ButtonWidget.builder(Text.of("View Structure List"), (btn) -> {
            switchScreen("structureList", this, null);
        }).dimensions(super.width / 2 - 116, super.height / 2 - 52 - 24, 232, 48).build();

        //Buttons should usually have a height of 20

        //Ass the buttons to the screen
        this.addDrawableChild(closeButton);
        this.addDrawableChild(upgradesButton);
        this.addDrawableChild(structureMenuButton);
        //Have a vertical scrollable list of structures on one side, maybe with an orthographic view of the structure in the corner
        //place previous structures?
        //view structure list/manage structures
        //->register new structure
        //show/hide camp boundary?
        //view/manage upgrades
        //view/manage approved player access
    }

    private void openClaimScreen() {
        //TODO make this screen prettier
        ButtonWidget claimOwnershipButton = ButtonWidget.builder(Text.of("Claim Ownership"), (btn) -> {

            sendOwnershipPacket();
            forceClose();

        }).dimensions(
                (super.width / 2) - 60,
                (super.height / 3) - 10,
                120,
                20
        ).build();

        this.addDrawableChild(claimOwnershipButton);
    }

    private void openStructureList() {
        ButtonWidget newStructureButton = ButtonWidget.builder(Text.of("New Structure"), (btn) -> {
            switchScreen("structureCreator", this, null);
        }).dimensions(
                (super.width / 2) + (backgroundWidth / 4) - 30,
                (super.height / 2) - (backgroundHeight / 4) - 10,
                60,
                20
        ).build();
        ButtonWidget closeButton = ButtonWidget.builder(Text.of("Back"), (btn) -> {
            this.close();
        }).dimensions(
                (super.width / 2) + (backgroundWidth / 4) - 30,
                (super.height / 2) + (backgroundHeight / 4) - 10,
                60,
                20
        ).build();

        //Fetch saved structures
        if (NomadsCampsClient.instance.getSlots() == null) {
            ClientPlayNetworking.send(new UpdateSlotsPayload(false, new ArrayList<>()));

            // Spinning up a thread just to populate this list might be overly memory-intensive
            Thread structureListWaiter = new Thread(this::receiveStructures);
            structureListWaiter.start();
        } else {
            structureListWidget structureList = new structureListWidget(client,
                    backgroundWidth / 2,
                    backgroundHeight - 10,
                    (super.width / 2 - backgroundWidth / 2) + 5,
                    (super.height / 2 - backgroundHeight / 2) + 5,
                    this
            );
            this.addDrawableChild(structureList);
        }

        this.addDrawableChild(newStructureButton);
        this.addDrawableChild(closeButton);
    }

    private void openStructureCreator() {
        TextFieldWidget nameField = new TextFieldWidget(textRenderer,
                (super.width / 2) - 60,
                (super.height / 2) - (backgroundHeight / 4) - 10,
                120,
                20,
                Text.of("Structure Name")
        );

        // TODO replace these temp text fields with bounding box system
        // region TEMP INPUT FIELDS
        TextFieldWidget xField = new TextFieldWidget(textRenderer,
                (super.width / 2) - 60,
                (super.height / 2) - (backgroundHeight / 4) + 20,
                40,
                20,
                Text.of("X")
        );
        TextFieldWidget yField = new TextFieldWidget(textRenderer,
                (super.width / 2) - 10,
                (super.height / 2) - (backgroundHeight / 4) + 20,
                40,
                20,
                Text.of("Y")
        );
        TextFieldWidget zField = new TextFieldWidget(textRenderer,
                (super.width / 2) + 40,
                (super.height / 2) - (backgroundHeight / 4) + 20,
                40,
                20,
                Text.of("Z")
        );

        TextFieldWidget lengthField = new TextFieldWidget(textRenderer,
                (super.width / 2) - 60,
                (super.height / 2) - (backgroundHeight / 4) + 50,
                40,
                20,
                Text.of("dX")
        );
        TextFieldWidget heightField = new TextFieldWidget(textRenderer,
                (super.width / 2) - 10,
                (super.height / 2) - (backgroundHeight / 4) + 50,
                40,
                20,
                Text.of("dY")
        );
        TextFieldWidget widthField = new TextFieldWidget(textRenderer,
                (super.width / 2) + 40,
                (super.height / 2) - (backgroundHeight / 4) + 50,
                40,
                20,
                Text.of("dZ")
        );

        this.addDrawableChild(xField);
        this.addDrawableChild(yField);
        this.addDrawableChild(zField);
        this.addDrawableChild(lengthField);
        this.addDrawableChild(heightField);
        this.addDrawableChild(widthField);
        // endregion TEMP INPUT FIELDS

        ButtonWidget saveButton = ButtonWidget.builder(Text.of("Save"), (btn) -> {
            //TODO refactor the BlockPos and Vec3i to use the new bounding box system
            //TODO reimplement each player having their own directory
            sendSavePacket(
                    NomadsCampsClient.instance.getSlots().get(currentSlotIndex),
                    new BlockPos(
                            Integer.parseInt(xField.getText()),
                            Integer.parseInt(yField.getText()),
                            Integer.parseInt(zField.getText())
                    )
            );
            close();
        }).dimensions(
                (super.width / 2) - (backgroundWidth / 4) - 30,
                (super.height / 2) + (backgroundHeight / 4) - 10,
                60,
                20
        ).build();
        ButtonWidget closeButton = ButtonWidget.builder(Text.of("Back"), (btn) -> {
            this.close();
        }).dimensions(
                (super.width / 2) + (backgroundWidth / 4) - 30,
                (super.height / 2) + (backgroundHeight / 4) - 10,
                60,
                20
        ).build();


        this.addDrawableChild(closeButton);
        this.addDrawableChild(saveButton);
        this.addDrawableChild(nameField);
    }

    private void openStructurePlacerTEMP() {

        TextFieldWidget xField = new TextFieldWidget(textRenderer,
                (super.width / 2) - 70,
                (super.height / 2) - (backgroundHeight / 4) + 20,
                40,
                20,
                Text.of("X")
        );
        TextFieldWidget yField = new TextFieldWidget(textRenderer,
                (super.width / 2) - 20,
                (super.height / 2) - (backgroundHeight / 4) + 20,
                40,
                20,
                Text.of("Y")
        );
        TextFieldWidget zField = new TextFieldWidget(textRenderer,
                (super.width / 2) + 30,
                (super.height / 2) - (backgroundHeight / 4) + 20,
                40,
                20,
                Text.of("Z")
        );


        ButtonWidget placeButton = ButtonWidget.builder(Text.of("place"), (btn) -> {
                    sendBuildPacket(
                            NomadsCampsClient.instance.getSlots().get(currentSlotIndex),
                            new BlockPos(new Vec3i(
                                    Integer.parseInt(xField.getText()),
                                    Integer.parseInt(yField.getText()),
                                    Integer.parseInt(zField.getText()))
                            )
                    );
                    forceClose();
                }
        ).dimensions(
                (super.width / 2) - 30,
                (super.height / 2) + (backgroundHeight / 4) - 10,
                60,
                20
        ).build();

        this.addDrawableChild(xField);
        this.addDrawableChild(yField);
        this.addDrawableChild(zField);
        this.addDrawableChild(placeButton);
    }

    private void openSlotEditor() {
        // TODO redo this (and un-expose the file name setter)
        TextFieldWidget nameField = new TextFieldWidget(
                textRenderer,
                width / 2 - 100,
                height / 2 - 40,
                200,
                20,
                Text.of("Structure Name")
        );
        nameField.setText(NomadsCampsClient.instance.getSlots().get(currentSlotIndex).structureName);

        //This one is temp
        TextFieldWidget filenameField = new TextFieldWidget(
                textRenderer,
                width / 2 - 100,
                height / 2,
                200,
                20,
                Text.of("Structure File Name")
        );
        filenameField.setText(NomadsCampsClient.instance.getSlots().get(currentSlotIndex).structureFileName.toString());

        ButtonWidget saveButton = ButtonWidget.builder(
                Text.of("Save"),
                (btn) -> {
                    NomadsCampsClient.instance.getSlots().get(currentSlotIndex).structureName = nameField.getText();
                    NomadsCampsClient.instance.getSlots().get(currentSlotIndex).structureFileName = Identifier.of(filenameField.getText());
                }
        ).dimensions(
                width / 2,
                height / 2 + 40,
                50,
                20)
        .build();

        this.addDrawableChild(nameField);
        this.addDrawableChild(filenameField);
        this.addDrawableChild(saveButton);
    }
    //endregion PAGE LAYOUTS

    // region HELPER METHODS
    protected CampSuppliesGUI switchScreen(String title, @Nullable Screen parent, @Nullable Integer index) {
        if(parent == null) {
            CampSuppliesGUI output = new CampSuppliesGUI(title);
            if (index != null) output.currentSlotIndex = index;
            this.client.setScreen(output);
            return output;
        } else {
            CampSuppliesGUI output = new CampSuppliesGUI(title, parent);
            if (index != null) output.currentSlotIndex = index;
            this.client.setScreen(output);
            return output;
        }
    }

    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(BACKGROUND_TEXTURE, i, j, 0, 0.0F, 0.0F, this.backgroundWidth, this.backgroundHeight, 256, 256);
    }
    // endregion HELPER METHODS

    // region NETWORKING
    public void receiveStructures() {
        try {
            while (NomadsCampsClient.instance.getSlots() == null)
                Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if(!address.equals("structureList"))
            return;

        structureListWidget structureList = new structureListWidget(client,
                backgroundWidth / 2,
                backgroundHeight - 10,
                (super.width / 2 - backgroundWidth / 2) + 5,
                (super.height / 2 - backgroundHeight / 2) + 5,
                this
        );
        this.addDrawableChild(structureList);
    }

    private void sendOwnershipPacket() {
        ClientPlayNetworking.send(new SetOwnerPayload());
    }

    private void sendBuildPacket(StructureSlot slot, BlockPos origin) {
        ClientPlayNetworking.send(new StructureActionPayload(1, slot, origin));
    }

    protected void sendRemovePacket(StructureSlot slot) {
        ClientPlayNetworking.send(new StructureActionPayload(2, slot, (BlockPos) BlockPos.ZERO));
    }

    protected void sendSavePacket(StructureSlot slot, BlockPos origin) {
        ClientPlayNetworking.send(new StructureActionPayload(3, slot, origin));
    }
    // endregion NETWORKING

    // Inner class to display the structure list
    class structureListWidget extends ElementListWidget<structureListWidget.structureEntry> {
        private final CampSuppliesGUI parentGui;

        // Constructor
        structureListWidget(MinecraftClient client, int width, int height, int x, int y, CampSuppliesGUI gui) {
            super(client, width + 4, height, y, 40);
            this.setX(x);
            parentGui = gui;

            Collator collator = Collator.getInstance(Locale.getDefault()); //What is this??? It's something used to sort the entries

            int index = 0;
            //Add entries to the list using this.addEntry(theEntryToAdd)
            for(StructureSlot s : NomadsCampsClient.instance.getSlots()) {
                //TODO don't forget to re-do this part!
                //Cut off the end of the string so the file extension (.nbt) isn't included
                //String structureName = s.substring(0, s.length() - 4);
                this.addEntry(new structureEntry(s, index++, width - 8, 40, this.getX()));
            }
        }

        // region METHODS
        public void setSelected(@Nullable structureEntry passedEntry) {
            super.setSelected(passedEntry);

            //Store the passed entry in the parent for use
        }

        //To control the location of the scroll bar
        @Override
        public int getRowLeft() {
            return this.getX() + 4;
        }

        @Override
        public int getRowWidth() {
            return this.width - 8;
        }

        @Override
        protected int getDefaultScrollbarX() {
            return this.getRowLeft() + this.getRowWidth() - 2;
        }
        // endregion METHODS

        // Inner inner class to represent an entry in the structure list
        class structureEntry extends ElementListWidget.Entry<structureEntry> {

            // region FIELDS
            private int x;

            private final int ELEMENT_PADDING = 5;

            // Elements in this entry
            private final StructureSlot structureSlot;
            //Icon?
            private final String name;
            private ButtonWidget nameButton;
            private ButtonWidget editButton;
            //Edit button?

            private ArrayList<ButtonWidget> children = new ArrayList<ButtonWidget>();
            // endregion FIELDS

            // Constructor
            public structureEntry(StructureSlot slot, int index, int width, int height, int x) {
                this.structureSlot = slot;
                this.name = slot.structureName;
                this.x = x;

                if(!structureSlot.isPlaced()) {
                    nameButton = ButtonWidget.builder(Text.of(name), (btn) -> {
                        parentGui.switchScreen("structurePlacerTemp", parentGui, index);
                    }).dimensions(0, 0, width - height - ELEMENT_PADDING, height).build();
                    children.add(nameButton);
                } else {
                    nameButton = ButtonWidget.builder(Text.of("Pack up " + name), (btn) -> {
                        parentGui.currentSlotIndex = index;
                        parentGui.sendRemovePacket(slot);
                    }).dimensions(0, 0, width - height - ELEMENT_PADDING, height).build();
                    children.add(nameButton);
                }

                editButton = ButtonWidget.builder(Text.of("E"), (btn) -> {
                    parentGui.switchScreen("slotEditor", parentGui, index);
                }).dimensions(0, 0, height, height).build();
                children.add(editButton);
                //other buttons...
            }

            // region METHODS
            public List<? extends Selectable> selectableChildren() {
                return children;
            }

            public List<? extends Element> children() {
                return children;
            }

            //Not entirely sure what these 3 are/do; I just copied them over from
            //CustomizeBuffetLevelScreen and switched the variables to match
//            @Override
//            public Text getNarration() {
//                return Text.translatable("narrator.select", new Object[]{Text.of(this.name)});
//            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                //context.drawCenteredTextWithShadow(CampSuppliesGUI.this.textRenderer, this.name, x + (CampSuppliesGUI.super.width / 4), y + 15, 16777215);
                nameButton.setPosition(getRowLeft(), (int) (y * 1.1 - 4));
                nameButton.render(context, mouseX, mouseY, tickDelta);

                editButton.setPosition(getRowLeft() + nameButton.getWidth() + ELEMENT_PADDING, (int) (y * 1.1 - 4));
                editButton.render(context, mouseX, mouseY, tickDelta);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                structureListWidget.this.setSelected(this);
                return super.mouseClicked(mouseX, mouseY, button);
            }
            // endregion METHODS
        }
    }
}
