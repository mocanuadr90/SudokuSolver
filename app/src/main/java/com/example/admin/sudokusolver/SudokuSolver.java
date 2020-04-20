package com.example.admin.sudokusolver;

import android.os.AsyncTask;
import android.util.Pair;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by admin on 7/7/2017.
 */

public class SudokuSolver {

    private int[][] board;                                 //matricea configuratiei jocului
    private int     filledGaps;                            //numarul de casute completate
    private int[][] boxLim;                                //matricea limitelor superioare/inferioara, stanga/dreapta a celor 9 patrate de 3x3
    private int[][] box;                                   //matrice ce retine daca in patratul i de 3x3 exista cifra j
    private int[]   boxFill;                               //vector ce retine numarul de cifre existente in patratul de 3x3 i
    private int[][] line;                                  //matrice ce retine daca pe linia i exista cifra j
    private int[]   lineFill;                              //vector ce retine numarul de cifre existenta pe linia i
    private int[][] col;                                   //matrice ce retine daca in coloana i exista cifra j
    private int[]   colFill;                               //vector ce retine numarul de cifre existente in coloana i
    private HashMap<Integer,ArrayList<Gap>> possible;      //structura de tip tabel de dispersie ce atribuie fiecarei cifre de la 1-9 o lista
                                                           //de elemente de tip Gap(casute libere) unde cifra poate fi inserata
    private static SudokuSolver instance;                  //unica instanta a clasei SudokuSolver
    private SudokuCanvas listener;

    //constructor clasa
    private SudokuSolver()
    {
        this.board = new int[10][10];
        this.filledGaps = 0;
        this.boxLim = new int[10][5];
        this.box = new int[10][10];
        this.boxFill = new int[10];
        this.line = new int[10][10];
        this.lineFill = new int[10];
        this.col = new int[10][10];
        this.colFill = new int[10];
        this.possible = new HashMap<Integer, ArrayList<Gap>>();
        for (int i = 1; i <= 9; i++)
        {
            possible.put(i,new ArrayList<Gap>());
        }
        createBoxLim();
    }

    //Unica instanta a clasei (Singleton)
    protected static SudokuSolver getInstance()
    {
        if(instance == null){
            instance = new SudokuSolver();
        }

        return instance;
    }

    protected void setListener(SudokuCanvas listener){
        this.listener = listener;
    }

    protected int[][] getBoard(){
        return board;
    }

    //delimiteaza cele 9 patrate de 3x3
    protected void createBoxLim()
    {
        int i;

        //stabileste limita inferioara si superioara
        //a primelor 3 patrate de sus(orizontala)
        for (i = 1; i <= 3; i++)
        {
            boxLim[i][1] = 1;
            boxLim[i][2] = 3;
        }

        //stabileste limita inferioara si superioara
        //a celor 3 patrate din mijloc(orizontala)
        for (i = 4; i <= 6; i++)
        {
            boxLim[i][1] = 4;
            boxLim[i][2] = 6;
        }

        //stabileste limita inferioara si superioara
        //a ultimelor 3 patrate de jos(orizontala)
        for (i = 7; i <= 9; i++)
        {
            boxLim[i][1] = 7;
            boxLim[i][2] = 9;
        }

        //stabileste limita din stanga si limita din dreapta
        //a primelor 3 patrate (pe verticala)
        for (i = 1; i <= 8; i += 3)
        {
            boxLim[i][3] = 1;
            boxLim[i][4] = 3;
        }

        //stabileste limita din stanga si limita din dreapta
        //a celor 3 patrate din mijloc (pe verticala)
        for (i = 2; i < 9; i += 3)
        {
            boxLim[i][3] = 4;
            boxLim[i][4] = 6;
        }

        //stabileste limita din stanga si limita din dreapta
        //a ultimelor 3 patrate (pe verticala)
        for (i = 3; i <= 9; i += 3)
        {
            boxLim[i][3] = 7;
            boxLim[i][4] = 9;
        }
    }

    protected void clearGame(){
        for(int i=1; i<10; i++){
            for(int j=1; j<10; j++){
                board[i][j]=0;
            }
        }

        this.filledGaps = 0;

        for(int i=1; i<10; i++){
            this.boxFill[i] = 0;
            this.lineFill[i] = 0;
            this.colFill[i] = 0;

            for(int j=1; j<10; j++){
                this.box[i][j] = 0;
                this.line[i][j] = 0;
                this.col[i][j] = 0;
            }
        }
        this.possible = new HashMap<Integer, ArrayList<Gap>>();
        for (int i = 1; i <= 9; i++)
        {
            possible.put(i,new ArrayList<Gap>());
        }
    }

    //in functie de coodonatele i si j
    //returneaza numarul patratului de 3x3
    //care contine casuta  de coord (i,j)
    protected int getBox(int i, int j)
    {
        int k;
        for (k = 1; k <= 9; k++)
        {
            //daca i este cuprins intre limita superioara
            //si limita inferioara a patratului k
            if (i >= boxLim[k][1] && i <= boxLim[k][2])
            {
                //daca j este cuprins intre limita din stanga
                //si limita din dreapta a patratului k
                if (j >= boxLim[k][3] && j <= boxLim[k][4])
                return k;
            }
        }
        return 0;
    }

    //insereaza o cifra pe pozitia (line,col)
    protected void insertDigit(int line, int col, int digit)
    {
        if (board[line][col] == 0) {
            filledGaps++;
        }
        this.board[line][col] = digit;
        int k = this.getBox(line, col);
        this.box[k][digit]++;//this.box[k][digit] = 1;
        this.boxFill[k]++;
        this.line[line][digit]++;//this.line[line][digit] = 1;
        this.lineFill[line]++;
        this.col[col][digit]++;//this.col[col][digit] = 1;
        this.colFill[col]++;
    }

    //verifica daca cifra digit poate fi inserata pe pozitia (i,j)
    //cifra digit nu se afla deja pe linia i sau coloana j
    //sau patratul de 3x3 k
    protected boolean valid(int digit, int i, int j)
    {
        int k;
        k = this.getBox(i, j);
        if (this.box[k][digit] == 1 || this.line[i][digit] == 1 || this.col[j][digit] == 1) {
            return false;
        }
        return true;
    }

    protected ArrayList<Pair<Integer, Integer>> validateConfiguration(){
        ArrayList<Pair<Integer, Integer>> invalids = new ArrayList<>();
        int i,j,k, digit;

        //0. check invalid lines
        for(i=1; i<10; i++){
            for(digit=1; digit<10; digit++){
                if(line[i][digit] > 1){
                    invalids.add(new Pair<Integer, Integer>(0,i));
                    break;
                }
            }
        }

        //1. check invalid columns
        for(j=1; j<10; j++){
            for(digit=1; digit<10; digit++){
                if(col[j][digit] > 1){
                    invalids.add(new Pair<Integer, Integer>(1,j));
                    break;
                }
            }
        }

        //2. check invalid boxes
        for(k=1; k<10; k++){
            for(digit=1; digit<10; digit++){
                if(box[k][digit] > 1){
                    invalids.add(new Pair<Integer, Integer>(2,k));
                    break;
                }
            }
        }

        return invalids;
    }

    //completeaza patratul de 3x3 x
    //ce are o singura cifra lipsa
    protected void completeBox(int x)
    {
        int digit, i, j;
        boolean stop = false;
        digit = 0;

        //depisteaza cifra lipsa
        for (i = 1; i <= 9; i++)
        {
            if (box[x][i] == 0)
            {
                digit = i;
                break;
            }
        }

        //depisteaza casuta libera
        for (i = boxLim[x][1]; i <= this.boxLim[x][2] && !stop; i++)
        {
            for (j = this.boxLim[x][3]; j <= this.boxLim[x][4] && !stop; j++)
            {
                if (this.board[i][j] == 0)
                {
                    this.board[i][j] = digit;
                    this.filledGaps++;
                    if (this.filledGaps == 81)
                        return;
                    this.box[x][digit] = 1;
                    this.boxFill[x]++;
                    this.line[i][digit]++;
                    this.lineFill[i]++;
                    this.col[j][digit] = 1;
                    this.colFill[j]++;

                    //daca in urma inserarii cifrei nr
                    //linia i mai are o singura casuta libera o completeaza
                    if (this.lineFill[i] == 8)
                        completeLine(i);

                    //daca in urma inserarii cifrei nr
                    //coloana j mai are o singura casuta libera o completeaza
                    if (this.colFill[j] == 8)
                        completeCol(j);
                    stop = true;
                }
            }
        }
    }

    //completeaza linia x
    //ce are o singura cifra lipsa
    protected void completeLine(int x)
    {
        int digit, i, j;
        digit = 0;
        boolean stop = false;

        //depisteaza cifra lipsa
        for (i = 1; i <= 9; i++)
        {
            if (this.line[x][i] == 0)
            {
                digit = i;
                break;
            }
        }

        //depisteaza casuta libera
        for (i = 1; i <= 9 && !stop; i++)
        {
            if (this.board[x][i] == 0)
            {
                this.board[x][i] = digit;
                this.filledGaps++;
                if (this.filledGaps == 81)
                    return;
                j = this.getBox(x, i);
                this.box[j][digit] = 1;
                this.boxFill[j]++;
                this.col[i][digit] = 1;
                this.colFill[i]++;
                if (this.boxFill[j] == 8)
                    completeBox(j);
                if (this.colFill[i] == 8)
                    completeCol(i);
                stop = false;
            }
        }
    }

    //completeaza coloana x
    //ce are o singura cifra lipsa
    protected void completeCol(int x)
    {
        int digit, i, j;
        digit = 0;
        boolean stop = false;

        //depisteaza cifra lipsa
        for (i = 1; i <= 9; i++)
        {
            if (this.col[x][i] == 0)
            {
                digit = i;
                break;
            }
        }

        //depisteaza casuta libera
        for (i = 1; i <= 9 && !stop; i++)
        {
            if (this.board[i][x] == 0)
            {
                this.board[i][x] = digit;
                this.filledGaps++;
                if (this.filledGaps == 81)
                    return;
                this.col[x][digit] = 1;
                this.colFill[x]++;
                j = this.getBox(i, x);
                this.box[j][digit] = 1;
                this.boxFill[j]++;
                this.line[i][digit] = 1;
                this.lineFill[i]++;
                if (this.boxFill[j] == 8)
                    completeBox(j);
                if (this.lineFill[i] == 8)
                    completeLine(i);
                stop = true;
            }
        }
    }

    //verifica daca listele de casute libere potentiale
    //sunt identice pentru cifrele a si b
    protected boolean isSimilar(int a, int b)
    {
        if (possible.get(a).size() == possible.get(b).size())
        {
            for (int i = 0; i < possible.get(a).size(); i++)
            {
                Gap q = possible.get(a).get(i);
                Gap r = possible.get(b).get(i);
                if (!q.equals(r))
                    return false;
            }
            return true;
        }
        else
            return false;
    }

    //pentru patratul de 3x3 x cauta sa insereze
    //acele cifre care se potrivesc intr-o unica casuta libera
    protected void checkBox(int x)
    {
        int i, j, digit, idx1, idx2;
        boolean repeat, elim, term, startOver, modified;
        Gap filled;
        int nrSim, poz;
        ArrayList<Integer> v;
        ArrayList<Gap> temp;
        HashSet<Gap> reserved = new HashSet<Gap>();

        elim = false;

        //cat timp exista o casuta libera
        //in care se potriveste o unica cifra
        do
        {
            repeat = false;

            //pentru fiecare cifra 1-9
            for (digit = 1; digit <= 9; digit++)
            {
                //daca nu exista in patratul de 3x3 curent
                if (this.box[x][digit] == 0)
                {
                    //daca nu s-a depistat casuta libera in care poate fi inserata cifra curenta in urma algoritmului secundar
                    if (elim == false)
                    {
                        //daca lista de casute libere potentiale pentru inserare
                        //este goala
                        if (possible.get(digit).size() == 0)
                        {
                            //cauta o casuta libera potentiala
                            //pentru inserarea cifrei digit
                            //si daca gaseste o adauga in lista
                            for (i = this.boxLim[x][1]; i <= this.boxLim[x][2]; i++)
                            {
                                for (j = this.boxLim[x][3]; j <= this.boxLim[x][4]; j++)
                                {
                                    Gap g = new Gap(i, j);

                                    //daca casuta este libera si nu este rezervata
                                    if (this.board[i][j] == 0 && !reserved.contains(g))
                                    {
                                        if (this.valid(digit, i, j))
                                            possible.get(digit).add(g);
                                    }
                                }
                            }
                        }
                    }

                    //daca exista o singura casuta libera potentiala
                    //cifra digit este inserata
                    if (possible.get(digit).size() == 1)
                    {
                        i = possible.get(digit).get(0).getLine();
                        j = possible.get(digit).get(0).getCol();
                        filled = new Gap(i,j);
                        insertDigit(i, j, digit);
                        if (this.filledGaps == 81)
                            return;
                        if (this.lineFill[i] == 8)
                            completeLine(i);
                        if (this.filledGaps == 81)
                            return;
                        if (this.colFill[j] == 8)
                            completeCol(j);
                        if (this.filledGaps == 81)
                            return;

                        //pentru fiecare cifra 1-9 diferita de cifra curenta (digit)
                        //verifica daca lista sa contine casuta anterior completata
                        //caz in care o sterge din lista
                        for (int d = 1; d <= 9; d++)
                        {
                            if (d != digit)
                            {
                                if (possible.get(d).contains(filled))
                                {
                                    possible.get(d).remove(filled);
                                    //daca lista ramasa in urma eliminarii
                                    //are un singur element se rezerva casuta libera
                                    //si se repeta algoritmul
                                    if (possible.get(d).size() == 1)
                                    {
                                        reserved.add(possible.get(d).get(0));
                                        repeat = true;
                                    }
                                }
                            }
                        }

                        //goleste lista asociata cifrei
                        possible.get(digit).clear();
                    }
                }
            }

            //daca nu mai exista nici o cifra
            //ce poate fi inserata pe o unica pozitie
            //si inca au ramas casute libere
            //se aplica al doilea algoritm de completare
            if (repeat == false && filledGaps < 81)
            {

                //daca sunt completate intre 3 si 6 casute
                //din cadrul patratului curent
                //verifica daca exista cifre ce pot fi inserate
                //in aceleasi casute libere
                if (this.boxFill[x] >= 3 && this.boxFill[x] < 7)
                {
                    poz = -1;
                    v = new ArrayList<>();

                    //depisteaza cifrele lipsa
                    for (digit = 1; digit <= 9; digit++)
                    {
                        if (box[x][digit] == 0)
                        {
                            v.add(digit);
                        }
                    }

                    i = 0;

                    //calculeaza numarul de cifre
                    //ce au listele de casute libere potentiale echivalente
                    while (i >= 0 && i < v.size())
                    {
                        nrSim = 1;
                        startOver = false;
                        modified = false;

                        for (j = i + 1; j < v.size(); j++)
                        {
                            if (this.isSimilar(v.get(i), v.get(j)))
                            {
                                nrSim++;
                                if (poz == -1)
                                {
                                    poz = i;
                                }
                            }
                        }

                        //adauga casutele libere potentiale din prima lista echivalenta
                        //intr-o lista temporara
                        if (poz >= 0)
                        {
                            temp = new ArrayList<>();

                            for(Gap g : possible.get(v.get(poz)))
                            {
                                temp.add(new Gap(g.getLine(), g.getCol()));
                            }

                            //daca numarul de cifre ce pot fi inserate
                            //in exact aceleasi casute libere
                            //este egal cu numarul de casute libere
                            if (nrSim == temp.size())
                            {
                                modified = true;

                                //pentru fiecare cifra din v
                                //elimina acele casute libere potentiale
                                //ce se gaseau in listele echivalente
                                for (idx1 = 0; idx1 < v.size(); idx1++)
                                {
                                    for (Gap g : temp)
                                    {
                                        possible.get(v.get(idx1)).remove(g);
                                    }

                                    //daca in urma eliminarii
                                    //lista cifrei curente contine
                                    //un singur element
                                    //algoritmul principal se va repeta
                                    //inserand astfel cifra curenta pe unica pozitie posibila
                                    if (this.possible.get(v.get(idx1)).size() == 1)
                                    {
                                        repeat = true;
                                        elim = true;
                                    }
                                }

                                //elimina cifrele pentru care
                                //lista de casute libere potentiale
                                //a devenit vida
                                for(Iterator<Integer> it = v.iterator(); it.hasNext(); ){
                                    int el = it.next();
                                    if(possible.get(el).size() == 0){
                                        it.remove();
                                    }
                                }

                                //daca in urma eliminarii
                                //raman mai mult de 2 cifre
                                //verifica pentru acestea similitudinea
                                //listelor de casute libere potentiale
                                if (v.size() > 2)
                                {
                                    term = false;
                                    for (idx1 = 0; idx1 < v.size() - 1 && term == false; idx1++)
                                    {
                                        for (idx2 = idx1 + 1; idx2 < v.size() && term == false; idx2++)
                                        {
                                            if (this.isSimilar(v.get(idx1), v.get(idx2)))
                                            {
                                                //in cazul in care gaseste doua liste similare
                                                //va relua algoritmul secundar si va retine pozitia primei cifre
                                                //avand o lista similara
                                                startOver = true;
                                                term = true;
                                                poz = idx1;
                                            }
                                            else
                                            {
                                                poz = 0;
                                            }
                                        }
                                    }
                                }
                            }
                            if (modified)
                            {
                                if (startOver)
                                    i = 0;
                                else
                                    i = -1;
                            }
                            else i++;
                        }
                        else
                            i++;
                    }
                }
            }
        }
        while (repeat);

        //pentru fiecare cifra elibereaza
        //lista de casute libere potentiale
        //daca aceasta nu este vida
        for (digit = 1; digit <= 9; digit++)
        {
            if (possible.get(digit).size() > 0)
                possible.get(digit).clear();
        }
    }

    //verifica linia i
    //pentru a gasi cifre lipsa
    //ce pot fi inserate pe o unica pozitie
    protected void checkLine(int i)
    {
        int j, x, digit, idx1, idx2;
        boolean repeat, elim, term, startOver, modified;
        Gap filled;
        int nrSim, poz;
        ArrayList<Integer> v;
        ArrayList<Gap> temp;
        HashSet<Gap> reserved = new HashSet<Gap>();

        elim = false;

        do
        {
            repeat = false;
            for (digit = 1; digit <= 9; digit++)
            {
                if (this.line[i][digit] == 0)
                {
                    if (elim == false)
                    {
                        if (possible.get(digit).size() == 0)
                        {
                            for (j = 1; j <= 9; j++)
                            {
                                Gap g = new Gap(i, j);

                                if (this.board[i][j] == 0 && !reserved.contains(g))
                                {
                                    if (this.valid(digit, i, j))
                                        possible.get(digit).add(g);
                                }
                            }
                        }
                    }

                    if (possible.get(digit).size() == 1)
                    {
                        j = possible.get(digit).get(0).getCol();
                        filled = new Gap(i,j);
                        insertDigit(i, j, digit);
                        if (this.filledGaps == 81)
                            return;
                        x = getBox(i, j);
                        if (this.boxFill[x] == 8)
                            this.completeBox(x);
                        if (this.filledGaps == 81)
                            return;
                        if (this.colFill[j] == 8)
                            this.completeCol(j);
                        if (this.filledGaps == 81)
                            return;

                        for (int d = 1; d <= 9; d++)
                        {
                            if (d != digit)
                            {
                                if (possible.get(d).contains(filled))
                                {
                                    possible.get(d).remove(filled);

                                    if (possible.get(d).size() == 1)
                                    {
                                        reserved.add(possible.get(d).get(0));
                                        repeat = true;
                                    }
                                }
                            }
                        }
                        possible.get(digit).clear();
                    }
                }
            }

            if (repeat == false && filledGaps < 81)
            {
                if (this.lineFill[i] >= 3 && this.lineFill[i] < 7)
                {
                    poz = -1;
                    v = new ArrayList<>();

                    for (digit = 1; digit <= 9; digit++)
                    {
                        if (line[i][digit] == 0)
                        {
                            v.add(digit);
                        }
                    }

                    x = 0;

                    while (x >= 0 && x < v.size())
                    {
                        nrSim = 1;
                        startOver = false;
                        modified = false;

                        for (j = x + 1; j < v.size(); j++)
                        {
                            if (this.isSimilar(v.get(x), v.get(j)))
                            {
                                nrSim++;
                                if (poz == -1)
                                {
                                    poz = x;
                                }
                            }
                        }

                        if (poz >= 0)
                        {
                            temp = new ArrayList<>();

                            for(Gap g : possible.get(v.get(poz)))
                            {
                                temp.add(new Gap(g.getLine(), g.getCol()));
                            }

                            if (nrSim == temp.size())
                            {
                                modified = true;

                                for (idx1 = 0; idx1 < v.size(); idx1++)
                                {
                                    for (Gap g : temp)
                                    {
                                        possible.get(v.get(idx1)).remove(g);
                                    }

                                    if (possible.get(v.get(idx1)).size() == 1)
                                    {
                                        repeat = true;
                                        elim = true;
                                    }
                                }

                                for(Iterator<Integer> it = v.iterator(); it.hasNext(); ){
                                    int el = it.next();
                                    if(possible.get(el).size() == 0){
                                        it.remove();
                                    }
                                }

                                if (v.size() > 2)
                                {
                                    term = false;
                                    for (idx1 = 0; idx1 < v.size() - 1 && term == false; idx1++)
                                    {
                                        for (idx2 = idx1 + 1; idx2 < v.size() && term == false; idx2++)
                                        {
                                            if (this.isSimilar(v.get(idx1), v.get(idx2)))
                                            {
                                                startOver = true;
                                                term = true;
                                                poz = idx1;
                                            }
                                            else
                                            {
                                                poz = 0;
                                            }
                                        }
                                    }
                                }
                            }
                            if (modified)
                            {
                                if (startOver)
                                    x = 0;
                                else
                                    x = -1;
                            }
                            else x++;
                        }
                        else
                            x++;
                    }
                }
            }
        }
        while (repeat);

        for (digit = 1; digit <= 9; digit++)
        {
            if (possible.get(digit).size() > 0)
                possible.get(digit).clear();
        }
    }

    //verifica coloana j
    //pentru a gasi cifre lipsa
    //ce pot fi inserate pe o unica pozitie
    protected void checkCol(int j)
    {
        int i, x, digit, idx1, idx2;
        boolean repeat, elim, term, startOver, modified;
        Gap filled;
        int nrSim, poz;
        ArrayList<Integer> v = new ArrayList<Integer>();
        ArrayList<Gap> temp;
        HashSet<Gap> reserved = new HashSet<Gap>();

        elim = false;

        do
        {
            repeat = false;
            for (digit = 1; digit <= 9; digit++)
            {
                if (this.col[j][digit] == 0)
                {
                    if (elim == false)
                    {
                        if (possible.get(digit).size() == 0)
                        {
                            for (i = 1; i <= 9; i++)
                            {
                                Gap g = new Gap(i, j);

                                if (this.board[i][j] == 0 && !reserved.contains(g))
                                {
                                    if (this.valid(digit, i, j))
                                        possible.get(digit).add(g);
                                }
                            }
                        }
                    }

                    if (possible.get(digit).size() == 1)
                    {
                        i = possible.get(digit).get(0).getLine();
                        filled = new Gap(i, j);
                        insertDigit(i, j, digit);
                        if (this.filledGaps == 81)
                            return;
                        x = getBox(i, j);
                        if (this.boxFill[x] == 8)
                            this.completeBox(x);
                        if (this.filledGaps == 81)
                            return;
                        if (this.lineFill[i] == 8)
                            this.completeLine(i);
                        if (this.filledGaps == 81)
                            return;

                        for (int d = 1; d <= 9; d++)
                        {
                            if (d != digit)
                            {
                                if (possible.get(d).contains(filled))
                                {
                                    possible.get(d).remove(filled);

                                    if (possible.get(d).size() == 1)
                                    {
                                        reserved.add(possible.get(d).get(0));
                                        repeat = true;
                                    }
                                }
                            }
                        }
                        possible.get(digit).clear();
                    }
                }
            }

            if (repeat == false && filledGaps < 81)
            {
                if (this.colFill[j] >= 3 && this.colFill[j] < 7)
                {
                    poz = -1;
                    v = new ArrayList<>();

                    for (digit = 1; digit <= 9; digit++)
                    {
                        if (possible.get(digit).size() > 0)
                        {
                            v.add(digit);
                        }
                    }

                    i = 0;

                    while (i >= 0 && i < v.size())
                    {
                        nrSim = 1;
                        startOver = false;
                        modified = false;

                        for (j = i + 1; j < v.size(); j++)
                        {
                            if (this.isSimilar(v.get(i), v.get(j)))
                            {
                                nrSim++;
                                if (poz == -1)
                                {
                                    poz = i;
                                }
                            }
                        }

                        if (poz >= 0)
                        {
                            temp = new ArrayList<Gap>();

                            for (Gap g : possible.get(v.get(poz)))
                            {
                                temp.add(new Gap(g.getLine(), g.getCol()));
                            }

                            if (nrSim == temp.size())
                            {
                                modified = true;

                                for (idx1 = 0; idx1 < v.size(); idx1++)
                                {
                                    for (Gap g : temp)
                                    {
                                        possible.get(v.get(idx1)).remove(g);
                                    }

                                    if (possible.get(v.get(idx1)).size() == 1)
                                    {
                                        repeat = true;
                                        elim = true;
                                    }
                                }

                                for(Iterator<Integer> it = v.iterator(); it.hasNext(); ){
                                    int el = it.next();
                                    if(possible.get(el).size() == 0){
                                        it.remove();
                                    }
                                }

                                if (v.size() > 2)
                                {
                                    term = false;
                                    for (idx1 = 0; idx1 < v.size() - 1 && term == false; idx1++)
                                    {
                                        for (idx2 = idx1 + 1; idx2 < v.size() && term == false; idx2++)
                                        {
                                            if (this.isSimilar(v.get(idx1), v.get(idx2)))
                                            {
                                                startOver = true;
                                                term = true;
                                                poz = idx1;
                                            }
                                            else
                                            {
                                                poz = 0;
                                            }
                                        }
                                    }
                                }
                            }
                            if (modified)
                            {
                                if (startOver)
                                    i = 0;
                                else
                                    i = -1;
                            }
                            else i++;
                        }
                        else
                            i++;
                    }
                }
            }
        }
        while (repeat);

        for (digit = 1; digit <= 9; digit++)
        {
            if (possible.get(digit).size() > 0)
                possible.get(digit).clear();
        }
    }

    protected boolean solve()
    {
        int i;

        long start = System.currentTimeMillis();

        while (true)
        {
            for (i = 1; i <= 9; i++)
            {
                this.checkBox(i);
                if (this.filledGaps == 81)
                    return true;
            }
            if (this.filledGaps == 81)
                return true;
            for (i = 1; i <= 9; i++)
            {
                this.checkLine(i);
                if (this.filledGaps == 81)
                    return true;
            }
            if (this.filledGaps == 81)
                return true;
            for (i = 1; i <= 9; i++)
            {
                this.checkCol(i);
                if (this.filledGaps == 81)
                    return true;
            }
            if (this.filledGaps == 81)
                return true;

            long current = System.currentTimeMillis();

            if (current - start > 4000)
                return this.filledGaps == 81;
        }
    }

    public TaskRunner createNewTask(){
        return new TaskRunner();
    }

    protected class TaskRunner extends AsyncTask<Void, Void, Boolean>{
        @Override
        public Boolean doInBackground(Void... params){
            return SudokuSolver.this.solve();
        }

        @Override
        public void onPostExecute(Boolean result){
            SudokuSolver.this.listener.showResult(result);
        }
    }
}
