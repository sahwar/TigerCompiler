package IR;

import java.util.ArrayList;

public class intToFloat extends regularInstruction{

    public Operand src;
    public Operand dest;

    public intToFloat(Operand src, Operand dest){
        this.src = src;
        this.dest = dest;
    }

    public Var def(){
        if (dest instanceof Var) return (Var)dest;
        else return null;
    }

    public ArrayList<Var> use(){
        ArrayList<Var> uses = new ArrayList<>();
        if (src instanceof Var) uses.add((Var)src);
        return uses;
    }




    public String toString(){
        return "intToFloat, " + src + ", " + dest;
    }
    public void accept(IRVisitor v) { v.visit(this); }

}