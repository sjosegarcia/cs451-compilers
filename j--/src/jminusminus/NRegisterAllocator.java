// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import java.util.ArrayList;
import java.util.BitSet;

import static jminusminus.NPhysicalRegister.*;

/**
 * The abstract base class for a register allocator that maps virtual registers (from LIR code) to
 * physical registers on the target (MIPS) machine.
 */
public abstract class NRegisterAllocator {
    /**
     * The control flow graph for a method.
     */
    protected NControlFlowGraph cfg;

    /**
     * Constructs an NRegisterAllocator object.
     *
     * @param cfg control flow graph for a method.
     */
    protected NRegisterAllocator(NControlFlowGraph cfg) {
        this.cfg = cfg;
        this.cfg.intervals = new ArrayList<NInterval>();
        for (int i = 0; i < cfg.registers.size(); i++) {
            this.cfg.intervals.add(new NInterval(i, cfg));
        }
        this.cfg.maxIntervals = this.cfg.intervals.size();
    }

    /**
     * Builds the intervals for a control flow graph.
     */
    protected void buildIntervals() {
        this.computeLocalLiveSets();
        this.computeGlobalLiveSets();
        for (int i = cfg.basicBlocks.size() - 1; i >= 0; i--) {
            NBasicBlock currBlock = cfg.basicBlocks.get(i);
            if (currBlock.lir.size() == 0) {
                continue;
            }
            int blockStart = currBlock.lir.get(0).id;
            int blockEnd = currBlock.lir.get(currBlock.lir.size() - 1).id;
            BitSet liveOut = currBlock.liveOut;
            for (int idx = liveOut.nextSetBit(0); idx >= 0; idx = liveOut.nextSetBit(idx + 1)) {
                cfg.intervals.get(idx).addOrExtendNRange(new NRange(blockStart, blockEnd));
            }
            for (int j = currBlock.lir.size() - 1; j >= 0; j--) {
                int currLIRid = currBlock.lir.get(j).id;
                NRegister output = currBlock.lir.get(j).write;
                if (output != null) {
                    cfg.intervals.get(output.number).newFirstRangeStart(currLIRid);
                    cfg.intervals.get(output.number).addUsePosition(currLIRid,
                            InstructionType.write);
                }
                ArrayList<NRegister> inputs = currBlock.lir.get(j).reads;
                for (NRegister reg : inputs) {
                    cfg.intervals.get(reg.number).addOrExtendNRange(new NRange(blockStart,
                            currLIRid));
                    cfg.intervals.get(reg.number).addUsePosition(currLIRid, InstructionType.read);
                }
            }
        }
    }

    /**
     * Preprocesses information needed for naive, linear, and graph register allocation schemes.
     */
    protected void preprocess() {
        // Allocate any fixed registers (a0, a1, a2, a3 and v0) that were assigned during generation
        // phase to the appropriate interval.
        for (int i = 0; i < 32; i++) {
            if (cfg.registers.get(i) != null) {
                cfg.intervals.get(i).pRegister = ((NPhysicalRegister) cfg.registers.get(i));
            }
        }

        // Assign stack offset (relative to fp) for formal parameters fourth and above, and stack
        // offset (relative to sp) for arguments fourth or above.
        for (NBasicBlock block : cfg.basicBlocks) {
            for (NLIRInstruction lir : block.lir) {
                if (lir instanceof NLIRLoadLocal) {
                    NLIRLoadLocal loadLocal = (NLIRLoadLocal) lir;
                    if (loadLocal.getLocal() >= 4) {
                        NInterval interval = cfg.intervals.get(((NVirtualRegister)
                                loadLocal.write).number());
                        interval.spill = true;
                        interval.offset = loadLocal.getLocal() - 3;
                        interval.offsetFrom = OffsetFrom.FP;
                    }
                }
            }
        }
    }

    /**
     * The work horse that does the allocation, implemented in the sub-classes of this class.
     */
    public abstract void allocation();

    /**
     * Prints the local and global live sets to standard output.
     *
     * @param p for pretty printing with indentation.
     */
    public void writeLivenessInfoToStdOut(PrettyPrinter p) {
        p.indentRight();
        p.printf("[[ LOCAL LIVENESS INFORMATION ]]\n\n");
        for (NBasicBlock block : cfg.basicBlocks) {
            p.printf("%s\n", block.id());
            String s = "";
            BitSet use = block.liveUse;
            for (int i = use.nextSetBit(0); i >= 0; i = use.nextSetBit(i + 1)) {
                if (i < 32) {
                    s += regInfo[i] + " ";
                } else {
                    s += "V" + i + " ";
                }
            }
            p.println("liveUse: " + s);
            s = "";
            BitSet def = block.liveDef;
            for (int i = def.nextSetBit(0); i >= 0; i = def.nextSetBit(i + 1)) {
                if (i < 32) {
                    s += regInfo[i] + " ";
                } else {
                    s += "V" + i + " ";
                }
            }
            p.printf("liveDef: %s\n\n", s);
        }
        p.printf("[[ GLOBAL LIVENESS INFORMATION ]]\n\n");
        for (int idx = cfg.basicBlocks.size() - 1; idx >= 0; idx--) {
            p.printf("%s\n", cfg.basicBlocks.get(idx).id());
            String s = "";
            BitSet in = cfg.basicBlocks.get(idx).liveIn;
            for (int i = in.nextSetBit(0); i >= 0; i = in.nextSetBit(i + 1)) {
                if (i < 32) {
                    s += regInfo[i] + " ";
                } else {
                    s += "V" + i + " ";
                }
            }
            p.println("liveIn: " + s);
            s = "";
            BitSet out = cfg.basicBlocks.get(idx).liveOut;
            for (int i = out.nextSetBit(0); i >= 0; i = out.nextSetBit(i + 1)) {
                if (i < 32) {
                    s += regInfo[i] + " ";
                } else {
                    s += "V" + i + " ";
                }
            }
            p.printf("liveOut: %s\n\n", s);
        }
        p.indentLeft();
    }

    // Iterates through a list of basic blocks in order, and sets their liveUse and liveDef
    // fields to the appropriate virtual registers.
    private void computeLocalLiveSets() {
        for (NBasicBlock block : cfg.basicBlocks) {
            block.liveUse = new BitSet(cfg.registers.size());
            block.liveDef = new BitSet(cfg.registers.size());
            for (NLIRInstruction inst : block.lir) {
                for (NRegister reg : inst.reads) {
                    if (!(block.liveDef.get(reg.number()))) {
                        block.liveUse.set(reg.number());
                    }
                }
                if (inst.write != null) {
                    block.liveDef.set(inst.write.number());
                }
            }
        }
    }

    // Iterates through a list of basic blocks in reverse order, and sets their lliveIn and
    // liveOut fields to reflect global use-def information.
    private void computeGlobalLiveSets() {
        boolean changed = false;
        for (NBasicBlock b : cfg.basicBlocks) {
            b.liveOut = new BitSet(cfg.registers.size());
        }
        do {
            changed = false;
            for (int i = cfg.basicBlocks.size() - 1; i >= 0; i--) {
                NBasicBlock currBlock = cfg.basicBlocks.get(i);
                BitSet newLiveOut = new BitSet(cfg.registers.size());
                for (NBasicBlock successor : currBlock.successors) {
                    newLiveOut.or(successor.liveIn);
                }
                if (!currBlock.liveOut.equals(newLiveOut)) {
                    currBlock.liveOut = newLiveOut;
                    changed = true;
                }
                currBlock.liveIn = (BitSet) currBlock.liveOut.clone();
                currBlock.liveIn.andNot(currBlock.liveDef);
                currBlock.liveIn.or(currBlock.liveUse);
            }
        } while (changed);
    }
}
