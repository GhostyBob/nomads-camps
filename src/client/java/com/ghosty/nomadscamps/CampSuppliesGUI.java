/// @Author GhostyBob
/// @Version 8/18/26

package com.ghosty.nomadscamps;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/// Represents the screen displayed to clients when they interact with camp supplies they own.
@Environment(EnvType.CLIENT)
public class CampSuppliesGUI extends Screen {

    /// A reference to the last visited screen.
    public Screen prevScreen = null;
    /// The name of the current GUI screen.
    public String address;
    /// A field to keep track of the structure slot currently being edited.
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
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void applyBlur(float delta) {
    }


    /// Constructs a screen of this GUI with no parent.
    /// Should only be used when opening the starting screen, as backing out of a
    /// screen using this constructor will close the GUI entirely.
    ///
    /// @param title       The address of the screen to display. Mainly used by the init method.
    /// @param suppliesPos The position of the CampBlockEntity that was clicked to open this GUI.
    public CampSuppliesGUI(String title, BlockPos suppliesPos) {
        super(Text.of(title));
        address = title;
        pos = suppliesPos;
    }

    /// Constructs a screen of this GUI with the given parent.
    /// Backing out of a screen using this constructor will switch to the parent screen.
    ///
    /// @param title       The address of the screen to display. Mainly used by the init method.
    /// @param prev        The screen to set as the parent of this screen.
    /// @param suppliesPos The position of the CampBlockEntity that was clicked to open this GUI.
    public CampSuppliesGUI(String title, Screen prev, BlockPos suppliesPos) {
        super(Text.of(title));
        address = title;
        prevScreen = prev;
        pos = suppliesPos;
    }


    /// Called by the base game when this object is done being constructed.
    /// Defines the layout this screen should use, based on the address field.
    @Override
    public void init() {
        switch (address) {
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

    /// Used by the base game to render the screen. It's only overridden
    /// to draw the background image below everything else.
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(BACKGROUND_TEXTURE, i, j, 0, 0.0F, 0.0F, this.backgroundWidth, this.backgroundHeight, 256, 256);
        super.render(context, mouseX, mouseY, delta);
    }

    /// Used by the base game when the player backs out of the GUI (default esc).
    /// Sets the displayed screen to this screen's parent, or nothing if the parent is null.
    @Override
    public void close() {
        assert this.client != null;
        this.client.setScreen(prevScreen);
    }

    /// Used by this mod to clear the GUI, regardless of whether this screen has a parent or not.
    public void forceClose() {
        assert this.client != null;
        this.client.setScreen(null);
    }

    // region PAGE LAYOUTS

    /// The page layout for the structure list; currently the first screen
    /// encountered when opening the GUI.
    private void openStructureList() {
        // Set up the close button
        ButtonWidget closeButton = ButtonWidget.builder(ScreenTexts.BACK, (btn) -> this.close()).dimensions(
                (width / 2) + (backgroundWidth / 2) - 65,
                (height / 2) + (backgroundHeight / 2) - 25,
                60,
                20
        ).build();
        this.addDrawableChild(closeButton);

        // Fetch saved structures
        if (NomadsCampsClient.instance.getSlots() == null) {
            queryStructures();
        } else {
            // Set up the structure list itself
            structureListWidget structureList = new structureListWidget(client,
                    backgroundWidth - 15,
                    backgroundHeight - 35,
                    (width / 2 - backgroundWidth / 2) + 5,
                    (height / 2 - backgroundHeight / 2) + 5,
                    this
            );
            this.addDrawableChild(structureList);
        }
    }

    /// The page layout for the structure placer, used when the player opens an
    /// entry in the structure list.
    private void openStructurePlacer() {

        // Set up the offset sliders for x, y, and z
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
                width / 2 - backgroundWidth / 2 + 20,
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

        // Set up the place button
        ButtonWidget placeButton = ButtonWidget.builder(Text.of("Place"), (btn) -> {
                    assert NomadsCampsClient.instance.getSlots() != null;
                    StructureSlot slot = NomadsCampsClient.instance.getSlots().get(currentSlotIndex);
                    NomadsCampsClient.sendBuildPacket(
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
                (width / 2) - (backgroundWidth / 4) - 30,
                (height / 2) + (backgroundHeight / 4) - 10,
                60,
                20
        ).build();
        this.addDrawableChild(placeButton);

        // Set up the close button
        ButtonWidget closeButton = ButtonWidget.builder(ScreenTexts.BACK, (btn) -> this.close()).dimensions(
                (width / 2) + (backgroundWidth / 4) - 30,
                (height / 2) + (backgroundHeight / 4) - 10,
                60,
                20
        ).build();
        this.addDrawableChild(closeButton);
    }

    /// The page layout for the slot editor. Used when the player presses the edit button
    /// on an entry in the structure list.
    /// Displays the slot's name, size, and location.
    /// This is where the player will eventually be able to upgrade their structure slots.
    /// TODO refine this screen & implement slot upgrades.
    private void openSlotEditor() {
        assert NomadsCampsClient.instance.getSlots() != null;
        StructureSlot currentSlot = NomadsCampsClient.instance.getSlots().get(currentSlotIndex);

        // Set up the structure name field
        TextFieldWidget nameField = new TextFieldWidget(
                textRenderer,
                width / 2 - 100,
                height / 2 - backgroundHeight / 2 + 10,
                200,
                20,
                Text.of("Structure Name")
        );
        nameField.setText(currentSlot.structureName);
        this.addDrawableChild(nameField);

        // region SLOT DESCRIPTION
        // Build the text for the slot size description
        StringBuilder slotDescBuilder = new StringBuilder("This slot encompasses a ");
        slotDescBuilder.append(currentSlot.sizeX());
        slotDescBuilder.append("x");
        slotDescBuilder.append(currentSlot.sizeY());
        slotDescBuilder.append("x");
        slotDescBuilder.append(currentSlot.sizeZ());
        slotDescBuilder.append(" block space");
        Text slotSize = Text.of(slotDescBuilder.toString());

        // Set up the line describing the slot size
        TextWidget slotSizeDescription = new TextWidget(
                width / 2 - backgroundWidth / 2,
                height / 2 - backgroundHeight / 4,
                backgroundWidth,
                textRenderer.fontHeight,
                slotSize,
                textRenderer);
        this.addDrawableChild(slotSizeDescription);

        // Build the text for the slot location description
        slotDescBuilder = new StringBuilder("and ");
        if (currentSlot.structureFileName.equals(NomadsCamps.DEFAULT_STRUCTURE_FILENAME)) {
            slotDescBuilder.append("has never been placed.");
        } else if (currentSlot.isPlaced()) {
            assert currentSlot.getOccupiedArea() != null;
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

        // Set up the line describing slot location
        TextWidget slotPosDescription = new TextWidget(
                width / 2 - backgroundWidth / 2,
                height / 2 - backgroundHeight / 4 + textRenderer.fontHeight + 2,
                backgroundWidth,
                textRenderer.fontHeight,
                slotPosition,
                textRenderer);
        this.addDrawableChild(slotPosDescription);
        // endregion SLOT DESCRIPTION

        // region SLOT UPGRADES
        // Set up the upgrade points display
        TextWidget upgradeCountDisplay = new TextWidget(
                width / 2 - backgroundWidth / 2,
                height / 2,
                backgroundWidth,
                textRenderer.fontHeight,
                Text.of(ScreenTexts.composeGenericOptionText(
                        Text.of("Available slot size upgrade points"),
                        Text.of(Integer.toString(NomadsCampsClient.instance.getTracker().unusedSlotSizeUpgrades)))),
                textRenderer);
        this.addDrawableChild(upgradeCountDisplay);

        // Set up the upgrade cost display
        TextWidget upgradeCostDisplay = new TextWidget(
                width / 2 - backgroundWidth / 2,
                height / 2 + textRenderer.fontHeight + 2,
                backgroundWidth,
                textRenderer.fontHeight,
                ScreenTexts.EMPTY,
                textRenderer);
        this.addDrawableChild(upgradeCostDisplay);

        // Set up the button for choosing a direction
        // to expand in and link it to the upgrade
        // cost display.
        DirectionCycleButton directionButton = new DirectionCycleButton(
                width / 2 - backgroundWidth / 2 + 10,
                height / 2 + backgroundHeight / 6,
                backgroundWidth / 2 - 15,
                20,
                null,
                ButtonWidget::onPress,
                upgradeCostDisplay
        );
        this.addDrawableChild(directionButton);

        // Set up the button to commit the upgrade. Only show it when
        // directionButton has been clicked at least once.
        ButtonWidget upgradeButton = ButtonWidget.builder(
                Text.of("Upgrade"),
                (btn) -> {
                    if(directionButton.getDirection() != null) {
                        NomadsCampsClient.sendUpgradePacket(currentSlot, directionButton.getDirection());
                        close();
                    }
                }
        ).dimensions(
                width / 2 + 5,
                height / 2 + backgroundHeight / 6,
                backgroundWidth / 2 - 15,
                20
        ).build();
        directionButton.linkUpgradeButton(upgradeButton);

        // endregion SLOT UPGRADES

        // Set up the save and close button
        ButtonWidget saveButton = ButtonWidget.builder(
                        ScreenTexts.DONE,
                        (btn) -> {
                            currentSlot.structureName = nameField.getText();
                            close();
                        }
                ).dimensions(
                        width / 2 + backgroundWidth / 4 + 5,
                        height / 2 + backgroundHeight / 2 - 30,
                        backgroundWidth / 4 - 15,
                        20)
                .build();
        this.addDrawableChild(saveButton);
    }
    //endregion PAGE LAYOUTS

    // region HELPER METHODS

    /// Controls switching to new screens within the GUI.
    /// Use this method instead of constructing new screens directly.
    ///
    /// @param title  The address of the screen to switch to.
    /// @param parent The (optional) screen to use as the new screen's parent.
    /// @param index  The index of the currently examined slot.
    ///                             Used when switching to the structure placer and editor.
    protected void switchScreen(String title, @Nullable Screen parent, @Nullable Integer index) {
        CampSuppliesGUI output;
        if (parent == null) {
            output = new CampSuppliesGUI(title, pos);
        } else {
            output = new CampSuppliesGUI(title, parent, pos);
        }
        if (index != null) output.currentSlotIndex = index;
        assert this.client != null;
        this.client.setScreen(output);
    }

    /// Handles the setup of the GUI's structure list when the client's structure
    /// slot list isn't set up yet.
    /// Spins up a thread running receiveStructures to wait for the return packet
    /// from the server.
    private void queryStructures() {
        NomadsCampsClient.sendQueryStructuresPacket();

        Thread structureListWaiter = new Thread(this::receiveStructures);
        structureListWaiter.start();
    }

    /// Waits for the client's list of structure slots to be populated, then sets up
    /// the structure list if its page is still being displayed.
    /// TODO Find something for this thread to do while it waits.
    private void receiveStructures() {
        try {
            // Wait for the list to become populated
            while (NomadsCampsClient.instance.getSlots() == null)
                Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Don't set up the structure list if the screen has changed.
        if (!address.equals("structureList"))
            return;

        // Set up the structure list
        CampSuppliesGUI.structureListWidget structureList = new structureListWidget(client,
                backgroundWidth - 15,
                backgroundHeight - 35,
                (width / 2 - backgroundWidth / 2) + 5,
                (height / 2 - backgroundHeight / 2) + 5,
                this
        );
        this.addDrawableChild(structureList);
    }
    // endregion HELPER METHODS

    /// A custom widget representing the list of structure slots displayed on the
    /// structure list screen.
    private static class structureListWidget extends ElementListWidget<structureListWidget.structureEntry> {
        /// A reference to the GUI containing this widget.
        private final CampSuppliesGUI parentGui;

        /// Constructs an instance of this widget.
        ///
        /// @param client The client instance running the GUI containing this widget.
        ///               Required for the super constructor.
        /// @param width  The width of this widget.
        /// @param height The height of this widget.
        /// @param x      The x position of the top-left corner of this widget.
        /// @param y      The y position of the top-left corner of this widget.
        /// @param gui    The GUI containing this widget.
        structureListWidget(MinecraftClient client, int width, int height, int x, int y, CampSuppliesGUI gui) {
            super(client, width + 4, height, y, 20);
            this.setX(x);
            parentGui = gui;

            // Used by the superclass to sort its list entries, but this widget sorts them manually.
            // Retained in case a working collator is needed on short notice.
            //Collator collator = Collator.getInstance(Locale.getDefault());

            // Populate the list
            ArrayList<StructureSlot> slots = NomadsCampsClient.instance.getSlots();
            if (slots == null) return;

            for (StructureSlot s : slots) {
                this.addEntry(new structureEntry(s, width - 8, 20));
            }
        }

        // region METHODS

        /// Overridden to give better control of the scroll bar position.
        @Override
        public int getRowLeft() {
            return this.getX() + 4;
        }

        /// Overridden to give better control of the scroll bar position.
        @Override
        public int getRowWidth() {
            return this.width - 8;
        }

        /// Overridden to give better control of the scroll bar position.
        @Override
        protected int getDefaultScrollbarX() {
            return this.getRowLeft() + this.getRowWidth() - 2;
        }
        // endregion METHODS

        /// Represents a single entry in a structureListWidget.
        class structureEntry extends ElementListWidget.Entry<structureEntry> {

            // region FIELDS
            /// Represents the number of pixels to place between the buttons in a structureEntry.
            private final int ELEMENT_PADDING = 2;

            /// A list of all the components of this entry.
            private final ArrayList<ButtonWidget> children = new ArrayList<>();

            /// The button leading to the structure placer screen.
            private final ButtonWidget nameButton;
            /// The button leading to the slot editor screen.
            private final ButtonWidget editButton;
            // endregion FIELDS

            /// Constructs a new structureEntry.
            ///
            /// @param slot   The structure slot being represented by this entry.
            /// @param width  The width of this entry (usually the width of the list minus some
            ///               padding). The name and edit buttons will stretch horizontally to fill most of
            ///               the width.
            /// @param height The height of this entry. The name and edit buttons will stretch
            ///               vertically to fill the full height.
            public structureEntry(StructureSlot slot, int width, int height) {
                String name = slot.structureName;

                // Set up the name button, prepending some text to indicate whether
                // it's currently placed
                if (!slot.isPlaced()) {
                    nameButton = ButtonWidget.builder(Text.of(name), (btn) ->
                                    parentGui.switchScreen("structurePlacer", parentGui, slot.getIndex()))
                            .dimensions(0, 0, width - 40 - ELEMENT_PADDING, height).build();
                    children.add(nameButton);
                } else {
                    nameButton = ButtonWidget.builder(Text.of("Pack up " + name), (btn) -> {
                        parentGui.currentSlotIndex = slot.getIndex();
                        NomadsCampsClient.sendRemovePacket(slot);
                        parentGui.close();
                    }).dimensions(0, 0, width - 40 - ELEMENT_PADDING, height).build();
                    children.add(nameButton);
                }

                // Set up the edit button
                editButton = ButtonWidget.builder(Text.of("Edit"), (btn) ->
                                parentGui.switchScreen("slotEditor", parentGui, slot.getIndex()))
                        .dimensions(0, 0, 40, height).build();
                children.add(editButton);
            }

            // region METHODS

            /// Used by the base game to tab-navigate through the components of this entry.
            public List<? extends Selectable> selectableChildren() {
                return children;
            }

            /// Used by the base game to render the components of this entry.
            /// In this case, it's functionally identical to calling selectableChildren.
            public List<? extends Element> children() {
                return children;
            }

            /// Used by the base game to draw this entry. Overridden to put the buttons
            /// in the proper place.
            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                nameButton.setPosition(getRowLeft(), (int) (y * 1.1 - 4));
                nameButton.render(context, mouseX, mouseY, tickDelta);

                editButton.setPosition(getRowLeft() + nameButton.getWidth() + ELEMENT_PADDING, (int) (y * 1.1 - 4));
                editButton.render(context, mouseX, mouseY, tickDelta);
            }

            /// Used by the base game to handle clicks on this entry.
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                structureListWidget.this.setSelected(this);
                return super.mouseClicked(mouseX, mouseY, button);
            }
            // endregion METHODS
        }
    }

    /// Custom widget to represent the offset sliders in the structure placer.
    private static class OffsetSliderWidget extends SliderWidget {
        /// The furthest distance (in either direction along any given axis)
        /// that a structure can be placed from its Camp Supplies.
        /// Values over ~32 aren't recommended so that the sliders feel good to control.
        private final int MAX_OFFSET = NomadsCamps.CONFIG.maxPlacementOffset();
        /// The offset currently stored in this slider. Formatted to be added to a
        /// blockPos dimension right out of the box.
        public int offset = 0;
        /// The label on this slider (not including the current slider value).
        private final Text label;

        /// Constructs an instance of this widget.
        ///
        /// @param x         The x position of the top left corner of this widget.
        /// @param y         The y position of the top left corner of this widget.
        /// @param width     The width of this widget.
        /// @param height    The height of this widget.
        /// @param dimension The dimension (x, y, or z) that this slider represents.
        ///                  Only used for the slider's label.
        /// @param value     The initial value that this slider should be set to.
        public OffsetSliderWidget(int x, int y, int width, int height, String dimension, double value) {
            super(x, y, width, height, ScreenTexts.EMPTY, value);
            label = Text.of(dimension + " Offset");
            updateMessage();
        }

        /// Updates the label displayed on the slider. Called by the base game whenever
        /// the slider is adjusted.
        @Override
        protected void updateMessage() {
            this.setMessage(ScreenTexts.composeGenericOptionText(
                    label,
                    Text.of(String.valueOf(offset))
            ));
        }

        /// Turns the value stored in the slider into something usable. Called by the
        /// base game whenever the slider is adjusted. In this case, it just formats
        /// the slider value into a block offset and stores it in offset to keep the
        /// value up to date.
        @Override
        protected void applyValue() {
            offset = (int) MathHelper.lerp(MathHelper.clamp(this.value, 0.0F, 1.0F), -MAX_OFFSET, MAX_OFFSET);
        }
    }

    /// Custom widget to represent the button that cycles through upgrade directions
    /// on the slot editor.
    private class DirectionCycleButton extends ButtonWidget {
        /// The direction currently represented by this button.
        @Nullable
        private Direction direction;
        /// A reference to the TextWidget that estimates upgrade costs.
        private final TextWidget trackerWidget;
        /// A reference to the ButtonWidget that commits the upgrade.
        private ButtonWidget upgradeButton;
        /// Tracks whether upgradeButton has already been added as a drawable child.
        private boolean isButtonShown = false;

        /// Constructs a ButtonWidget using the super constructor. Is not set to any
        /// direction if dir is passed as null. Otherwise, starts with the passed
        /// direction. Always pass ButtonWidget::onPressed as action to this constructor.
        protected DirectionCycleButton(
                int x, int y, int width, int height,
                @Nullable Direction dir, DirectionCycleButton.PressAction action,
                TextWidget tracker) {
            super(
                    x,
                    y,
                    width,
                    height,
                    ScreenTexts.composeGenericOptionText(
                            Text.of("Direction"),
                            (dir == null ? ScreenTexts.ELLIPSIS : Text.of(dir.asString()))
                    ),
                    action, DEFAULT_NARRATION_SUPPLIER
            );
            direction = dir;
            trackerWidget = tracker;
        }

        /// Logic for pressing this button. Cycles the stored direction and
        /// updates trackerWidget and upgradeButton accordingly.
        @Override
        public void onPress() {
            cycleDirection();
            updateTrackerText();
            showUpgradeButton();
        }

        /// @return The direction currently represented by this button.
        public @Nullable Direction getDirection() {
            return direction;
        }

        /// Cycles through all 6 directions in the following order:
        ///
        /// NORTH -> SOUTH -> EAST -> WEST -> UP -> DOWN -> NORTH -> etc.
        public void cycleDirection() {
            if(direction == null) direction = Direction.DOWN;

            direction = switch (direction) {
                case NORTH -> Direction.SOUTH;
                case SOUTH -> Direction.EAST;
                case EAST -> Direction.WEST;
                case WEST -> Direction.UP;
                case UP -> Direction.DOWN;
                default -> Direction.NORTH;
            };

            setMessage(ScreenTexts.composeGenericOptionText(
                    Text.of("Direction"),
                    Text.of(direction.asString())));
        }

        /// Used to set up the upgradeButton that's updated when this
        /// button is clicked.
        ///
        /// @param button The button to link.
        public void linkUpgradeButton(ButtonWidget button) {
            upgradeButton = button;
        }

        /// Adds the upgrade button as a drawable child if it hasn't
        /// been added already. Called when clicking this button.
        private void showUpgradeButton() {
            if (!isButtonShown) {
                addDrawableChild(upgradeButton);
                isButtonShown = true;
            }
        }

        /// Updates the text of trackerWidget to reflect changes
        /// made to this button's state.
        private void updateTrackerText() {
            assert NomadsCampsClient.instance.getSlots() != null;
            assert getDirection() != null;
            int cost = NomadsCamps.calculateUpgradeCost(
                    NomadsCampsClient.instance.getSlots().get(currentSlotIndex),
                    getDirection()
            );

            trackerWidget.setMessage(Text.of(
                    "This upgrade will cost " +
                            cost
                            + " point" +
                            (Math.abs(cost) == 1 ? "" : "s")
                            + "."));
        }
    }
}
