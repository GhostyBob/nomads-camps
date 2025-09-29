package com.ghosty.nomadscamps;

import com.ghosty.nomadscamps.networking.CampBlockSavePayload;
import com.ghosty.nomadscamps.networking.CampBlockSetOwnerPayload;
import com.ghosty.nomadscamps.networking.QueryStructuresPayload;
import com.ghosty.nomadscamps.util.IEntityDataSaver;
import com.ghosty.nomadscamps.util.SuppliesData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.entity.StructureBlockBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Nullable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CampSuppliesGUI extends Screen {
    public Screen prevScreen = null;
    public String address;
    public static ArrayList<String> savedStructures;
    private TextWidget structureListLoadingText;
    private static final Identifier BACKGROUND_TEXTURE = Identifier.ofVanilla("textures/gui/demo_background.png");
    private int backgroundWidth = 248;
    private int backgroundHeight = 166;
    public BlockPos pos;
    //demo_background.png is 248x166px
    public CampSuppliesGUI(String title, BlockPos passedPos) {
        super(Text.of(title));
        address = title;
        pos = passedPos;
    }

    public CampSuppliesGUI(String title, Screen prev, BlockPos passedPos) {
        super(Text.of(title));
        address = title;
        prevScreen = prev;
        pos = passedPos;
    }

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
            default:
                close();
        }
    }

    //Don't pause or blur the screen
    @Override
    public boolean shouldPause() {return false;}
    @Override
    protected void applyBlur(float delta) {}

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        drawBackground(context, delta, mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);
    }

    private void openHome() {
        ButtonWidget closeButton = ButtonWidget.builder(Text.of("Close"), (btn) -> {
            this.close();
        }).dimensions(super.width / 2 - 116, super.height / 2 + 52 - 24, 232, 48).build();

        ButtonWidget upgradesButton = ButtonWidget.builder(Text.of("Manage Upgrades"), (btn) -> {
            switchScreen("upgrades", this);
        }).dimensions(super.width / 2 - 116, super.height / 2 - 24, 232, 48).build();

        ButtonWidget structureMenuButton = ButtonWidget.builder(Text.of("View Structure List"), (btn) -> {
            switchScreen("structureList", this);
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

    private void openStructureList() {
        ButtonWidget newStructureButton = ButtonWidget.builder(Text.of("New Structure"), (btn) -> {
            switchScreen("structureCreator", this);
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
        ClientPlayNetworking.send(new QueryStructuresPayload());
        structureListLoadingText = new TextWidget(
                (super.height / 2 - backgroundHeight / 2) + 5,
                (super.width / 2 - backgroundWidth / 2) + 5,
                backgroundWidth / 2,
                backgroundHeight - 10,
                Text.of("Fetching saved structures..."),
                textRenderer);
        this.addDrawableChild(structureListLoadingText);

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

        ButtonWidget saveButton = ButtonWidget.builder(Text.of("Save"), (btn) -> {
            //TODO implement button func
            sendSavePacket(pos, Identifier.of(NomadsCamps.MOD_ID, MinecraftClient.getInstance().player.getNameForScoreboard() + "/" + nameField.getText().toLowerCase()), new BlockPos(0, -60, 0), new Vec3i(3, 3, 3));
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

    private void openClaimScreen() {
        //TODO make this screen prettier
        ButtonWidget claimOwnershipButton = ButtonWidget.builder(Text.of("Claim Ownership"), (btn) -> {

            sendOwnershipPacket();

        }).dimensions(
                (super.width / 2) - 60,
                (super.height / 3) - 10,
                120,
                20
        ).build();

        this.addDrawableChild(claimOwnershipButton);
    }

    public void receiveStructures(ArrayList<String> names) {
        savedStructures = names;

        if(address.equals("structureList")) {
            structureListWidget structureList = new structureListWidget(client,
                    backgroundWidth / 2,
                    backgroundHeight - 10,
                    (super.width / 2 - backgroundWidth / 2) + 5,
                    (super.height / 2 - backgroundHeight / 2) + 5
            );
            this.addDrawableChild(structureList);
            this.remove(structureListLoadingText);
        }
    }

    @Override
    public void close() {this.client.setScreen(prevScreen);}

    protected void switchScreen(String title, @Nullable Screen parent) {
        if(parent == null) {
            this.client.setScreen((new CampSuppliesGUI(title, pos)));
        } else {
            this.client.setScreen(new CampSuppliesGUI(title, parent, pos));
        }
    }

    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(BACKGROUND_TEXTURE, i, j, 0, 0.0F, 0.0F, this.backgroundWidth, this.backgroundHeight, 256, 256);
    }

    private boolean sendSavePacket(BlockPos suppliesPos, Identifier structureName, BlockPos origin, Vec3i size) {
        ClientPlayNetworking.send(new CampBlockSavePayload(suppliesPos, structureName, origin, Vec3d.of(size)));
        return true;
    }

    private boolean sendOwnershipPacket() {
        ClientPlayNetworking.send(new CampBlockSetOwnerPayload(pos));
        return true;
    }


    //-----------------------------------------------------------------------------------\\

    class structureListWidget extends ElementListWidget<structureListWidget.structureEntry> {
        structureListWidget(MinecraftClient client, int width, int height, int x, int y) {
            super(client, width + 4, height, y, 40);
            this.setX(x);

            Collator collator = Collator.getInstance(Locale.getDefault()); //What is this??? Its something used to sort the entries

            //Add entries to the list using this.addEntry(theEntryToAdd)
            for(String s : CampSuppliesGUI.savedStructures) {
                this.addEntry(new structureEntry(s, width - 8, 40, this.getX()));
            }
        }

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


        class structureEntry extends ElementListWidget.Entry<structureEntry> {
            private int x;

            //Icon?
            private String name;
            private ButtonWidget nameButton;
            //Edit button?

            private ArrayList<ButtonWidget> children = new ArrayList<ButtonWidget>();

            public structureEntry(String structureName, int width, int height, int x) {
                this.name = structureName;
                this.x = x;

                nameButton = ButtonWidget.builder(Text.of(structureName), (btn) -> {
                    //Whatever the button will do
                }).dimensions(0, 0, width, height).build();
                children.add(nameButton);

                //other buttons...
            }

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
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                structureListWidget.this.setSelected(this);
                return super.mouseClicked(mouseX, mouseY, button);
            }
        }
    }
}
