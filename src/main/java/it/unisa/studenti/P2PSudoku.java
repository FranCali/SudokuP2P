package it.unisa.studenti;

import de.ad.sudoku.*;
import de.ad.sudoku.Grid.Cell;

import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.storage.Data;

public class P2PSudoku implements SudokuGame {

    Generator generator = new Generator();
    Random random = new Random();

    final private PeerDHT peer;


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
        scanner.close();
        
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

        this.storeSudoku(gameName, grid);

        return grid;
    }

    public boolean join(String gameName, String nickname){
        
        return false;
    }

    public Grid getSudoku(String gameName) throws ClassNotFoundException, IOException{
        FutureGet futureGet = peer.get(Number160.createHash(gameName)).start();
        futureGet.awaitUninterruptibly();
        if(futureGet.isSuccess()){
            Grid grid = Grid.emptyGrid();
            
            try{
                grid = (Grid) futureGet.dataMap().values().iterator().next().object();
                return grid;
            }catch(NoSuchElementException e){
                System.out.println("Game not found");
            }
        }
        else{
            System.out.println("Game not found");
        }
        return Grid.emptyGrid();
    }

    public Integer placeNumber(String gameName, int i, int j, int number){
        int point = 0;

        return point;
    }


    private void storeSudoku(String gameName, Grid grid) throws IOException{
        peer.put(Number160.createHash(gameName)).data(new Data(grid)).start().awaitUninterruptibly();
    }

}