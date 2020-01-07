package it.unisa.studenti.dao;

import java.io.IOException;
import java.util.Map;
import java.util.Random;

import de.ad.sudoku.Grid;
import de.ad.sudoku.Grid.Cell;
import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.FuturePut;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.Number640;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.storage.Data;
import net.tomp2p.utils.Pair;

public class SudokuDAO {
    private static Random random = new Random();

    public static boolean initGlobalSudoku(PeerDHT peer, String gameName, Grid grid) {
        try{
            FutureGet futureGet = peer.get(Number160.createHash(gameName)).start();
			futureGet.awaitUninterruptibly();
			if(futureGet.isSuccess() && !futureGet.isEmpty()){
				return false;
			}
            if(futureGet.isSuccess()) {
                peer.put(Number160.createHash(gameName)).data(new Data(grid)).start().awaitUninterruptibly();   
                return true; 
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Grid getGlobalSudoku(PeerDHT peer, String gameName){
        Grid grid = Grid.emptyGrid();
        try{
            FutureGet futureGet = peer.get(Number160.createHash(gameName)).getLatest().start();
            futureGet.awaitUninterruptibly();
            if(futureGet.isSuccess()){
                if(futureGet.isEmpty()) return null;
                //grid = (Grid) futureGet.dataMap().values().iterator().next().object();
                grid = (Grid) futureGet.data().object();
                return grid;
            }  
        }catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean storeGlobalSudoku(PeerDHT peerDHT, String gameName, int row, int col, int number) throws ClassNotFoundException, InterruptedException, IOException {
		Pair<Number640, Byte> pair2 = null;
		for (int i = 0; i < 5; i++) {
			Pair<Number160, Data> pair = getAndUpdateGlobalSudoku(peerDHT, gameName, row, col, number);
			if (pair == null) {
				return false;
			}
			FuturePut fp = peerDHT
					.put(Number160.createHash(gameName))
                    .data(pair.element1().prepareFlag(), pair.element0())
                    .start().awaitUninterruptibly();
            pair2 = checkVersions(fp.rawResult());
			// 1 is PutStatus.OK_PREPARED
			if (pair2 != null && pair2.element1() == 1) {
				break;
			}
			System.out.println("get delay or fork - put");
			// if not removed, a low ttl will eventually get rid of it
			peerDHT.remove(Number160.createHash(gameName)).versionKey(pair.element0()).start()
					.awaitUninterruptibly();
			Thread.sleep(random.nextInt(500));
		}
		if (pair2 != null && pair2.element1() == 1) {
			peerDHT.put(Number160.createHash(gameName))
					.versionKey(pair2.element0().versionKey()).putConfirm()
					.data(new Data()).start().awaitUninterruptibly();
			return true;
		} else {
			System.out
					.println("we cannot handle this kind of inconsistency automatically, handing over the the API dev");
		}
		return false;
    }

    private static Pair<Number160, Data> getAndUpdateGlobalSudoku(PeerDHT peerDHT, String gameName, int row, int col, int number) throws InterruptedException, ClassNotFoundException, IOException {
		Pair<Number640, Data> pair = null;
		for (int i = 0; i < 5; i++) {
            FutureGet fg = peerDHT.get(Number160.createHash(gameName)).getLatest().start().awaitUninterruptibly();
			
			if(fg.isSuccess() && fg.isEmpty()){
				return null;
			}
			// check if all the peers agree on the same latest version, if not
			// wait a little and try again
            pair = checkVersions(fg.rawData());
			if (pair != null) {
				break;
			}
			System.out.println("get delay or fork - get");
			Thread.sleep(random.nextInt(500));
		}
		// we got the latest data
		if (pair != null) {
            // update operation is append
            Grid grid = (Grid) pair.element1().object();
            Cell globalCell = grid.getCell(row, col);
            globalCell.setValue(number);
			Data newData = new Data(grid);
			Number160 v = pair.element0().versionKey();
            long version = v.timestamp() + 1;
			newData.addBasedOn(v);
            //since we create a new version, we can access old versions as well
			return new Pair<Number160, Data>(new Number160(version, newData.hash()), newData);
		}
		return null;
    }

    private static <K> Pair<Number640, K> checkVersions(Map<PeerAddress, Map<Number640, K>> rawData) {
		Number640 latestKey = null;
		K latestData = null;
		for (Map.Entry<PeerAddress, Map<Number640, K>> entry : rawData.entrySet()) {
			if (latestData == null && latestKey == null) {
				latestData = entry.getValue().values().iterator().next();
                latestKey = entry.getValue().keySet().iterator().next();
			} else {
				if (!latestKey.equals(entry.getValue().keySet().iterator()
						.next())
						|| !latestData.equals(entry.getValue().values()
								.iterator().next())) {
					return null;
				}
			}
		}
		return new Pair<Number640, K>(latestKey, latestData);
	}
}