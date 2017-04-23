package org.dynmap.blockscan.statehandlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableMap;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;

/**
 * This state handler is used for blocks which preserve a simple 1-1 correlation between 
 * metadata values and block state, except for also having a 'short' parameter (referring to 
 * the piston being drawn short versus not, as for piston base blocks when the extension is open)
 * 
 * @author Mike Primm
 */
public class PistonMetadtataStateHandler implements IStateHandlerFactory {
    /**
     * This method is used to examining the BlockStateContainer of a block to determine if the state mapper can handle the given block
     * @param block - Block object
     * @param bsc - BlockStateContainer object
     * @returns IStateHandler if the handler factory believes it can handle this block type, null otherwise
     */
    public IStateHandler canHandleBlockState(Block block, BlockStateContainer bsc) {
        IProperty<?> shortp = bsc.getProperty("short");
        if (shortp == null) {
            return null;
        }
        List<IBlockState> state = bsc.getValidStates();
        // More states than metadata values - cannot handle this
        if (state.size() > 32) {
            return null;
        }
        IBlockState[] metavalues = new IBlockState[16];
        IBlockState[] shortpmetavalues = new IBlockState[16];
        for (IBlockState s : state) {
            int meta = block.getMetaFromState(s);   // Lookup meta for this state
            String is_shortp = s.getValue(shortp).toString();
            // If out of range, or duplicate, we cannot handle
            if ((meta < 0) || (meta > 15)) {
                return null;
            }
            else if (is_shortp.equals("false")) {
                if (metavalues[meta] != null) {
                    return null;
                }
                else {
                    metavalues[meta] = s;
                }
            }
            else {
                if (shortpmetavalues[meta] != null) {
                    return null;
                }
                else {
                    shortpmetavalues[meta] = s;
                }
            }
        }
        // Fill in any missing metadata with default state
        for (int i = 0; i < metavalues.length; i++) {
            if (metavalues[i] == null) {
                metavalues[i] = block.getDefaultState();
            }
            if (shortpmetavalues[i] == null) {
                shortpmetavalues[i] = block.getDefaultState();
            }
        }
        // Return handler object
        return new OurHandler(metavalues, shortpmetavalues);
    }

    class OurHandler implements IStateHandler {
        private String[] string_values;
        private Map<String, String>[] map_values;
        
        OurHandler(IBlockState[] states, IBlockState[] shortpstates) {
            string_values = new String[32];
            map_values = new Map[32];
            // Handle short states first
            for (int i = 0; i < 16; i++) {
                IBlockState bs = shortpstates[i];
                HashMap<String, String> m = new HashMap<String,String>();
                StringBuilder sb = new StringBuilder();
                for (Entry<IProperty<?>, Comparable<?>> p : bs.getProperties().entrySet()) {
                    if (sb.length() > 0) sb.append(",");
                    sb.append(p.getKey().getName()).append("=").append(p.getValue().toString());
                    m.put(p.getKey().getName(), p.getValue().toString());
                }
                map_values[i] = m;
                string_values[i] = sb.toString();
            }
            // Handle unshort states second
            for (int i = 0; i < 16; i++) {
                IBlockState bs = states[i];
                HashMap<String, String> m = new HashMap<String,String>();
                StringBuilder sb = new StringBuilder();
                for (Entry<IProperty<?>, Comparable<?>> p : bs.getProperties().entrySet()) {
                    if (sb.length() > 0) sb.append(",");
                    sb.append(p.getKey().getName()).append("=").append(p.getValue().toString());
                    m.put(p.getKey().getName(), p.getValue().toString());
                }
                map_values[16+i] = m;
                string_values[16+i] = sb.toString();
            }
        }
        @Override
        public String getName() {
            return "PistonMetadataState";
        }
        @Override
        public int getBlockStateIndex(int blockid, int blockmeta) {
            return blockmeta; //TODO: we need the logic for looking for whether short (extension present) or not
        }
        @Override
        public Map<String, String>[] getBlockStateValueMaps() {
            return map_values;
        }
        @Override
        public String[] getBlockStateValues() {
            return string_values;
        }
    }
}
