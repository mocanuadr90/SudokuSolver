package com.example.admin.sudokusolver;

/**
 * Created by admin on 7/7/2017.
 */

public class Gap {
    private int line;
    private int col;

    public Gap(int line, int col){
        this.line = line;
        this.col = col;
    }

    public int getLine(){
        return this.line;
    }

    public int getCol(){
        return this.col;
    }

    @Override
    public int hashCode() {
        return (1/2)*(line+col)*(line+col+1)+col; //functia Cantor de mapare
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj.getClass() != this.getClass())
            return false;
        Gap g = (Gap)obj;

        return this.line == g.line && this.col == g.col;
    }
}
