package eu.pb4.mapcanvas.api.core;

import eu.pb4.mapcanvas.api.utils.CanvasUtils;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Minimal implementation of DrawableCanvas, used to store images independent of maps
 */
public final class CanvasImage implements DrawableCanvas, IconContainer {
    private final int width;
    private final int height;
    private final byte[] data;
    private final Set<ImageCanvasIcon> icons = new HashSet<>();
    private int iconId = 0;

    public CanvasImage(int width, int height) {
        this(width, height, new byte[width * height]);
    }

    protected CanvasImage(int width, int height, byte[] data) {
        this.data = data;
        this.width = width;
        this.height = height;
    }

    public static CanvasImage from(BufferedImage image) {
        var width = image.getWidth();
        var height = image.getHeight();

        var canvas = new CanvasImage(image.getWidth(), image.getHeight());


        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                canvas.setRaw(x, y, CanvasUtils.findClosestRawColorARGB(image.getRGB(x, y)));
            }
        }

        return canvas;
    }

    public static CanvasImage from(MapState state) {
        var canvas = new CanvasImage(CanvasUtils.MAP_DATA_SIZE, CanvasUtils.MAP_DATA_SIZE);

        for (int x = 0; x < CanvasUtils.MAP_DATA_SIZE; x++) {
            for (int y = 0; y < CanvasUtils.MAP_DATA_SIZE; y++) {
                canvas.setRaw(x, y, state.colors[x + y * CanvasUtils.MAP_DATA_SIZE]);
            }
        }

        for (var icon : state.getIcons()) {
            canvas.createIcon(icon.getType(), icon.getX() - CanvasUtils.MAP_DATA_SIZE, icon.getZ() - CanvasUtils.MAP_DATA_SIZE, icon.getRotation(), icon.getText());
        }

        return canvas;
    }

    @Nullable
    public static CanvasImage from(NbtCompound nbt) {
        try {
            if (nbt.getString("DataType").equals("MapCanvasImage") && nbt.getInt("Version") != 0) {
                final int width = nbt.getInt("Width");
                final int height = nbt.getInt("Height");

                var data = nbt.getByteArray("Data");

                var image = new CanvasImage(width, height, Arrays.copyOf(data, data.length));

                for (var tmpIcon : nbt.getList("Icons", NbtElement.COMPOUND_TYPE)) {
                    var icon = (NbtCompound) tmpIcon;
                    image.createIcon(
                            MapIcon.Type.byId(icon.getByte("Type")),
                            icon.getBoolean("Vis"),
                            icon.getInt("X"),
                            icon.getInt("Y"),
                            icon.getByte("Rot"),
                            icon.contains("Text", NbtElement.STRING_TYPE)
                                    ? Text.Serializer.fromLenientJson(icon.getString("Text"))
                                    : null
                    );
                }

                return image;
            } else if (nbt.contains("DataVersion", NbtElement.INT_TYPE) && nbt.contains("data", NbtElement.COMPOUND_TYPE)) {
                return from(MapState.fromNbt(nbt));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public byte getRaw(int x, int y) {
        if (x >= this.width || y >= this.height || x < 0 || y < 0) {
            return 0;
        }

        return this.data[x + y * this.width];
    }

    @Override
    public void setRaw(int x, int y, byte color) {
        if (x >= this.width || y >= this.height || x < 0 || y < 0) {
            return;
        }

        this.data[x + y * this.width] = color;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public Collection<CanvasIcon> getIcons() {
        return Collections.unmodifiableCollection(this.icons);
    }

    @Override
    public CanvasIcon createIcon() {
        var icon = new ImageCanvasIcon(this.iconId++);
        this.icons.add(icon);
        return icon;
    }

    @Override
    public CanvasIcon createIcon(MapIcon.Type type, boolean visible, int x, int y, byte rotation, @Nullable Text text) {
        var icon = new ImageCanvasIcon(this.iconId++, visible, type, x, y, rotation, text);
        this.icons.add(icon);
        return icon;
    }

    @Override
    public void removeIcon(CanvasIcon icon) {
        this.icons.remove(icon);
    }

    private final class ImageCanvasIcon implements CanvasIcon {
        public final int id;
        private Text text;
        private MapIcon.Type type = MapIcon.Type.PLAYER;
        private int x = 0;
        private int y = 0;
        private byte rotation = 0;
        private boolean isVisible = false;

        protected ImageCanvasIcon(int id) {
            this.id = id;
        }

        protected ImageCanvasIcon(int id, boolean visible, MapIcon.Type type, int x, int y, byte rotation, @Nullable Text text) {
            this.id = id;
            this.type = type;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.text = text;
            this.isVisible = visible;
        }

        @Override
        public MapIcon.Type getType() {
            return this.type;
        }

        @Override
        public void setType(MapIcon.Type type) {
            if (this.type != type) {
                this.type = type;
            }
        }

        @Override
        public int getX() {
            return this.x;
        }

        @Override
        public int getY() {
            return this.y;
        }

        @Override
        public byte getRotation() {
            return this.rotation;
        }

        @Override
        public void move(int x, int y, byte rotation) {
            if (this.x != x || this.y != y || this.rotation != rotation) {
                this.x = x;
                this.y = y;
                this.rotation = rotation;
            }
        }

        @Override
        public boolean isVisible() {
            return this.isVisible;
        }

        @Override
        public void setVisibility(boolean visibility) {
            if (this.isVisible != visibility) {
                this.isVisible = visibility;
            }
        }

        @Override
        public Text getText() {
            return this.text;
        }

        @Override
        public void setText(@Nullable Text text) {
            if (!Objects.equals(this.text, text)) {
                this.text = text;
            }
        }

        @Override
        public DrawableCanvas getOwningCanvas() {
            return CanvasImage.this;
        }
    }
}
