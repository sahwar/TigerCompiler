package IR;

import java.util.ArrayList;

public class ret extends jump {

    Operand retVal;

    public ret(Operand retVal){
        this.retVal = retVal;

    }

    public Var def(){
        return (Var)retVal;
    }

    public ArrayList<Var> use(){
        ArrayList<Var> uses = new ArrayList<>();
        return uses;
    }

    public String toString(){
        return  "return, " + (retVal == null ? "" : retVal) + ", ,";
    }
    public void accept(IRVisitor v) { v.visit(this); }
}
