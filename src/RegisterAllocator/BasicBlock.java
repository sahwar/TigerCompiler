package RegisterAllocator;

import IR.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Function;

import Util.DiNode;

// A Basic block is a stream of instructions that always execute together
// A Basic block has a start label, however some basic blocks have no associated label
// e.g. an instruciton immediately following a goto instruction

public class BasicBlock extends DiNode {


    private ArrayList<IR> instructions = new ArrayList<>();


    private ArrayList<HashSet<Var>> live = new ArrayList<>();


    private boolean liveness_initialized = false;
    public void initLiveness(){
        if (!liveness_initialized){
            for (int i = 0; i <= size(); i++){
                live.add(new HashSet<Var>());
            }
            liveness_initialized = true;
        }
    }
    public boolean makeChanges(){
        boolean changes = false;
        for (int i = 0; i < size(); i++){
            HashSet<Var> additions = new HashSet<>();
            additions.addAll(out(i));
            additions.remove(getInstruction(i).def());
            additions.addAll(getInstruction(i).use());
            if (!in(i).containsAll(additions)){
                changes = true;
                in(i).addAll(additions);
            }
        }
        return changes;
    }

    // Stores the last use of a var in the block
    private HashMap<Var, Integer> lastUse = new HashMap<>();

    // Stores the last definition of a var in the block
    private HashMap<Var, Integer> lastDef = new HashMap<>();

    // Variable definitions that are inherited from ancestors
    public HashSet<Var> defsIn = new HashSet<>();

    // Variable definitions that are used by the successors
    public HashSet<Var> defsOut = new HashSet<>();

    private boolean builtLiveness = false;

    public FunctionPrologue functionPrologue = null;

    // Used to detect cycles in loops and break out of them
    // All the blocks in a cycle should have set a variable to
    // live in all instructions by the end
    private boolean cycleDetect = false;

    public void buildLivenessGlobal() {
        if (builtLiveness) {
            return;
        }
        builtLiveness = true;
        // Function prologue adds argument definitions
        if (functionPrologue != null) {
            for (Var arg : functionPrologue.arguments) {
                lastDef.put(arg, 0);
                lastUse.put(arg, 0);
                defsOut.add(arg);
                continue;
            }
        }
        // Set of variables that must be live the entire block
        // because previous iterations of the loop are referenced
        HashSet<Var> loopVars = new HashSet<>();
        for (int i = 0; i < size(); i++) {
            if (getInstruction(i) instanceof FunctionPrologue) {
            }
            Var def = getInstruction(i).def();
            ArrayList<Var> uses = getInstruction(i).use();
            // Start a new live range
            if (def != null) {
                lastDef.put(def, i);
                lastUse.put(def, i);
                live.get(i).add(def);
            }
            // If there is a use extend from the last use
            for (Var use : uses) {
                int start = 0;
                if (lastDef.containsKey(use)) {
                    start = lastDef.get(use).intValue() + 1;
                } else {
                    // No previous blocks to look for definition means an error
                    if (pred.isEmpty()) {
                        System.out.println("Error: " + use.name + " is used without initialization");
                        System.exit(1);
                    }
                    // No definition in this block so look in previous blocks
                    cycleDetect = true;
                    for (DiNode p : pred) {
                        if (((BasicBlock)p).useVarBySuccessor(use)) {
                            // The whole block becomes live
                            loopVars.add(use);
                        }
                    }
                    cycleDetect = false;
                    defsIn.add(use);
                }
                for (int j = start; j <= i; j++) {
                    live.get(i).add(use);
                }
                lastUse.put(def, i);
            }
        }
        for (Var c : loopVars) {
            defsIn.add(c);
            defsOut.add(c);
            for (int i = 0; i < size(); i++) {
                live.get(i).add(c);
            }
            lastUse.put(c, size() - 1);
        }
    }

    // Called when a successor uses a var
    // Returns if there is a cycle (and the var has to be marked live the entire block
    public boolean useVarBySuccessor(Var var) {
        // Leave if we are in a cycle
        if (cycleDetect) {
            return true;
        }
        // Make sure our liveness is calculated
        buildLivenessGlobal();

        // Find the last use
        if (lastUse.containsKey(var)) {
            // Simply extend the liveness to the end of the block
            for (int i = lastUse.get(var).intValue() + 1; i < size(); i++) {
                live.get(i).add(var);
            }
        } else {
            if (pred.isEmpty()) {
                System.out.println("Error: " + var.name + " is used without initialization");
                System.exit(1);
            }
            // No definition in this block so look in previous blocks
            cycleDetect = true;
            for (DiNode p : pred) {
                if (((BasicBlock)p).useVarBySuccessor(var)) {
                    // The whole block becomes live
                    for (int i = 0; i < size(); i++) {
                        live.get(i).add(var);
                    }
                    defsIn.add(var);
                    lastUse.put(var, size() - 1);
                    defsOut.add(var);
                    return true;
                }
            }
            cycleDetect = false;
            defsIn.add(var);

            // The whole block becomes live
            for (int i = 0; i < size(); i++) {
                live.get(i).add(var);
            }
        }
        // Last use is at end of the block now
        lastUse.put(var, size() - 1);
        defsOut.add(var);
        return false;
    }

    public void calcLiveness(){

        initLiveness();

        boolean changes;
        do{
            changes = makeChanges();

        } while (changes);
    }

    // get live variables before instruction i
    public HashSet<Var> in(int i){
        return live.get(i);
    }
    // get live variables after instruction i
    public HashSet<Var> out(int i){
        return live.get(i+1);
    }
    // get in live variables of whole block (equal to liveness of first instruction in)
    public HashSet<Var> blockIn(){
        if (size() == 0) return null;
        else return live.get(0);
    }
    // get out live variables of whole block (equal to liveness of last instruction out)
    public HashSet<Var> blockOut(){
        if (size() == 0) return null;
        else return live.get(size());
    }

    // the index into the original instruction stream where the BB starts
    public int startIndex;


    public Label startLabel;

    public BasicBlock(int startIndex){
        this.startIndex = startIndex;
    }
    public BasicBlock(Label startLabel, int startIndex){
        this.startLabel = startLabel;
        this.startIndex = startIndex;
    }

    public void addInstruction(IR instruction){
        instructions.add(instruction);
    }

    public IR getInstruction(int i){
        return instructions.get(i);
    }

    public ArrayList<IR> instructions(){
        return instructions;
    }

    public IR lastInstruction(){
        if (instructions.size() == 0) return null;
        else return instructions.get(instructions.size() - 1);
    }

    public int size(){
        return instructions.size();
    }

    public Var def(int i){
        if (i < 0) return null;
        return instructions.get(i).def();
    }

    public ArrayList<Var> use(int i){
        return instructions.get(i).use();
    }

    public String toString(){
        String out = "BB[";
        // Dummy entry/exit block
        if (size() == 0){
            out += "empty";
        }
        // Regular block
        else{
            out += "line  " + startIndex + "-" + (startIndex + size() - 1);
        }
        out += "]";
        out += ((startLabel == null) ? "unnamed" : startLabel);
        return out;
    }

}
