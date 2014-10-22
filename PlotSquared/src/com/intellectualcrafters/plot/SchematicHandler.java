package com.intellectualcrafters.plot;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.intellectualcrafters.jnbt.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Created by Citymonstret on 2014-09-15.
 */
public class SchematicHandler {

	public boolean paste(Location location, Schematic schematic, Plot plot) {
		if (schematic == null) {
			PlotMain.sendConsoleSenderMessage("Schematic == null :|");
			return false;
		}
		try {
		    
		    Dimension demensions = schematic.getSchematicDimension();
	        
		    int WIDTH = demensions.getX();
	        int LENGTH = demensions.getZ();
	        int HEIGHT = demensions.getY();
		    
		    DataCollection[] blocks = schematic.getBlockCollection();
		    
		    
		    
		    Location l1 = PlotHelper.getPlotBottomLoc(plot.getWorld(), plot.getId());
		    
		    int sy = location.getWorld().getHighestBlockYAt(l1.getBlockX()+1, l1.getBlockZ()+1);
		    
		    l1 = l1.add(1, sy-1, 1);
		    
            World world = location.getWorld();
            
		    for (int x = 0; x < WIDTH; x++) {
	            for (int z = 0; z < LENGTH; z++) {
	                for (int y = 0; y < HEIGHT; y++) {
	                    int index = y * WIDTH * LENGTH + z * WIDTH + x;
	                    
	                    short id = blocks[index].getBlock();
	                    byte data = blocks[index].getData();
	                    
	                    Block block = world.getBlockAt(l1.getBlockX()+x, l1.getBlockY()+y, l1.getBlockZ()+z);
	                    
	                    PlotBlock plotblock = new PlotBlock(id, data);
	                    
	                    PlotHelper.setBlock(block, plotblock);
	                }
	            }
            }
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}

	public Schematic getSchematic(String name) {
		{
			File parent =
					new File(JavaPlugin.getPlugin(PlotMain.class).getDataFolder() + File.separator + "schematics");
			if (!parent.exists()) {
				parent.mkdir();
			}
		}
		File file =
				new File(JavaPlugin.getPlugin(PlotMain.class).getDataFolder() + File.separator + "schematics"
						+ File.separator + name + ".schematic");
		if (!file.exists()) {
			PlotMain.sendConsoleSenderMessage(file.toString() + " doesn't exist");
			return null;
		}

		Schematic schematic = null;
		try {
			InputStream iStream = new FileInputStream(file);
			NBTInputStream stream = new NBTInputStream(new GZIPInputStream(iStream));
			CompoundTag tag = (CompoundTag) stream.readTag();
			stream.close();
			Map<String, Tag> tagMap = tag.getValue();

			byte[] addId = new byte[0];
			if (tagMap.containsKey("AddBlocks")) {
				addId = ByteArrayTag.class.cast(tagMap.get("AddBlocks")).getValue();
			}
			short width = ShortTag.class.cast(tagMap.get("Width")).getValue();
			short length = ShortTag.class.cast(tagMap.get("Length")).getValue();
			short height = ShortTag.class.cast(tagMap.get("Height")).getValue();

			byte[] b = ByteArrayTag.class.cast(tagMap.get("Blocks")).getValue();
			byte[] d = ByteArrayTag.class.cast(tagMap.get("Data")).getValue();
			short[] blocks = new short[b.length];

			Dimension dimension = new Dimension(width, height, length);

			for (int index = 0; index < b.length; index++) {
				if ((index >> 1) >= addId.length) { // No corresponding
					// AddBlocks index
					blocks[index] = (short) (b[index] & 0xFF);
				}
				else {
					if ((index & 1) == 0) {
						blocks[index] = (short) (((addId[index >> 1] & 0x0F) << 8) + (b[index] & 0xFF));
					}
					else {
						blocks[index] = (short) (((addId[index >> 1] & 0xF0) << 4) + (b[index] & 0xFF));
					}
				}
			}

			DataCollection[] collection = new DataCollection[b.length];

			for (int x = 0; x < b.length; x++) {
				collection[x] = new DataCollection(blocks[x], d[x]);
			}

			schematic = new Schematic(collection, dimension, file);
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return schematic;
	}

	public static class Schematic {
		private DataCollection[] blockCollection;
		private Dimension schematicDimension;
		private File file;

		public Schematic(DataCollection[] blockCollection, Dimension schematicDimension, File file) {
			this.blockCollection = blockCollection;
			this.schematicDimension = schematicDimension;
			this.file = file;
		}

		public File getFile() {
			return this.file;
		}

		public Dimension getSchematicDimension() {
			return this.schematicDimension;
		}

		public DataCollection[] getBlockCollection() {
			return this.blockCollection;
		}
	}

	public class Dimension {
		private int x;
		private int y;
		private int z;

		public Dimension(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public int getX() {
			return this.x;
		}

		public int getY() {
			return this.y;
		}

		public int getZ() {
			return this.z;
		}
	}
	public boolean save(CompoundTag tag, String path) {
        try {
            OutputStream stream = new FileOutputStream(path);
            NBTOutputStream output = new NBTOutputStream(stream);
            output.writeTag(tag);
            output.close();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
	}
	
	public CompoundTag getCompoundTag(World world, Plot plot) {

	    // loading chunks
	    final Location pos1 = PlotHelper.getPlotBottomLoc(world, plot.id).add(1, 0, 1);
        final Location pos2 = PlotHelper.getPlotTopLoc(world, plot.id);
        for (int i = (pos1.getBlockX() / 16) * 16; i < (16 + ((pos2.getBlockX() / 16) * 16)); i += 16) {
            for (int j = (pos1.getBlockZ() / 16) * 16; j < (16 + ((pos2.getBlockZ() / 16) * 16)); j += 16) {
                Chunk chunk = world.getChunkAt(i, j);
                chunk.load(true);
            }
        }
        
        // TODO get blocks
        
        // save as a schematic
        int MAX_SIZE = Short.MAX_VALUE - Short.MIN_VALUE;
        
        return null;
        
//        int width = region.getWidth();
//        int height = region.getHeight();
//        int length = region.getLength();
//
//        if (width > MAX_SIZE) {
//            throw new IllegalArgumentException("Width of region too large for a .schematic");
//        }
//        if (height > MAX_SIZE) {
//            throw new IllegalArgumentException("Height of region too large for a .schematic");
//        }
//        if (length > MAX_SIZE) {
//            throw new IllegalArgumentException("Length of region too large for a .schematic");
//        }
//
//        // ====================================================================
//        // Metadata
//        // ====================================================================
//
//        HashMap<String, Tag> schematic = new HashMap<String, Tag>();
//        schematic.put("Width", new ShortTag("Width", (short) width));
//        schematic.put("Length", new ShortTag("Length", (short) length));
//        schematic.put("Height", new ShortTag("Height", (short) height));
//        schematic.put("Materials", new StringTag("Materials", "Alpha"));
//        schematic.put("WEOriginX", new IntTag("WEOriginX", min.getBlockX()));
//        schematic.put("WEOriginY", new IntTag("WEOriginY", min.getBlockY()));
//        schematic.put("WEOriginZ", new IntTag("WEOriginZ", min.getBlockZ()));
//        schematic.put("WEOffsetX", new IntTag("WEOffsetX", offset.getBlockX()));
//        schematic.put("WEOffsetY", new IntTag("WEOffsetY", offset.getBlockY()));
//        schematic.put("WEOffsetZ", new IntTag("WEOffsetZ", offset.getBlockZ()));
//
//        // ====================================================================
//        // Block handling
//        // ====================================================================
//
//        byte[] blocks = new byte[width * height * length];
//        byte[] addBlocks = null;
//        byte[] blockData = new byte[width * height * length];
//        List<Tag> tileEntities = new ArrayList<Tag>();
//
//        for (Vector point : region) {
//            Vector relative = point.subtract(min);
//            int x = relative.getBlockX();
//            int y = relative.getBlockY();
//            int z = relative.getBlockZ();
//
//            int index = y * width * length + z * width + x;
//            BaseBlock block = clipboard.getBlock(point);
//
//            // Save 4096 IDs in an AddBlocks section
//            if (block.getType() > 255) {
//                if (addBlocks == null) { // Lazily create section
//                    addBlocks = new byte[(blocks.length >> 1) + 1];
//                }
//
//                addBlocks[index >> 1] = (byte) (((index & 1) == 0) ?
//                        addBlocks[index >> 1] & 0xF0 | (block.getType() >> 8) & 0xF
//                        : addBlocks[index >> 1] & 0xF | ((block.getType() >> 8) & 0xF) << 4);
//            }
//
//            blocks[index] = (byte) block.getType();
//            blockData[index] = (byte) block.getData();
//
//            // Store TileEntity data
//            CompoundTag rawTag = block.getNbtData();
//            if (rawTag != null) {
//                Map<String, Tag> values = new HashMap<String, Tag>();
//                for (Entry<String, Tag> entry : rawTag.getValue().entrySet()) {
//                    values.put(entry.getKey(), entry.getValue());
//                }
//
//                values.put("id", new StringTag("id", block.getNbtId()));
//                values.put("x", new IntTag("x", x));
//                values.put("y", new IntTag("y", y));
//                values.put("z", new IntTag("z", z));
//
//                CompoundTag tileEntityTag = new CompoundTag("TileEntity", values);
//                tileEntities.add(tileEntityTag);
//            }
//        }
//
//        schematic.put("Blocks", new ByteArrayTag("Blocks", blocks));
//        schematic.put("Data", new ByteArrayTag("Data", blockData));
//        schematic.put("TileEntities", new ListTag("TileEntities", CompoundTag.class, tileEntities));
//
//        if (addBlocks != null) {
//            schematic.put("AddBlocks", new ByteArrayTag("AddBlocks", addBlocks));
//        }
//
//        // ====================================================================
//        // Entities
//        // ====================================================================
//
//        List<Tag> entities = new ArrayList<Tag>();
//        for (Entity entity : clipboard.getEntities()) {
//            BaseEntity state = entity.getState();
//
//            if (state != null) {
//                Map<String, Tag> values = new HashMap<String, Tag>();
//
//                // Put NBT provided data
//                CompoundTag rawTag = state.getNbtData();
//                if (rawTag != null) {
//                    values.putAll(rawTag.getValue());
//                }
//
//                // Store our location data, overwriting any
//                values.put("id", new StringTag("id", state.getTypeId()));
//                values.put("Pos", writeVector(entity.getLocation().toVector(), "Pos"));
//                values.put("Rotation", writeRotation(entity.getLocation(), "Rotation"));
//
//                CompoundTag entityTag = new CompoundTag("Entity", values);
//                entities.add(entityTag);
//            }
//        }
//
//        schematic.put("Entities", new ListTag("Entities", CompoundTag.class, entities));
//
//
//        CompoundTag schematicTag = new CompoundTag("Schematic", schematic);
//        return schematicTag;
    }

	public class DataCollection {
		private short block;
		private byte data;

		public DataCollection(short block, byte data) {
			this.block = block;
			this.data = data;
		}

		public short getBlock() {
			return this.block;
		}

		public byte getData() {
			return this.data;
		}
	}
}
