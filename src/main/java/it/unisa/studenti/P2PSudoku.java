package it.unisa.studenti;

import de.ad.sudoku.*;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;

import java.io.IOException;
import java.net.InetAddress;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.storage.Data;

public class P2PSudoku implements SudokuGame {

    private Grid globalSudoku;
    private Grid localSudoku;

    Generator generator = new Generator();
    Random random = new Random();

    final private PeerDHT peer;
    ArrayList<String> players = new ArrayList<>();


    public P2PSudoku(int peerId) throws Exception{
        peer = new PeerBuilderDHT(new PeerBuilder(Number160.createHash(peerId)).ports(4000 + peerId).start()).start();
        FutureBootstrap fb;
        fb = this.peer.peer().bootstrap().inetAddress(InetAddress.getByName("127.0.0.1")).ports(4001).start();
        fb.awaitUninterruptibly();
        if(fb.isSuccess())
            peer.peer().discover().peerAddress(fb.bootstrapTo().iterator().next()).start().awaitUninterruptibly();
    }

    public Grid generateNewSudoku(String gameName) throws Exception{
        int command = -1;
        Scanner scanner = new Scanner(System.in);
        Grid grid = generator.generate(0);

        do{
            System.out.println("Chose difficulty: \n1)Easy \n2)Medium \n3)Hard");
            command = scanner.nextInt();
        }while(command!=1 && command!=2 && command!=3);
        
        switch(command){
            case 1: 
                grid = generator.generate(43); //Easy
                break;
            case 2: 
                grid = generator.generate(51); //Medium
                break;
            case 3: 
                grid = generator.generate(56); //Hard
                break;
        }

        
        if(this.storeGlobalSudoku(gameName + "_empty", grid)){ //Store the initiated version
            this.storeGlobalSudoku(gameName, grid);
            this.localSudoku = grid;
            return this.localSudoku;
        } 
        else{
            return null;
        }
    }

    public boolean join(String gameName, String nickname) throws ClassNotFoundException, IOException{
        String key = gameName + "_players";
        
        Grid grid = this.getGlobalSudoku(gameName); 

        if(!grid.isEmptyGrid()) //Sudoku game found
            peer.put(Number160.createHash(key)).data(new Data(grid)).start().awaitUninterruptibly();

        return false;
    }



    private ArrayList<String> getGamePlayers(String gameName) throws ClassNotFoundException, IOException{
        ArrayList<String> players = new ArrayList<>();

        FutureGet futureGet = peer.get(Number160.createHash(gameName + "_players")).start();
        futureGet.awaitUninterruptibly();
        if(futureGet.isSuccess()){
            
            try{
                players = (ArrayList<String>) futureGet.dataMap().values().iterator().next().object();
            }catch(NoSuchElementException e){
                System.out.println("Players not found");
                System.exit(0);
            }
        }
        else{
            System.out.println("Players not found");
            System.exit(0);
        }

        return players;
    }

    public Grid getGlobalSudoku(String gameName) throws ClassNotFoundException, IOException{
        Grid grid = Grid.emptyGrid();

        FutureGet futureGet = peer.get(Number160.createHash(gameName)).start();
        futureGet.awaitUninterruptibly();
        if(futureGet.isSuccess()){
            
            try{
                grid = (Grid) futureGet.dataMap().values().iterator().next().object();
            }catch(NoSuchElementException e){
                System.out.println("Game not found");
                System.exit(0);
            }
        }
       
        return grid;
    }

   

    public Integer placeNumber(String gameName, int i, int j, int number){
        int point = 0;

        return point;
    }

    private boolean storeGlobalSudoku(String gameName, Grid grid) {
        try{
            FutureGet futureGet = peer.get(Number160.createHash(gameName)).start();
            futureGet.awaitUninterruptibly();
            if(futureGet.isSuccess() && futureGet.isEmpty()){
                peer.put(Number160.createHash(gameName)).data(new Data(grid)).start().awaitUninterruptibly();   
                return true; 
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void leaveNetwork(){
        
    }

    @Override
    public Grid getSudoku(String gameName) {
        return this.localSudoku;
    }


    public static void clearScreen() {  
        System.out.print("\033[H\033[2J");  
        System.out.flush();  
    }
}