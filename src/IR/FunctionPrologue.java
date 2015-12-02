package IR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

// Stores critical information about a function for use in codegen
public class FunctionPrologue extends instruction {
    public ArrayList<NamedVar> arguments = new ArrayList<>();
    public HashMap<String, Integer> argumentOffsetMap = new HashMap<>();
    public HashMap<String, Integer> temporaryOffsetMap = new HashMap<>();
    public HashMap<Register.Reg, Integer> usedRegsOffsetMap = new HashMap<>();
    public int argumentCount = 0;
    public int temporaryCount = 0;
    public int usedRegsCount = 0;

    public FunctionLabel epilogueLabel = null;

    public void addOperand(Operand var) {
        if (var instanceof TempVar && ((TempVar) var).isLocal) {
            if (!temporaryOffsetMap.containsKey(((TempVar)var).name)) {
                temporaryOffsetMap.put(((TempVar)var).name, temporaryCount);
                temporaryCount++;
            }
        }
        if (var instanceof Register) {
            if (!usedRegsOffsetMap.containsKey(((Register) var).register)) {
                usedRegsOffsetMap.put(((Register) var).register, usedRegsCount);
                usedRegsCount++;
            }
        }
    }

    public void buildArgumentMap() {
        for (NamedVar v : arguments) {
            argumentOffsetMap.put(v.name, argumentCount);
            argumentCount++;
        }
    }

    public Var def() {
        return null;
    }

    public ArrayList<Var> use() {
        return new ArrayList<>();
    }

    public void accept(IRVisitor v) {
        v.visit(this);
    }

    public String toString() {
        return "";
    }
}
