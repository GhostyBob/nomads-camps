package com.ghosty.nomadscamps;

import com.ghosty.nomadscamps.networking.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.NotImplementedException;
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
    /// A field to keep track of the structure slot currently being edited
    private int currentSlotIndex = -1;
    /// The location of the camp supplies that opened this screen, for structure placement offset purposes.
    private final BlockPos pos;
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
    public CampSuppliesGUI(String title, BlockPos suppliesPos) {
        super(Text.of(title));
        address = title;
        pos = suppliesPos;
    }
    public CampSuppliesGUI(String title, Screen prev, BlockPos suppliesPos) {
        super(Text.of(title));
        address = title;
        prevScreen = prev;
        pos = suppliesPos;
    }
    // endregion CONSTRUCTORS

    // region PUBLIC METHODS
    @Override
    public void init() {
        switch(address) {
            case "home", "structureCreator":
                throw new NotImplementedException();
            case "structureList":
                openStructureList();
                break;
            case "structurePlacer":
                openStructurePlacer();
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
    private void openStructureList() {
        ButtonWidget closeButton = ButtonWidget.builder(ScreenTexts.BACK, (btn) -> {
            this.close();
        }).dimensions(
                (width / 2) + (backgroundWidth / 2) - 65,
                (height / 2) + (backgroundHeight / 2) - 25,
                60,
                20
        ).build();
        this.addDrawableChild(closeButton);

        //Fetch saved structures
        if (NomadsCampsClient.instance.getSlots() == null) {
            queryStructures();
        } else {
            structureListWidget structureList = new structureListWidget(client,
                    backgroundWidth - 15,
                    backgroundHeight - 35,
                    (super.width / 2 - backgroundWidth / 2) + 5,
                    (super.height / 2 - backgroundHeight / 2) + 5,
                    this
            );
            this.addDrawableChild(structureList);
        }
    }

    private void openStructurePlacer() {

        OffsetSliderWidget xOffsetSlider = new OffsetSliderWidget(
                width / 2 - backgroundWidth / 2 + 20,
                height / 2 - backgroundHeight / 4 - 30,
                backgroundWidth - 40,
                20,
                "X",
                0.5F
        );
        addDrawableChild(xOffsetSlider);

        OffsetSliderWidget yOffsetSlider = new OffsetSliderWidget(
                width / 2  - backgroundWidth / 2 + 20,
                height / 2 - backgroundHeight / 4,
                backgroundWidth - 40,
                20,
                "Y",
                0.5F
        );
        addDrawableChild(yOffsetSlider);

        OffsetSliderWidget zOffsetSlider = new OffsetSliderWidget(
                width / 2 - backgroundWidth / 2 + 20,
                height / 2 - backgroundHeight / 4 + 30,
                backgroundWidth - 40,
                20,
                "Z",
                0.5F
        );
        addDrawableChild(zOffsetSlider);

        ButtonWidget placeButton = ButtonWidget.builder(Text.of("Place"), (btn) -> {
                    StructureSlot slot = NomadsCampsClient.instance.getSlots().get(currentSlotIndex);
                    sendBuildPacket(
                            slot,
                            new BlockPos(
                                    pos.getX() + (xOffsetSlider.offset),
                                    pos.getY() + (yOffsetSlider.offset),
                                    pos.getZ() + (zOffsetSlider.offset)
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
        this.addDrawableChild(placeButton);
    }

    private void openSlotEditor() {
        StructureSlot currentSlot = NomadsCampsClient.instance.getSlots().get(currentSlotIndex);

        // TODO refine this screen
        // region NAME FIELD
        TextFieldWidget nameField = new TextFieldWidget(
                textRenderer,
                width / 2 - 100,
                height / 2 - (3 * (backgroundHeight / 8)),
                200,
                20,
                Text.of("Structure Name")
        );
        nameField.setText(currentSlot.structureName);
        this.addDrawableChild(nameField);
        // endregion NAME FIELD

        // region SLOT DESCRIPTION
        StringBuilder slotDescBuilder = new StringBuilder("This slot encompasses a ");
        slotDescBuilder.append(currentSlot.sizeX());
        slotDescBuilder.append("x");
        slotDescBuilder.append(currentSlot.sizeY());
        slotDescBuilder.append("x");
        slotDescBuilder.append(currentSlot.sizeZ());
        slotDescBuilder.append(" block space.");
        Text slotSize = Text.of(slotDescBuilder.toString());

        TextWidget slotSizeDescription = new TextWidget(
                width / 2 - backgroundWidth / 2,
                height / 2 - (2 * textRenderer.fontHeight),
                backgroundWidth,
                textRenderer.fontHeight,
                slotSize,
                textRenderer);
        this.addDrawableChild(slotSizeDescription);

        slotDescBuilder = new StringBuilder("This structure ");
        if(currentSlot.structureFileName.equals(NomadsCamps.DEFAULT_STRUCTURE_FILENAME))
        {
            slotDescBuilder.append("has never been placed.");
        }
        else if(currentSlot.isPlaced()) {
            slotDescBuilder.append("is placed near ");
            slotDescBuilder.append(currentSlot.getOccupiedArea().getMinX());
            slotDescBuilder.append(", ");
            slotDescBuilder.append(currentSlot.getOccupiedArea().getMinY());
            slotDescBuilder.append(", ");
            slotDescBuilder.append(currentSlot.getOccupiedArea().getMinZ());
            slotDescBuilder.append(".");
        } else {
            slotDescBuilder.append("is currently in storage.");
        }
        Text slotPosition = Text.of(slotDescBuilder.toString());

        TextWidget slotPosDescription = new TextWidget(
                width / 2 - backgroundWidth / 2,
                height / 2 + textRenderer.fontHeight,
                backgroundWidth,
                textRenderer.fontHeight,
                slotPosition,
                textRenderer);
        this.addDrawableChild(slotPosDescription);
        // endregion SLOT DESCRIPTION

        // region SAVE / CLOSE BUTTONS
        ButtonWidget saveButton = ButtonWidget.builder(
                ScreenTexts.DONE,
                (btn) -> {
                    NomadsCampsClient.instance.getSlots().get(currentSlotIndex).structureName = nameField.getText();
                    close();
                }
        ).dimensions(
                width / 2 - backgroundWidth / 2 + 10,
                height / 2 + backgroundHeight / 2 - 30,
                backgroundWidth / 2 - 15,
                20)
        .build();
        this.addDrawableChild(saveButton);

        ButtonWidget closeButton = ButtonWidget.builder(
                        ScreenTexts.CANCEL,
                        (btn) -> {
                            close();
                        }
                ).dimensions(
                        width / 2 + 5,
                        height / 2 + backgroundHeight / 2 - 30,
                        backgroundWidth / 2 - 15,
                        20)
                .build();
        this.addDrawableChild(closeButton);
        // endregion SAVE / CLOSE BUTTONS
    }
    //endregion PAGE LAYOUTS

    // region HELPER METHODS
    protected CampSuppliesGUI switchScreen(String title, @Nullable Screen parent, @Nullable Integer index) {
        if(parent == null) {
            CampSuppliesGUI output = new CampSuppliesGUI(title, pos);
            if (index != null) output.currentSlotIndex = index;
            this.client.setScreen(output);
            return output;
        } else {
            CampSuppliesGUI output = new CampSuppliesGUI(title, parent, pos);
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
    private void queryStructures() {
        ClientPlayNetworking.send(new UpdateSlotsPayload(false, new ArrayList<>()));

        // TODO: Spinning up a thread just to populate this list might be overly memory-intensive
        Thread structureListWaiter = new Thread(this::receiveStructures);
        structureListWaiter.start();
    }

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
                backgroundWidth - 15,
                backgroundHeight - 35,
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
        ClientPlayNetworking.send(new StructureActionPayload(2, slot, BlockPos.ORIGIN));
    }

    protected void sendSavePacket(StructureSlot slot, BlockPos origin) {
        ClientPlayNetworking.send(new StructureActionPayload(3, slot, origin));
    }
    // endregion NETWORKING

    // Inner class to display the structure list
    private class structureListWidget extends ElementListWidget<structureListWidget.structureEntry> {
        private final CampSuppliesGUI parentGui;

        // Constructor
        structureListWidget(MinecraftClient client, int width, int height, int x, int y, CampSuppliesGUI gui) {
            super(client, width + 4, height, y, 20);
            this.setX(x);
            parentGui = gui;

            Collator collator = Collator.getInstance(Locale.getDefault()); //What is this??? It's something used to sort the entries

            //Add entries to the list using this.addEntry(theEntryToAdd)
            for(StructureSlot s : NomadsCampsClient.instance.getSlots()) {
                this.addEntry(new structureEntry(s, width - 8, 20, this.getX()));
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

            private final int ELEMENT_PADDING = 2;

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
            public structureEntry(StructureSlot slot, int width, int height, int x) {
                this.structureSlot = slot;
                this.name = slot.structureName;
                this.x = x;

                if(!structureSlot.isPlaced()) {
                    nameButton = ButtonWidget.builder(Text.of(name), (btn) -> {
                        parentGui.switchScreen("structurePlacer", parentGui, slot.getIndex());
                    }).dimensions(0, 0, width - 40 - ELEMENT_PADDING, height).build();;
                    children.add(nameButton);
                } else {
                    nameButton = ButtonWidget.builder(Text.of("Pack up " + name), (btn) -> {
                        parentGui.currentSlotIndex = slot.getIndex();
                        parentGui.sendRemovePacket(slot);
                        parentGui.close();
                    }).dimensions(0, 0, width - 40 - ELEMENT_PADDING, height).build();
                    children.add(nameButton);
                }

                editButton = ButtonWidget.builder(Text.of("Edit"), (btn) -> {
                    parentGui.switchScreen("slotEditor", parentGui, slot.getIndex());
                }).dimensions(0, 0, 40, height).build();
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

    // Inner class to represent placement offset sliders
    private class OffsetSliderWidget extends SliderWidget {
        private final int maxOffset = 16;
        public int offset = 0;
        private Text label;

        public OffsetSliderWidget(int x, int y, int width, int height, String dimension, double value) {
            super(x, y, width, height, ScreenTexts.EMPTY, value);
            label = Text.of(dimension + " Offset");
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(ScreenTexts.composeGenericOptionText(
                    label,
                    Text.of(String.valueOf(offset))
            ));
        }

        @Override
        protected void applyValue() {
            offset = (int) MathHelper.lerp(MathHelper.clamp(this.value, 0.0F, 1.0F), -maxOffset, maxOffset);
        }
    }
}
