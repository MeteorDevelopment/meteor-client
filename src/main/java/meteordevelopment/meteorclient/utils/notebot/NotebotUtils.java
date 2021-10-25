/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.notebot;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.enums.Instrument;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class NotebotUtils {
    public static boolean isValidInstrument(BlockPos pos, InstrumentType instrument) {
        switch (instrument) {
            case Any:
                return true;

            case NotDrums: {
                BlockState state = mc.world.getBlockState(pos);
                if (state.getBlock() == Blocks.NOTE_BLOCK) {
                    Instrument instr = state.get(NoteBlock.INSTRUMENT);
                    if (instr == Instrument.BASEDRUM)
                        return false;
                    else if (instr == Instrument.HAT)
                        return false;
                    else if (instr == Instrument.SNARE)
                        return false;
                    else return instr != Instrument.COW_BELL;
                } else {
                    BlockState block = mc.world.getBlockState(pos.down());
                    if (block.getMaterial() == Material.AGGREGATE)
                        return false;
                    else if (block.getMaterial() == Material.GLASS)
                        return false;
                    else if (block.getMaterial() == Material.STONE)
                        return false;
                    else return block.getBlock() != Blocks.IRON_BLOCK;
                }
            }

            case Harp: {
                BlockState state = mc.world.getBlockState(pos);
                if (state.getBlock() == Blocks.NOTE_BLOCK) {
                    return (state.get(NoteBlock.INSTRUMENT) == Instrument.HARP);
                } else {
                    BlockState block = mc.world.getBlockState(pos.down());
                    if (block.getMaterial() == Material.WOOD)
                        return false;
                    else if (block.getMaterial() == Material.AGGREGATE)
                        return false;
                    else if (block.getMaterial() == Material.GLASS)
                        return false;
                    else if (block.getMaterial() == Material.STONE)
                        return false;
                    else if (block.getBlock() == Blocks.GOLD_BLOCK)
                        return false;
                    else if (block.getBlock() == Blocks.CLAY)
                        return false;
                    else if (block.getBlock() == Blocks.PACKED_ICE)
                        return false;
                    else if (block.getMaterial() == Material.WOOL)
                        return false;
                    else if (block.getBlock() == Blocks.BONE_BLOCK)
                        return false;
                    else if (block.getBlock() == Blocks.IRON_BLOCK)
                        return false;
                    else if (block.getBlock() == Blocks.SOUL_SAND)
                        return false;
                    else if (block.getBlock() == Blocks.PUMPKIN)
                        return false;
                    else if (block.getBlock() == Blocks.EMERALD_BLOCK)
                        return false;
                    else if (block.getBlock() == Blocks.HAY_BLOCK)
                        return false;
                    else return block.getBlock() != Blocks.GLOWSTONE;
                }
            }
            case Banjo: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getBlock() == Blocks.HAY_BLOCK);
            }
            case Bass: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getMaterial() == Material.WOOD);
            }
            case Bells: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getBlock() == Blocks.GOLD_BLOCK);
            }
            case Bit: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getBlock() == Blocks.EMERALD_BLOCK);
            }
            case Chimes: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getBlock() == Blocks.PACKED_ICE);
            }
            case CowBell: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getBlock() == Blocks.SOUL_SAND);
            }
            case Didgeridoo: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getBlock() == Blocks.PUMPKIN);
            }
            case Flute: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getBlock() == Blocks.CLAY);
            }
            case Guitar: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getMaterial() == Material.WOOL);
            }
            case IronXylophone: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getBlock() == Blocks.IRON_BLOCK);
            }
            case Pling: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getBlock() == Blocks.GLOWSTONE);
            }
            case Xylophone: {
                BlockState block = mc.world.getBlockState(pos.down());
                return (block.getBlock() == Blocks.BONE_BLOCK);
            }
            default:
                return false;
        }

    }

    public static boolean isValidInstrumentNbsFile(byte type, InstrumentType instrument) {
        switch (instrument) {
            case Any:
                return true;

            case NotDrums: {
                if (type == 2) return false; //basedrum
                else if (type == 3) return false; //snare
                else return type != 4; //hat
            }

            case Harp:
                return (type == 0);
            case Bass:
                return (type == 1);
            case Bells:
                return (type == 7);
            case Flute:
                return (type == 6);
            case Chimes:
                return (type == 8);
            case Guitar:
                return (type == 5);
            case Xylophone:
                return (type == 9);
            case IronXylophone:
                return (type == 10);
            case CowBell:
                return (type == 11);
            case Didgeridoo:
                return (type == 12);
            case Bit:
                return (type == 13);
            case Banjo:
                return (type == 14);
            case Pling:
                return (type == 15);
            default:
                return true;
        }
    }

    public static boolean isValidIntrumentTextFile(int type, InstrumentType instrument) {
        switch (instrument) {
            case Any:
                return true;

            case NotDrums: {
                if (type == 1)
                    return false; // basedrum
                else if (type == 2)
                    return false; // snare
                else if (type == 3)
                    return false; // hat
                else if (type == 11)
                    return false; // cow_bell
            }

            case Harp:
                return (type == 0);
            case Bass:
                return (type == 4);
            case Bells:
                return (type == 6);
            case Flute:
                return (type == 5);
            case Chimes:
                return (type == 8);
            case Guitar:
                return (type == 7);
            case Xylophone:
                return (type == 9);
            case IronXylophone:
                return (type == 10);
            case CowBell:
                return (type == 11);
            case Didgeridoo:
                return (type == 12);
            case Bit:
                return (type == 13);
            case Banjo:
                return (type == 14);
            case Pling:
                return (type == 15);
            default:
                return true;
        }
    }

    public enum InstrumentType {
        Any, NotDrums, Harp, Bass, Bells, Flute, Chimes, Guitar, Xylophone, IronXylophone, CowBell, Didgeridoo, Bit, Banjo, Pling
    }

    public static SoundEvent getInstrumentSound(InstrumentType instrument) {
        switch (instrument) {
            case Bass:
                return SoundEvents.BLOCK_NOTE_BLOCK_BASS;
            case Bells:
                return SoundEvents.BLOCK_NOTE_BLOCK_BELL;
            case Flute:
                return SoundEvents.BLOCK_NOTE_BLOCK_FLUTE;
            case Chimes:
                return SoundEvents.BLOCK_NOTE_BLOCK_CHIME;
            case Guitar:
                return SoundEvents.BLOCK_NOTE_BLOCK_GUITAR;
            case Xylophone:
                return SoundEvents.BLOCK_NOTE_BLOCK_XYLOPHONE;
            case IronXylophone:
                return SoundEvents.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE;
            case CowBell:
                return SoundEvents.BLOCK_NOTE_BLOCK_COW_BELL;
            case Didgeridoo:
                return SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO;
            case Bit:
                return SoundEvents.BLOCK_NOTE_BLOCK_BIT;
            case Banjo:
                return SoundEvents.BLOCK_NOTE_BLOCK_BANJO;
            case Pling:
                return SoundEvents.BLOCK_NOTE_BLOCK_PLING;
            default:
                return SoundEvents.BLOCK_NOTE_BLOCK_HARP;
        }
    }
}
